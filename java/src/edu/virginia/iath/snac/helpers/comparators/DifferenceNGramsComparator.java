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
 * <li> Sort by difference between strings (number of ngrams not matched) (ascending)
 * <li> Sort by number of overlapping ngrams (descending)
 * <li> Sort by alternate names (descending)
 * <li> Sort by population (descending)
 * </ol>
 * @author Robbie Hott
 *
 */
public class DifferenceNGramsComparator implements Comparator<GeolocationString> {

	@Override
	public int compare(GeolocationString o1, GeolocationString o2) {
		// -1 = o1 less than object o2
		// 0  = o1 equal to object o2
		// +1 = o1 greater than object o2

		if (o1.getDifference() == o2.getDifference()){
			if ( o1.getOverlap() == o2.getOverlap() ) {
				if (o2.getNumAltNames() == o1.getNumAltNames()) {
					return o2.getPopulation() - o1.getPopulation();
				}
				return o2.getNumAltNames() - o1.getNumAltNames();
			}
			return o2.getOverlap() - o1.getOverlap();
		}
		return o1.getDifference() - o2.getDifference();
	}


}
