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

import java.text.ParseException;
import java.util.Calendar;
import java.util.Date;

import org.apache.commons.lang3.time.DateFormatUtils;
import org.apache.commons.lang3.time.DateUtils;

/**
 * Date parser for Java based on conventions used in SNAC.   Given a string, tries multiple parsings for a date,
 * converting them into a Java Date object.  Dates are returned in an ISO standard format, YYYY-MM-DD.
 * 
 * @author Robbie Hott
 *
 */
public class DateParserHelper {
	
	private String dateString = null;
	private String[] dateStr = null;
	private Date[] dates = null;
	private String[] dateStrModifier = null;
	private static String outputFormat = "yyyy-MM-dd";
	

	public DateParserHelper(String d) {
		dates = new Date[2];
		dateStr = new String[2];
		dateStrModifier = new String[2];
		
		// Store the date string locally
		dateString = d.trim();
		
		// Parse the dates into Date objects
		runParser();
	}
	
	private void runParser() {
		// Check for date range.  If so, parse separately
		if (dateString.contains("-")) {
			dateStr[0] = dateString.substring(0, dateString.indexOf("-")).trim();
			dateStr[1] = dateString.substring(dateString.indexOf("-") + 1).trim();
			
			// parse the two dates
			parseDate(0);
			parseDate(1);
		} else {
			dateStr[0] = dateString.trim();
			dates[1] = null;
			dateStr[1] = null;
			
			// parse the date
			parseDate(0);
		}
	}

	public boolean isRange() {
		return dates[1] != null;
	}

	public String firstDate() {
		return getOutputString(dates[0]) + handleModifier(0);
	}

	public String secondDate() {
		return getOutputString(dates[1]) + handleModifier(0);
	}

	public String getDate() {
		return getOutputString(dates[0]) + handleModifier(0);
	}
	
	private void parseDate(int i) {
		dateStrModifier[i] = null;
		
		if (dateStr[i].contains("circa")) {
			dateStrModifier[i] = "circa";
			dateStr[i] = dateStr[i].replace("circa", "");
		}
		
		try {
			// Currently we are ignoring "-" in the text, since that is used for ranges in dates
			dates[i] = DateUtils.parseDate(dateStr[i].trim(),
					"yyyy", /*"yyyy-MM", "yyyy-M", "yyyy-M-d", "yyyy-M-dd", "yyyy-MM-d", "yyyy-MM-dd",*/ // standard dates
					"MMMMM dd, yyyy", "MMM dd, yyyy", "MMM dd yyyy", "MMMMM dd, yyyy", "yyyy MMM dd",
					"dd MMM, yyyy", "dd MMMMM, yyyy", "yyyy, MMM dd", "yyyy, MMMMM dd", "yyyy, MMM. dd",
					"MMMMM yyyy", "MMM yyyy"
					);
		} catch (ParseException e) {
			dates[i] = null;
		}
	}
	
	private String handleModifier(int i) {
		String output = "";
		if (dateStrModifier[i] != null) {
			output += " ";
			if (dateStrModifier[i].equals("circa")) {
				Calendar d = Calendar.getInstance();
				d.setTime(dates[i]);
				d.add(Calendar.YEAR, -3);
				output += "( " + DateFormatUtils.format(d.getTime(), outputFormat) + " -- ";
				d.setTime(dates[i]);
				d.add(Calendar.YEAR, 3);
				output += DateFormatUtils.format(d.getTime(), outputFormat) + ")";
				
			}
		}
		return output;
	}
	
	private String getOutputString(Date d) {
		return (d == null) ? "unparsable" : DateFormatUtils.format(d, outputFormat);
	}

}
