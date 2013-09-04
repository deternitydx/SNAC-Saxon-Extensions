/**
 * 
 */
package edu.virginia.iath.snac;



import java.lang.reflect.Method;
import java.util.*;

// imports from ext_simple
import java.io.File;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import net.sf.saxon.s9api.*;
// import net.sf.saxon.functions.*;

// Need these additional imports:
import net.sf.saxon.lib.ExtensionFunctionCall;
import net.sf.saxon.lib.ExtensionFunctionDefinition;
import net.sf.saxon.expr.XPathContext;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.om.*;
import net.sf.saxon.TransformerFactoryImpl;

// The signature for SequenceType conflicts with s9api, and value.SequenceType is required so we can get
// SINGLE_INTEGER so we must use the full classpath for SequenceType in the code.
import net.sf.saxon.value.*;



/**
 * @author jh2jf
 *
 */
public class SnacTransform {

	/**
	 * Tramsform method to be called from main().  Performs the Saxon transform.
	 * @param sourcePath
	 * @param xsltPath
	 */
	public static void simpleTransform(String sourcePath,
			String xsltPath) throws Exception {
		
		// Build the transformer factory
		TransformerFactory tFactory = TransformerFactory.newInstance();
		TransformerFactoryImpl tFactoryImpl = (TransformerFactoryImpl) tFactory;
		net.sf.saxon.Configuration saxonConfig = tFactoryImpl.getConfiguration();

		// Need to register each of the extensions built as defined below.
		//saxonConfig.registerExtensionFunction(new AddTwo());



		Transformer transformer =
				tFactory.newTransformer(new StreamSource(new File(xsltPath)));
		/*
		 * Send output to stdout. If you want to write a file use new StreamResult(new File("foo.xml"))
		 * or some variation of StreamResult.
		 */
		transformer.transform(new StreamSource(new File(sourcePath)),
				new StreamResult(System.out));

	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		try {
			simpleTransform(args[0], args[1]);
		} catch (Exception e) {
			System.out.println("Command line usage: java SnacTransform [XML File] [XSL Template]");
			e.printStackTrace();
		}
	}

}
