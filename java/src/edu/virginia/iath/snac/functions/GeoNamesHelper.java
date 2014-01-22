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
