#!/bin/bash

javac -d bin/ -classpath ../../saxon/saxon9he.jar:../../javarepo/commons-lang3-3.1/commons-lang3-3.1.jar:. src/edu/virginia/iath/snac/*.java src/edu/virginia/iath/snac/functions/*.java src/edu/virginia/iath/snac/helpers/*.java 
