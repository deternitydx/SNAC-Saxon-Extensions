<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet 
		xmlns:xsl="http://www.w3.org/1999/XSL/Transform" 
		xmlns:xs="http://www.w3.org/2001/XMLSchema" 
		xmlns:lib="http://example.com/" 
		xmlns:rel="http://example.com/relator" 
		xmlns:bl="http://example.com/bl" 
		xmlns:eac="urn:isbn:1-931666-33-4" 
		xmlns:date="http://exslt.org/dates-and-times" 
		xmlns:exsl="http://exslt.org/common" 
		xmlns:xlink="http://www.w3.org/1999/xlink" 
		xmlns:saxext="http://example.com/saxon-extension"
                xmlns="urn:isbn:1-931666-33-4"
                xmlns:snac="urn:isbn:1-931666-33-4"
		version="2.0" extension-element-prefixes="date exsl" exclude-result-prefixes="xsl saxext xs lib eac xlink rel bl">
  <xsl:include href="lib/java-date-lib.xsl"/>
  <xsl:include href="lib/java-geo-lib.xsl"/>
  <xsl:output method="xml" indent="yes"/>
  <xsl:template match="node()|@*" xmlns="urn:isbn:1-931666-33-4">
    <xsl:copy>
      <xsl:apply-templates select="@*|node()"/>
    </xsl:copy>
  </xsl:template>
  <xsl:template match="placeEntry" xmlns="urn:isbn:1-931666-33-4">
      <!-- Parse the date in here -->
      <xsl:call-template name="tpt_query_cheshire">
        <xsl:with-param name="geostring">
          <xsl:copy-of select="."/>
        </xsl:with-param>
      </xsl:call-template>
  </xsl:template>
</xsl:stylesheet>
