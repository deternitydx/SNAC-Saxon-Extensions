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
import java.io.StringReader;
import java.util.Arrays;
import java.util.List;

import javax.xml.transform.stream.StreamSource;

import org.xml.sax.InputSource;

import net.sf.saxon.s9api.DocumentBuilder;
import net.sf.saxon.s9api.Processor;
import net.sf.saxon.s9api.SaxonApiException;
import net.sf.saxon.s9api.XdmNode;

import edu.virginia.iath.snac.helpers.DateParserHelper;
import edu.virginia.iath.snac.helpers.SNACDate;


public class Test {
	public static void main(String args[]) throws SaxonApiException {
		/**
		String test = "1983-12.3";
		
		String[] strarr = test.split("[\\s.,-]+");
		
		System.out.println(Arrays.toString(strarr) + " " + strarr.length);
		**/
		
		DateParserHelper dph = new DateParserHelper("1520-2156");
		List<SNACDate> l = dph.getDates();
		
		String xml = "<results>";
		for (SNACDate d : l) {
			xml += "<date>\n";
			xml += "<orig>" + d.getOriginalDate() + "</orig>\n";
			xml += "<parse>" + d.getParsedDate() + "</parse>\n";
			xml += "<notBefore>" + d.getNotBefore() + "</notBefore>\n";
			xml += "<notAfter>" + d.getNotAfter() + "</notAfter>\n";
			xml += "</date>\n";
			System.out.println(d.getOriginalDate());
			System.out.println(d.getParsedDate());
			System.out.println(d.getNotBefore());
			System.out.println(d.getNotAfter());
			System.out.println(d.getType());
		}
		xml += "</results>";
		
		System.out.println(dph.wasParsed());
		
		
		Processor proc = new Processor(false);
        DocumentBuilder builder = proc.newDocumentBuilder();
        
        XdmNode xdm = builder.build(new StreamSource(new StringReader(xml)));
        
		System.out.println(xdm.toString());
		
	}
}
