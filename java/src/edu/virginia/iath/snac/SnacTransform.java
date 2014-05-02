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
package edu.virginia.iath.snac;

import edu.virginia.iath.snac.functions.DateParser;
import edu.virginia.iath.snac.functions.GeoNamesCheshire;
import edu.virginia.iath.snac.functions.GeoNamesWebLookup;

// XML imports
import java.io.File;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

// Saxon Imports:
import net.sf.saxon.TransformerFactoryImpl;
import net.sf.saxon.Transform;
import net.sf.saxon.Configuration;



/**
 * SnacTransform package.  Loads extensions and executes Saxon's transform.
 * 
 * @author Robbie Hott
 *
 */
public class SnacTransform extends net.sf.saxon.Transform {

	/**
	 * Transform method to be called from main().  Performs the Saxon transform.
	 * @param sourcePath
	 * @param xsltPath
	 *
	public static void simpleTransform(String sourcePath,
			String xsltPath) throws Exception {

		// Build the transformer factory
		TransformerFactory tFactory = TransformerFactory.newInstance();
		TransformerFactoryImpl tFactoryImpl = (TransformerFactoryImpl) tFactory;
		net.sf.saxon.Configuration saxonConfig = tFactoryImpl.getConfiguration();

		// Need to register each of the extensions built as defined below.
		saxonConfig.registerExtensionFunction(new DateParser());
		saxonConfig.registerExtensionFunction(new GeoNamesWebLookup());
		saxonConfig.registerExtensionFunction(new GeoNamesCheshire());

		// Create the transformer object
		Transformer transformer =
				tFactory.newTransformer(new StreamSource(new File(xsltPath)));
		
		// Send output to stdout.
		transformer.transform(new StreamSource(new File(sourcePath)),
				new StreamResult(System.out));

	}
	*/

	protected void initializeConfiguration(Configuration config) {
		config.registerExtensionFunction(new DateParser());
		config.registerExtensionFunction(new GeoNamesWebLookup());
		config.registerExtensionFunction(new GeoNamesCheshire());
        }

	/**
	 * Parses command line arguments and sends the first two to Saxon's transform method call.
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		try {
			//simpleTransform(args[0], args[1]);
			(new SnacTransform()).doTransform(args, "java net.sf.saxon.Transform");
		} catch (Exception e) {
			System.out.println("Error Initializing Saxon's Default Transform");
			e.printStackTrace();
		}
	}

}
