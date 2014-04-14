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
import java.io.StringReader;
import java.net.URL;

import javax.xml.transform.stream.StreamSource;

import org.apache.commons.lang3.StringEscapeUtils;
import org.json.JSONArray;
import org.json.JSONObject;

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
 * GeoNames web lookup SAXON extension.
 * 
 * @author Robbie Hott
 *
 */
public class GeoNamesWebLookup extends ExtensionFunctionDefinition {

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
		return new StructuredQName("saxext", "http://example.com/saxon-extension", "geonames-weblookup");
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

	/**
	 * Define the class that contains the JAVA function call.
	 * 
	 * @author Robbie Hott
	 *
	 */
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
		 * 		...
		 * </return>
		 * 
		 * 
		 * 
		 * @param context the context of the call
		 * @param arguments the arguments supplied to the call
		 * @return Sequence of GeoNames resulting XML
		 * 
		 */
		@Override
		public Sequence call(XPathContext context, Sequence[] arguments)
		{
			Sequence seq = null;
			String xml = "";
			
			// Read in the argument into a string
			String locationStr = null;
			try {
				locationStr = arguments[0].iterate().next().getStringValue();
			} catch (XPathException e) {
				locationStr = "";
			}
			
			try
			{
				// Saxon is WONDERFUL and removes escaped characters, so we must re-escape them
				// Using the Apache Commons Lang's org.apache.commons.lang3.StringEscapeUtils
				locationStr = StringEscapeUtils.escapeXml(locationStr);
				
				String line; String json = "";
				URL url = new URL("http://api.geonames.org/search?q=" + locationStr + "&maxRows=1&username=rwb3y&type=json");
				BufferedReader in = new BufferedReader(new InputStreamReader(url.openStream()));
				while ((line = in.readLine()) != null) {
					json += line;
				}
				JSONObject obj = new JSONObject(json);
				
				if (obj != null && obj.has("geonames") && !obj.isNull("geonames")) {
					// read the first item
					JSONArray res = obj.getJSONArray("geonames");
					JSONObject cur = res.getJSONObject(0); // read in the first one
					
					// Build an XML object out of the results
					xml = "<return original=\""+locationStr+"\">";
					xml += "<geonamesid>"+cur.getInt("geonameId")+"</geonamesid>";
					xml += "<name>"+cur.getString("name")+"</name>";
					xml += "<admincode1>"+cur.getString("adminCode1")+"</admincode1>";
					xml += "<country>"+cur.getString("countryName")+"</country>";
					xml += "<lat>"+cur.getDouble("lat")+"</lat>";
					xml += "<lon>"+cur.getDouble("lng")+"</lon>";
					xml += "</return>";
					
					
					
				} else {
					// nothing was parsed
					xml = "<return original=\""+locationStr+"\">";
					xml += "</return>";
				}
				


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
