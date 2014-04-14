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
package edu.virginia.iath.snac.helpers.datastructures;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

import org.apache.commons.lang3.time.DateFormatUtils;
import org.apache.commons.lang3.time.DateUtils;

/**
 * Date storage class for SNAC parsing.  This object stores all information about an individual date,
 * including the original string, all modifiers to the date, notbefore and notafter dates, and the
 * output format to convert this date to a string.  This object also contains the code to parse the
 * date string (after pre-processing) into a Calendar object.
 * 
 * @author Robbie Hott
 *
 */
public class SNACDate {
	
	/**
	 * Standalone date.
	 */
	public final static int STANDALONE = 0;
	/**
	 * The first part of a date range.
	 */
	public final static int FROM_DATE = 1;
	/**
	 * The second part of a date range.
	 */
	public final static int TO_DATE = 2;
	
	// The date string that's been simplified
	private String dateStr = null;
	// The original date string for this date
	private String origDateStr = null;
	// The date that's not before
	private Calendar notBefore = null;
	// The date that's not after
	private Calendar notAfter = null;
	// the date value of this date
	private Calendar date = null;
	// Modifiers for this date string
	private ArrayList<String> dateStrModifier = null;
	// Output format for this date object
	private String outputFormat = "yyyy-MM-dd";
	// Whether or not this date was parsed
	private boolean parsed = false;
	// If this date is part of a series or individual
	private int type = 0;
	
	/**
	 * Constructor.  Creates a standalone <code>SNACDate</code> with the
	 * date string.
	 * 
	 * @param date Date string to be parsed in this object.
	 */
	public SNACDate(String date) {
		origDateStr = dateStr = date;
		dateStrModifier = new ArrayList<String>();
		type = SNACDate.STANDALONE;
	}
	
	/**
	 * Constructor. Creates a <code>SNACDate</code> object of type <code>t</code>
	 * with the date string.
	 * 
	 * @param date Date string to be parsed in this object.
	 * @param t Type of date to create (Standalone, FromDate, ToDate)
	 */
	public SNACDate(String date, int t) {
		origDateStr = dateStr = date;
		dateStrModifier = new ArrayList<String>();
		type = t;
	}
	
	/**
	 * Checks to see if this date is part of a range.
	 * 
	 * @return True if part of a range, false otherwise.
	 */
	public boolean isRange() {
		return type > 0;
	}
	
	/**
	 * Checks to see if this date is the end of a date range.
	 * 
	 * @return True if end of a date range, false otherwise.
	 */
	public boolean isToDate() {
		return type == SNACDate.TO_DATE;
	}
	
	/**
	 * Checks to see if this date was parsed correctly.
	 * 
	 * @return True if parsed, false otherwise.
	 */
	public boolean wasParsed() {
		return parsed;
	}
	
	
	
	
	/**
	 * Updates the output format of the string. Use this method after parsing the dates, but
	 * before the getting the String of the normalized date.  This method formats the date string
	 * according to the information available.  If there is a season, it does not modify. If there
	 * is a decade, it sets the date to return only years.  If there is month or date information, it
	 * sets the date to return those appropriately.
	 */
	public void updateOutputFormat() {
		if (dateStrModifier.contains("season"))
			return;
		
		if (dateStrModifier.contains("decade")) {
			outputFormat = "yyyy";
			return;
		}
		
		//Lower case the string, split on "or" in case this is a range, then count the number of spaces.
		switch(dateStr.toLowerCase().split("or")[0].trim().split("[\\s.,-]+").length) {
			case 1:
				outputFormat = "yyyy";
				break;
			case 2:
				outputFormat = "yyyy-MM";
				break;
			default:
				outputFormat = "yyyy-MM-dd";	
		}
	}
	
	/**
	 * Validates this date as truly parsed.  Sets parsed to true.
	 */
	public void validateParsed() {
		parsed = true;
	}
	
