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
 * SnacTransform.  Loads extensions and executes Saxon's transform.
 * 
 * @author Robbie Hott
 *
 */
public class SnacTransform extends net.sf.saxon.Transform {

	/**
	 * Override of Transform's configuration initialization function.  We use
	 * this to add our new extension functions to the transform.
	 * @param config Saxon Configuration
	 *
	*/
	protected void initializeConfiguration(Configuration config) {
		// Need to register each of the extensions built as defined below.
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
			(new SnacTransform()).doTransform(args, "java net.sf.saxon.Transform");
		} catch (Exception e) {
			System.out.println("Error Initializing Saxon's Default Transform");
			e.printStackTrace();
		}
	}

}
