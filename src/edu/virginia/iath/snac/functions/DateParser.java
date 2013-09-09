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
				
				if (parser.isRange()) // there is a range
					outStr = parser.firstDate() + " to " + parser.secondDate();
				else				// no range, just one date
					outStr = parser.getDate();

				

				// Write the value out to a Sequence object:
				// First, create a list of all objects to be returned.  Using an ArrayList to store the XdmItems,
				// which may be atomic values
				List<XdmItem> outputs = new ArrayList<XdmItem>();
				
				// Add the first date (always here)
				outputs.add(new XdmAtomicValue(parser.firstDate()));
				
				// If we have a range, then also add the second date to the list of values
				if (parser.isRange())
					outputs.add(new XdmAtomicValue(parser.secondDate()));
				
				// Convert the ArrayList into an XdmValue
				XdmValue itm = new XdmValue(outputs);
				// Get the Sequence from the XdmValue to return to Saxon
				seq = itm.getUnderlyingValue();

			}
			catch (Exception sae)
			{
				// If something went wrong, then just return the value "unparseable" to Saxon.
				seq = (new XdmAtomicValue("unparseable")).getUnderlyingValue();
			}

			return seq;

		}
	}
}