	/**
	 * Parses this <code>SNACDate</code>'s date string into the correct Java Calendar objects for the date.
	 * If there is an <code>or</code> in the date string, it parses into not-before and not-after dates (with a null
	 * exact date).  Else, it parses the date directly.  If the date was unable to be parsed, it sets the parsed date
	 * to null.  Empty to-dates (end of a range) are allowed.
	 * 
	 * @return True if date was successfully parsed, false otherwise.
	 */
	public boolean parseDate() {
		
		try {
			// check for OR, which means parse only the date, but as notBefore/notAfter
			if (dateStr.toLowerCase().contains("or")) {
				notBefore = parseDate(dateStr.substring(0, dateStr.toLowerCase().indexOf("or")));
				notAfter = parseDate(dateStr.substring(dateStr.toLowerCase().indexOf("or") + 2));
				date = null;
			} else {
				date = parseDate(dateStr);
			}
			parsed = true;
			return true;
		} catch (Exception e) {
			date = null;
		}
		// if this is a to-date, then even empty should be allowed to be parsed.
		if (type == SNACDate.TO_DATE) {
			parsed = true;
			return true;
		}
		return false;
	}
	
	/**
	 * Parses the parameter string into a Java Calendar object.  This meothod uses Apache's date parser utilities
	 * to catch multiple forms of dates available.  This prioritizes for year first, then combinations of month,
	 * day, and year in most human writeable forms.
	 * 
	 * @param str String to be parsed.
	 * @return Calendar object of the given date string.
	 * @throws ParseException If date cannot be parsed, this method throws the ParseExeption.
	 */
	private Calendar parseDate(String str) throws ParseException {
		Calendar date;
		date = Calendar.getInstance();
			
		date.setTime(DateUtils.parseDate(str.trim(),
				"yyyy", "yyyy,", /*"yyyy-MM", "yyyy-M", "yyyy-M-d", "yyyy-M-dd", "yyyy-MM-d", "yyyy-MM-dd",*/ // standard dates
				"MMMMM dd, yyyy", "MMM dd, yyyy", "MMM. d, yyyy", "MMM dd yyyy", "MMM dd,yyyy", "MMMdd, yyyy",
				"MMMMM dd, yyyy", "yyyy MMM dd", "yyyy MMM. dd", "dd MMM, yyyy", "dd MMMMM, yyyy", "yyyy, MMM dd",
				"yyyy, MMMMM dd", "yyyy, MMM. dd", "MMM. yyyy", "MMMMM yyyy", "MMMMM, yyyy", "MMM yyyy", 
				"MMM, yyyy", "yyyy, MMM. d", "yyyy, MMMMM d", "yyyy, MMM", "yyyy, MMM.", "yyyy, MMMMM",
				"yyyy, dd MMM.", "yyyy, dd MMMMM", "yyyy, dd MMM", "yyyy, MMM.dd", "yyyy,MMM.dd", "yyyy,MMM. dd",
				"yyyy, MMMd", "yyyy, MMMMMd", "yyyy, MMM.d", "yyyyMMMd", "yyyyMMMMMd", "yyyy, MMM, d", "yyyy. MMM. d",
				"yyyy MMM", "yyyy, MMM.", "yyyy MMMMM", "yyyy, MMMMM", "yyyy,MMMMM dd", "yyyy,MMM dd", "yyyy,MMM. dd",
				"yyyy. MMM", "yyyy. MMM.", "yyyy. MMMMM", "yyyy. MMM d", "yyyy. MMMMM d", "yyyy. MMM. d"
				));
		return date;
	}
	
