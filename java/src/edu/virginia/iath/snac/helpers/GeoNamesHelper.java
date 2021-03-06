/**
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
import java.net.Socket;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.commons.lang3.StringEscapeUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.w3c.dom.Document;

import edu.virginia.iath.snac.helpers.comparators.DifferenceNGramsComparator;
import edu.virginia.iath.snac.helpers.comparators.FlexibleNGramsComparator;
import edu.virginia.iath.snac.helpers.comparators.OriginalNGramsComparator;
import edu.virginia.iath.snac.helpers.datastructures.GeolocationString;

/**
 * GeoNamesHelper Class, used for querying cheshire for Geonames results.  Also utilizes
 * Google AutoCorrect to fix spelling errors and get more information search strings
 * before searching in cheshire.
 * 
 * @author Robbie Hott
 */
public class GeoNamesHelper {
	private static final boolean debug = false;
	private Socket cheshire;
	private PrintWriter out;
	private BufferedReader in;
	private ArrayList<String> results;
	private HashSet<String> uniqueResults;
	private ArrayList<String> overkill;
	private ArrayList<String> betterResults;
	private int numResults = 0;
	private boolean didNGramsSearch = false;
	private double discountConfidence = 1;

	// Maps of relevant places (countries and US states)
	private Map<String, String> countries = null;
	private Map<String, String> states = null;

