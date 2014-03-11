/**
 *        The Institute for Advanced Technology in the Humanities
 *        
 *        Copyright 2013 University of Virginia. Licensed under the Educational Community License, Version 2.0 (the
 *        "License"); you may not use this file except in compliance with the License. You may obtain a copy of the
 *        License at
 *        
 *        http://opensource.org/licenses/ECL-2.0
 *        http://www.osedu.org/licenses/ECL-2.0
 *        
 *        Unless required by applicable law or agreed to in writing, software distributed under the License is
 *        distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See
 *        the License for the specific language governing permissions and limitations under the License.
 *
 *
 */
package edu.virginia.iath.snac.helpers;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringReader;
import java.net.Socket;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.commons.lang3.StringEscapeUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.w3c.dom.Document;

/**
 * GeoNamesHelper Class, used for querying cheshire for Geonames results.  Also utilizes
 * Google AutoCorrect to fix spelling errors and get more information search strings
 * before searching in cheshire.
 * 
 * @author Robbie Hott
 */
public class GeoNamesHelper {
	private Socket cheshire;
	private PrintWriter out;
	private BufferedReader in;
	private ArrayList<String> results;
	private HashSet<String> uniqueResults;
	private ArrayList<String> overkill;
	private ArrayList<String> betterResults;
	private int numResults = 0;

	// Maps of relevant places (countries and US states)
	private Map<String, String> countries = null;
	private Map<String, String> states = null;
	
	
	private String type;
	
	// Document parsers for the XML parsing of the results
	private Document resultDoc;
	

	/**
	 * Default Constructor: Initializes all lists and pre-fills country and state lookup maps.
	 */
	public GeoNamesHelper() {
		countries = getCountries();
		states = getStates();
		results = new ArrayList<String>();
		uniqueResults = new HashSet<String>();
		overkill = new ArrayList<String>();
		betterResults = new ArrayList<String>();
	}

	/**
	 * Connects to cheshire via a Socket on localhost, port 12345.
	 * 
	 * @return True if connection was successful, false otherwise.
	 */
	public boolean connect() {
		try {
			System.err.println("Starting cheshire search");
			cheshire = new Socket("localhost", 12345);
			out =
					new PrintWriter(cheshire.getOutputStream(), true);
			in =
					new BufferedReader(
							new InputStreamReader(cheshire.getInputStream()));

			// Init cheshire
			out.println("init");
			System.err.println(in.readLine());
			return true;
		} catch (Exception e) {
			return false;
		}

	}

	/**
	 * Disconnects the Socket connection to cheshire.
	 * 
	 * @return True.
	 */
	public boolean disconnect() {

		try {
			// close cheshire
			out.println("close");
			out.close();
			cheshire.close();
		} catch (Exception e) {
			// do nothing
		}
		return true;
	}

	/**
	 * Gets the Geonames XML top result from cheshire.
	 * 
	 * @return XML String for result at position 1.
	 */
	public String getCheshireResultString() {
		String result = "";
		try {
			// Ask for the top entry
			out.println("display default 1 1");
			in.skip(17);
			while (in.ready()) {
				result += in.readLine();
			}

			// cleanup the result
			result = result.substring(0, result.length()-1);
			return result;
		} catch (Exception e) {}

		return null;
	}

	/**
	 * Gets the Geonames XML result from cheshire at the given offset <code>start</code>. 
	 * Indexing starts at 1.
	 * 
	 * @param start Index within the cheshire results. 
	 * @return XML String for result at position <code>start</code>
	 */
	public String getCheshireResultString(int start) {
		String result = "";
		try {
			// Ask for the top entry
			out.println("display default "+start+" 1");
			in.skip(17);
			while (in.ready()) {
				result += in.readLine();
			}

			// cleanup the result
			result = result.substring(0, result.length()-1);
			return result;
		} catch (Exception e) {}

		return null;
	}

	/**
	 * Parses the cheshire result string for the number of matches found.
	 * 
	 * @param cheshireResult Cheshire result string.
	 * @return Number of results found in cheshire.
	 */
	private int getResultCount(String cheshireResult) {
		if (cheshireResult != null) {
			String[] info = cheshireResult.split("Default ");
			if (info != null && info[1] != null) 
				return Integer.parseInt(info[1]);
		}
		return 0;
	}

