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
		String xml = "";

		DateParserHelper dph = new DateParserHelper("[1910s]-1942");

		if (dph.wasParsed()) {
			List<SNACDate> dates = dph.getDates();

			// Build an XML object out of the results
			xml = "<return>";
			for (SNACDate d : dates) {
				if (d.getType() == SNACDate.FROM_DATE)
					xml += "<dateRange>\n<fromDate";
				else if (d.getType() == SNACDate.TO_DATE)
					xml += "<toDate";
				else
					xml += "<date";
				if (!d.getParsedDate().equals("null"))
					xml += " standardDate=\"" + d.getParsedDate() + "\"";
				if (!d.getNotBefore().equals("null"))
					xml += " notBefore=\"" + d.getNotBefore() + "\"";
				if (!d.getNotAfter().equals("null"))
					xml += " notAfter=\"" + d.getNotAfter() + "\"";
				xml += ">";
				xml += d.getOriginalDate();
				if (d.getType() == SNACDate.FROM_DATE)
					xml += "</fromDate>\n";
				else if (d.getType() == SNACDate.TO_DATE)
					xml += "</toDate></dateRange>\n";
				else
					xml += "</date>\n";
			}
			xml += "</return>";

		} else {
			// nothing was parsed
			xml = "<return>\n";
			xml += "<date standardDate=\"suspiciousDate\">" + dph.getOriginalDate() + "</date>\n";
			xml += "</return>";
		}

		System.out.println(dph.wasParsed());


		Processor proc = new Processor(false);
		DocumentBuilder builder = proc.newDocumentBuilder();

		XdmNode xdm = builder.build(new StreamSource(new StringReader(xml)));

		System.out.println(xdm.toString());


		String hi = "March &amp; April 1954";
		String[] posibilities = hi.split("[ .]*,[ .]*");
		System.out.println(Arrays.toString(hi.trim().split("[ .]*and[ .]*|[ .]*&amp;[ .]*")));
		for (int i = 0; i < posibilities.length; i++)
			System.out.println(posibilities[i].trim().matches("\\d{3}\\d*") || posibilities[i].trim().matches("[.]*\\d{3}\\d*[ .]*-[ .]*\\d{3}\\d*[.]*"));
		
	}
}
