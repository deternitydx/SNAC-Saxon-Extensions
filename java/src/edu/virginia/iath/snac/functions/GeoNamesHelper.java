package edu.virginia.iath.snac.functions;

import java.util.HashMap;
import java.util.Locale;

public class GeoNamesHelper {
	
	public HashMap<String, String> getCountries() {
		HashMap<String, String> countries = new HashMap<String, String>();
		for (String iso : Locale.getISOCountries()) {
			Locale l = new Locale("", iso);
			countries.put(l.getDisplayCountry().toLowerCase(), iso);
		}
		return countries;
	}
	
	public HashMap<String, String> getStates() {
		HashMap<String, String> states = new HashMap<String, String>();
		// from the internet: http://stackoverflow.com/questions/11005751/is-there-a-util-to-convert-us-state-name-to-state-code-eg-arizona-to-az
		states.put("Alabama","AL");states.put("Alaska","AK");states.put("Alberta","AB");states.put("American Samoa","AS");states.put("Arizona","AZ");states.put("Arkansas","AR");states.put("Armed Forces (AE)","AE");states.put("Armed Forces Americas","AA");states.put("Armed Forces Pacific","AP");states.put("British Columbia","BC");states.put("California","CA");states.put("Colorado","CO");states.put("Connecticut","CT");states.put("Delaware","DE");states.put("District Of Columbia","DC");states.put("Florida","FL");states.put("Georgia","GA");states.put("Guam","GU");states.put("Hawaii","HI");states.put("Idaho","ID");states.put("Illinois","IL");states.put("Indiana","IN");states.put("Iowa","IA");states.put("Kansas","KS");states.put("Kentucky","KY");states.put("Louisiana","LA");states.put("Maine","ME");states.put("Manitoba","MB");states.put("Maryland","MD");states.put("Massachusetts","MA");states.put("Michigan","MI");states.put("Minnesota","MN");states.put("Mississippi","MS");states.put("Missouri","MO");states.put("Montana","MT");states.put("Nebraska","NE");states.put("Nevada","NV");states.put("New Brunswick","NB");states.put("New Hampshire","NH");states.put("New Jersey","NJ");states.put("New Mexico","NM");states.put("New York","NY");states.put("Newfoundland","NF");states.put("North Carolina","NC");states.put("North Dakota","ND");states.put("Northwest Territories","NT");states.put("Nova Scotia","NS");states.put("Nunavut","NU");states.put("Ohio","OH");states.put("Oklahoma","OK");states.put("Ontario","ON");states.put("Oregon","OR");states.put("Pennsylvania","PA");states.put("Prince Edward Island","PE");states.put("Puerto Rico","PR");states.put("Quebec","PQ");states.put("Rhode Island","RI");states.put("Saskatchewan","SK");states.put("South Carolina","SC");states.put("South Dakota","SD");states.put("Tennessee","TN");states.put("Texas","TX");states.put("Utah","UT");states.put("Vermont","VT");states.put("Virgin Islands","VI");states.put("Virginia","VA");states.put("Washington","WA");states.put("West Virginia","WV");states.put("Wisconsin","WI");states.put("Wyoming","WY");states.put("Yukon Territory","YT");
		return states;
	}
	
	// Checks for fuzzy state names in the second position in the comma separated string
	public String checkForUSState(String first, String second) {
		String retVal = null;
		
		// Pre-matching filters.  Some short names don't match in the state names.
		if (second.equals("fla")) { // fla can be short for florida
			return "fl";
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