	/**
	 * Parses the cheshire result string ("Default...") for the number of results
	 * found, then gets the cheshire Geonames XML string for the top result of this
	 * query and addes it to each list.  Then, it gets the rest of the XML results from
	 * cheshire and adds them to the overkill list. 
	 * 
	 * @param cheshireResult Cheshire result string.
	 * @return True if at least one result was added, false otherwise.
	 */
	private boolean addResult(String cheshireResult) {

		// Debug info:
		// System.err.println(cheshireResult);

		int count = getResultCount(cheshireResult);
		if (count > 0) {
			// add the total to the number of results
			numResults += count;

			// add the top result of this group to the results arraylist
			String result = getCheshireResultString();
			if (result != null) {
				results.add(result);
				uniqueResults.add(result);
				overkill.add(result);
			}

			// OVERKILL
			// for 2 on up, get the cheshire results and add them to overkill
			for(int i = 2; i < count; i++) {
				result = getCheshireResultString(i);
				if (result != null) overkill.add(result);
			}

			return true;
		}
		return false;
	}

	/**
	 * Looks up the query string in a map of countries and ISO abbreviations.  If the query matches either
	 * an abbreviation or a country, cheshire is queried exactly for tht country
	 * 
	 * @param query String to match for a country.
	 * @return True if a country was found, false otherwise.
	 */
	protected boolean searchForCountry(String query) {
		String cheshireResult = null;
		try
		{
			if (countries.containsKey(query)) { // we have a country!
				// Do a simple country look up

				// Replacing double quote (") with single quote (') in the search string 1/8/14, since some
				// country codes equal the cheshire commands.  (For example, GE for Georgia is the same as
				// greater or equal to.)  Single quotes appear to parse the value as literal, whereas double
				// quotes still treat it as the cheshire command (no escaping).
				// 2/3/14 Replaced the @ with a direct serach on country and an exact (no truncating) match
				//   on the country name.  This fixes US, but hopefully doesn't break any others.
				out.println("find xcountry '" + countries.get(query) + "' and xintlname[5=100] '" + query +"'");
				System.err.println("Searched for country code: " + countries.get(query) + " and country: " + query);
				cheshireResult = in.readLine();
				System.err.println(cheshireResult);
				addResult(cheshireResult);
				return true;
			} else if (countries.values().contains(query)) { // we have an iso abbreviation
				String countryName = "";
				for (String key : countries.keySet()) {
					if (countries.get(key).equals(query))
						countryName = key;
				}
				out.println("find xcountry '" + query + "' and xintlname[5=100] '" + countryName +"'");
				System.err.println("Searched for country code: " + query + " and country: " + countryName);
				cheshireResult = in.readLine();
				System.err.println(cheshireResult);
				addResult(cheshireResult);
				return true;
				
			}
		} catch (Exception e) {
			return false;
		}
		return false;
	}


