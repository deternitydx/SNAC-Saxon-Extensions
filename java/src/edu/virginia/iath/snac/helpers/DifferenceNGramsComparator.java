package edu.virginia.iath.snac.helpers;

import java.util.Comparator;

public class DifferenceNGramsComparator implements Comparator<NGramString> {

	@Override
	public int compare(NGramString o1, NGramString o2) {
		// -1 = o1 less than object o2
		// 0  = o1 equal to object o2
		// +1 = o1 greater than object o2


		// Sorting method:
		// 1. Sort by difference between strings (number of ngrams not matched) (ascending)
		// 2. Sort by number of overlapping ngrams (descending)
		// 3. Sort by alternate names (descending)
		// 4. Sort by population (descending)
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
