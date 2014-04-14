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

import java.lang.reflect.Method;


//Saxon Imports
import net.sf.saxon.lib.ExtensionFunctionCall;
import net.sf.saxon.lib.ExtensionFunctionDefinition;
import net.sf.saxon.om.Item;
import net.sf.saxon.om.Sequence;
import net.sf.saxon.om.SequenceIterator;
import net.sf.saxon.om.StructuredQName;
import net.sf.saxon.expr.XPathContext;
import net.sf.saxon.s9api.DocumentBuilder;
import net.sf.saxon.s9api.Processor;
import net.sf.saxon.s9api.XdmNode;
import net.sf.saxon.trans.XPathException;


/**
 * Generic Java method call SAXON extension.
 * 
 * @author Robbie Hott
 *
 */
public class JavaMethodCall extends ExtensionFunctionDefinition {

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
		return new StructuredQName("saxext", "http://example.com/saxon-extension", "java-call");
	}

	/**
	 * Define the argument types accepted by the function
	 */
	@Override
	public net.sf.saxon.value.SequenceType[] getArgumentTypes()
	{
		return new net.sf.saxon.value.SequenceType[] {
				net.sf.saxon.value.SequenceType.ANY_SEQUENCE	// First and only parameter is a single string
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
		 * @param context the context of the call
		 * @param arguments the arguments supplied to the call
		 * @return Sequence the output of the call
		 * 
		 */
		@SuppressWarnings("unused")
		@Override
		public Sequence call(XPathContext context, Sequence[] arguments)
		{
			Sequence seq = null;
			String xml = "";
			
			// Read in the argument into a string
			String className = null;
			String methodName = null;
			try {
				SequenceIterator<? extends Item> i = arguments[0].iterate();
				className = i.next().getStringValue();
				methodName = i.next().getStringValue();
			} catch (XPathException e) {
				
			}
			
			
					
					// Build an XML object out of the results
					xml = "<return>";
					xml += "</return>";
					
			

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
	
	/**
	 * Main method used for testing purposes.
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		try {
			String cla = "java.util.Calendar";
			String method = "getInstance";
			String param = "2.5";
			Class<?> c = Class.forName(cla);
			//System.out.println(Arrays.toString(c.getMethods()));
			Method[] methods = c.getMethods();
			boolean called = false;
			for (int i = 0; i < methods.length; i++) {
				Method m = methods[i];
				if (m.getName().equals(method)) {
					try {
						m.setAccessible(true);
						System.out.println(m.invoke(c.newInstance()));
						System.out.println(m.invoke(new Object(), Double.parseDouble(param)));
						called = true;
						break;
					} catch (Exception e) {
						System.err.println("Something went wrong calling the method: " + m.toString());
						e.printStackTrace();
					}
				}
			}
			if (!called) {
				System.err.println("Could not call method.");
			}
			
			//Method m = c.getMethod(method, new Class[] {Object.class});
			//System.out.println(m.invoke(c.newInstance(), param));
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		} catch (SecurityException e) {
			e.printStackTrace();
		}
		
		
	}
}
