#!/bin/sh

keytool -genkey -keyalg RSA -alias dummy -keystore keystore -storepass password -validity 720 -keysize 2048