	/**
	 * Looks up the query string in a map of states and state abbreviations.  If the query matches either
	 * an abbreviation or state name, cheshire is queried exactly for that state.
	 * 
	 * @param query String to match for a state.
	 * @return True if a state was found, false otherwise.
	 */
	protected boolean searchForState(String query) {
		String cheshireResult = null;
		try
		{
			if (states.containsKey(query)) { // we have a US state!
				// Do a simple state lookup
				out.println("find exactname[5=100] '"+ query +"' and admin1 '"+ states.get(query) +"' and feature_type 'adm1'");
				System.err.println("Searched for state code: " + states.get(query) + " and state name: " + query);
				cheshireResult = in.readLine();
				System.err.println(cheshireResult);
			} else if (states.values().contains(query)) { // we have a US state!
				// Do a reverse state lookup
				String stateName = "";
				for(String key : states.keySet()) {
					if (states.get(key).equals(query))
						stateName = key;
				}
				out.println("find exactname[5=100] '"+ stateName +"' and admin1 '"+ query +"' and feature_type 'adm1'");
				System.err.println("Searched for state code: " + query + " and state name: " + stateName);
				cheshireResult = in.readLine();
				System.err.println(cheshireResult);
			}

			if (cheshireResult != null) {
				addResult(cheshireResult);
				return true;
			}
		} catch (Exception e) {
			return false;
		}
		return false;
	}

	
	/**
	 * Search for a given query string.  This method is what should be called from another class to
	 * handle the entire query process.  It performs queries in a few steps:
	 * <ol>
	 * <li> Look up the query in a list of countries for a match (country name)
	 * <li> Look up the query in a list of US/Canada states for a match (state name or abbreviation)
	 * <li> Query Google Map's AutoComplete API for a normalized version of the string (fixes spelling errors)
	 * <li> Perform exact queries on the Google normalized string
	 * <li> Perform the undesired queries (n-grams, etc) on the Google normalized string and original query
	 * </ol>
	 * It then returns true or false if at least one result was found.
	 * 
	 * @param query The query string on which to search.
	 * @return True if there is at least one result, false otherwise.
	 */
	public boolean queryCheshire( String query) {


		String country = null;


		/*******************
		 * Start by searching for countries, then for states
		 */
		searchForCountry(query);
		searchForState(query);


		/*******************
		 * If no country or state, then do more in-depth queries
		 */


		String first = query;
		String second = query;
		// Split the string into two parts. They may be identical if there is no comma
		if (query.contains(",")) {
			first = query.substring(0, query.indexOf(",")).trim();
			second = query.substring(query.indexOf(",")+1, query.length());
			second = second.trim();
			// if second are initials with a space in between, then remove the space
			if (second.length() <= 3) {
				second = second.replace(" ", "");
			}
		}

		/*******
		 * The code below searches for exact matches to the query string given.
		 */
		/*
		  System.err.println("Searching for: " + query + " as 1." + first + "; 2." + second);
		  // City first
		  exactQueries(first, second, null, "pplc");
		  // Then populated place
		  exactQueries(first, second, null, "ppl");
		  // Then admin 1
		  exactQueries(first, second, null, "adm1");
		  // Then admin 2
		  exactQueries(first, second, null, "adm2");
		  // Then all others
		  exactQueries(first, second, null, null);
		*/
	

		/**
		 * The code below searches Google for an autocorrected value and type.
		 */
		
		// Query Google for an auto correct and type of place, then query Cheshire for the geonames matches
		if (this.numResults == 0) {
			String google = cleanString(this.getGoogleAutoCorrectValue(query), true);
			System.err.println("Google string: " + google + "; with type: " + type);
			String[] terms = google.split(",");
			System.err.println(Arrays.toString(terms));
			System.err.println(terms.length);
			if (terms.length >= 3) {
				country =  terms[terms.length - 1].trim().toLowerCase();
				second = terms[terms.length - 2].trim().toLowerCase();
				first = "";
				// changing to only use the first value in the CSVs to determine first.  this will likely
				// be better for geonames and cheshire's search method. (2/27/2014)
				first = terms[0].trim().toLowerCase();
			} else {
				first = terms[0].trim().toLowerCase();
				if (terms.length > 1) {
					second = terms[1].trim().toLowerCase();
				} else {
					second = first;
				}
			}
			System.err.println("Searching for: " + query + " as 1." + first + "; 2." + second + "; country. " + countries.get(country));
			
			// If we have a ppl type (populated place), search for the city first, then use the generic ppl list
			if (type != null && type.equals("ppl"))
				exactQueries(first, second, country, "pplc");
			
			exactQueries(first, second, country, type);
			

		}
		

		// try the last-ditch effort
		if (numResults == 0) {
			System.err.println("Last-ditch searching for: " + query);
			undesiredQueries(first, second, country, query);
			betterResults = getSimilarLengthResults(first);
		}

		// Return whether a result was found
		if (results.size() > 0)
			return true; 
		return false;
	}

