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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import edu.virginia.iath.snac.helpers.datastructures.SNACDate;

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

	

	/**
	 * Constructor.  Initialize this date object with the parameter string.  Automatically runs the parser
	 * to gather information about the date contained in the parameter.
	 * 
	 * @param d Date string to parse.
	 */
	public DateParserHelper(String d) {
		// Initialize dates
		dates = new ArrayList<SNACDate>();
		
		// Store the date string locally
		original = d.trim();
		
		// Parse the dates into Date objects
		runParser();
	}
	
	/**
	 * Get the original date string.
	 * 
	 * @return Date string passed to the constructor.
	 */
	public String getOriginalDate() {
		return original;
	}
	
	/**
	 * Date Parser. This is the heart of the date processor, it runs all the preprocessing, splits the string based on the assumptions
	 * below, parses and creates <code>SNACDate</code> objects for each date in the input string.
	 * 
	 * Assumptions in processing:
	 * <ol>
	 * <li> commas take priority only when separating 4-digit years or 4-digit years with ranges
	 * <li> "and"s take second priority, stating that there are multiple dates or date ranges
	 * <li> "-"s and "through"s take next priority, denoting date ranges
	 * <li> Since New Style and Old Style are ambiguous (Julian vs Gregorian vs ??), we'll ignore them completely
	 * </ol>
	 */
	private void runParser() {
		dateStringPreprocess();
		
		/**
		 * Some important assumptions made in the processing stage
		 * 
		 * 1) commas take priority only when separating 4-digit years or 4-digit years with ranges
		 * 2) "and"s take second priority, stating that there are multiple dates or date ranges
		 * 3) "-"s and "through"s take next priority, denoting date ranges
		 * 
		 * Since New Style and Old Style are ambiguous (Julian vs Gregorian vs ??), we'll ignore them completely
		 * 
		 */
		ArrayList<String> tokens = new ArrayList<String>();
		ArrayList<String> tokens2 = new ArrayList<String>();
		// Parse the string on Commas
		String[] possibilities = original.split("[ .]*,[ .]*");
		boolean allYears = true;
		for (int i = 0; i < possibilities.length; i++) {
			if (possibilities[i].trim().matches("\\d{3}\\d*") || possibilities[i].trim().matches("[.]*\\d{3}\\d*[ .]*[-‐][ .]*\\d{3}\\d*[.]*"))
				tokens.add(possibilities[i]);
			else {
				allYears = false;
				break;
			}
		}
		// If the commas only separate years or ranges, then each of them may be added
		if (!allYears) {
			tokens.clear();
			tokens.add(original);
		}
		
		// For each token, split on "and", adding each new element to tokens2.  This should be fairly straightforward
		for (String token : tokens) {
			String[] indivs = token.trim().split("[ .]*and[ .]*|[ .]*&amp;[ .]*");
			for (int i = 0; i < indivs.length; i++)
				tokens2.add(indivs[i]);
		}
		
		// Check each token for a year.  If one phrase is missing a year, but another has it, then copy it across
		if (tokens2.size() > 1) {
			for (int i = 0; i < tokens2.size(); i++) {
				String supplement = "";
				Pattern p = Pattern.compile("(\\d\\d\\d+)");
				
				// check to ensure there is a number somewhere in the from date
				if (!tokens2.get(i).matches(".*\\d.*")) {
					if (i == 0) { // look at the last element
						Matcher m = p.matcher(tokens2.get(tokens2.size() - 1));
						if (m.find()) {
							supplement += m.group(1) + " ";
						}
					}
					else { // look at the first element
						Matcher m = p.matcher(tokens2.get(0));
						if (m.find()) {
							supplement += m.group(1) + " ";
						}
						
					}
				}
				tokens2.set(i, supplement + tokens2.get(i));
			}
		}
			
		// Reset the pointers
		tokens.clear();
		tokens = tokens2;
		
		// for each token, split on - and "through" and create a date for each
		for (String token : tokens) {
			// pad the token in case date ranges are empty, but exist
			String tmp = " " + token + " ";
			// split on common range indicators
			String[] range = tmp.split("[-‐–]|through");
			
			if (range.length > 1){
				String supplement = "";
				Pattern p = Pattern.compile("(\\d\\d\\d+)");
				
				// check to ensure there is a number somewhere in the from date
				if (!range[0].matches(".*\\d.*") && !range[0].trim().isEmpty()) {
					// If not, grab one from the to date, if possible
					Matcher m = p.matcher(range[1]);
					
					if (m.find()) {
						supplement += m.group(1) + " ";
					}
				}
				dates.add(new SNACDate(supplement + range[0].trim(), SNACDate.FROM_DATE));
				
				// check to ensure there is a number somewhere in the to date
				supplement = "";
				if (!range[1].matches(".*\\d.*") && !range[1].trim().isEmpty()) {
					// If not, grab one from the from date, if possible
					Matcher m = p.matcher(range[0]);
					
					if (m.find()) {
						supplement += m.group(1) + " ";
					}
				}
				dates.add(new SNACDate(supplement + range[1].trim(), SNACDate.TO_DATE));
			} else {
			
				dates.add(new SNACDate(token.trim()));
			}
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

	/**
	 * Checks to see if this date string starts with a date range (ex: 1800-1859)
	 * 
	 * @return True if a date range, false otherwise
	 */
	public boolean isRange() {
		return dates.get(0).isRange();
	}

	/**
	 * Checks whether all portions of this date string was parsed.
	 * 
	 * @return True if all date portions were parsed correctly, false otherwise.
	 */
	public boolean wasParsed() {
		boolean parsed = true;
		for (SNACDate d : dates) {
			parsed &= d.wasParsed();
		}
		return parsed;
	}
	
	/**
	 * Get all the date objects from this helper.
	 * 
	 * @return List of all <code>SNACDate</code> objects parsed from this string.
	 */
	public List<SNACDate> getDates() {
		return dates;
	}

	
	/**
	 * Preprocess the date string.  Performs the following actions:
	 * <ul>
	 * <li>Replace the XML &apos; special characters with actual apostrophes (')
	 * </ul>
	 * 
	 */
	private void dateStringPreprocess() {
		// Handle apostrophes that have been converted
		original = original.replaceAll("&apos;", "'");
		
	}
	
	/**
	 * Preprocess the <code>SNACDate</code> parameter based on certain conditions.  Adds modifiers
	 * based on some conventions:
	 * <ul>
	 * <li> Update Sept to Sep, since it is the only 4-letter month human convention.
	 * <li> Remove brackets.
	 * <li> Remove circa, ca, c, and replace it with the circa modifier.
	 * <li> Look for decades (1800s, 1800's, 180?, 18xx) and add the decade modifier.
	 * <li> Look for fuzzy dates (1800 ?, About 1850) and add the fuzzy modifier.
	 * <li> Look for seasons and add the season modifier.
	 * </ul>
	 * @param d <code>SNACDate</code> to preprocess.
	 */
	private void parsePreprocess(SNACDate d) {

		/**
		 * Fixes for non-standardized date formats
		 */
		// Handle non-standard month representations
		d.setString(d.getString().replace("Sept.", "Sep."));
		d.setString(d.getString().replace("Sept ", "Sep "));
		// Handle dates surrounded with []
		if (d.getString().endsWith("]") && d.getString().startsWith("["))
			d.setString(d.getString().substring(1, d.getString().length() -1));
		
	
		/**
		 * Handling actual date keywords such as circa, centuries, questions, etc
		 */
		// Look for and handle the circa/Circa/... keyword
		if (d.getString().toLowerCase().matches(".*circa.*|.*ca\\..*|^c\\..*|.*\\sc\\..*")) {
			d.addModifier("circa");
			
			d.updateString("circa");
			d.updateString("Circa");
			d.updateString("ca.");
			d.updateString("Ca.");
			d.updateString("c.");
			d.trimString();
		}
		
		// Look for decades (s after the date)
		if (d.getString().matches(".*\\d\\d\\d+'*s.*")) {
			d.addModifier("decade");

			d.setString(d.getString().replaceFirst("(?<=\\d)'*s", ""));
			d.trimString();
		}
		
		if (d.getString().matches("\\s*\\d\\d\\d\\?.*")) {
			// Also a decade
			d.addModifier("decade");
			d.updateString("?", "0");
			d.trimString();
		}
		
		if (d.getString().matches("\\s*\\d\\d+x+.*")) {
			// Also a decade
			d.addModifier("decade");
			d.updateString("x", "0");
			d.trimString();
		}
		
		// Look for fuzzy dates (some form of "[?]", "(?)", ...)
		if (d.getString().contains("?")) {
			d.addModifier("fuzzy");
			
			d.updateString("[?]", "");
			d.updateString("(?)", "");
			d.updateString("?", "");
			d.trimString();
		}

		// Treat "About" as fuzzy as well
		if (d.getString().toLowerCase().contains("about")) {
			d.addModifier("fuzzy");
			
			d.updateString("About");
			d.updateString("about");
			d.trimString();
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
	
	/**
	 * Postprocess the date at position <code>i</code> in the dates list.  If the date is the second part of
	 * a date range, but too low compared with the first date in the range (as in 1800-9), then fix the
	 * second part correctly based on the first (AND the two dates: 1800 and 0009 would be 1809).
	 * 
	 * @param i index of the date to postprocess.
	 */
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
	
	/**
	 * Call the parsing functions for date d.  This includes calling the preprocessor,
	 * the date's parse method, and updating the date's output format (based on which
	 * information is available).
	 * 
	 * @param d Date to be parsed.
	 */
	private void parseDate(SNACDate d) {
		
		// preprocess the date string, including handling boundary cases and special date types.
		parsePreprocess(d);
		d.parseDate();
		d.updateOutputFormat();
	}
	

}
