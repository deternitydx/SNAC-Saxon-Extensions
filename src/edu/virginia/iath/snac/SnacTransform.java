package edu.virginia.iath.snac;

import edu.virginia.iath.snac.functions.DateParser;

// XML imports
import java.io.File;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

// Saxon Imports:
import net.sf.saxon.TransformerFactoryImpl;




/**
 * @author Robbie Hott
 *
 */
public class SnacTransform {

	/**
	 * Transform method to be called from main().  Performs the Saxon transform.
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
		saxonConfig.registerExtensionFunction(new DateParser());

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
		try {
			simpleTransform(args[0], args[1]);
		} catch (Exception e) {
			System.out.println("Command line usage: java SnacTransform [XML File] [XSL Template]");
			e.printStackTrace();
		}
	}

}