	/**
	 * Performs exact queries in Cheshire based on the query string's parts (first, second, country).  These results are usually good and ordered,
	 * such that the top result is usually the best.
	 * <p>
	 * Searches are performed in the following order:
	 * <ol>
	 * <li> Exact match (no truncation) on <code>first</code> as the place name and <code>second</code> as the admin1 code
	 * <li> Exact match (no truncation) on <code>first</code> as the international place name and <code>second</code> as the admin1 code
	 * <li> If <code>second</code> contains a US state (in some form): Exact match (no truncation) on <code>first</code> as the place name 
	 * 		and the state abbreviation generated from <code>second</code> as the admin1 code
	 * <li> If <code>second</code> contains a country name (in some form): Exact match (no truncation) on <code>first</code> as the place name 
	 * 		and the country ISO abbreviation generated from <code>second</code> as the country code
	 * <li> If there is a <code>country</code> given: Exact match (no truncation) on <code>first</code> as the place name 
	 * 		and the country ISO abbreviation generated from <code>country</code> as the country code
	 * </ol>
	 * 
	 * @param first First part of the search string (before the comma), usually a place name
	 * @param second Second part of the search string (after the comma), usually a state
	 * @param country Country string, in ISO format
	 * @param type Feature type string
	 * @return Top Geonames XML result as a String, if found, or null otherwise.
	 */
	public String exactQueries(String first, String second, String country, String type) {
		String cheshireResult = null;
		String countryQuery = "";
		String typeQuery = "";
		// Set up the country, if it exists
		if (country != null && countries.containsKey(country)) {
			countryQuery = " and xcountry '" + countries.get(country) + "'"; 
		}
		
		if (type != null) {
			if (!type.equals("ppl"))
				typeQuery = " and feature_type '" + type + "'";
			else
				typeQuery = " and (feature_type 'ppl' or feature_type 'ppla' or feature_type 'ppla2' or feature_type 'ppla3')";
		}

		try
		{
			// First try an EXACT query for first as the name and second as the admin1 (state-level)
			//
			// removing the @ on exactname 1/6/14 because it's causing some errors in the search
			// removing the @ on admin1 1/6/14 because it's reordering the results away from what we want
			//  ex: searching exactname west point and admin1 @ ny gives west point, the cape, while
			//      searching exactname west point and admin1 ny gives the city of west point
			// replacing the " with ' in the query string to escape the search terms-- 1/8/16.  Apparently
			//   " doesn't actually escape if there are Cheshire commands in the search term
			// adding [5=100] on exactname 1/15/14 to do a true exact match (without only does a
			//   startsWith match in cheshire
			out.println("find exactname[5=100] '" + first + "' and admin1 '" + second + "'" + countryQuery + typeQuery);
			cheshireResult = in.readLine();
			addResult(cheshireResult);

			// Next try an EXACT query for first as an international name and second as the admin1 (state-level)
			// 
			// Check for an international name matching, which may be a little better
			out.println("find xintlname[5=100] '" + first + "' and admin1 '" + second + "'" + countryQuery + typeQuery);
			cheshireResult = in.readLine();
			addResult(cheshireResult);

			// Next, if first and second are not identical.  This leads to other possible queries
			if (!first.equals(second)) {

				// Check to see if second is a US state and if so, grab the two letter abbreviation 
				// and do an EXACT query for first as name and state abbreviation as admin1 (state-level)
				//
				// If we have something that may be a "city,state", let's look that up now just in case
				String stateSN = checkForUSState(first, second);
				if (stateSN != null) {
					// Do the query
					out.println("find exactname[5=100] '" + first + "' and admin1 '" + stateSN + "'" + countryQuery + typeQuery);
					cheshireResult = in.readLine();
					addResult(cheshireResult);
				} 


				// Check to see if second is a Country and if so, grab the two letter abbreviation
				// and do an EXACT query for first as international name and country code as country
				//
				// TODO 1/8/14 Here we should look up locality, country.  Since we didn't find a US
				// city, st, we should make sure we catch something like zurich, switzerland.  So,
				// the tokens after the comma should be checked for country code
				if (countries.containsKey(second)) {
					// the second set of tokens is a country! Get the country code and search

					// NOTE: we're going to search for international names, since we may have non-ascii characters such as
					// umlauts.
					out.println("find xcountry '" + countries.get(second) + "' and xintlname[5=100] @ '" + first +"'" + typeQuery);
					System.err.println("Searched for country code: " + countries.get(second) + " and placename: " + first + typeQuery);
					cheshireResult = in.readLine();
					addResult(cheshireResult);
				}
				

				if (country != null && countries.containsKey(country)) {
					// redo the last search but with country instead of second
					out.println("find xcountry '" + countries.get(country) + "' and xintlname[5=100] '" + first +"'" + typeQuery);
					System.err.println("Searched for country code: " + countries.get(country) + " and placename: " + first + typeQuery);
					cheshireResult = in.readLine();
					addResult(cheshireResult);
				}
			}

			if (results.size() > 0)
				return results.get(0);

			return null;	
		} catch (Exception e) {
			return null;
		}
	}

