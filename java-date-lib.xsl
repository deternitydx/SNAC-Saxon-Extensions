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
    <xsl:template name="tpt_parse_date_java">
      <xsl:param name="date"/>
    
      <xsl:variable name="dates" select="saxext:date-parser($date)"/>
      <xsl:choose>
        <xsl:when test="count($dates) = 1">
          <date>
            <xsl:attribute name="standardDate" select="$dates"/>
            <xsl:value-of select="$date"/>
          </date>
        </xsl:when>
        <xsl:when test="count($dates) = 2">
          <dateRange>
            <fromDate>
              <xsl:attribute name="standardDate" select="$dates[1]"/>
              <xsl:value-of select="$dates[1]"/>
            </fromDate>
            <toDate>
              <xsl:attribute name="standardDate" select="$dates[2]"/>
              <xsl:value-of select="$dates[2]"/>
            </toDate>
          </dateRange>
        </xsl:when>
      </xsl:choose>
    </xsl:template>


    <!-- Split the date in XSLT using regular expressions, then send each portion individually to the
         Java date-parser fucntion.  Each portion is handled separately. -->
    <xsl:template name="tpt_parse_date_java_xsl">
      <xsl:param name="date"/>

      <xsl:variable name="dates">
        <xsl:analyze-string select="."
                            regex="([^-]+)">
          <xsl:matching-substring>
            <date>
              <xsl:attribute name="standardDate" select="saxext:date-parser(.)"/>
              <xsl:value-of select="."/>
            </date>
          </xsl:matching-substring>
        </xsl:analyze-string>
      </xsl:variable>
      <xsl:variable name="numDates" select="count($dates/*)"/>
      <xsl:choose>
        <xsl:when test="$numDates = 1">
          <xsl:copy-of select="$dates"/>
        </xsl:when> 
        <xsl:when test="$numDates = 2">
          <dateRange>
            <fromDate>
              <xsl:attribute name="standardDate" select="($dates/date)[1]/@standardDate"/>
              <xsl:value-of select="($dates/date)[1]"/>
            </fromDate>
            <toDate>
              <xsl:attribute name="standardDate" select="($dates/date)[2]/@standardDate"/>
              <xsl:value-of select="($dates/date)[2]"/>
            </toDate>
          </dateRange>
        </xsl:when>
      </xsl:choose>
    </xsl:template>

</xsl:stylesheet>
