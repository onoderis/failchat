#!/bin/sh
VERSION=$(cat pom.xml | xq -e /project/version)

#mvn package

# TODO fix this command
jdeps --list-deps target/failchat-v"$VERSION"/failchat-"$VERSION".jar | \
  sed 's/   //g' | \
  sed '/JDK removed internal API/d' | \
  sed '/java.base\//d' | \
  tr '\n' ',' | \
  sed 's/$/jdk.crypto.ec/'
