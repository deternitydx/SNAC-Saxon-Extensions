package edu.virginia.iath.snac.helpers;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;

import org.apache.commons.lang3.StringEscapeUtils;
import org.json.JSONArray;
import org.json.JSONObject;

public class GeoNamesHelper {
	private Socket cheshire;
	private PrintWriter out;
	private BufferedReader in;
	private ArrayList<String> results;
	private HashSet<String> uniqueResults;
	private HashSet<String> overkill;
	private int numResults = 0;

	// Maps of relevant places (countries and US states)
	private Map<String, String> countries = null;
	private Map<String, String> states = null;

	public GeoNamesHelper() {
		countries = getCountries();
		states = getStates();
		results = new ArrayList<String>();
		uniqueResults = new HashSet<String>();
		overkill = new HashSet<String>();
	}

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

	// FOR OVERKILL
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

	private int getResultCount(String cheshireResult) {
		if (cheshireResult != null) {
			String[] info = cheshireResult.split("Default ");
			if (info != null && info[1] != null) 
				return Integer.parseInt(info[1]);
		}
		return 0;
	}

	private boolean addResult(String cheshireResult) {

		// Debug info:
		System.err.println(cheshireResult);

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
				out.println("find xcountry @ '" + countries.get(query) + "' and xintlname @ '" + query +"'");
				System.err.println("Searched for country code: " + countries.get(query) + " and country: " + query);
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


	protected boolean searchForState(String query) {
		String cheshireResult = null;
		try
		{
			if (states.containsKey(query)) { // we have a US state!
				// Do a simple state lookup
				out.println("find exactname[5=100] @ '"+ query +"' and admin1 '"+ states.get(query) +"'");
				System.err.println("Searched for state code: " + states.get(query) + " and state name: " + query);
				cheshireResult = in.readLine();
				System.err.println(cheshireResult);
			} else if (states.keySet().contains(query)) { // we have a US state!
				// Do a reverse state lookup
				String stateName = "";
				for(String key : states.keySet()) {
					if (states.get(key).equals(query))
						stateName = key;
				}
				out.println("find exactname[5=100] @ '"+ stateName +"' and admin1 '"+ query +"'");
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

	public String queryCheshire( String query) {


		String cheshireResult = null;



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

		System.err.println("Searching for: " + query + " as 1." + first + "; 2." + second);
		exactQueries(first, second, null);


		// If we still haven't got a match yet, then query Google for an auto correct and try again
		if (this.numResults == 0) {
			// INSERT GOOGLE HERE
			String country = null;
			String google = cleanString(this.getGoogleAutoCorrectValue(query));
			System.err.println("Google string: " + google);
			String[] terms = google.split(",");
			System.err.println(Arrays.toString(terms));
			System.err.println(terms.length);
			if (terms.length >= 3) {
				country =  terms[terms.length - 1].trim().toLowerCase();
				second = terms[terms.length - 2].trim().toLowerCase();
				first = "";
				for (int i = 0; i < terms.length - 2; i++) {
					first += terms[i].trim().toLowerCase();
				}
			} else {
				first = terms[0].trim().toLowerCase();
				if (terms.length > 1) {
					second = terms[1].trim().toLowerCase();
				} else {
					second = first;
				}
			}
			System.err.println("Searching for: " + query + " as 1." + first + "; 2." + second + "; country. " + country);
			exactQueries(first, second, country);

		}

		// try the last-ditch effort
		System.err.println("Last-ditch searching for: " + query);
		undesiredQueries(first, second, query);



		if (results.size() > 0)
			return results.get(0);
		return null;
	}

	public String exactQueries(String first, String second, String country) {
		String cheshireResult = null;
		String countryQuery = "";
		// Set up the country, if it exists
		if (country != null && countries.containsKey(country)) {
			countryQuery = " and xcountry '" + countries.get(country) + "'"; 
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
			out.println("find exactname[5=100] '" + first + "' and admin1 '" + second + "'" + countryQuery);
			cheshireResult = in.readLine();
			addResult(cheshireResult);

			// Next try an EXACT query for first as an international name and second as the admin1 (state-level)
			// 
			// Check for an international name matching, which may be a little better
			out.println("find xintlname[5=100] '" + first + "' and admin1 '" + second + "'" + countryQuery);
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
					out.println("find exactname[5=100] '" + first + "' and admin1 '" + stateSN + "'" + countryQuery);
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
					out.println("find xcountry @ '" + countries.get(second) + "' and xintlname[5=100] @ '" + first +"'");
					System.err.println("Searched for country code: " + countries.get(second) + " and placename: " + first);
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

	public String undesiredQueries(String first, String second, String query) {
		String cheshireResult = null;

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
				out.println("find name @ '" + first + "' and admin1 @ '" + second + "'");
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
				out.println("find ngram_name_wadmin '" + query + "' and exactname @ '" + first + "'");
				cheshireResult = in.readLine();
				addResult(cheshireResult);
			}

			// Next, try a looking for matching ngrams
			if (numResults == 0) {
				out.println("find ngram_wadmin '" + query + "' and name_wadmin @ '" + query + "'");
				cheshireResult = in.readLine();
				addResult(cheshireResult);
			}

			// Next, try looking for just ngrams and keyword name
			if (numResults == 0) {
				out.println("find ngram_wadmin '" + query + "' and name @ '" + first + "'");
				cheshireResult = in.readLine();
				addResult(cheshireResult);
			}

			// Finally, just check ngrams
			if (numResults == 0) {
				System.err.println("Last ditch search");
				out.println("find ngram_wadmin '" + query + "'");
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
	public String getAllUniqueResults() {
		String result = "";
		for (String res : uniqueResults) {
			result += res;
		}
		return result;
	}

	// IN ORDER, but may have duplicates
	public String getAllOrderedResults() {
		String result = "";
		for (String res : results) {
			result += res;
		}
		return result;
	}

	// OVERKILL (NO ORDER)
	public String getAllResultsCheshireEverReturned() {
		String result = "";
		for (String res: overkill) {
			result += res;
		}
		return result;
	}

	public int getNumResults() {
		return numResults;
	}

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

	public String cleanString(String string) {
		String result = StringEscapeUtils.escapeXml(string);
		//Normalize the string
		result = result.toLowerCase().replaceAll("\\.", " ");
		// Clean up the string
		result = result.replace("(", "");
		result = result.replace(")", "");
		result = result.replace("]", "");
		result = result.replace("[", "");
		result = result.trim();

		return result;
	}

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

		return countries;
	}

	public HashMap<String, String> getStates() {
		HashMap<String, String> states = new HashMap<String, String>();
		// Fix for District of Columbia
		states.put("district of columbia", "dc");

		// from the internet: http://stackoverflow.com/questions/11005751/is-there-a-util-to-convert-us-state-name-to-state-code-eg-arizona-to-az
		states.put("alabama","al");states.put("alaska","ak");states.put("alberta","ab");states.put("american samoa","as");states.put("arizona","az");states.put("arkansas","ar");states.put("armed forces (ae)","ae");states.put("armed forces americas","aa");states.put("armed forces pacific","ap");states.put("british columbia","bc");states.put("california","ca");states.put("colorado","co");states.put("connecticut","ct");states.put("delaware","de");states.put("district of columbia","dc");states.put("florida","fl");states.put("georgia","ga");states.put("guam","gu");states.put("hawaii","hi");states.put("idaho","id");states.put("illinois","il");states.put("indiana","in");states.put("iowa","ia");states.put("kansas","ks");states.put("kentucky","ky");states.put("louisiana","la");states.put("maine","me");states.put("manitoba","mb");states.put("maryland","md");states.put("massachusetts","ma");states.put("michigan","mi");states.put("minnesota","mn");states.put("mississippi","ms");states.put("missouri","mo");states.put("montana","mt");states.put("nebraska","ne");states.put("nevada","nv");states.put("new brunswick","nb");states.put("new hampshire","nh");states.put("new jersey","nj");states.put("new mexico","nm");states.put("new york","ny");states.put("newfoundland","nf");states.put("north carolina","nc");states.put("north dakota","nd");states.put("northwest territories","nt");states.put("nova scotia","ns");states.put("nunavut","nu");states.put("ohio","oh");states.put("oklahoma","ok");states.put("ontario","on");states.put("oregon","or");states.put("pennsylvania","pa");states.put("prince edward island","pe");states.put("puerto rico","pr");states.put("quebec","pq");states.put("rhode island","ri");states.put("saskatchewan","sk");states.put("south carolina","sc");states.put("south dakota","sd");states.put("tennessee","tn");states.put("texas","tx");states.put("utah","ut");states.put("vermont","vt");states.put("virgin islands","vi");states.put("virginia","va");states.put("washington","wa");states.put("west virginia","wv");states.put("wisconsin","wi");states.put("wyoming","wy");states.put("yukon territory","yt");
		return states;
	}

	// Checks for fuzzy state names in the second position in the comma separated string
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
			return obj.getString("description");
		} catch (Exception e) {
			System.err.println("Error connecting to google.");
			e.printStackTrace();
			return query;
		}
	}
}
