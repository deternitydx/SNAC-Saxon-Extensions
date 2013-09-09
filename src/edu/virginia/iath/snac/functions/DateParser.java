package edu.virginia.iath.snac.functions;

import edu.virginia.iath.snac.helpers.DateParserHelper;

//Saxon Imports
import net.sf.saxon.lib.ExtensionFunctionCall;
import net.sf.saxon.lib.ExtensionFunctionDefinition;
import net.sf.saxon.om.Sequence;
import net.sf.saxon.om.StructuredQName;
import net.sf.saxon.expr.XPathContext;
import net.sf.saxon.s9api.XdmAtomicValue;
import net.sf.saxon.s9api.XdmItem;


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
		return new net.sf.saxon.value.SequenceType[] {net.sf.saxon.value.SequenceType.SINGLE_STRING};
	}

	/**
	 * Define the result type returned by the function
	 */
	@Override
	public net.sf.saxon.value.SequenceType getResultType(net.sf.saxon.value.SequenceType[] suppliedArgumentTypes)
	{
		return net.sf.saxon.value.SequenceType.SINGLE_STRING;
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
			String outStr = null;
			try
			{
				// Read in the argument into a string
				String dateStr = arguments[0].iterate().next().getStringValue();
				DateParserHelper parser = new DateParserHelper(dateStr);
				
				if (parser.isRange()) // there is a range
					outStr = parser.firstDate() + " to " + parser.secondDate();
				else				// no range, just one date
					outStr = parser.getDate();

				

				// Write the value out to a Sequence object
				XdmItem itm = new XdmAtomicValue(outStr);
				seq = itm.getUnderlyingValue();

			}
			catch (Exception sae)
			{
				seq = (new XdmAtomicValue("unparseable")).getUnderlyingValue();
			}

			return seq;

		}
	}
}
