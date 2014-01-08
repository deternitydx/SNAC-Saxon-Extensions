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
package edu.virginia.iath.snac.functions;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringReader;
import java.net.Socket;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.xml.transform.stream.StreamSource;

import org.apache.commons.lang3.StringEscapeUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import edu.virginia.iath.snac.helpers.DateParserHelper;
import edu.virginia.iath.snac.helpers.SNACDate;

//Saxon Imports
import net.sf.saxon.lib.ExtensionFunctionCall;
import net.sf.saxon.lib.ExtensionFunctionDefinition;
import net.sf.saxon.om.Sequence;
import net.sf.saxon.om.StructuredQName;
import net.sf.saxon.expr.XPathContext;
import net.sf.saxon.s9api.DocumentBuilder;
import net.sf.saxon.s9api.Processor;
import net.sf.saxon.s9api.XdmNode;
import net.sf.saxon.trans.XPathException;


/**
 * Date parser SAXON extension.
 * 
 * @author Robbie Hott
 *
 */
public class GeoNamesCheshire extends ExtensionFunctionDefinition {

	/**
	 * Required serializer field
	 */
	private static final long serialVersionUID = -7035621335523088267L;



	/**
	 * Define the function name for saxon's XSLT parser
	 */
	@Override
	public StructuredQName getFunctionQName()
	{
		return new StructuredQName("saxext", "http://example.com/saxon-extension", "geonames-cheshire");
	}

	/**
	 * Define the argument types accepted by the function
	 */
	@Override
	public net.sf.saxon.value.SequenceType[] getArgumentTypes()
	{
		return new net.sf.saxon.value.SequenceType[] {
				net.sf.saxon.value.SequenceType.SINGLE_STRING	// First and only parameter is a single string
		};
	}

	/**
	 * Define the result type returned by the function
	 */
	@Override
	public net.sf.saxon.value.SequenceType getResultType(net.sf.saxon.value.SequenceType[] suppliedArgumentTypes)
	{
		return net.sf.saxon.value.SequenceType.ANY_SEQUENCE;	// Returns a sequence (of strings)
	}

	/**
	 * Method that instantiates the Extension's function
	 */
	@Override
	public ExtensionFunctionCall makeCallExpression()
	{
		return new FunctionCall();
	}

	private static class FunctionCall extends ExtensionFunctionCall
	{
		/**
		 * Required serialization string
		 */
		private static final long serialVersionUID = -5561386421950940517L;

		/**
		 * Function call method.  This is what actually performs the action of the function call
		 * 
		 * The function will return valid XML, consisting of:
		 * 
		 * <return>
		 * 		<date>...</date>
		 * 		...
		 * </return>
		 * 
		 * or
		 * 
		 * <return>
		 * 	  <dateRange>
		 * 		<fromDate>...</fromDate>
		 * 		<toDate>...</toDate>
		 * 	  </dateRange>
		 * 	  ...
		 * </return>
		 * 
		 * or a combination of dates and dateRanges inside a return.
		 * 
		 * 
		 * @param XPathContext context the context of the call
		 * @param Sequence[] arguments the arguments supplied to the call
		 * @return Sequence the output of the call
		 * 
		 */
		@Override
		public Sequence call(XPathContext context, Sequence[] arguments)
		{
			Sequence seq = null;
			String xml = "";
			String result = "";
			GeoNamesHelper helper = new GeoNamesHelper();

			// Read in the argument into a string
			String locationStr = null;
			try {
				locationStr = arguments[0].iterate().next().getStringValue();
			} catch (XPathException e) {
				locationStr = "";
			}

			try
			{

				Socket cheshire = new Socket("localhost", 12345);
				PrintWriter out =
						new PrintWriter(cheshire.getOutputStream(), true);
				BufferedReader in =
						new BufferedReader(
								new InputStreamReader(cheshire.getInputStream()));

				// Init cheshire
				out.println("init");
				System.err.println(in.readLine());

				// Saxon is WONDERFUL and removes escaped characters, so we must re-escape them
				// Using the Apache Commons Lang's org.apache.commons.lang3.StringEscapeUtils
				locationStr = StringEscapeUtils.escapeXml(locationStr);
				//Normalize the string
				locationStr = locationStr.toLowerCase().replaceAll("\\.", "");
				// Clean up the string
				locationStr = locationStr.replace("(", "");
				locationStr = locationStr.replace(")", "");
				locationStr = locationStr.replace("]", "");
				locationStr = locationStr.replace("[", "");
				
				// Maps of relevant places (countries and US states)
				Map<String, String> countries = helper.getCountries();
				Map<String, String> states = helper.getStates();
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
					out.println("find exactname @ '"+ locationStr +"' and admin1 '"+ states.get(locationStr) +"'");
					System.err.println("Searched for state code: " + states.get(locationStr) + " and state name: " + locationStr);
					String info = in.readLine();
					System.err.println(info);
				} else { // no country or state, do more in-depth queries

					String first = locationStr;
					String second = locationStr;
					if (locationStr.contains(",")) {
						first = locationStr.substring(0, locationStr.indexOf(","));
						second = locationStr.substring(locationStr.indexOf(",")+1, locationStr.length());
						second = second.trim();
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
					out.println("find exactname '" + first + "' and admin1 '" + second + "'");
					String info = in.readLine();
					System.err.println(info);
					if (info.contains(" 0")) {

						// If we have something that may be a "city,st", let's look that up now just in case
						if (!first.equals(second)) {
							String stateSN = helper.checkForUSState(first, second);

							if (stateSN != null) {
								// Do the query
								out.println("find exactname '" + first + "' and admin1 '" + stateSN + "'");
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
									out.println("find xcountry @ '" + countries.get(second) + "' and xintlname @ '" + first +"'");
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
				} // end else

				// Ask for the top entry
				out.println("display default 1 1");
				in.skip(17);
				while (in.ready()) {
					result += in.readLine();
				}

				// close cheshire
				out.println("close");

				// cleanup the result
				result = result.substring(0, result.length()-1);

				// Build an XML object out of the results
				xml = "<return original=\""+locationStr+"\">";
				xml += "<![CDATA[" + result + "]]>";
				xml += "</return>";



			}
			catch (Exception sae)
			{
				// If something went wrong, then just return the value "unparseable" to Saxon.
				// nothing was parsed
				xml = "<return>\n";
				xml += "</return>";
			}


			// Parse XML into an XdmNode
			Processor proc = new Processor(false);
			DocumentBuilder builder = proc.newDocumentBuilder();

			try {
				XdmNode xdm = builder.build(new StreamSource(new StringReader(xml)));
				seq = xdm.getUnderlyingValue();
			} catch (Exception sae) {}

			return seq;

		}


	}
}
