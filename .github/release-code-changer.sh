#!/bin/bash
# Hi, this is a release code changer to change code when you released a new version.
# This changes the README.md and pom.xml at the moment.
# Mabe i will add here more features or change them.
# Configuration

# What plugin version should be in the pom.xml
POM-RELEASE=2.4.20-SNAPSHOT

# What version should be shown in the README.md latest badge
RELEASEVER=2.4.11


sed -i "0,:WorldSystem/.*/:s:WorldSystem/$RELEASEVER/" ./README.md $>/dev/null 2>&1
sed -i "0,:<version>.*</version>:s:<version>$POM-RELEASE</version>" ./../pom.xml $>/dev/null 2>&1
exit 0