	/**
	 * Performs the undesired Cheshire queries.  These are usually guaranteed to be unreliable, as they return many results
	 * and Cheshire does not do a great job in ordering the results.  The best match for the query will usually NOT be the
	 * first one returned.  These should only be used as a very last result!
	 * <p>
	 * The searches are performed in the following order:
	 * <ol>
	 * <li> Ranked keyword results by name (of <code>first</code>) and admin1 codes (of <code>second</code>), with a direct search in country
	 * <li> N-grams results by name and admin codes (of entire query string), ranked by exact matches to the exact name (in <code>first</code>)
	 * <li> N-grams results by name and admin codes (of entire query string), ranked by keyword name and admin codes match (in the entire query string)
	 * <li> N-grams results by name and admin codes (of entire query string), ranked by keyword name (only) match (in <code>first</code>)
	 * <li> N-grams results by name and admin codes (of entire query string), ranked by the number of matching n-grams (NO ORDER ON BEST MATCHING)
	 * </ol>
	 * 
	 * @param first First part of the query string, usually the place name
	 * @param second Second part of the query string, usually the state
	 * @param country Country string, if one exists
	 * @param query Full query string to use in last ditch n-grams search
	 * @return Top Cheshire XML result as String, if one exists, else returns null
	 */
	public String undesiredQueries(String first, String second, String country, String query) {
		String cheshireResult = null;
		String countryQuery = "";
		// Set up the country, if it exists
		/*
		if (country != null && countries.containsKey(country)) {
			countryQuery = " and xcountry '" + countries.get(country) + "'"; 
		}
		*/


		try
		{
			//**********************************************************************************************************
			// Undesired Searching Mechanisms
			// TODO: These should only be used if the above fail, and each one should taken individually.  If one
			// 		 succeeds, the rest should NOT be done
			//
			// TODO: Also, these methods should have their confidence penalized heavily!  They are NOT guaranteed to return
			//		 what we want at ALL.

			// Next, try a ranking name query by keyword
			if (numResults == 0) {
				out.println("find name @ '" + first + "' and admin1 @ '" + second + "'" + countryQuery);
				cheshireResult = in.readLine();
				addResult(cheshireResult);
			}

			// 1. find ngram_name_wadmin and exactname (find the exact name but with ngrams for the admin code)
			// 2. find ngram_name_wadmin and admin1 search (no alternate names)

			// Print out a message
			if (numResults == 0) {
				System.err.println("Switching to ngrams search.");
			}

			// (1 above) Next, try a query on just ngrams in the name/admin code plus ranking of exact name (for bad state names)
			if (numResults == 0) {
				out.println("find ngram_name_wadmin '" + query + "' and exactname @ '" + first + "'" + countryQuery);
				cheshireResult = in.readLine();
				addResult(cheshireResult);
			}

			// Next, try a looking for matching ngrams
			if (numResults == 0) {
				out.println("find ngram_wadmin '" + query + "' and name_wadmin @ '" + query + "'" + countryQuery);
				cheshireResult = in.readLine();
				addResult(cheshireResult);
			}

			// Next, try looking for just ngrams and keyword name
			if (numResults == 0) {
				out.println("find ngram_wadmin '" + query + "' and name @ '" + first + "'" + countryQuery);
				cheshireResult = in.readLine();
				addResult(cheshireResult);
			}

			// Finally, just check ngrams
			if (numResults == 0) {
				System.err.println("Last ditch search");
				out.println("find ngram_wadmin '" + query + "'" + countryQuery);
				cheshireResult = in.readLine();
				addResult(cheshireResult);
			}


			//**********************************************************************************************************

			if (results.size() > 0)
				return results.get(0);

			return null;	
		} catch (Exception e) {
			return null;
		}
	}

	// IN NO PARTICULAR ORDER
	/**
	 * Gets all unique top Cheshire results (in Geonmaes XML format), in no particular order.
	 * 
	 * @return String of unique concatenated XML results from Cheshire.
	 */
	public String getAllUniqueResults() {
		String result = "";
		for (String res : uniqueResults) {
			result += res;
		}
		return result;
	}

