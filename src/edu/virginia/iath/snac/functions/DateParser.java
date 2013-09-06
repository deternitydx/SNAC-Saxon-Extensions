package edu.virginia.iath.snac.functions;


//imports from ext_simple
import java.io.File;
import java.util.Date;
import java.text.DateFormat;

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.apache.commons.lang3.time.DateFormatUtils;
import org.apache.commons.lang3.time.DateUtils;

import net.sf.saxon.s9api.*;
//import net.sf.saxon.functions.*;

//Need these additional imports:
import net.sf.saxon.lib.ExtensionFunctionCall;
import net.sf.saxon.lib.ExtensionFunctionDefinition;
import net.sf.saxon.expr.XPathContext;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.om.*;
import net.sf.saxon.TransformerFactoryImpl;

//The signature for SequenceType conflicts with s9api, and value.SequenceType is required so we can get
//SINGLE_INTEGER so we must use the full classpath for SequenceType in the code.
import net.sf.saxon.value.*;

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
		public net.sf.saxon.om.Sequence call(XPathContext context, net.sf.saxon.om.Sequence[] arguments)
		{
			net.sf.saxon.om.Sequence seq = null;
			String outStr = null;
			try
			{
				// Read in the argument into a string
				String dateStr = arguments[0].iterate().next().getStringValue();

				// Check for date range.  If so, parse separately
				if (dateStr.contains("-")) {
					String date1Str = dateStr.substring(0, dateStr.indexOf("-"));
					String date2Str = dateStr.substring(dateStr.indexOf("-") + 1);

					Date date1 = DateUtils.parseDate(date1Str.trim(),
							"yyyy", /*"yyyy-MM", "yyyy-M", "yyyy-M-d", "yyyy-M-dd", "yyyy-MM-d", "yyyy-MM-dd",*/ // standard dates
							"MMMMM dd, yyyy", "MMM dd, yyyy", "MMM dd yyyy", "MMMMM dd, yyyy", "yyyy MMM dd",
							"dd MMM, yyyy", "dd MMMMM, yyyy", "yyyy, MMM dd", "yyyy, MMMMM dd", "yyyy, MMM. dd",
							"MMMMM yyyy", "MMM yyyy"
							);

					Date date2 = DateUtils.parseDate(date2Str.trim(),
							"yyyy", /*"yyyy-MM", "yyyy-M", "yyyy-M-d", "yyyy-M-dd", "yyyy-MM-d", "yyyy-MM-dd",*/ // standard dates
							"MMMMM dd, yyyy", "MMM dd, yyyy", "MMM dd yyyy", "MMMMM dd, yyyy", "yyyy MMM dd",
							"dd MMM, yyyy", "dd MMMMM, yyyy", "yyyy, MMM dd", "yyyy, MMMMM dd", "yyyy, MMM. dd",
							"MMMMM yyyy", "MMM yyyy"
							);
					outStr = (date1 == null || date2 == null) ? "unparsable" : DateFormatUtils.format(date1, "yyyy-MM-dd") + " to " + DateFormatUtils.format(date2, "yyyy-MM-dd");
				} else {

					// Right now, we ignore dates that have hyphens in them, since those are date ranges
					Date date = DateUtils.parseDate(dateStr.trim(),
							"yyyy", /*"yyyy-MM", "yyyy-M", "yyyy-M-d", "yyyy-M-dd", "yyyy-MM-d", "yyyy-MM-dd",*/ // standard dates
							"MMMMM dd, yyyy", "MMM dd, yyyy", "MMM dd yyyy", "MMMMM dd, yyyy", "yyyy MMM dd",
							"dd MMM, yyyy", "dd MMMMM, yyyy", "yyyy, MMM dd", "yyyy, MMMMM dd", "yyyy, MMM. dd",
							"MMMMM yyyy", "MMM yyyy"
							);

					// Convert string to a date object by Java's date parser
					//DateFormat df = DateFormat.getInstance();
					//Date date = df.parse(dateStr);

					// Create the output string
					outStr = (date == null) ? "unparsable" : DateFormatUtils.format(date, "yyyy-MM-dd");
				}				

				// Write the value out to a Sequence object
				XdmItem itm = new XdmAtomicValue(outStr);
				seq = itm.getUnderlyingValue();

			}
			catch (Exception sae)
			{
				//sae.printStackTrace();
				seq = (new XdmAtomicValue("unparseable")).getUnderlyingValue();
			}

			// Works!
			return seq;

			// Works also, but not very useful.
			// return arguments[0];
		}
	}
}
