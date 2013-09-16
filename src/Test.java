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
