/**
 *        The Institute for Advanced Technology in the Humanities
 *        
 *        Copyright 2014 University of Virginia. Licensed under the Educational Community License, Version 2.0 (the
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
package edu.virginia.iath.snac.helpers.comparators;

import java.util.Comparator;
import edu.virginia.iath.snac.helpers.datastructures.GeolocationString;


/**
 * Comparator for ngram strings.  This one sorts in the following manner:
 * <ol>
 * <li> Sort by alternate names (descending)
 * <li> Sort by string length (ascending)
 * <li> Sort by population (descending)
 * </ol>
 * @author Robbie Hott
 *
 */
public class FlexibleNGramsComparator implements Comparator<GeolocationString> {

	@Override
	public int compare(GeolocationString o1, GeolocationString o2) {
		// -1 = o1 less than object o2
		// 0  = o1 equal to object o2
		// +1 = o1 greater than object o2

		// Assume we're only considering similar overlapping ngrams, so we're not going to sort by ngram overlap

		// Sorting method:
		// 1. Sort by number of alternate names (descending)
		// 2. Sort by string length (ascending)
		// 3. Sort by population (descending)
		if (o1.getNumAltNames() == o2.getNumAltNames()){
			if ( o1.getStringLength() == o2.getStringLength() ) {
				return o2.getPopulation() - o1.getPopulation();
			}
			return o1.getStringLength() - o2.getStringLength();
		}
		return o2.getNumAltNames() - o1.getNumAltNames();
	}


}
