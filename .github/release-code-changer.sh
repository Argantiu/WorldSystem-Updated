#!/bin/bash
# This is a test.sh be careful!
# Release code changer to have it easy to build

# Get config informations
PRE-RELEASE=$(cat ./workflows/release-code-changer.yml | grep pre.release: | cut -d ':' -f2)
RELEASEVER=$(cat ./workflows/release-code-changer.yml | grep release.version: | cut -d ':' -f2)

grep sed -i "s:$DSERVERFOLDER:g" ./values.txt $>/dev/null 2>&1 WorldSystem/2.4.11/
