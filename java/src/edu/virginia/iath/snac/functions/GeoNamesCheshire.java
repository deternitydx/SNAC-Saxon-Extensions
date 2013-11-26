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

				// Check to see if we have a country!
				Map<String, String> countries = new HashMap<String, String>();
				for (String iso : Locale.getISOCountries()) {
					Locale l = new Locale("", iso);
					countries.put(l.getDisplayCountry().toLowerCase(), iso);
				}
				if (countries.containsKey(locationStr)) { // we have a country!
					// Do a simple country look up

					out.println("find xcountry @ \"" + countries.get(locationStr) + "\" and xintlname @ \"" + locationStr +"\"");
					System.err.println("Searched for country code: " + countries.get(locationStr) + " and country:sd " + locationStr);
					String info = in.readLine();
					System.err.println(info);
				} else { // no country, do more in-depth queries

					String first = locationStr;
					String second = locationStr;
					if (locationStr.contains(",")) {
						first = locationStr.substring(0, locationStr.indexOf(","));
						second = locationStr.substring(locationStr.indexOf(","), locationStr.length());
					}

					System.err.println("Searching for: " + locationStr + " as 1." + first + "; 2." + second);
					// Query cheshire

					// Do exact query first
					out.println("find exactname @ \"" + first + "\" and admin1 @ \"" + second + "\"");
					String info = in.readLine();
					System.err.println(info);
					if (info.contains(" 0")) {

						// Next, try a ranking name query by keyword
						out.println("find name @ \"" + first + "\" and admin1 @ \"" + second + "\"");
						info = in.readLine();
						System.err.println(info);
						if (info.contains(" 0")) {

							// TODO: Should in here try:
							// 1. find ngram_name_wadmin and exactname (find the exact name but with ngrams for the admin code)
							// 2. find ngram_name_wadmin and admin1 search (no alternate names)

							// (1 above) Next, try a query on just ngrams in the name/admin code plus ranking of exact name (for bad state names)
							out.println("find ngram_name_wadmin \"" + locationStr + "\" and exactname @ \"" + first + "\"");
							info = in.readLine();
							System.err.println(info);
							if (info.contains(" 0")) { 

								// Next, try a looking for matching ngrams
								out.println("find ngram_wadmin \"" + locationStr + "\" and name_wadmin @ \"" + locationStr + "\"");
								info = in.readLine();
								System.err.println(info);
								if (info.contains(" 0")) {
									// Next, try looking for just ngrams and keyword name
									out.println("find ngram_wadmin \"" + locationStr + "\" and name @ \"" + first + "\"");
									info = in.readLine();
									System.err.println(info);

									if (info.contains(" 0")) {
										// Finally, just check ngrams
										out.println("find ngram_wadmin \"" + locationStr + "\"");
										info = in.readLine();
										System.err.println(info);
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
