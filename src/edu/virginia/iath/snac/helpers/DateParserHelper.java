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

import java.util.ArrayList;
import java.util.List;

/**
 * Date parser for Java based on conventions used in SNAC.   Given a string, tries multiple parsings for a date,
 * converting them into a Java Date object.  Dates are returned in an ISO standard format, YYYY-MM-DD.
 * 
 * @author Robbie Hott
 *
 */
public class DateParserHelper {
	private String original = null;
	private ArrayList<SNACDate> dates = null;

	

	public DateParserHelper(String d) {
		// Initialize dates
		dates = new ArrayList<SNACDate>();
		
		// Store the date string locally
		original = d.trim();
		
		// Parse the dates into Date objects
		runParser();
	}
	
	public String getOriginalDate() {
		return original;
	}
	
	private void runParser() {
		// Check for date range.  If so, parse separately
		dateStringPreprocess();
		
		if (original.contains("-")) {
			dates.add(new SNACDate(original.substring(0, original.indexOf("-")).trim(), SNACDate.FROM_DATE));
			dates.add(new SNACDate(original.substring(original.indexOf("-") + 1).trim(), SNACDate.TO_DATE));
			
		} else if (original.contains("through")) {
			dates.add(new SNACDate(original.substring(0, original.indexOf("through")).trim(), SNACDate.FROM_DATE));
			dates.add(new SNACDate(original.substring(original.indexOf("through") + 7).trim(), SNACDate.TO_DATE));
			
		} else {
			dates.add(new SNACDate(original.trim()));
		}
		
		for (SNACDate d : dates) {
			// parse the dates found
			parseDate(d);
		}
		
		for (int i = 0; i < dates.size(); i++) {
			parsePostprocess(i);
		}
		
		for (SNACDate d : dates) {
			// Handle any modifiers to each of these dates
			d.handleModifiers();
		}
	}

	public boolean isRange() {
		return dates.get(0).isRange();
	}

	public boolean wasParsed() {
		boolean parsed = true;
		for (SNACDate d : dates) {
			parsed &= d.wasParsed();
		}
		return parsed;
	}
	
	public List<SNACDate> getDates() {
		return dates;
	}

	
	private void dateStringPreprocess() {
		
		// Handle dates surrounded with []
		if (original.endsWith("]") && original.startsWith("["))
			original = original.substring(1, original.length() -1);
		// Handle dates surrounded with ()
		if (original.endsWith(")") && original.startsWith("("))
			original = original.substring(1, original.length() -1);
		
	}
	
	private void parsePreprocess(SNACDate d) {

		/**
		 * Fixes for non-standardized date formats
		 */
		// Handle non-standard month representations
		d.setString(d.getString().replace("Sept.", "Sep."));
		// Handle dates surrounded with []
		if (d.getString().endsWith("]") && d.getString().startsWith("["))
			d.setString(d.getString().substring(1, d.getString().length() -1));
		
	
		/**
		 * Handling actual date keywords such as circa, centuries, questions, etc
		 */
		// Look for and handle the circa/Circa/... keyword
		if (d.getString().contains("circa") || d.getString().contains("Circa") || d.getString().contains("ca.")) {
			d.addModifier("circa");
			
			d.updateString("circa");
			d.updateString("Circa");
			d.updateString("ca.");
		}
		
		// Look for decades (s after the date)
		if (d.getString().endsWith("s")) {
			d.addModifier("decade");
			
			d.setString(d.getString().substring(0,d.getString().length() -1));
		}
		
		// Look for fuzzy dates (some form of "[?]", "(?)", ...)
		if (d.getString().contains("?")) {
			d.addModifier("fuzzy");
			
			d.updateString("[?]", "");
			d.updateString("(?)", "");
			d.updateString("?", "");
		}
		
		// Look for seasons
		String lowercase = d.getString().toLowerCase();
		if (lowercase.contains("fall") || lowercase.contains("autumn")) {
			d.addModifier("season");
			d.addModifier("fall");

			d.updateString("fall", "");
			d.updateString("autumn", "");
			d.updateString("Fall", "");
			d.updateString("Autumn", "");
		}
		if (lowercase.contains("spring")) {
			d.addModifier("season");
			d.addModifier("spring");

			d.updateString("spring", "");
			d.updateString("Spring", "");
		}
		if (lowercase.contains("winter")) {
			d.addModifier("season");
			d.addModifier("winter");

			d.updateString("winter", "");
			d.updateString("Winter", "");
		}
		if (lowercase.contains("summer")) {
			d.addModifier("season");
			d.addModifier("summer");

			d.updateString("summer", "");
			d.updateString("Summer", "");
		}

		/**
		 * Trim out extra punctuation 
		 */
		d.trimString();
		
	}
	
	private void parsePostprocess(int i) {
		// Check to see if this date is incorrectly too low compared with the first one
		SNACDate cur = dates.get(i);
		if (cur.isToDate() && cur.getDate() != null) { // We are past the first date, but in a date range
			int year1 = dates.get(i - 1).getYear();
			int year2 = cur.getYear();
			
			if (year2 < year1) {
				// there is a problem with this date range, it's backwards
				if (year2 < 10) {
					// fix up based on single digit
					int diff = year2 - (year1 % 10);
					cur.setYear(year1 + diff);
				} else if (year2 < 100) {
					// fix up based on double digit
					int diff = year2 - (year1 % 100);
					cur.setYear(year1 + diff);
				}
			}
		}
		
	}
	
	private void parseDate(SNACDate d) {
		
		// preprocess the date string, including handling boundary cases and special date types.
		parsePreprocess(d);
		d.parseDate();
		d.updateOutputFormat();
	}
	

}
