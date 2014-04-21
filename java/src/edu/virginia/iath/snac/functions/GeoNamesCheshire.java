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

import java.io.StringReader;
import javax.xml.transform.stream.StreamSource;

import edu.virginia.iath.snac.helpers.GeoNamesHelper;

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
 * Cheshire GeoNames SAXON extension.
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

	/**
	 * Define the class that contains the JAVA function call.
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
		 * <pre>
		 * {@code
		 * <return original="Original Query String" score="Confidence">
		 * 		<score>Confidence Score</score>
		 * 		<name>Normalized Place Name</name>
		 * 		<geonameId>GeoNames Place ID</geonameId>
		 * 		<admin1>Administrative 1 Code</admin1>
		 * 		<country>2-digit Country Code</country>
		 * 		<latitude>Latitude</latitude>
		 * 		<longitude>Longitude</longitude>
		 * 		<topResults>
		 * 			<place>
		 * 				<name>Normalized Place Name</name>
		 * 				<geonameId>GeoNames Place ID</geonameId>
		 * 				<admin1>Administrative 1 Code</admin1>
		 * 				<country>2-digit Country Code</country>
		 * 				<latitude>Latitude</latitude>
		 * 				<longitude>Longitude</longitude>
		 * 			</place>
		 * 			...
		 * 		</topResults>
		 * </return>
		 * }
		 * </pre>
		 * 
		 * 
		 * @param context the context of the call
		 * @param arguments the arguments supplied to the call
		 * @return Sequence the GeoNames result(s)
		 * 
		 */
		@Override
		public Sequence call(XPathContext context, Sequence[] arguments)
		{
			Sequence seq = null;
			String xml = "";
			boolean success = false;;
			GeoNamesHelper helper = new GeoNamesHelper();

			// Read in the argument into a string
			String locationStr = null;
			try {
				locationStr = arguments[0].iterate().next().getStringValue();
			} catch (XPathException e) {
				locationStr = "";
			}
			
			// Saxon is WONDERFUL and removes escaped characters, so we must re-escape them
			// Using the Apache Commons Lang's org.apache.commons.lang3.StringEscapeUtils
			locationStr = helper.cleanString(locationStr, false);
			
			// Connect to Cheshire
			helper.connect();
			
			success = helper.queryCheshire(locationStr);
			
			helper.disconnect();

			
			// Build the result
			if (success) {
				// Build an XML object out of the results
				xml = "<return original=\""+locationStr+"\" score=\"" + helper.getConfidence() + "\">";
				xml += "<score>" + helper.getConfidence() + "</score>";
				//xml += "<searchLevel>" + helper.getLevelOfSearch() + "</searchLevel>";
				xml += helper.getTopReturnResult();
				xml += "<otherResults>" + helper.getNextUniqueReturnResults(20) + "</otherResults>";
				
				xml += "</return>";
			}
			else
			{
				// If something went wrong, then just return an empty XML.
				// nothing was parsed
				xml = "<return>\n";
				xml += "</return>";
			}

			xml = xml.replace("&", "&amp;");

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