	// IN ORDER, but may have duplicates
	/**
	 * Gets all top Cheshire results (in Geonames XML format), in order based on when they were found.  
	 * Matches for more exact queries will be first, with the top match first.
	 * 
	 * @return String of concatenated XML results from Cheshire.
	 */
	public String getAllOrderedResults() {
		String result = "";
		for (String res : results) {
			result += res;
		}
		return result;
	}

	// OVERKILL
	/**
	 * Gets all Cheshire results (in Geonames XML format) that were returned.  This method is OVERKILL.
	 * 
	 * @return String of concatenated XML results from Cheshire.
	 */
	public String getAllResultsCheshireEverReturned() {
		String result = "";
		for (String res: overkill) {
			result += res;
		}
		return result;
	}
	
	/**
	 * Gets all Cheshire results (in Geonames XML format) that were returned.  This method is OVERKILL.
	 * 
	 * @return String of concatenated XML results from Cheshire.
	 */
	public String getAllFixedUpResultsCheshireEverReturned() {
		String result = "";
		for (String res: betterResults) {
			result += res;
		}
		return result;
	}


	/**
	 * Gets the total number of results.
	 * 
	 * @return total number of results.
	 */
	public int getNumResults() {
		return numResults;
	}

	/**
	 * Generates a confidence of the result that has been found.  Right now, it's generated as:
	 * 
	 *   number of times the top result appeared / total number of results
	 *   
	 * @return confidence of the top result.
	 */
	public double getConfidence() {
		// This is an interesting measure, but we'll try it

		// If no results, then we're not confident
		if (numResults == 0) return 0.0;

		// confidence now is defined as 1 / numResults.  We're only returning the top result, but it's out of X
		// return (1.0 / numResults);

		// new idea for confidence:  1 / number of unique results we kept.  That's 1 / number of top results found
		// return (1.0) / uniqueResults.size();

		// return (1.0) / (numResults - (double) uniqueResults.size());

		// confidence now is defined as 
		//     (number of times top result appeared) / numResults.  
		// 
		int count = 0;
		String result = results.get(0);

		for (String res : results) {
			if (result.equals(res))
				count++;
		}

		return ((double) count) / numResults;

	}

	/**
	 * Cleans a string of extraneous characters, to create a normal form.  Removes parentheses and brackets.  
	 * Replaces periods (.) with spaces if the periodToSpace parameter is true, else it removes periods.
	 * 
	 * @param string String to clean.
	 * @param periodToSpace If true, replaces periods with spaces. If false, removes periods.
	 * @return Cleaned string.
	 */
	public String cleanString(String string, boolean periodToSpace) {
		String result = StringEscapeUtils.escapeXml(string);
		//Normalize the string
		if (periodToSpace)
			result = result.toLowerCase().replaceAll("\\.", " ");
		
		result = result.toLowerCase().replaceAll("\\.", "");
		// Clean up the string
		result = result.replace("(", "");
		result = result.replace(")", "");
		result = result.replace("]", "");
		result = result.replace("[", "");
		result = result.trim();

		return result;
	}

	/**
	 * Creates a hashmap of countries of the world as full name -> ISO standard abbreviation
	 * 
	 * @return Hashmap of all countries
	 */
	public HashMap<String, String> getCountries() {
		HashMap<String, String> countries = new HashMap<String, String>();
		for (String iso : Locale.getISOCountries()) {
			Locale l = new Locale("", iso);
			countries.put(l.getDisplayCountry().toLowerCase(), iso);
		}

		// Fix for United Kingdom being called Great Britain or England
		countries.put("england", "gb");
		countries.put("great britain", "gb");

		// Fix for U.S.S.R., which does exist in geonames, but only as a political entity
		countries.put("ussr", "ru");
		
		// Since we seem to have issues matching the united states, let's add it!
		countries.put("united states", "us");

		return countries;
	}

