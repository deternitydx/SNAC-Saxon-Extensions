package edu.virginia.iath.snac.helpers;

import java.util.HashSet;
import java.util.Set;

public class NGramString implements Comparable<NGramString> {
	
	private String string;
	private int ngramLength;
	private Set<String> ngrams;
	private NGramString master;
	private int overlapWithMaster;
	private Object data;
	private int population;
	private int altnames;

	public NGramString(String s, int n) {
		string = s;
		ngramLength = n;
		ngrams = generateNGrams(string);
		master = null;
		overlapWithMaster = 0;
		population = 0;
		altnames = 0;
	}
	
	public void storeData(Object d) {
		data = d;
	}

	public void setPopulation(String pop) {
		try {
			this.population = Integer.parseInt(pop);
		} catch (Exception e) {
			this.population = -1;
		}
	}
	
	public void setNumAltNames(int n) {
		this.altnames = n;
	}
	
	public Object getData() {
		return data;
	}
	
	private Set<String> generateNGrams(String str) {
		Set<String> ret = new HashSet<String>();
		for (int i=0; i < str.length() - ngramLength; i++) {
			ret.add(str.substring(i, i + ngramLength));
		}
		return ret;
	}
	
	public int getNumberOfNGrams() {
		return ngrams.size();
	}
	
	public int getNGramOverlap(NGramString ngs) {
		Set<String> tmp = new HashSet<String>();
		tmp.addAll(ngrams);
		tmp.retainAll(ngs.ngrams);
		return tmp.size();
	}
	
	public int getOverlap() { 
		return overlapWithMaster;
	}

	public void setNGramMaster(NGramString query) {
		master = query;
		overlapWithMaster = getNGramOverlap(query);
	}

	@Override
	public int compareTo(NGramString o) {
		// -1 = less than object o
		// 0  = equal to object o
		// +1 = greater than object o
		
		// If we don't have the right instance, then sort bad ones to back
		if (!(o instanceof NGramString))
			return -1;
		
		NGramString other = (NGramString) o;
		
		// Sort first by ngram overlap with master, then by string length
		
		// if this has more overlap, it should come first
		if (this.overlapWithMaster > other.overlapWithMaster)
			return -1;
		// if other has more overlap, it should come first
		else if (other.overlapWithMaster > this.overlapWithMaster)
			return 1;
		// lastly, if equal, sort by string length (shorter strings should come first)
		else {
			// if string lengths are equal, then the population should be the sort (desc)
			if ( this.string.length() == other.string.length() ) {
				if (this.altnames == other.altnames){
					return other.population - this.population;
				}
				return other.altnames - this.altnames;
				
				/**
				 * sorting by alt names, then by population (to test)
				 *
				if (this.population == other.population) {
					// if populations are equal, sort desc by number of alternate names
					return other.altnames - this.altnames;
				}
				return other.population - this.population;
				*/
			}
			return this.string.length() - other.string.length();
		}
	}
	
	@Override
	public String toString() {
		return "[str=" + string + ", overlap=" + overlapWithMaster + "]";
	}
	
	

}
