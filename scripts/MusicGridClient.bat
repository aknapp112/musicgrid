
c:
cd c:\MusicGridClient\scripts

"C:\Program Files\Java\jdk1.7.0_01\bin\java.exe" -cp ../lib/mgc.jar;../lib/sshj.jar;../lib/jzlib.jar;../lib/slf4j-api-1.6.2.jar;../lib/bcprov-jdk16-146.jar;../lib/logback-core-0.9.30.jar;../lib/logback-classic-0.9.30.jar;../lib/jat.jar;../conf/logback.xml com.consultknapp.musicgridclient.MusicGridClient %1 %2 %3 >> ../log/mgc.log