	/**
	 * Handle all the modifiers on this string.  This should be called as a post-processing step
	 * after the exact date has been processed.  It adds not-before and not-after dates based on the
	 * exact date and the following criteria:
	 * <ul>
	 * <li> If the circa modifier is applied, give +/- 3 years
	 * <li> If the fuzzy modifier is applied, give +/- 1 year
	 * <li> If the decade modifier is applied and the year is a multiple of 100, it's a century, give
	 * a range of exact date to exact date plus 99 years (ex: 1800-1899). Clears the exact date.
	 * <li> If the decade modifier is applied and the year is a multiple of 10, it's a decade, give
	 * a range of exact date to exact date plus 9 years (ex: 1810-1819). Clears the exact date.
	 * <li> If the season modifier is applied, look up the season dates and use those as the
	 * range.  Clears the exact date.
	 * </ul>
	 */
	public void handleModifiers() {
		if (!dateStrModifier.isEmpty()) {
			if (dateStrModifier.contains("circa")) {
				notBefore = Calendar.getInstance();
				notBefore.setTime(date.getTime());
				notBefore.add(Calendar.YEAR, -3);
				notAfter = Calendar.getInstance();
				notAfter.setTime(date.getTime());
				notAfter.add(Calendar.YEAR, 3);
				
			}
			

			if (dateStrModifier.contains("fuzzy")) {
				notBefore = Calendar.getInstance();
				notBefore.setTime(date.getTime());
				notBefore.add(Calendar.YEAR, -1);
				notAfter = Calendar.getInstance();
				notAfter.setTime(date.getTime());
				notAfter.add(Calendar.YEAR, 1);
				
			}
			
			if (dateStrModifier.contains("decade")) {
				// Create a calendar for this date
				int year = date.get(Calendar.YEAR);
				if (year % 100 == 0) { // dealing with centuries
					notBefore = Calendar.getInstance();
					notBefore.setTime(date.getTime());
					notAfter = Calendar.getInstance();
					notAfter.setTime(date.getTime());
					notAfter.add(Calendar.YEAR, 99);
				} else if (year % 10 == 0) { // dealing with decades
					notBefore = Calendar.getInstance();
					notBefore.setTime(date.getTime());
					notAfter = Calendar.getInstance();
					notAfter.setTime(date.getTime());
					notAfter.add(Calendar.YEAR, 9);
				}
				
				// Clear the date if it's a decade
				date = null;
			}
			
			if (dateStrModifier.contains("season")) {
				String season = dateStrModifier.get(dateStrModifier.indexOf("season") + 1);
				int year = date.get(Calendar.YEAR);
				
				Calendar[] seasonDates = getSeasonDates(season, year);
				notBefore = seasonDates[0];
				notAfter = seasonDates[1];
				
				// Clear the date if it's actually a season 
				date = null;
			}
			
			
		}
	}
	
	/**
	 * Gets the season dates for the given season and year.  Since the season dates only change
	 * +/- 2 days across most of time, we store a lookup table and calculate them directly.
	 * Winter starts in December of the previous year and ends in the given year.
	 * 
	 * @param seasonStr Season to lookup (summer, spring, fall, autumn, winter)
	 * @param year 4-digit year.
	 * @return Array of Java Calendar objects containing the beginning and end dates of the season.
	 */
	private Calendar[] getSeasonDates(String seasonStr, int year) {
		Calendar[] seasonDates = new Calendar[2];
		String season = seasonStr.toLowerCase().trim();
		
		seasonDates[0] = Calendar.getInstance();
		seasonDates[1] = Calendar.getInstance();
		
		// Note: Java is WEIRD:  0 = JANUARY, 1 = FEBRUARY, ...
		if (season.equals("winter")) {
			seasonDates[0].set(year - 1, Calendar.DECEMBER, 21);
			seasonDates[1].set(year, Calendar.MARCH, 19);
			
		} else if (season.equals("spring")) {
			seasonDates[0].set(year, Calendar.MARCH, 20);
			seasonDates[1].set(year, Calendar.JUNE, 20);
			
		} else if (season.equals("fall") || season.equals("autumn")) {
			seasonDates[0].set(year, Calendar.SEPTEMBER, 22);
			seasonDates[1].set(year, Calendar.DECEMBER, 20);
			
		} else if (season.equals("summer")) {
			seasonDates[0].set(year, Calendar.JUNE, 21);
			seasonDates[1].set(year, Calendar.SEPTEMBER, 21);
			
		}
		
		return seasonDates;
	}

	/**
	 * Gets the cleaned-up version of the original date string stored.
	 * 
	 * @return Cleaned version of the date string.
	 */
	public String getString() {
		return dateStr;
	}

	/**
	 * Replace the cleaned-up version of the date string with the parameter.
	 * 
	 * @param replace String with which to replace the date string.
	 */
	public void setString(String replace) {
		dateStr = replace;
	}
	
