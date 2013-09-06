#!/bin/bash
export CLASSPATH=$HOME/saxon/saxon9he.jar:$HOME/javarepo/commons-lang3-3.1/commons-lang3-3.1.jar:$HOME/snac_utils/java/bin:$CLASSPATH
java edu.virginia.iath.snac.SnacTransform ${1+"$@"}
