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

    <!-- Parse dates solely in Java.  Send the date parameter to the Java date-parser function, then parse the 1-2 outputs -->
    <xsl:template name="tpt_ping_geonames">
      <xsl:param name="geostring"/>
      <xsl:variable name="location" select="saxext:geonames-weblookup($geostring)"/>
	  <placeEntry>
		<xsl:attribute name="original" select="$geostring"/> 
		<xsl:copy-of select="$location/return/node()"/>
	  </placeEntry>
    </xsl:template>

    <xsl:template name="tpt_query_cheshire">
      <xsl:param name="geostring"/>
      <xsl:variable name="location" select="saxext:geonames-cheshire($geostring)"/>
	  <placeEntry>
		<xsl:attribute name="original" select="$geostring"/> 
		<xsl:copy-of select="$location/return/node()"/>
	  </placeEntry>
    </xsl:template>
</xsl:stylesheet>
