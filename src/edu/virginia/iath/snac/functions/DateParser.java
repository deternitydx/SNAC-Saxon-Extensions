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
import java.util.List;

import javax.xml.transform.stream.StreamSource;

import org.apache.commons.lang3.StringEscapeUtils;

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
public class DateParser extends ExtensionFunctionDefinition {

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
		return new StructuredQName("saxext", "http://example.com/saxon-extension", "date-parser");
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
		 * or a combination of dates and dateRanges inside a dateSet.
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
			
			// Read in the argument into a string
			String dateStr = null;
			try {
				dateStr = arguments[0].iterate().next().getStringValue();
			} catch (XPathException e) {
				dateStr = "";
			}
			
			try
			{
				// Saxon is WONDERFUL and removes escaped characters, so we must re-escape them
				dateStr = StringEscapeUtils.escapeXml(dateStr);
				
				DateParserHelper parser = new DateParserHelper(dateStr);
				
				
				
				// Check to see if the values were parsed
				if (parser.wasParsed()) {
					
					List<SNACDate> dates = parser.getDates();
					
					// Build an XML object out of the results
					xml = "<return>";
					for (SNACDate d : dates) {
						if (d.getType() == SNACDate.FROM_DATE)
							xml += "<dateRange>\n<fromDate";
						else if (d.getType() == SNACDate.TO_DATE)
							xml += "<toDate";
						else
							xml += "<date";
						if (!d.getParsedDate().equals("null"))
							xml += " standardDate=\"" + d.getParsedDate() + "\"";
						if (!d.getNotBefore().equals("null"))
							xml += " notBefore=\"" + d.getNotBefore() + "\"";
						if (!d.getNotAfter().equals("null"))
							xml += " notAfter=\"" + d.getNotAfter() + "\"";
						xml += ">";
						xml += d.getOriginalDate();
						if (d.getType() == SNACDate.FROM_DATE)
							xml += "</fromDate>\n";
						else if (d.getType() == SNACDate.TO_DATE)
							xml += "</toDate></dateRange>\n";
						else
							xml += "</date>\n";
					}
					xml += "</return>";
					
					
					
				} else {
					// nothing was parsed
					xml = "<return>\n";
					xml += "<date standardDate=\"suspiciousDate\" info=\"nothing parsable\">" + parser.getOriginalDate() +"</date>\n";
					xml += "</return>";
				}
				


			}
			catch (Exception sae)
			{
				// If something went wrong, then just return the value "unparseable" to Saxon.
				// nothing was parsed
				xml = "<return>\n";
				xml += "<date standardDate=\"suspiciousDate\" info=\"exception\">"+ dateStr + "</date>\n";
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
