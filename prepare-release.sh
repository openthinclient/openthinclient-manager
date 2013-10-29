#!/bin/bash

if [ "$1x" = "x" ]; then

	echo "Please specify the release version"
	exit 1
fi

RELEASE=$1
echo "Preparing the release version $RELEASE"

# Update the versions on most parts of the project
mvn versions:set -DnewVersion=$RELEASE

# Iterate over all apacheds modules as they are not automatically upadated
for dir in `ls -1 -d server/apacheds-*`; do
	cd $dir
	mvn versions:set -DnewVersion=$RELEASE
	cd -
done

git add pom.xml
git add "**/pom.xml"

git commit -m"PREPARING RELEASE $RELEASE"

#create the tag of the project with finalized versions
git tag "openthinclient-$RELEASE"

find . -type f -name pom.xml.versionsBackup -exec rm {} \;

# push the changes that we've made to the codebase
git push
git push --tags
