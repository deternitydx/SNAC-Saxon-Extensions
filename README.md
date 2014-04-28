SNAC-Saxon-Extensions
=====================

Saxon HE Extensions for use with the SNAC project.  Includes Java parsers for human written dates and geographical names.  Serves as a sample for creating other extensions to Saxon without the built-in Saxon libraries.

Documentation
-------------

More detailed installation and usage instructions are available at the [SNAC Saxon Extensions website](http://deternitydx.githun.io/SNAC-Saxon-Extensions "SNAC Saxon Extensions").

JavaDoc documentation is available [here](http://deternitydx.github.io/SNAC-Saxon-Extensions/javadoc/ "JavaDoc Documentation").


Installation
------------

Using the SNAC Saxon extensions requires Saxon 9.5+ HE, which can be downloaded from the Saxon website [here](http://sourceforge.net/projects/saxon/files/ "Saxon Download Page")

The Java date parser extension provided requires Apache Commons, which is made freely available [here](http://commons.apache.org/proper/commons-lang/download_lang.cgi "Apache Commons").

The Geonames parser exension provided requires Cheshire, which is made freely available [here](http://cheshire.berkeley.edu/ "Cheshire").  More detailed installation instructions for Cheshire are available at the [SNAC Saxon Extensions website](http://deternitydx.githun.io/SNAC-Saxon-Extensions "SNAC Saxon Extensions").
  

Usage
------

See the xslt/date.xsl and xslt/place.xsl for samples on how to use the parsing libraries.  The xslt wrappers for the Java methods are availabe in xslt/lib/.