	/**
	 * Add the given modifier to this <code>SNACDate</code> object. Valid modifiers are
	 * decade, season, season identifiers (spring, winter, summer, fall, autumn), fuzzy,
	 * and circa.
	 * 
	 * @param modifier Modifier string to apply.
	 */
	public void addModifier(String modifier) {
		dateStrModifier.add(modifier);
	}
	
	/**
	 * Run a find-replace on the date string to clean it up.
	 * 
	 * @param find Regular expression to search for.
	 * @param replace String with which to replace each find instance.
	 */
	public void updateString(String find, String replace) {
		dateStr = dateStr.replace(find, replace);
	}
	
	/**
	 * Remove instances of <code>find<code> from the date string to clean it up.
	 * 
	 * @param find Regular expression for sequences to remove.
	 */
	public void updateString(String find) {
		updateString(find, "");
	}
	
	/**
	 * Trim out all white space, brackets, parentheses, apostrophes, colons, extra
	 * spaces, periods, and commas from the date string.
	 */
	public void trimString() {

		// Quick fixes, including ending with a period
		if (dateStr.endsWith("."))
			dateStr = dateStr.substring(0, dateStr.length() -1);
		if (dateStr.endsWith(","))
			dateStr = dateStr.substring(0, dateStr.length() -1);
		dateStr = dateStr.replace("(", "");
		dateStr = dateStr.replace(")", "");
		dateStr = dateStr.replace("[", "");
		dateStr = dateStr.replace("]", "");
		dateStr = dateStr.replace("'", "");
		dateStr = dateStr.replace(":", "");
		dateStr = dateStr.replace("  ", " ");
		
		// Trim down before returning, just to be sure.
		dateStr = dateStr.trim();
	}
	
	/**
	 * Get the date object (parsed) from this <code>SNACDate</code>.
	 * @return Date of the parsed string, or null if unparsed.
	 */
	public Date getDate() {
		if (date != null)
			return date.getTime();
		return null;
	}
	
	/**
	 * Get the year of the parsed date.
	 * 
	 * @return Year of the parsed date.
	 */
	public int getYear() {
		return date.get(Calendar.YEAR);
	}
	
	/**
	 * Update the year of the parsed date to the parameter.
	 * 
	 * @param year Year to update the current parsed object
	 */
	public void setYear(int year) {
		date.set(Calendar.YEAR, year);
	}
	
	/**
	 * Get the formatted date string from a given Calendar object.  This uses the output format
	 * set up using the <code>updateOutputFormat</code> method.
	 * 
	 * @param d Calendar object to be parsed to a String.
	 * @return Normalized String of the parsed date.
	 */
	private String formattedDate(Calendar d) {
		return (d == null) ? "null" : DateFormatUtils.format(d.getTime(), outputFormat);
	}
	
	@Override
	public String toString() {
		return "{" + getString() + ": " + getParsedDate() + " (" + getNotBefore() + "--" + getNotAfter() + ") "+ dateStrModifier+ "}";
	}
	
	/**
	 * Get the normalized version of the parsed date as a String.
	 * 
	 * @return Parsed date string, null if not parsed.
	 */
	public String getParsedDate() {
		return formattedDate(date);
	}
	
	/**
	 * Get the normalized version of the parsed not-before date as a String.
	 * 
	 * @return Parsed date string, null if not parsed.
	 */
	public String getNotBefore() {
		return formattedDate(notBefore);
	}
	
	/**
	 * Get the normalized version of the parsed not-after date as a String.
	 * 
	 * @return Parsed date string, null if not parsed.
	 */
	public String getNotAfter() {
		return formattedDate(notAfter);
	}
	
	/**
	 * Get the original uncleaned date string used to create this object.
	 * 
	 * @return Original date String.
	 */
	public String getOriginalDate() {
		return origDateStr;
	}
	
	/**
	 * Get the type of this <code>SNACDate</code> object: Standalone, FromDate, ToDate.
	 * 
	 * @return Type of date (as constant).
	 */
	public int getType() {
		return type;
	}
	

}
