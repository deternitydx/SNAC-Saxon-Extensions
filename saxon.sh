#!/bin/bash
#
# Setup of the script
#
# Set this to the path where you downloaded the saxon JAR file
export SAXON_HOME=/media/shannon/saxon/saxon9he.jar

# Set this to the path where you downloaded the Apache Commons JAR
export APACHE_COMMONS=/media/shannon/javarepo/commons-lang3-3.1/commons-lang3-3.1.jar

# Set this to the path where you compiled the Java source
export SNAC_JAVA=/media/shannon/SNAC-Saxon-Extensions/java/bin

export CLASSPATH=$SAXON_HOME:$APACHE_COMMONS:$SNAC_JAVA:$CLASSPATH
java edu.virginia.iath.snac.SnacTransform ${1+"$@"}
