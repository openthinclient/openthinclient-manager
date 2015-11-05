#!/bin/bash

# the version of the manager that shall be used.
VERSION=2.0.0-SNAPSHOT
DIST=manager-runtime-standalone-$VERSION-distribution.tar.gz

HOME=/home/vagrant

if which java >/dev/null; then
   	echo "skip java 8 installation"
else
	echo "java 8 installation"
	apt-get install --yes python-software-properties
	add-apt-repository ppa:webupd8team/java
	apt-get update -qq
	echo debconf shared/accepted-oracle-license-v1-1 select true | /usr/bin/debconf-set-selections
	echo debconf shared/accepted-oracle-license-v1-1 seen true | /usr/bin/debconf-set-selections
	apt-get install --yes oracle-java8-installer
	yes "" | apt-get -f install
fi

cd $HOME

if [ -f $HOME/$DIST ]; then
    echo "Deleting existing distribution"
    rm $HOME/$DIST
fi

if [ -e $HOME/openthinclient-manager ]; then
	echo "Stopping running application"

	$HOME/openthinclient-manager/bin/openthinclient-manager stop

    echo "Deleting existing manager installation"
    rm -rf $HOME/openthinclient-manager
fi

echo "Copying distribution file $DIST"
cp /vagrant/runtime/standalone/target/$DIST $HOME

echo "Extracting..."
tar zxf $DIST

# FIXME this is temporary and should be part of the tar
mkdir openthinclient-manager/logs

echo "Correcting permissions"
chmod +x openthinclient-manager/bin/openthinclient-manager
chmod +x openthinclient-manager/bin/wrapper*linux*

echo "Starting the openthinclient manager as $(whoami)"

openthinclient-manager/bin/openthinclient-manager start

echo "Ready!"