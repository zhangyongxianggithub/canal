#!/bin/bash 

current_path=`pwd`
case "`uname`" in
    Linux)
		bin_abs_path=$(readlink -f $(dirname $0))
		;;
	*)
		bin_abs_path=`cd $(dirname $0); pwd`
		;;
esac
base=${bin_abs_path}/..
canal_conf=$base/conf/canal.properties
logback_configurationFile=$base/conf/logback.xml
export LANG=en_US.UTF-8
export BASE=$base

if [ -f $base/bin/canal.pid ] ; then
	echo "found canal.pid , Please run stop.sh first ,then startup.sh" 2>&2
    exit 1
fi

if [ ! -d $base/logs/canal ] ; then 
	mkdir -p $base/logs/canal
fi

## set java path
if [ -z "$JAVA" ] ; then
  JAVA=$(which java)
fi
if [ -z "$JAVA" ]; then
  	echo "Cannot find a Java JDK. Please set either set JAVA or put java (>=1.5) in your PATH." 2>&2
    exit 1
fi
JAVA_OPTS="$JAVA_OPTS -Xss128m -XX:+AggressiveOpts -XX:-UseBiasedLocking -XX:-OmitStackTraceInFastThrow -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=$base/logs"
JAVA_OPTS="$JAVA_OPTS -XX:+UseFastAccessorMethods -XX:+PrintAdaptiveSizePolicy -XX:+PrintTenuringDistribution"
JAVA_OPTS="-server -Xms2g -Xmx8g -Xmn1g -XX:SurvivorRatio=2 -XX:PermSize=96m -XX:MaxPermSize=256m -XX:MaxTenuringThreshold=15 -XX:+DisableExplicitGC $JAVA_OPTS"
JAVA_OPTS=" $JAVA_OPTS -Djava.awt.headless=true -Djava.net.preferIPv4Stack=true -Dfile.encoding=UTF-8"
CANAL_OPTS="-DappName=otter-canal -Dlogback.configurationFile=$logback_configurationFile -Dcanal.conf=$canal_conf"

if [ -e $canal_conf -a -e $logback_configurationFile ]
then 
	
	for i in $base/lib/*;
		do CLASSPATH=$i:"$CLASSPATH";
	done
 	CLASSPATH="$base/conf:$CLASSPATH";
 	
 	echo "cd to $bin_abs_path for workaround relative path"
  	cd $bin_abs_path
 	
	echo LOG CONFIGURATION : $logback_configurationFile
	echo canal conf : $canal_conf 
	echo CLASSPATH :$CLASSPATH
	$JAVA $JAVA_OPTS $JAVA_DEBUG_OPT $CANAL_OPTS -classpath .:$CLASSPATH com.alibaba.otter.canal.deployer.CanalLauncher
else 
	echo "canal conf("$canal_conf") OR log configration file($logback_configurationFile) is not exist,please create then first!"
fi