	/**
	 * Creates a hashmap of US/Canada states and territories as full name -> abbreviation
	 * 
	 * @return Hashmap of all states and abbreviations
	 */
	public HashMap<String, String> getStates() {
		HashMap<String, String> states = new HashMap<String, String>();
		// Fix for District of Columbia
		states.put("district of columbia", "dc");

		// from the internet: http://stackoverflow.com/questions/11005751/is-there-a-util-to-convert-us-state-name-to-state-code-eg-arizona-to-az
		states.put("alabama","al");states.put("alaska","ak");states.put("alberta","ab");states.put("american samoa","as");states.put("arizona","az");states.put("arkansas","ar");states.put("armed forces (ae)","ae");states.put("armed forces americas","aa");states.put("armed forces pacific","ap");states.put("british columbia","bc");states.put("california","ca");states.put("colorado","co");states.put("connecticut","ct");states.put("delaware","de");states.put("district of columbia","dc");states.put("florida","fl");states.put("georgia","ga");states.put("guam","gu");states.put("hawaii","hi");states.put("idaho","id");states.put("illinois","il");states.put("indiana","in");states.put("iowa","ia");states.put("kansas","ks");states.put("kentucky","ky");states.put("louisiana","la");states.put("maine","me");states.put("manitoba","mb");states.put("maryland","md");states.put("massachusetts","ma");states.put("michigan","mi");states.put("minnesota","mn");states.put("mississippi","ms");states.put("missouri","mo");states.put("montana","mt");states.put("nebraska","ne");states.put("nevada","nv");states.put("new brunswick","nb");states.put("new hampshire","nh");states.put("new jersey","nj");states.put("new mexico","nm");states.put("new york","ny");states.put("newfoundland","nf");states.put("north carolina","nc");states.put("north dakota","nd");states.put("northwest territories","nt");states.put("nova scotia","ns");states.put("nunavut","nu");states.put("ohio","oh");states.put("oklahoma","ok");states.put("ontario","on");states.put("oregon","or");states.put("pennsylvania","pa");states.put("prince edward island","pe");states.put("puerto rico","pr");states.put("quebec","pq");states.put("rhode island","ri");states.put("saskatchewan","sk");states.put("south carolina","sc");states.put("south dakota","sd");states.put("tennessee","tn");states.put("texas","tx");states.put("utah","ut");states.put("vermont","vt");states.put("virgin islands","vi");states.put("virginia","va");states.put("washington","wa");states.put("west virginia","wv");states.put("wisconsin","wi");states.put("wyoming","wy");states.put("yukon territory","yt");
		return states;
	}

	/**
	 * Checks for fuzzy state names in the second position in the comma separated string
	 * 
	 * @param first First part of the query string (pre comma)
	 * @param second Second part of the query string (after the comma)
	 * @return The two-letter state abbreviation, if one exists, or null if not.
	 */
	public String checkForUSState(String first, String second) {
		String retVal = null;

		// Pre-matching filters.  Some short names don't match in the state names.
		if (second.equals("fla")) { // fla can be short for florida
			return "fl";
		}

		if (second.equals("n mex")) { // n mex can be short for new mexico
			return "nm";
		}

		// Lookup the partial name in a list of all full state names.  This will find, for example,
		// "mich" in "michigan".
		for (String state : states.keySet()) {
			if (state.toLowerCase().contains(second)) {
				// they should be unique enough that we should only have one of these
				retVal = states.get(state).toLowerCase();
				break;
			}
		}
		return retVal;
	}

	/**
	 * Queries the Google Autocorrect API and returns the top comma separated result from Google.  Also stores the type in the type field.
	 * 
	 * @param query Search string to query google
	 * @return Top result from google
	 */
	public String getGoogleAutoCorrectValue(String query) {
		String key = "AIzaSyD09OiYs3KGpaK4oxT7nXteUZBy-0by7oE";
		String server = "https://maps.googleapis.com/maps/api/place/autocomplete/json?sensor=false&";
		URL url;
		try {
			String json = "";
			String line;
			url = new URL(server + "input=" + URLEncoder.encode(query, "UTF-8") + "&key=" + key);
			BufferedReader in = new BufferedReader(new InputStreamReader(url.openStream()));
			while ((line = in.readLine()) != null) {
				json += line;
			}
			JSONObject obj = new JSONObject(json);
			JSONArray list = obj.getJSONArray("predictions");
			obj = list.getJSONObject(0);
			String description = obj.getString("description");
			System.err.println(obj);
			list = obj.getJSONArray("types");
			System.err.println(list);
			String type = list.getString(0);
			this.type = parseGoogleType(type);
			return description;
		} catch (Exception e) {
			System.err.println("Error connecting to google.");
			e.printStackTrace();
			return query;
		}
	}

