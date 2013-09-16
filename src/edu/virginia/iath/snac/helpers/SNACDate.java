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
	
	public final static int STANDALONE = 0;
	public final static int FROM_DATE = 1;
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
	
	public SNACDate(String date) {
		origDateStr = dateStr = date;
		dateStrModifier = new ArrayList<String>();
		type = 0;
	}
	
	public SNACDate(String date, int t) {
		origDateStr = dateStr = date;
		dateStrModifier = new ArrayList<String>();
		type = t;
	}
	
	public boolean isRange() {
		return type > 0;
	}
	
	public boolean isToDate() {
		return type == this.TO_DATE;
	}
	
	public boolean wasParsed() {
		return parsed;
	}
	
	
	
	
	public void updateOutputFormat() {
		if (dateStrModifier.contains("season"))
			return;
		
		if (dateStrModifier.contains("decade")) {
			outputFormat = "yyyy";
			return;
		}
		
		switch(dateStr.split("[\\s.,-]+").length) {
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
	
	public void validateParsed() {
		parsed = true;
	}
	
	public boolean parseDate() {
		try {
			date = Calendar.getInstance();
			
			date.setTime(DateUtils.parseDate(dateStr.trim(),
				"yyyy", "yyyy,", /*"yyyy-MM", "yyyy-M", "yyyy-M-d", "yyyy-M-dd", "yyyy-MM-d", "yyyy-MM-dd",*/ // standard dates
				"MMMMM dd, yyyy", "MMM dd, yyyy", "MMM. d, yyyy", "MMM dd yyyy", "MMMMM dd, yyyy", "yyyy MMM dd", "yyyy MMM. dd",
				"dd MMM, yyyy", "dd MMMMM, yyyy", "yyyy, MMM dd", "yyyy, MMMMM dd", "yyyy, MMM. dd",
				"MMMMM yyyy", "MMM yyyy", "yyyy, MMM. d", "yyyy, MMMMM d", "yyyy, MMM", "yyyy, MMM.", "yyyy, MMMMM",
				"yyyy, dd MMM.", "yyyy, dd MMMMM", "yyyy, dd MMM", "yyyy, MMM.dd", "yyyy,MMM.dd", "yyyy,MMM. dd",
				"yyyy, MMMd", "yyyy, MMMMMd", "yyyy, MMM.d", "yyyyMMMd", "yyyyMMMMMd", "yyyy, MMM, d", "yyyy. MMM. d",
				"yyyy MMM", "yyyy, MMM.", "yyyy MMMMM", "yyyy, MMMMM", "yyyy,MMMMM dd", "yyyy,MMM dd", "yyyy,MMM. dd"
				));
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
	
	private Calendar[] getSeasonDates(String seasonStr, int year) {
		Calendar[] seasonDates = new Calendar[2];
		String season = seasonStr.toLowerCase().trim();
		
		seasonDates[0] = Calendar.getInstance();
		seasonDates[1] = Calendar.getInstance();
		
		// Note: Java is WEIRD:  0 = JANUARY, 1 = FEBRUARY, ...
		if (season.equals("winter")) {
			seasonDates[0].set(year, Calendar.DECEMBER, 21);
			seasonDates[1].set(year + 1, Calendar.MARCH, 19);
			
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

	public String getString() {
		// TODO Auto-generated method stub
		return dateStr;
	}

	public void setString(String replace) {
		// TODO Auto-generated method stub
		dateStr = replace;
	}
	
	public void addModifier(String modifier) {
		dateStrModifier.add(modifier);
	}
	
	public void updateString(String find, String replace) {
		dateStr = dateStr.replace(find, replace);
	}
	
	public void updateString(String find) {
		dateStr = dateStr.replace(find, "");
	}
	
	public void trimString() {

		// Quick fixes, including ending with a period
		if (dateStr.endsWith("."))
			dateStr = dateStr.substring(0, dateStr.length() -1);
		if (dateStr.endsWith(","))
			dateStr = dateStr.substring(0, dateStr.length() -1);
		dateStr = dateStr.replace("(", "");
		dateStr = dateStr.replace(")", "");
		dateStr = dateStr.replace("'", "");
		dateStr = dateStr.replace(":", "");
		dateStr = dateStr.replace("  ", " ");
		
		// Trim down before returning, just to be sure.
		dateStr = dateStr.trim();
	}
	
	public Date getDate() {
		if (date != null)
			return date.getTime();
		return null;
	}
	
	public int getYear() {
		return date.get(Calendar.YEAR);
	}
	
	public void setYear(int year) {
		date.set(Calendar.YEAR, year);
	}
	
	private String formattedDate(Calendar d) {
		return (d == null) ? "null" : DateFormatUtils.format(d.getTime(), outputFormat);
	}
	
	@Override
	public String toString() {
		return "{" + getParsedDate() + " (" + getNotBefore() + "--" + getNotAfter() + ")}";
	}
	
	public String getParsedDate() {
		return formattedDate(date);
	}
	
	public String getNotBefore() {
		return formattedDate(notBefore);
	}
	
	public String getNotAfter() {
		return formattedDate(notAfter);
	}
	
	public String getOriginalDate() {
		return origDateStr;
	}
	
	public int getType() {
		return type;
	}
	

}
