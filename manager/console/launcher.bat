set START=c:\programme\java\jre1.5.0_07\bin\java.exe
set START=%START% -Xbootclasspath/a:c:\programme\Java\jre1.5.0_07\lib\javaws.jar;c:\Programme\Java\jre1.5.0_07\lib\deploy.jar
set START=%START% -classpath c:\Programme\Java\jre1.5.0_07\lib\deploy.jar 
set START=%START% -Djnlpx.home=c:\Programme\Java\jre1.5.0_07\bin
set START=%START% -Djnlpx.splashport=3286
set START=%START% -Djnlpx.jvm=c:\programme\java\jre1.5.0_07\bin\javaw.exe
set START=%START% -Djnlpx.remove=false
set START=%START% -Djava.security.policy=file:c:\programme\java\jre1.5.0_07\lib\security\javaws.policy
set START=%START% -DtrustProxy=true
set START=%START% -Xverify:remote
set START=%START% -Djnlpx.heapsize=NULL,NULL
set START=%START% -Xdebug -Xnoagent -Xrunjdwp:transport=dt_socket,suspend=y,server=y,address=8888
set START=%START% com.sun.javaws.Main
set START=%START% http://localhost:8080/openthinclient-console/master.jnlp

%START%