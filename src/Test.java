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
import java.util.Arrays;
import java.util.List;

import edu.virginia.iath.snac.helpers.DateParserHelper;
import edu.virginia.iath.snac.helpers.SNACDate;


public class Test {
	public static void main(String args[]) {
		/**
		String test = "1983-12.3";
		
		String[] strarr = test.split("[\\s.,-]+");
		
		System.out.println(Arrays.toString(strarr) + " " + strarr.length);
		**/
		
		DateParserHelper dph = new DateParserHelper("1520-");
		List<SNACDate> l = dph.getDates();
		
		for (SNACDate d : l) {
			System.out.println(d.getOriginalDate());
			System.out.println(d.getParsedDate());
			System.out.println(d.getNotBefore());
			System.out.println(d.getNotAfter());
			System.out.println(d.getType());
		}
		
		System.out.println(dph.wasParsed());
		
		
	}
}
