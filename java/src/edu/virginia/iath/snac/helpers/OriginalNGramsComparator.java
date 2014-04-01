package edu.virginia.iath.snac.helpers;

import java.util.Comparator;

public class OriginalNGramsComparator implements Comparator<NGramString> {

	@Override
	public int compare(NGramString o1, NGramString o2) {
		// -1 = o1 less than object o2
		// 0  = o1 equal to object o2
		// +1 = o1 greater than object o2


		// Sort first by ngram overlap with master, then by string length

		// if this has more overlap, it should come first
		if (o1.getOverlap() > o2.getOverlap())
			return -1;
		// if other has more overlap, it should come first
		else if (o2.getOverlap() > o1.getOverlap())
			return 1;
		// lastly, if equal, sort by string length (shorter strings should come first)
		else {
			// if string lengths are equal, then the population should be the sort (desc)
			if ( o1.getStringLength() == o2.getStringLength() ) {
				if (o1.getNumAltNames() == o2.getNumAltNames()){
					return o2.getPopulation() - o1.getPopulation();
				}
				return o2.getNumAltNames() - o1.getNumAltNames();

				/**
				 * sorting by alt names, then by population (to test)
				 *
				if (o1.getPopulation() == o2.getPopulation()) {
					// if populations are equal, sort desc by number of alternate names
					return o2.getNumAltNames() - o1.getNumAltNames();
				}
				return o2.getPopulation() - o1.getPopulation();
				 */
			}
			return o1.getStringLength() - o2.getStringLength();
		}

	}

}
