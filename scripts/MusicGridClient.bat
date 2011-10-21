
c:
cd c:\MusicGridClient\scripts

java -cp ../lib/mgc.jar;../lib/sshj.jar;../lib/jzlib.jar;../lib/slf4j-api-1.6.2.jar;../lib/bcprov-jdk16-146.jar;../lib/logback-core-0.9.30.jar;../lib/logback-classic-0.9.30.jar;../conf/logback.xml com.consultknapp.musicgridclient.MusicGridClient %1 %2 %3 >> ../log/mgc.log