	/**
	 * Parses the Google location type and returns a valid Geonames location type
	 * @param type2 Google location type
	 * @return Geonames location type, or null if there is not a good match
	 */
	private String parseGoogleType(String type2) {
		if (type2.equals("locality"))
			return "ppl";
		if (type2.equals("administrative_area_level_2"))
			return "adm2";
		if (type2.equals("administrative_area_level_1"))
			return "adm1";
		if (type2.equals("establishment"))
			return null;
		if (type2.equals("natural_feature"))
			return null;
		
		return null;
	}
	
	/**
	 * Gets the top Geonames result, in Geonames XML format
	 * 
	 * @return String Geonames XML result, or null if there was a problem
	 */
	public String getGeonamesEntry() {
		if (results.size() > 0)
			return results.get(0);
		return null;
	} 
	
	/**
	 * Parses out and returns the geonames id from the top Geonames result
	 * 
	 * @return String geonames id or null if there was a problem
	 */
	public String getGeonamesId() {
		if (resultDoc == null)
			parseResult();
		if (resultDoc != null) {
			return resultDoc.getElementsByTagName("geonameid").item(0).getTextContent();
		}
		return null;
	}
	
	/**
	 * Parses out and returns the name from the top Geonames result
	 * 
	 * @return String name or null if there was a problem
	 */
	public String getGeonamesName() {
		if (resultDoc == null)
			parseResult();
		if (resultDoc != null) {
			return resultDoc.getElementsByTagName("name").item(0).getTextContent();
		}
		return null;
	}
	
	/**
	 * Parses out and returns the latitude from the top Geonames result
	 * 
	 * @return String latitude or null if there was a problem
	 */
	public String getGeonamesLatitude() {
		if (resultDoc == null)
			parseResult();
		if (resultDoc != null) {
			return resultDoc.getElementsByTagName("latitude").item(0).getTextContent();
		}
		return null;
	}
	
	/**
	 * Parses out and returns the longitude from the top Geonames result
	 * 
	 * @return String longitude or null if there was a problem
	 */
	public String getGeonamesLongitude() {
		if (resultDoc == null)
			parseResult();
		if (resultDoc != null) {
			return resultDoc.getElementsByTagName("longitude").item(0).getTextContent();
		}
		return null;
	}
	
	/**
	 *  Parses the resulting Geonames XML row into the Document object resultDoc for later use.
	 */
	private void parseResult() {
		try {
			DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
			resultDoc = dBuilder.parse(new ByteArrayInputStream(results.get(0).getBytes()));
			
			resultDoc.getDocumentElement().normalize();
			
		} catch (Exception e) {
			resultDoc = null;
			
		}
	}
	
	/**
	 * Parses the given cheshire Geonames XML result and returns the name.
	 * 
	 * @param cheshireResult Geonames XML result string.
	 * @return String name from the result.
	 */
	private String getGeonamesName(String cheshireResult) {
		try {
			DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
			Document res = dBuilder.parse(new ByteArrayInputStream(results.get(0).getBytes()));
			
			res.getDocumentElement().normalize();
			return res.getElementsByTagName("name").item(0).getTextContent();
		} catch (Exception e) {
			return null;
			
		}
	}
	
	/**
	 *  The following code helps in dealing with ngrams issues
	 */
	
	
	private ArrayList<String> getSimilarLengthResults(String first) {
		// Sanity checks
		if (first == null)
			return new ArrayList<String>();
		if (overkill == null || overkill.isEmpty())
			return new ArrayList<String>();
		
		ArrayList<String> sls = new ArrayList<String>();
		int strLen = first.length();
		
		for (String candidateXML : this.overkill) {
			if (candidateXML != null) {
				String candidate = getGeonamesName(candidateXML);
				if (candidate != null && candidate.length() <= strLen + 2 && candidate.length() >= strLen - 2) {
					sls.add(candidateXML);
				}
			}
		}
		
		return sls;
		
	}
}
