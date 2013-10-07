#!/bin/bash
export CLASSPATH=$HOME/saxon/saxon9he.jar:$HOME/jars/*.jar:$CLASSPATH
java edu.virginia.iath.snac.SnacTransform ${1+"$@"}
