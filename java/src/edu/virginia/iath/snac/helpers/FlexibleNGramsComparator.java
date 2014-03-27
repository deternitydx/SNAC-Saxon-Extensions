package edu.virginia.iath.snac.helpers;

import java.util.Comparator;

public class FlexibleNGramsComparator implements Comparator<NGramString> {

	@Override
	public int compare(NGramString o1, NGramString o2) {
		// -1 = o1 less than object o2
		// 0  = o1 equal to object o2
		// +1 = o1 greater than object o2

		// Assume we're only considering similar overlapping ngrams, so we're not going to sort by ngram overlap

		// Sorting method:
		// 1. Sort by string length (ascending)
		// 2. Sort by number of alternate names (descending)
		// 3. Sort by population (descending)
		if ( o1.getStringLength() == o2.getStringLength() ) {
			if (o1.getNumAltNames() == o2.getNumAltNames()){
				return o2.getPopulation() - o1.getPopulation();
			}
			return o2.getNumAltNames() - o1.getNumAltNames();
		}
		return o1.getStringLength() - o2.getStringLength();
	}


}
