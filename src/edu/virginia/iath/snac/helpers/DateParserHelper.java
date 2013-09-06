package edu.virginia.iath.snac.helpers;

import java.text.ParseException;
import java.util.Date;

import org.apache.commons.lang3.time.DateFormatUtils;
import org.apache.commons.lang3.time.DateUtils;

public class DateParserHelper {
	
	private String dateString = null;
	private String date1Str = null;
	private String date2Str = null;
	private Date date1 = null;
	private Date date2 = null;
	private String date1StrModifier = null;
	private String date2StrModifier = null;
	private static String outputFormat = "yyyy-MM-dd";
	

	public DateParserHelper(String dateStr) {
		// Store the date string locally
		dateString = dateStr.trim();
		
		// Parse the dates into Date objects
		runParser();
	}
	
	private void runParser() {
		// Check for date range.  If so, parse separately
		if (dateString.contains("-")) {
			date1Str = dateString.substring(0, dateString.indexOf("-")).trim();
			date2Str = dateString.substring(dateString.indexOf("-") + 1).trim();

			date1 = parseDate(date1Str);
			date2 = parseDate(date2Str);
		} else {
			date1Str = dateString.trim();
			date1 = parseDate(dateString);
		}
	}

	public boolean isRange() {
		return date2 != null;
	}

	public String firstDate() {
		return getOutputString(date1);
	}

	public String secondDate() {
		return getOutputString(date2);
	}

	public String getDate() {
		return getOutputString(date1);
	}
	
	private Date parseDate(String d) {
		try {
			// Currently we are ignoring "-" in the text, since that is used for ranges in dates
			return DateUtils.parseDate(d.trim(),
					"yyyy", /*"yyyy-MM", "yyyy-M", "yyyy-M-d", "yyyy-M-dd", "yyyy-MM-d", "yyyy-MM-dd",*/ // standard dates
					"MMMMM dd, yyyy", "MMM dd, yyyy", "MMM dd yyyy", "MMMMM dd, yyyy", "yyyy MMM dd",
					"dd MMM, yyyy", "dd MMMMM, yyyy", "yyyy, MMM dd", "yyyy, MMMMM dd", "yyyy, MMM. dd",
					"MMMMM yyyy", "MMM yyyy"
					);
		} catch (ParseException e) {
			return null;
		}
	}
	
	private String getOutputString(Date d) {
		return (d == null) ? "unparsable" : DateFormatUtils.format(d, outputFormat);
	}

}
