/*
 *                 Sun Public License Notice
 * 
 * The contents of this file are subject to the Sun Public License
 * Version 1.0 (the "License"). You may not use this file except in
 * compliance with the License. A copy of the License is available at
 * http://www.sun.com/
 * 
 * The Original Code is NetBeans OS/2 Launcher. 
 * The Initial Developer of the Original Code is Laszlo Kishalmi. 
 * Portions Copyright 2004-2004 Laszlo Kishalmi. All Rights Reserved.
 */
call RXFUNCADD "SysLoadFuncs", "RexxUtil", "sysloadfuncs";
call SysLoadFuncs;

PARSE ARG IDE_ARGS 
parse source . . prg_name

n = setlocal()
ENV = "OS2ENVIRONMENT"

tmpdir = value("tmp",,ENV)
jdkhome = value("JDK_HOME",,ENV)
netbeans_os2opt = VALUE("netbeans_os2opt",,ENV);

if length(jdkhome) = 0 then jdkhome = value("JAVA_HOME",,ENV)

plathome = directory(FILESPEC("drive", prg_name)||FILESPEC("path", prg_name)||"..");

jargs = '-Dnetbeans.home="'||plathome||'" '
java_launcher = 'java'

clusters = ''
cluster_roots.0 = 0

prefixcp = ''
postfixcp = ''
args = ''

nbenvfile = SysTempFilename(noslash(tmpdir)||"\nbenv??.???")
nbenvnullsep=false

'@set >'||nbenvfile
call parse_args(IDE_ARGS)

jargs='-Dnetbeans.dirs="'||clusters||'" '||jargs

/*
 * check JDK
 */
if \ exists(jdkhome||"\lib\tools.jar") then do
  say "Cannot find java. Please use the --jdkhome switch."
  exit 2
end
 
/*
 * main loop
 */
restart=1
first_time_starting=1

updater_class="org.netbeans.updater.UpdaterFrame"

do while restart

    cp = ""
    updatercp = ""
        
	cp = build_cp(plathome)
	
	if (exists(plathome||"\modules\ext\updater.jar")) then updatercp=plathome||"\modules\extupdater.jar"
	cp = cp||search_jars(jdkhome||"\lib")
	cp = prefixcp||cp||postfixcp
	
	updatercp = cp||updatercp
	
	if first_time_starting then do
	  run_updater = look_for_pre_runs(plathome) 
	  do I = 1 to cluster_roots.0 
	    run_updater = run_updater | look_for_pre_runs(cluster_roots.I)
	  end
	  run_updater = run_updater | look_for_pre_runs(userdir)
	  if run_updater then call do_run_updater
	  first_time_starting = 0
	end
	
	'@'||jdkhome||'\bin\'||java_launcher||' -Djdk.home="'||jdkhome||'" -cp "'||cp||'" -Dnetbeans.user="'||userdir||'" -Dnetbeans.osenv="'||nbenvfile||'" -Dnetbeans.osenv.nullsep='||nbenvnullsep||' '||jargs||' org.netbeans.Main '||args 
	
	exitcode = rc
	
	run_updater = look_for_post_runs(plathome)
    do I = 1 to cluster_roots.0 
      run_updater = run_updater | look_for_post_runs(cluster_roots.I)
    end
    run_updater = run_updater | look_for_post_runs(userdir)
	
	if run_updater then call do_run_updater
	
	restart = run_updater
	
end
'@del '||nbenvfile
n = endlocal()
exit exitcode

/*
 * Parse arguments
 */
