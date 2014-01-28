package edu.virginia.iath.snac.functions;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class GeoNamesHelper {
	private PrintWriter out;
	private BufferedReader in;
	
	public boolean connect() {
		try {
			System.err.println("Starting cheshire search");
			Socket cheshire = new Socket("localhost", 12345);
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
		} catch (Exception e) {
			// do nothing
		}
		return true;
	}
	
	public String queryCheshire( String locationStr) {
		
		String result = "";
		try
		{

			// Maps of relevant places (countries and US states)
			Map<String, String> countries = getCountries();
			Map<String, String> states = getStates();
			if (countries.containsKey(locationStr)) { // we have a country!
				// Do a simple country look up

				// Replacing double quote (") with single quote (') in the search string 1/8/14, since some
				// country codes equal the cheshire commands.  (For example, GE for Georgia is the same as
				// greater or equal to.)  Single quotes appear to parse the value as literal, whereas double
				// quotes still treat it as the cheshire command (no escaping).
				out.println("find xcountry @ '" + countries.get(locationStr) + "' and xintlname @ '" + locationStr +"'");
				System.err.println("Searched for country code: " + countries.get(locationStr) + " and country: " + locationStr);
				String info = in.readLine();
				System.err.println(info);
			} else if (states.containsKey(locationStr)) { // we have a US state!
				// Do a simple state lookup
				out.println("find exactname[5=100] @ '"+ locationStr +"' and admin1 '"+ states.get(locationStr) +"'");
				System.err.println("Searched for state code: " + states.get(locationStr) + " and state name: " + locationStr);
				String info = in.readLine();
				System.err.println(info);
			} else if (states.keySet().contains(locationStr)) { // we have a US state!
				// Do a reverse state lookup
				String stateName = "";
				for(String key : states.keySet()) {
					if (states.get(key).equals(locationStr))
						stateName = key;
				}
				out.println("find exactname[5=100] @ '"+ stateName +"' and admin1 '"+ locationStr +"'");
				System.err.println("Searched for state code: " + locationStr + " and state name: " + stateName);
				String info = in.readLine();
				System.err.println(info);
			} else { // no country or state, do more in-depth queries

				String first = locationStr;
				String second = locationStr;
				if (locationStr.contains(",")) {
					first = locationStr.substring(0, locationStr.indexOf(",")).trim();
					second = locationStr.substring(locationStr.indexOf(",")+1, locationStr.length());
					second = second.trim();
					// if second are initials with a space in between, then remove the space
					if (second.length() <= 3) {
						second = second.replace(" ", "");
					}
				}

				System.err.println("Searching for: " + locationStr + " as 1." + first + "; 2." + second);
				// Query cheshire

				// Do exact query first
				// removing the @ on exactname 1/6/14 because it's causing some errors in the search
				// removing the @ on admin1 1/6/14 because it's reordering the results away from what we want
				//  ex: searching exactname west point and admin1 @ ny gives west point, the cape, while
				//      searching exactname west point and admin1 ny gives the city of west point
				// replacing the " with ' in the query string to escape the search terms-- 1/8/16.  Apparently
				//   " doesn't actually escape if there are Cheshire commands in the search term
				// adding [5=100] on exactname 1/15/14 to do a true exact match (without only does a
				//   startsWith match in cheshire
				out.println("find exactname[5=100] '" + first + "' and admin1 '" + second + "'");
				String info = in.readLine();
				System.err.println(info);
				if (info.contains(" 0")) {

					// Check for an international name matching, which may be a little better
					out.println("find xintlname[5=100] '" + first + "' and admin1 '" + second + "'");
					info = in.readLine();
					System.err.println(info);

					if (info.contains(" 0")) {

						// If we have something that may be a "city,state", let's look that up now just in case
						if (!first.equals(second)) {
							String stateSN = checkForUSState(first, second);

							if (stateSN != null) {
								// Do the query
								out.println("find exactname[5=100] '" + first + "' and admin1 '" + stateSN + "'");
								info = in.readLine();
								System.err.println(info);
							} else {
								// shortcut to continue
								info = " 0";
							}

						} else {
							// shortcut to continue
							info = " 0";
						}

						if (info.contains(" 0")) {

							// TODO 1/8/14 Here we should look up locality, country.  Since we didn't find a US
							// city, st, we should make sure we catch something like zurich, switzerland.  So,
							// the tokens after the comma should be checked for country code

							if (!first.equals(second)) {
								if (countries.containsKey(second)) {
									// the second set of tokens is a country! Get the country code and search

									// NOTE: we're going to search for international names, since we may have non-ascii characters such as
									// umlauts.
									out.println("find xcountry @ '" + countries.get(second) + "' and xintlname[5=100] @ '" + first +"'");
									System.err.println("Searched for country code: " + countries.get(second) + " and placename: " + first);
									info = in.readLine();
									System.err.println(info);
								} else {
									info = " 0";
								}
							} else {
								info = " 0";
							}

							if (info.contains(" 0")) {

								// Next, try a ranking name query by keyword
								out.println("find name @ '" + first + "' and admin1 @ '" + second + "'");
								info = in.readLine();
								System.err.println(info);
								if (info.contains(" 0")) {

									// TODO: Should in here try:
									// 1. find ngram_name_wadmin and exactname (find the exact name but with ngrams for the admin code)
									// 2. find ngram_name_wadmin and admin1 search (no alternate names)

									// Print out a message
									System.err.println("Switching to ngrams search.");

									// (1 above) Next, try a query on just ngrams in the name/admin code plus ranking of exact name (for bad state names)
									out.println("find ngram_name_wadmin '" + locationStr + "' and exactname @ '" + first + "'");
									info = in.readLine();
									System.err.println(info);
									if (info.contains(" 0")) { 

										// Next, try a looking for matching ngrams
										out.println("find ngram_wadmin '" + locationStr + "' and name_wadmin @ '" + locationStr + "'");
										info = in.readLine();
										System.err.println(info);
										if (info.contains(" 0")) {
											// Next, try looking for just ngrams and keyword name
											out.println("find ngram_wadmin '" + locationStr + "' and name @ '" + first + "'");
											info = in.readLine();
											System.err.println(info);

											if (info.contains(" 0")) {
												// Finally, just check ngrams
												System.err.println("Last ditch search");
												out.println("find ngram_wadmin '" + locationStr + "'");
												info = in.readLine();
												System.err.println(info);
											}
										}
									}
								}
							}
						}
					}
				}
			} // end else

			// Ask for the top entry
			out.println("display default 1 1");
			in.skip(17);
			System.err.println("=== ===== ===== ===== =====");
			while (in.ready()) {
				String line = in.readLine();
				result += line; // in.readLine();
				System.err.println(line);
			}

			System.err.println("=== ===== ===== ===== =====");

			// cleanup the result
			result = result.substring(0, result.length()-1);
			return result;
		} catch (Exception e) {
			return null;
		}
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
		HashMap<String,String> states = getStates();
		for (String state : states.keySet()) {
			if (state.toLowerCase().contains(second)) {
				// they should be unique enough that we should only have one of these
				retVal = states.get(state).toLowerCase();
				break;
			}
		}
		return retVal;
	}
}