	// Fields required when using Google
	private static final String googleAPIKey = null;
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
	 * Connects to cheshire via a Socket on localhost, port 7010.
	 * 
	 * @return True if connection was successful, false otherwise.
	 */
	public boolean connect() {
		try {
			cheshire = new Socket("localhost", 7010);
			out =
					new PrintWriter(cheshire.getOutputStream(), true);
			in =
					new BufferedReader(
							new InputStreamReader(cheshire.getInputStream()));

			// Init cheshire
			out.println("init");
			// Read the connection info off the buffer
			in.readLine();
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

	public int getLevelOfSearch() {
		if (this.didNGramsSearch) {
			return 1;
		}
		return 0;
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
				// 2/3/14 Replaced the @ with a direct search on country and an exact (no truncating) match
				//   on the country name.  This fixes US, but hopefully doesn't break any others.
				out.println("find xcountry '" + countries.get(query) + "' and xintlname[5=100] '" + query +"' and feature_type 'pcli'");
				cheshireResult = in.readLine();
				addResult(cheshireResult);
				return true;
			} else if (countries.values().contains(query)) { // we have an iso abbreviation
				String countryName = "";
				for (String key : countries.keySet()) {
					if (countries.get(key).equals(query))
						countryName = key;
				}
				out.println("find xcountry '" + query + "' and xintlname[5=100] '" + countryName +"' and feature_type 'pcli'");
				cheshireResult = in.readLine();
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
			if (states.containsKey(query)) { // we have a US state name!
				// Do a simple state lookup
				out.println("find exactname[5=100] '"+ query +"' and admin1 '"+ states.get(query) +"' and feature_type 'adm1'");
				cheshireResult = in.readLine();
			} else if (states.values().contains(query)) { // we have a US state abbreviation!
				// Do a reverse state lookup
				String stateName = "";
				for(String key : states.keySet()) {
					if (states.get(key).equals(query))
						stateName = key;
				}
				out.println("find exactname[5=100] '"+ stateName +"' and admin1 '"+ query +"' and feature_type 'adm1'");
				cheshireResult = in.readLine();
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
	 * <li> Look up the query in a list of US/Canada states for a match (state name or abbreviation)
	 * <li> Look up the query in a list of countries for a match (country name)
	 * <li> Perform exact queries on a comma-separated version of the query string
	 * <li> Perform the undesired queries (n-grams) on the original query string, and post processes the results.
	 * </ol>
	 * It then returns true if at least one result was found.
	 * 
	 * @param query The query string on which to search.
	 * @return True if there is at least one result, false otherwise.
	 */
	public boolean queryCheshire( String query) {


		String country = null;


		if (GeoNamesHelper.debug) System.err.println("==================================================================================");
		if (GeoNamesHelper.debug) System.err.println("Starting Search \t\t\tSearch String: " + query + "\n=====================");


		/*******************
		 * Start by searching for states, then for countries
		 */
		searchForState(query);
		if (GeoNamesHelper.debug) System.err.println("Queried for state.\t\t\tResults: " + this.numResults);

		searchForCountry(query);
		if (GeoNamesHelper.debug) System.err.println("Queried for country.\t\t\tResults: " + this.numResults);


		/*******************
		 * If no country or state, then do more in-depth queries with the string split into
		 * comma-separated portions.
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
		 * The code below searches for exact matches to the query string given.  The first part of the string is used as the
		 * place name, the second is used for clarification purposes (state, country, etc).  We do the searching in a particular
		 * order, searching for specific types of places first.
		 * 
		 * The query is searched through the GeoNames data for exact matches in the following order:
		 *  1. City (type code pplc)
		 *  2. Populated Place (type code ppl*)
		 *  3. Administrative Organization Level 1 (type code adm1). These are states.
		 *  4. Administrative Organization Level 2 (type code adm2). These are county-level orgs.
		 *  5. Anything else. (Buildings, streams, lakes, etc).
		 */
		if (GeoNamesHelper.debug) System.err.println("Broke string into parts.\n\t1: " + first + "\n\t2: " + second + "==");

		// City first
		exactQueries(first, second, null, "pplc");
		if (GeoNamesHelper.debug) System.err.println("XQueried for City.\t\t\tResults: " + this.numResults);

		// Then populated place
		exactQueries(first, second, null, "ppl");
		if (GeoNamesHelper.debug) System.err.println("XQueried for Populated Place.\t\tResults: " + this.numResults);

		// Then admin 1
		exactQueries(first, second, null, "adm1");
		if (GeoNamesHelper.debug) System.err.println("XQueried for Admin1.\t\t\tResults: " + this.numResults);

		// Then admin 2
		exactQueries(first, second, null, "adm2");
		if (GeoNamesHelper.debug) System.err.println("XQueried for Admin2.\t\t\tResults: " + this.numResults);

		// Then all others
		exactQueries(first, second, null, null);
		if (GeoNamesHelper.debug) System.err.println("XQueried for Others.\t\t\tResults: " + this.numResults);



		/**
		 * The code below searches Google for an autocorrected value and type.
		 */
		// queryGoogle(query);




		/**
		 * No exact searches found matches, so we'll switch to ngrams search followed by
		 * post-processing of those results.
		 */
		if (numResults == 0) {

			// Mark that we made it to this undesirable place
			didNGramsSearch = true;

			// Perform the undesired queries
			undesiredQueries(first, second, country, query);

			// Sort, filter and order the results by post-processing.  This includes splitting strings and
			// possible matches into ngrams and calculating difference/overlap/etc metrics.
			betterResults = this.getOrderedResultsByNGramsDifference(first, 3);

			// If we made it here, then the ordering of betterResults is better than that of the
			// real results, so we'll replace results with betterResults
			// NOTE: We allow betterResults to replace results even if the size of the better results is 0!
			//       Since we're only in this if statement because our exact searching returned no results,
			//		 then any results thrown out by the calculation of betterResults are okay to throw out.
			if (betterResults.size() >= 0) {
				results.clear();
				results = betterResults;
			}
		}

		// Return whether a result was found
		if (results.size() > 0) {
			if (GeoNamesHelper.debug) System.err.println("===================\nFound a result.\n\tName: " + this.getGeonamesName() + "\n\tConfidence: " + this.getConfidence());
			return true; 
		}
		return false;
	}
	
	
	/**
	 * Search for a given query string as an exact match.  It first normalizes the string by connecting
	 * to the Google AutoCorrect API to fix spelling errors, etc.  Google will also provide other information
	 * such as country and place type, which are used.  The resulting normalized string is used for exact
	 * searching in Cheshire.
	 * 
	 * It then returns true if at least one result was found.
	 * 
	 * @param query The query string on which to search.
	 * @return True if there is at least one result, false otherwise.
	 */
	public boolean queryGoogle(String query) {

		String first = null;
		String second = null;
		String country = null;

		// Query Google for an auto correct and type of place, then query Cheshire for the geonames matches
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

		// If we have a ppl type (populated place), search for the city first, then use the generic ppl list
		if (type != null && type.equals("ppl"))
			exactQueries(first, second, country, "pplc");

		exactQueries(first, second, country, type);

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
			if (GeoNamesHelper.debug) System.err.println("   Exact Name w/ admin1.\tResults: " + this.numResults);


			// Next try an EXACT query for first as an international name and second as the admin1 (state-level)
			// 
			// Check for an international name matching, which may be a little better
			out.println("find xintlname[5=100] '" + first + "' and admin1 '" + second + "'" + countryQuery + typeQuery);
			cheshireResult = in.readLine();
			addResult(cheshireResult);
			if (GeoNamesHelper.debug) System.err.println("   Exact Intl Name w/ admin1.\tResults: " + this.numResults);

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
					if (GeoNamesHelper.debug) System.err.println("   Exact Name w/ US State.\tResults: " + this.numResults);
				} 


				// Check to see if second is a Country and if so, grab the two letter abbreviation
				// and do an EXACT query for first as international name and country code as country
				if (countries.containsKey(second)) {
					// NOTE: we're going to search for international names, since we may have non-ascii characters such as
					// umlauts.
					out.println("find xcountry '" + countries.get(second) + "' and xintlname[5=100] @ '" + first +"'" + typeQuery);
					cheshireResult = in.readLine();
					addResult(cheshireResult);
					if (GeoNamesHelper.debug) System.err.println("   Exact Intl Name w/ country.\tResults: " + this.numResults);
				}

				// redo the last search but with country instead of second (if country is set)
				if (country != null && countries.containsKey(country)) {
					out.println("find xcountry '" + countries.get(country) + "' and xintlname[5=100] '" + first +"'" + typeQuery);
					cheshireResult = in.readLine();
					addResult(cheshireResult);
					if (GeoNamesHelper.debug) System.err.println("   Exact Intl Name w/ country2.\tResults: " + this.numResults);
				}
			}

			if (results.size() > 0)
				return results.get(0);

			return null;	
		} catch (Exception e) {
			System.err.println("Error in query: " + first + ", " + second);
			e.printStackTrace();
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

		try
		{
			//**********************************************************************************************************
			// Undesired Searching Mechanisms
			// These should only be used if the above fail, and each one should taken individually.  If one
			// succeeds, the rest should NOT be done
			//
			// Also, these methods should have their confidence penalized heavily!  They are NOT guaranteed to return
			// what we want at ALL.

			// Try a ranking name query by keyword
			if (numResults == 0) {
				out.println("find name @ '" + first + "' and admin1 @ '" + second + "'" + countryQuery);
				cheshireResult = in.readLine();
				addResult(cheshireResult);
				if (GeoNamesHelper.debug) System.err.println("Query keyword Name w/ admin1.\t\tResults: " + this.numResults);
			}

			// Print out a message if DEBUG
			if (GeoNamesHelper.debug && numResults == 0) 
				System.err.println("== Switching to NGRAMS Searching ==");


			// Search Geonames name, admin, alt names for ngram matches.  Cheshire does NOT do post processing to clean up the
			// results, so these results must be post processed!
			if (numResults == 0) {
				out.println("find ngram_wadmin '" + query + "'");
				cheshireResult = in.readLine();
				addResult(cheshireResult);
				if (GeoNamesHelper.debug) System.err.println("Query ngrams w/admin on entire string.\tResults: " + this.numResults);
			}

			/**
			 * The below searches are some ways to find better ngrams matches.  These provided some help in getting better order
			 * out of Cheshire, which is why they were tried in a particular order.  However, with post-processing of the search
			 * above, these become unnecessary as post-processing produces better results than any of these individually.
			 */
			/* 
			// Next, try a query on just ngrams in the name/admin code plus ranking of exact name (for bad state names)
			if (numResults == 0) {
				out.println("find ngram '" + first + "' and exactname @ '" + first + "'" + countryQuery);
				cheshireResult = in.readLine();
				addResult(cheshireResult);
			}

			// Next, try a looking for matching ngrams
			if (numResults == 0) {
				out.println("find ngram '" + first + "' and name_wadmin @ '" + query + "'" + countryQuery);
				cheshireResult = in.readLine();
				addResult(cheshireResult);
			}

			// Next, try looking for just ngrams and keyword name
			if (numResults == 0) {
				out.println("find ngram '" + first + "' and name @ '" + first + "'" + countryQuery);
				cheshireResult = in.readLine();
				addResult(cheshireResult);
			}

			// Finally, just check ngrams
			if (numResults == 0) {
				out.println("find ngram_all_wadmin '" + query + "'" + countryQuery);
				cheshireResult = in.readLine();
				addResult(cheshireResult);
			}
			 */
			//**********************************************************************************************************

			if (results.size() > 0)
				return results.get(0);

			return null;	
		} catch (Exception e) {
			return null;
		}
	}

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


	/**
	 * Gets all top Cheshire results (in Geonames XML format), in order based on when they were found.  
	 * Matches for more exact queries will be first, with the top match first.  There may be duplicates
	 * in this list.
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


	/**
	 * Gets top <code>max</code> Cheshire results (in Geonames XML format), in order based on when they were found.  
	 * Matches for more exact queries will be first, with the top match first. There may be duplicates in this list.
	 * 
	 * @param max Maximum number of results to return
	 * @return String of concatenated XML results from Cheshire.
	 */
	public String getAllOrderedResults(int max) {
		String result = "";
		int i = 0;
		for (String res : results) {
			result += res;
			if (i++ > max) break;
		}
		return result;
	}


	/**
	 * Gets all Cheshire results (in Geonames XML format) that were returned.  This method is OVERKILL.
	 * There may be duplicates in this list.
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
	 * Gets all Cheshire results (in Geonames XML format) that were returned that are filtered/sorted by ngrams.
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
	 * However, we do some discounting of the confidence if an ngrams search was performed.
	 *   
	 * @return confidence of the top result.
	 */
	public double getConfidence() {

		// If no results, then we're not confident
		if (numResults == 0) return 0.0;
		
		/**
		 * Former ideas for confidence measures:
		 * 
		 * 1. confidence now is defined as 1 / numResults.  We're only returning the top result, but it's out of X
		 * 		// return (1.0 / numResults);
		 * 2. new idea for confidence:  1 / number of unique results we kept.  That's 1 / number of top results found
		 *		// return (1.0) / uniqueResults.size();
		 * 3. remove the unique results
		 *      // return (1.0) / (numResults - (double) uniqueResults.size());
		 * 
		 */

		if (!this.didNGramsSearch) {
			// EXACT SEARCHES
			// This method of confidence is only when an exact search returned our result.
			//   Define the standard confidence of our exact search.  This means that we found multiple exact matches,
			//   such as country and state, city and state, etc.
			//     (number of times top result appeared) / numResults.  
			// 
			int count = 0;
			String result = results.get(0);

			// For each XML result, check to see how many times the top result was selected.  We only want to count
			// unique results.  But, we want to favor our result if it appeared multiple times in our searches,
			// so we won't use 1 / numberUniqueResults. (The more times it shows up, the more confident we are).
			for (String res : results) {
				if (result.equals(res))
					count++;
			}

			return ((double) count) / numResults;
		} else {
			// NGRAMS SEARCHES
			// These searches return many many more results than we should count, and therefore the standard
			// 1/numResults will return a bogus confidence, always around .0001.  So, we must consider a different
			// method of confidence in this case.
			//   Define the confidence of ngrams as 1 / number of similar results found.  We can ignore most results.

			// Number of similar results are stored in numResults (by the cleanup method of betterResults)

			return (1.0 / numResults) * discountConfidence;

		}

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
		
		// If the string starts with a parenthesis, then we don't want to consider that as part of a
		// clarification string, so we'll remove it first
		if (result.startsWith("(")) {
			result = result.substring(1);
		}

		// Convert (place) to , place
		// for now, not using the regex, but replacing the ( with , and ) with nothing
		if (result.contains("(") && result.contains(")")) {
			result = result.replace("(", ", ");
			result = result.replace(")", "");
		}

		// Clean up the string
		result = result.replace("(", "");
		result = result.replace(")", "");
		result = result.replace("]", "");
		result = result.replace("[", "");
		result = result.replace(":", "");

		// Remove possessives:
		result = result.replace("'s", "");
		result = result.replace("'s", "");

		result = result.replace("&quot;", "");
		result = result.replace("&apos;s", "");

		// Trim the string
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

		// Since we seem to have issues matching the united states and mexico, let's add it!
		countries.put("united states", "us");
		countries.put("mexico", "mx");

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
		states.put("alabama", "al");
		states.put("alaska", "ak");
		states.put("alberta", "ab");
		states.put("american samoa", "as");
		states.put("arizona", "az");
		states.put("arkansas", "ar");
		states.put("armed forces (ae)", "ae");
		states.put("armed forces americas", "aa");
		states.put("armed forces pacific", "ap");
		states.put("british columbia", "bc");
		states.put("california", "ca");
		states.put("colorado", "co");
		states.put("connecticut", "ct");
		states.put("delaware", "de");
		states.put("district of columbia", "dc");
		states.put("florida", "fl");
		states.put("georgia", "ga");
		states.put("guam", "gu");
		states.put("hawaii", "hi");
		states.put("idaho", "id");
		states.put("illinois", "il");
		states.put("indiana", "in");
		states.put("iowa", "ia");
		states.put("kansas", "ks");
		states.put("kentucky", "ky");
		states.put("louisiana", "la");
		states.put("maine", "me");
		states.put("manitoba", "mb");
		states.put("maryland", "md");
		states.put("massachusetts", "ma");
		states.put("michigan", "mi");
		states.put("minnesota", "mn");
		states.put("mississippi", "ms");
		states.put("missouri", "mo");
		states.put("montana", "mt");
		states.put("nebraska", "ne");
		states.put("nevada", "nv");
		states.put("new brunswick", "nb");
		states.put("new hampshire", "nh");
		states.put("new jersey", "nj");
		states.put("new mexico", "nm");
		states.put("new york", "ny");
		states.put("newfoundland", "nf");
		states.put("north carolina", "nc");
		states.put("north dakota", "nd");
		states.put("northwest territories", "nt");
		states.put("nova scotia", "ns");
		states.put("nunavut", "nu");
		states.put("ohio", "oh");
		states.put("oklahoma", "ok");
		states.put("ontario", "on");
		states.put("oregon", "or");
		states.put("pennsylvania", "pa");
		states.put("prince edward island", "pe");
		states.put("puerto rico", "pr");
		states.put("quebec", "pq");
		states.put("rhode island", "ri");
		states.put("saskatchewan", "sk");
		states.put("south carolina", "sc");
		states.put("south dakota", "sd");
		states.put("tennessee", "tn");
		states.put("texas", "tx");
		states.put("utah", "ut");
		states.put("vermont", "vt");
		states.put("virgin islands", "vi");
		states.put("virginia", "va");
		states.put("washington", "wa");
		states.put("west virginia", "wv");
		states.put("wisconsin", "wi");
		states.put("wyoming", "wy");
		states.put("yukon territory", "yt");
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
		String key = GeoNamesHelper.googleAPIKey;
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
			list = obj.getJSONArray("types");
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
			Document res = dBuilder.parse(new ByteArrayInputStream(cheshireResult.getBytes()));

			res.getDocumentElement().normalize();
			return res.getElementsByTagName("name").item(0).getTextContent();
		} catch (Exception e) {
			return null;

		}
	}

	/**
	 * Parses the given cheshire Geonames XML result and returns the population.
	 * 
	 * @param cheshireResult Geonames XML result string.
	 * @return String population from the result.
	 */
	private String getGeonamesPop(String cheshireResult) {
		try {
			DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
			Document res = dBuilder.parse(new ByteArrayInputStream(cheshireResult.getBytes()));

			res.getDocumentElement().normalize();
			return res.getElementsByTagName("population").item(0).getTextContent();
		} catch (Exception e) {
			return null;

		}
	}

	/**
	 * Parses the given cheshire Geonames XML result and returns the number of alternate names.
	 * 
	 * @param cheshireResult Geonames XML result string.
	 * @return int number of alternate names.
	 */
	private int getGeonamesNumAltNames(String cheshireResult) {
		try {
			DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
			Document res = dBuilder.parse(new ByteArrayInputStream(cheshireResult.getBytes()));

			res.getDocumentElement().normalize();
			return res.getElementsByTagName("alt").getLength();
		} catch (Exception e) {
			return 0;

		}
	}
	
	/**
	 * Parses the given cheshire Geonames XML result and creates the return XML for this object.
	 * 
	 * @param cheshireResult Geonames XML result string.
	 * @return String return XML of the geonames string
	 */
	private String getXMLReturnValue(String cheshireResult) {
		try {
			DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
			Document res = dBuilder.parse(new ByteArrayInputStream(cheshireResult.getBytes()));

			res.getDocumentElement().normalize();
			
			String ret = "";
			ret += "<geonameId>" + res.getElementsByTagName("geonameid").item(0).getTextContent() + "</geonameId>";
			ret += "<name>" + res.getElementsByTagName("name").item(0).getTextContent() + "</name>";
			ret += "<admin1>" + res.getElementsByTagName("admin1").item(0).getTextContent() + "</admin1>";
			ret += "<country>" + res.getElementsByTagName("country_code").item(0).getTextContent() + "</country>";
			ret += "<latitude>" + res.getElementsByTagName("latitude").item(0).getTextContent() + "</latitude>";
			ret += "<longitude>" + res.getElementsByTagName("longitude").item(0).getTextContent() + "</longitude>";
			
			return ret;
		} catch (Exception e) {
			return "<error>Couldn't parse geonames result correctly.</error>";

		}
	}

	/**
	 * Gets top Cheshire result (in normalized XML format).
	 * 
	 * @return String of XML result.
	 */
	public String getTopReturnResult() {
		return this.getXMLReturnValue(results.get(0));
	}
	
	/**
	 * Gets top <code>max</code> Cheshire results (in normalized XML format), in order based on when they were found.  
	 * Matches for more exact queries will be first, with the top match first. There may be duplicates in this list.
	 * 
	 * @param max Maximum number of results to return
	 * @return String of concatenated XML results, each wrapped in a place tag.
	 */
	public String getTopReturnResults(int max) {
		String result = "";
		int i = 0;
		for (String res : results) {
			result += "<place>" + getXMLReturnValue(res) + "</place>\n";
			if (i++ > max) break;
		}
		return result;
	}
	
	/**
	 * Gets <code>max</code> next Cheshire results (in normalized XML format), in no particular order. It ignores the
	 * first best match, only returning results 2 to max.  There are no duplicates in this list.
	 * 
	 * @param max Maximum number of results to return
	 * @return String of concatenated XML results, each wrapped in a place tag.
	 */
	public String getNextUniqueReturnResults(int max) {
		String result = "";
		int i = 0;
		HashSet<String> uniqueResults = new HashSet<String>();
		uniqueResults.addAll(results);
		if (results.size() > 0) 
			uniqueResults.remove(results.get(0));
		for (String res : uniqueResults) {
			result += "<place>" + getXMLReturnValue(res) + "</place>\n";
			if (i++ > max) break;
		}
		return result;
	}

	/**
	 *  The following code post-processes NGRAMS searches
	 *  ========================================================================
	 */
	
	/**
	 * Orders the candidate results from Cheshire to produce a better resulting set.  Given the first part of the
	 * query string and the length of ngrams used, this method breaks <code>first</code> into ngrams.  It then
	 * breaks the GeoName <code>name</code> for each of the Cheshire results into ngrams as well.  It then adds
	 * all results that have at least 1 ngram-overlap with <code>first</code> to a list, then sorts them using
	 * the DifferenceNGramsComparator, which sorts in the following order:
	 * <ol>
	 * <li> Sort by difference between strings (number of ngrams not matched) (ascending)
	 * <li> Sort by number of overlapping ngrams (descending)
	 * <li> Sort by alternate names (descending)
	 * <li> Sort by population (descending)
	 * </ol>
	 * 
	 * Once sorted, it calculates a discount on the confidence of the first element being the search result we want.
	 * This discount is as follows:
	 * <ol>
	 * <li> If the first result's name matches <code>first</code>, discount confidence by 50%.
	 * <li> If the first result's name contains <code>first</code>, discount confidence by 90%.
	 * <li> Else, discount confidence by 99%.
	 * </ol>
	 * 
	 * Also, the number of total results stored is calculated by the top 3 tiers of matches, ie the top three levels
	 * of ngram overlap or ngram difference from <code>first</code>.
	 * 
	 * @param first First part of the query string
	 * @param ngramLength Number of characters per ngram
	 * @return List of ordered results
	 */
	private ArrayList<String> getOrderedResultsByNGramsDifference(String first, int ngramLength) {
		ArrayList<String> ret = new ArrayList<String>();
		ArrayList<GeolocationString> toSort = new ArrayList<GeolocationString>();

		GeolocationString ngramFirst = new GeolocationString(first.toLowerCase().trim(), ngramLength);

		// Put each candidate from overkill into the new object
		for (String candidateXML : this.overkill) {
			if (candidateXML != null) {
				String candidate = getGeonamesName(candidateXML);
				if (candidate != null) {
					GeolocationString tmp = new GeolocationString(candidate.toLowerCase().replace("(historical)", "").trim(), ngramLength);
					tmp.setNGramMaster(ngramFirst);
					tmp.storeData(candidateXML);
					tmp.setPopulation(getGeonamesPop(candidateXML));
					tmp.setNumAltNames(getGeonamesNumAltNames(candidateXML));
					if (tmp.getOverlap() > 1)
						toSort.add(tmp);
				}
			}
		}
		
		Collections.sort(toSort, new DifferenceNGramsComparator());


		for (GeolocationString sorted : toSort) {
			ret.add((String) sorted.getData());
		}

		// Do some number crunching for the confidence information
		// Since at least one result has been sorted.
		if (toSort.size() > 0) {
			int count = 0;
			if (toSort.get(0).getString().equals(ngramFirst.getString())) { 
				// we have an exact match at top! So, only count the number of exact names present.
				// This is simplified, since we might have matched an alternate name.
				for (GeolocationString str : toSort) {
					if (str.getString().equals(ngramFirst.getString())) {
						count++;
					}
				}
			} else {
				int curOverlap = toSort.get(0).getOverlap();
				int curDiff = toSort.get(0).getDifference();
				int stepDown = 0;
				for (GeolocationString str : toSort) {
					if (str.getOverlap() != curOverlap || str.getDifference() != curDiff) {
						if (stepDown > 2) break; // if we've passed two steps down, then quit.
						stepDown++;
						curOverlap = str.getOverlap();
						curDiff = str.getDifference();
					}
					count++;
				}
			}
			this.numResults = count;

			if (toSort.get(0).getString().replace(" ", "").equals(ngramFirst.getString().replace(" ", "")))
				this.discountConfidence  = 0.5; // Can't trust even an exact match from ngrams at 100%
			else if (toSort.get(0).getString().replace(" ", "").contains(ngramFirst.getString().replace(" ", "")))
				this.discountConfidence  = 0.1; // If it's not an exact match, we can trust it even less
			else 
				this.discountConfidence  = 0.01; // If it doesn't even contain all of the original string, it's REALLY hard to trust

			System.err.println("Discounting confidence: " + this.discountConfidence);

		}	

		return ret;


	}


	/**
	 * Use the list of all results given by Cheshire and the length of the first part of the query string
	 * to produce a smaller list of results that only differ *in length* from the first part of the query string by
	 * <code>diff</code> number of characters.  Preserves order of <code>overkill</code>.
	 * 
	 * @param first First part of the query string
	 * @param diff Number of characters of allowable difference
	 * @return List of results that are <code>diff</code> number of characters longer/shorter than <code>first</code>
	 */
	@SuppressWarnings("unused")
	private ArrayList<String> getSimilarLengthResults(String first, int diff) {
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
				if (candidate != null && candidate.length() <= strLen + diff && candidate.length() >= strLen - diff) {
					sls.add(candidateXML);
				}
			}
		}