parse_args: 
  PARSE ARG ideargs
  
  DO I = 1 to WORDS(ideargs)
    param = WORD(ideargs, I);
    select 
      when (param = "-?") | (param = "-h") | (param = "--help") | (param = "-help") then do
        say "Usage: nbexec.cmd {options} arguments"
        say ""
        say "General options:"
        say "  --help                show this help" 
        say "  --jdkhome <path>      path to Java(TM) 2 SDK, Standard Edition"
        say "  -J<jvm_option>        pass <jvm_option> to JVM"
        say ""
        say "  --cp:p <classpath>    prepend <classpath> to classpath"
        say "  --cp:a <classpath>    append <classpath> to classpath"
        say ""
        say "Platform specific options:"
        say "  --os2-windowed        invoke windowed JVM launcher (javaw)"
        if (symbol("netbeans_os2opt") = "VAR") then 
          say netbeans_os2opt
        say ""
        args = " --help"
      end
      when (param = "-jdkhome") | (param = "--jdkhome") then do
        jdkhome = WORD(ideargs, I + 1);
        I = I + 1
      end
      when (param = "-userdir") | (param = "--userdir") then do
        userdir = WORD(ideargs, I + 1);
        I = I + 1
      end
      when (param = "-cp") | (param = "-cp:a") | (param = "--cp") | (param = "--cp:a") then do
        cp = WORD(ideargs, I + 1);
        postfixcp = postfixcp || cp || ";"
        I = I + 1
      end
      when (param = "-cp:p") | (param = "--cp:p") then do
        cp = WORD(ideargs, I + 1);
        prefixcp = prefixcp || cp || ";"
        I = I + 1
      end
      when param = "--clusters" then do
        clusters_ = translate(WORD(ideargs, I + 1), " ", ";")
        do C = 1 to WORDS(clusters_)
          call add_cluster(WORD(clusters_, C));
        end
        
        I = I + 1
      end
      when left(param, 2) = "-J" then do
        jopt = substr(param, 3)
        jargs = jargs || " " || jopt
      end
      when param = "--os2-windowed" then do
        java_launcher = "javaw.exe"
      end
      otherwise do     
        args = args || " " || param
      end
    end
  end
return

/*
 * build classpath
 */
build_cp: procedure
  parse arg base
  cp = ""
  cp = cp || search_jars(base||"\lib")
  cp = cp || search_jars(base||"\lib\locale")
return cp

do_run_updater:
  '@'||jdkhome||'\bin\'||java_launcher||' -classpath "'||updatercp||'" '||jargs||' -Dnetbeans.user='||userdir||' '||updater_class||' '||args 
return

look_for_pre_runs: procedure 
  
  parse arg base_dir 
  do_run = 0
  dir = base_dir||"\update\download"
  rc = SysFileTree(dir||"\*.nbm", nbm_files, "FO")
  do_run = do_run | (exists(dir||"\install_later.xml") & (nbm_files.0 > 0))
  
return do_run

look_for_post_runs: procedure
  
  parse arg base_dir 
  do_run = 0
  
  dir = base_dir||"\update\download"
  rc = SysFileTree(dir||"\*.nbm", nbm_files, "FO")
  do_run = do_run | (\exists(dir||"\install_later.xml") & (nbm_files.0 > 0))
  
return do_run

/*
 * Removes blackslash off directory name and returns the result.
 */
noslash: procedure
  parse arg directory
  directory=strip(directory, 't', '\')
return directory

/*
 * search for jar and zip files in a directory.
 */
search_jars: procedure
  parse arg dir;
  result = '';
  rc = SysFileTree(noslash(dir)|| "\*.jar", files, "FO")
  do i = 1 to files.0
    result = result||files.i||';'
  end
  rc = SysFileTree(noslash(dir)|| "\*.zip", files, "FO")
  do i = 1 to files.0
    result = result||files.i||';'
  end
return result;

add_cluster: procedure expose cluster_roots. clusters
  parse arg dir
  if length(clusters) > 0 then   
    clusters = clusters ||';'|| dir
  else clusters = dir
  index = cluster_roots.0 + 1
  cluster_roots.index = dir
  cluster_roots.0 = index
  
return

/*
 * Check the existence of a file.
 */
exists: procedure
  parse arg file
  fileex = length(stream(file, "C", "QUERY EXISTS")) > 0;
return fileex
