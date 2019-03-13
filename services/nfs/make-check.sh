#!/bin/sh

clear

set -x

#make check DST=localhost:/home/osboxes/.otc-home/nfs/root
#make check DST=localhost:/root
make check DST=localhost:/openthinclient
