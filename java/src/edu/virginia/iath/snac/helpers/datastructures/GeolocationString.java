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
package edu.virginia.iath.snac.helpers.datastructures;

import java.util.HashSet;
import java.util.Set;

/**
 * NGram String
 * 
 * Object for storing a geolocation string, its ngrams and other information about the place.
 * 
 * @author Robbie Hott
 *
 */
public class GeolocationString {

	private String string;
	private int ngramLength;
	private Set<String> ngrams;
	
	@SuppressWarnings("unused")
	private GeolocationString master;
	private int overlapWithMaster;
	private int differenceFromMaster;
	private Object data;
	private int population;
	private int altnames;

	/**
	 * Constructor.  Creates an object with the given string and breaks it into ngrams
	 * of the length provided.
	 * @param str Original string
	 * @param n Number of ngrams
	 */
	public GeolocationString(String str, int n) {
		string = str;
		ngramLength = n;
		ngrams = generateNGrams(string);
		master = null;
		overlapWithMaster = 0;
		differenceFromMaster = Integer.MAX_VALUE;
		population = 0;
		altnames = 0;
	}

	/**
	 * Data to store with this object.   May be any type of object.
	 * 
	 * @param d Data to store
	 */
	public void storeData(Object d) {
		data = d;
	}

	/**
	 * Stores the population for this object. 
	 * 
	 * @param pop String containing the population
	 */
	public void setPopulation(String pop) {
		try {
			this.population = Integer.parseInt(pop);
		} catch (Exception e) {
			this.population = -1;
		}
	}

	/**
	 * Gets the population.
	 * 
	 * @return population of the geolocation represented by this object
	 */
	public int getPopulation() {
		return this.population;
	}

	/**
	 * Sets the number of alternate names for this geolocation
	 * 
	 * @param n Number of alternate names
	 */
	public void setNumAltNames(int n) {
		this.altnames = n;
	}

	/**
	 * Gets the number of alternate names for this geolocation
	 * 
	 * @return Number of alternate names
	 */
	public int getNumAltNames() {
		return this.altnames;
	}

	/**
	 * Gets the length of the original string
	 * 
	 * @return String length
	 */
	public int getStringLength() {
		return this.string.length();
	}

	/**
	 * Returns the data object stored with this geolocation
	 * 
	 * @return Object
	 */
	public Object getData() {
		return data;
	}

	/**
	 * Generates the ngrams of the given string, returning them as a set.
	 * 
	 * @param str String to parse into ngrams
	 * @return Set of ngram Strings.
	 */
	private Set<String> generateNGrams(String str) {
		Set<String> ret = new HashSet<String>();
		for (int i=0; i < str.length() - ngramLength; i++) {
			ret.add(str.substring(i, i + ngramLength));
		}
		return ret;
	}

	/**
	 * Gets the number of ngrams in the current string
	 * 
	 * @return Number of ngrams
	 */
	public int getNumberOfNGrams() {
		return ngrams.size();
	}

	/**
	 * Given another geolocation string, it computes the number of overlapping
	 * ngrams between this object and that.
	 * 
	 * @param ngs Other string to compare with
	 * @return Number of overlapping ngrams
	 */
	public int getNGramOverlap(GeolocationString ngs) {
		Set<String> tmp = new HashSet<String>();
		tmp.addAll(ngrams);
		tmp.retainAll(ngs.ngrams);
		return tmp.size();
	}

	/**
	 * Gets the overlap with the master geolocation string.
	 * 
	 * @return Overlap with the master string.
	 */
	public int getOverlap() { 
		return overlapWithMaster;
	}

	/**
	 * Gets the difference (number of differing ngrams) from the master geolocation string.
	 * 
	 * @return Difference with the master string.
	 */
	public int getDifference() {
		return differenceFromMaster;
	}

	/**
	 * Set the master geolocation string.  Computes and stores the overlap and difference.
	 * 
	 * @param query Master geolocation string.
	 */
	public void setNGramMaster(GeolocationString query) {
		master = query;
		overlapWithMaster = getNGramOverlap(query);
		differenceFromMaster = (this.ngrams.size() - overlapWithMaster) // number of extra ngrams in this string
				+ (query.ngrams.size() - overlapWithMaster); // number of extra ngrams in the query string
	}

	

	@Override
	public String toString() {
		return "[str=" + string + ", overlap=" + overlapWithMaster + "]";
	}

	/**
	 * Gets the original string.
	 * 
	 * @return Original String.
	 */
	public String getString() {
		return string;
	}
}
