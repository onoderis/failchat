#!/bin/sh
mvn -DpushChanges=false release:prepare release:clean
