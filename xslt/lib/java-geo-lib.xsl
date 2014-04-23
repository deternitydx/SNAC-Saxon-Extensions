<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                version="2.0"
                xmlns:lib="http://example.com/"
                xmlns:rel="http://example.com/relators"
                xmlns:xs="http://www.w3.org/2001/XMLSchema"
                xmlns:saxon="http://saxon.sf.net/"
                xmlns:functx="http://www.functx.com"
                xmlns:marc="http://www.loc.gov/MARC21/slim"
                xmlns:eac="urn:isbn:1-931666-33-4"
                xmlns:madsrdf="http://www.loc.gov/mads/rdf/v1#"
                xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#"
                xmlns:mads="http://www.loc.gov/mads/"
                xmlns:snac="http://socialarchive.iath.virginia.edu/" 
		xmlns:snacwc="http://socialarchive.iath.virginia.edu/worldcat"
		            xmlns:saxext="http://example.com/saxon-extension"
                exclude-result-prefixes="eac lib xs saxon xsl madsrdf rdf mads functx marc snacwc rel saxext"
                >

    <!-- 
         Author: Robbie Hott
         The Institute for Advanced Technology in the Humanities
         
         Copyright 2013 University of Virginia. Licensed under the Educational Community License, Version 2.0 (the
         "License"); you may not use this file except in compliance with the License. You may obtain a copy of the
         License at
         
         http://opensource.org/licenses/ECL-2.0
         http://www.osedu.org/licenses/ECL-2.0
         
         Unless required by applicable law or agreed to in writing, software distributed under the License is
         distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See
         the License for the specific language governing permissions and limitations under the License.

         This is a library of xml templates for date parsing shared by several XSLT scripts.
    -->

    <!-- Query Geonames web service for the geographical entry.  This returns a simplified format from Geonames,
	 and is not useful in SNAC processing -->
    <xsl:template name="tpt_ping_geonames">
      <xsl:param name="geostring"/>
      <xsl:variable name="location" select="saxext:geonames-weblookup($geostring)"/>
	  <placeEntry>
		<xsl:attribute name="original" select="$geostring"/> 
		<xsl:copy-of select="$location/return/node()"/>
	  </placeEntry>
    </xsl:template>

    <!-- Query Cheshire's Geonames index for the geographical information
	 Parameter: the geographical string to lookup
	 Note: Cheshire must be available on the localhost, running on port 7010.  
	 See http://deternitydx.github.io/SNAC-Saxon-Extensions for information on setting up Cheshire.
	 
	 Returns the snac:placeEntry in correct formatting.

	 A note on the Java implementation:
	 Matching is done through a variety of steps.  First, an exact string search is performed for
	 US/Can State, then Country.  If a match is found, no more searching will be performed.  If there
	 are no matches, the string will be broken down into a first part (up to a comma or open paren) and
	 the second part.  Exact string searching on each part (as place, state; place, country; etc) will
	 be performed for (in order) cities, populated places, administrative level 1, administrative level 2,
	 and other places (buildings, rivers, streams, lakes, areas, etc).  If any exact match is found,
	 the top results will be returned.
		The score for these matches will be of the form (top result may appear twice):
			times top result found / total number of results 
	 	So, if only one exact match found, score of 1.0 (100%). However, if 3 results are found,
		the score for the top result will be 0.33 (33%).  It is still likely, however, that the
		top result is the preferred of the 3.
	 If no exact string matching was found, we perform an ngrams search for the entire string in
	 cheshire (name, alternate names, admin1 code).  There are usually over a thousand results, in which
	 cheshire does not do a great job sorting.  Post-processing is done in the following manner to
	 bubble up better results to the top, however these results cannot be guaranteed because of the vast
	 quantity of results found.  The first part of the string (before a comma/open paren) is broken down to
	 ngrams as well as the geonames name element of each cheshire result.  The cheshire results are then
	 sorted based on matches to the search string ngrams:
		1. Sort by difference of ngrams (number of combined ngrams that differ between the two strings) ASC
		2. Sort by overlap of ngrams (number of ngrams shared between the two strings) DESC
		3. Number of alternate names in the cheshire result DESC
			The more popular a place is, the more likely other languages will have made nicknames for
			this place.  It is used here as an approximation to popularity of the place.
		4. Population of the cheshire result DESC
			A secondary approximation to popularity: how many people live there.
	 The score for ngrams matches is heavily discounted, since as noted above, they cannot be trusted. In
	 counting the number of results, after sorting, only those matches 2 deviations away from the top cheshire
	 result are counted (those differing by less than one or two levels of overlapping ngrams).  The score is
	 derived similarly to the score above:
			1 / number of top results
	 The discount is applied as follows:
		1. If the first part of the string exactly matches the top cheshire result's name, discount by 50%
			if exact match and only one top result, top score would be 0.5 (50%)
		2. If the first part of the string is contained in the top cheshire results' name, discount by 90%
			if contained in and only one top result, top score would be 0.1 (10%)
		3. Otherwise, discount by 99%	 
			if only one result, the top score would be 0.01 (1%)

	 For more information and documentation on the Java code, including full JavaDoc documentation, see
		http://deternitydx.github.io/SNAC-Saxon-Extensions

	 Tradeoff parameter: cutoff for score.  Currently set at 0.06 (below), based on empirical tests of 871 place
	 names, this gave a good balance between false positives (about 2% in our sample) and false negatives. This
	 errs on the side of false negatives, where many places would match, but are considered BestMaybeSame rather
	 than LikelySame.  Increase the number to increase false negatives and decrease false positives.  Using 0.5
	 would remove all false positives, since the only results with greater score than 0.5 are exact matches, but
	 were only 50% of our 871-item sample.  Decrease this threshhold and there will be more false positives.
	 
	 -->
    <xsl:template name="tpt_query_cheshire">
      <xsl:param name="geostring"/>
      <xsl:variable name="location" select="saxext:geonames-cheshire($geostring)"/>
      <xsl:variable name="geonamesAddr" select='"http://www.geonames.org/"'/>
	  <snac:placeEntry>
		<placeEntry><xsl:value-of select="$geostring"/></placeEntry>
		<xsl:choose>
			<!-- The below cutoff is the tradeoff between false positives and false negatives.  0.5
				would eliminate all nearly all false positives.  At 0.06, empirically we see
				2% false positives and 31% unmatched (not all false negatives) -->
			<xsl:when test="$location/return/score > 0.06">
				<snac:placeEntryLikelySame>
					<xsl:attribute name="vocabularySource" select="concat($geonamesAddr, $location/return/geonameId)"/>
					<xsl:attribute name="certaintyScore" select="normalize-space($location/return/score)"/>
					<xsl:attribute name="latitude" select="normalize-space($location/return/latitude)"/>
					<xsl:attribute name="longitude" select="normalize-space($location/return/longitude)"/>
					<xsl:attribute name="countryCode" select="normalize-space($location/return/country)"/>
					<xsl:attribute name="administrativeCode" select="normalize-space($location/return/admin1)"/>
					<xsl:value-of select="$location/return/name"/>
				</snac:placeEntryLikelySame>
			</xsl:when>
			<xsl:otherwise>
				<snac:placeEntryBestMaybeSame>
					<xsl:attribute name="vocabularySource" select="concat($geonamesAddr, $location/return/geonameId)"/>
					<xsl:attribute name="certaintyScore" select="normalize-space($location/return/score)"/>
					<xsl:attribute name="latitude" select="normalize-space($location/return/latitude)"/>
					<xsl:attribute name="longitude" select="normalize-space($location/return/longitude)"/>
					<xsl:attribute name="countryCode" select="normalize-space($location/return/country)"/>
					<xsl:attribute name="administrativeCode" select="normalize-space($location/return/admin1)"/>
					<xsl:value-of select="$location/return/name"/>
				</snac:placeEntryBestMaybeSame>
			</xsl:otherwise>
		</xsl:choose>
		<xsl:for-each select="$location/return/otherResults/place">
			<snac:placeEntryMaybeSame>
				<xsl:attribute name="vocabularySource" select="concat($geonamesAddr, geonameId)"/>
				<xsl:attribute name="latitude" select="normalize-space(latitude)"/>
				<xsl:attribute name="longitude" select="normalize-space(longitude)"/>
				<xsl:attribute name="countryCode" select="normalize-space(country)"/>
				<xsl:attribute name="administrativeCode" select="normalize-space(admin1)"/>
				<xsl:value-of select="name"/>
			</snac:placeEntryMaybeSame>
		</xsl:for-each>
	  </snac:placeEntry>
    </xsl:template>
</xsl:stylesheet>
