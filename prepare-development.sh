#!/bin/bash

if [ "$1x" = "x" ]; then

	echo "Please specify the development version"
	exit 1
fi

DEVELOPMENT=$1
echo "Preparing the development version $DEVELOPMENT"

# Update the versions on most parts of the project
mvn versions:set -DnewVersion=$DEVELOPMENT

# Iterate over all apacheds modules as they are not automatically upadated
for dir in `ls -1 -d server/apacheds-*`; do
	cd $dir
	mvn versions:set -DnewVersion=$DEVELOPMENT
	cd -
done

git add pom.xml
git add "**/pom.xml"

git commit -m"PREPARING NEXT DEVELOPMENT CYCLE $DEVELOPMENT"

find . -type f -name pom.xml.versionsBackup -exec rm {} \;

# push the changes that we've made to the codebase
git push