		return sls;
	}

	/**
	 * Orders the candidate results from Cheshire to produce a better resulting set.  Given the first part of the
	 * query string and the length of ngrams used, this method breaks <code>first</code> into ngrams.  It then
	 * breaks the GeoName <code>name</code> for each of the Cheshire results into ngrams as well.  It then adds
	 * all results that have at least 1 ngram-overlap with <code>first</code> to a list, then sorts them using
	 * the overlap (default) comparator, which sorts in the following order:
	 * <ol>
	 * <li> Sort by number of overlapping ngrams (descending)
	 * <li> Sort by difference between strings (number of ngrams not matched) (ascending)
	 * <li> Sort by alternate names (descending)
	 * <li> Sort by population (descending)
	 * </ol>
	 * 
	 * @param first First part of the query string
	 * @param ngramLength Number of characters per ngram
	 * @return List of ordered results
	 */
	@SuppressWarnings("unused")
	private ArrayList<String> getOrderedResultsByNGrams(String first, int ngramLength) {
		ArrayList<String> ret = new ArrayList<String>();
		ArrayList<GeolocationString> toSort = new ArrayList<GeolocationString>();

		GeolocationString ngramFirst = new GeolocationString(first.toLowerCase().trim(), ngramLength);

		// Put each candidate from overkill into the new object
		for (String candidateXML : this.overkill) {
			if (candidateXML != null) {
				String candidate = getGeonamesName(candidateXML);
				if (candidate != null) {
					GeolocationString tmp = new GeolocationString(candidate.toLowerCase().replace("(historical)", "").trim(), ngramLength);
					tmp.setNGramMaster(ngramFirst);
					tmp.storeData(candidateXML);
					tmp.setPopulation(getGeonamesPop(candidateXML));
					tmp.setNumAltNames(getGeonamesNumAltNames(candidateXML));
					if (tmp.getOverlap() > 1)
						toSort.add(tmp);
				}
			}
		}

		Collections.sort(toSort, new OriginalNGramsComparator());

		for (GeolocationString sorted : toSort) {
			ret.add((String) sorted.getData());
		}

		return ret;


	}

	
	/**
	 * Orders the candidate results from Cheshire to produce a better resulting set.  Given the first part of the
	 * query string and the length of ngrams used, this method breaks <code>first</code> into ngrams.  It then
	 * breaks the GeoName <code>name</code> for each of the Cheshire results into ngrams as well.  It then adds
	 * all results that have at least 1 ngram-overlap with <code>first</code> to a list, then sorts them using
	 * the FlexibleNGramsComparator, which sorts in the following order:
	 * <ol>
	 * <li> Sort by alternate names (descending)
	 * <li> Sort by string length (descending)
	 * <li> Sort by population (descending)
	 * </ol>
	 * 
	 * @param first First part of the query string
	 * @param ngramLength Number of characters per ngram
	 * @return List of ordered results
	 */
	@SuppressWarnings("unused")
	private ArrayList<String> getOrderedResultsByNGramsFlexible(String first, int ngramLength) {
		ArrayList<String> ret = new ArrayList<String>();
		ArrayList<GeolocationString> toSort = new ArrayList<GeolocationString>();

		GeolocationString ngramFirst = new GeolocationString(first.toLowerCase().trim(), ngramLength);

		// Put each candidate from overkill into the new object
		for (String candidateXML : this.overkill) {
			if (candidateXML != null) {
				String candidate = getGeonamesName(candidateXML);
				if (candidate != null) {
					GeolocationString tmp = new GeolocationString(candidate.toLowerCase().replace("(historical)", "").trim(), ngramLength);
					tmp.setNGramMaster(ngramFirst);
					tmp.storeData(candidateXML);
					tmp.setPopulation(getGeonamesPop(candidateXML));
					tmp.setNumAltNames(getGeonamesNumAltNames(candidateXML));
					if (tmp.getOverlap() > 1)
						toSort.add(tmp);
				}
			}
		}

		if (!toSort.isEmpty()) {
			Collections.sort(toSort, new OriginalNGramsComparator());

			int maxOverlap = toSort.get(0).getOverlap();
			int flexOverlap = maxOverlap - 1;

			// Remove all items that have overlap less than flexOverlap
			Iterator<GeolocationString> itr = toSort.iterator();
			while (itr.hasNext()) {
				GeolocationString cur = itr.next();
				if (cur.getOverlap() < flexOverlap)
					itr.remove(); 
			}

			Collections.sort(toSort, new FlexibleNGramsComparator());

			for (GeolocationString sorted : toSort) {
				ret.add((String) sorted.getData());
			}
		}

		return ret;


	}

}
