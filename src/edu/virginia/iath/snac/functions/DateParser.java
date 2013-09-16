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

import java.util.ArrayList;
import java.util.List;

import edu.virginia.iath.snac.helpers.DateParserHelper;
import edu.virginia.iath.snac.helpers.SNACDate;

//Saxon Imports
import net.sf.saxon.lib.ExtensionFunctionCall;
import net.sf.saxon.lib.ExtensionFunctionDefinition;
import net.sf.saxon.om.Sequence;
import net.sf.saxon.om.StructuredQName;
import net.sf.saxon.expr.XPathContext;
import net.sf.saxon.s9api.XdmAtomicValue;
import net.sf.saxon.s9api.XdmItem;
import net.sf.saxon.s9api.XdmValue;


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
		 * The return value will always be a Sequence in one of the following specifications:
		 * 
		 * Unable to parse:
		 * 		suspiciousDate
		 * 
		 * One date:
		 * 		standard date
		 * 		original date
		 * 		not before standard date (or "null")
		 * 		not after standard date (or "null")
		 * 
		 * Date range (two dates)
		 * 		first date standard date
		 * 		first date original date
		 * 		first date not before standard date (or "null")
		 * 		first date not after standard date (or "null")
		 * 		second date standard date
		 * 		second date original date
		 * 		second date not before standard date (or "null")
		 * 		second date not after standard date (or "null")
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
			try
			{
				// Read in the argument into a string
				String dateStr = arguments[0].iterate().next().getStringValue();
				DateParserHelper parser = new DateParserHelper(dateStr);
				
				/*
				if (parser.isRange()) // there is a range
					outStr = parser.firstDate() + " to " + parser.secondDate();
				else				// no range, just one date
					outStr = parser.getDate();
				*/
				

				// Write the value out to a Sequence object:
				// First, create a list of all objects to be returned.  Using an ArrayList to store the XdmItems,
				// which may be atomic values
				List<XdmItem> outputs = new ArrayList<XdmItem>();
				
				
				// Check to see if the values were parsed
				if (parser.wasParsed()) {
					
					List<SNACDate> dates = parser.getDates();
					
					for (SNACDate date : dates) {
						outputs.add(new XdmAtomicValue(date.getParsedDate()));
						outputs.add(new XdmAtomicValue(date.getOriginalDate()));
						outputs.add(new XdmAtomicValue(date.getNotBefore()));
						outputs.add(new XdmAtomicValue(date.getNotAfter()));
					}
					
					
				} else {
					// nothing was parsed
					outputs.add(new XdmAtomicValue("suspiciousDate"));
				}
				
				// Convert the ArrayList into an XdmValue
				XdmValue itm = new XdmValue(outputs);
				// Get the Sequence from the XdmValue to return to Saxon
				seq = itm.getUnderlyingValue();

			}
			catch (Exception sae)
			{
				// If something went wrong, then just return the value "unparseable" to Saxon.
				seq = (new XdmAtomicValue("extraSuspiciousDate")).getUnderlyingValue();
			}

			return seq;

		}
	}
}
