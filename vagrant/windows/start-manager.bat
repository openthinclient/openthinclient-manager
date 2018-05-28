
@echo off

REM by default, the process will be waiting for a debugger to be connected.
REM To change this behaviour, change suspend=y to suspend=n
java -Xmx1G -agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=5005 -Dmanager.home=C:\manager-home -jar manager-runtime-standalone-selfcontained.jar --debug
