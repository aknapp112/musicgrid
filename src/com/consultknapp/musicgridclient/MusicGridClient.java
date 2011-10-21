/*
 * Copyright 2010, 2011 sshj contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.consultknapp.musicgridclient;


import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.common.IOUtils;
import net.schmizz.sshj.connection.channel.direct.Session;
import net.schmizz.sshj.connection.channel.direct.Session.Command;
import net.schmizz.sshj.xfer.FileSystemFile;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;



/** This examples demonstrates how a remote command can be executed. */
public class MusicGridClient {

    public static void main(String... args)
            throws IOException {
    	
    	Logger log = LoggerFactory.getLogger(MusicGridClient.class);
    	Properties myProps = new Properties();
    	FileInputStream MyInputStream = new FileInputStream("mgc.props");
    	myProps.load(MyInputStream);
    	
    	log.info("Starting MusicGridClient!");
    	log.info("loading properties from property file");
    	
    	String username = myProps.getProperty("user.name");
    	String password = myProps.getProperty("user.password");
    	String hostname = myProps.getProperty("host.name");
    	String serverCmd = myProps.getProperty("host.command");
    	String trackerURL = myProps.getProperty("host.tracker.url");
    	String torrentWatchDir = myProps.getProperty("torrent.watch.dir");
    	String torrentTempDir = myProps.getProperty("torrent.temp.dir");
    	String torrentDest = myProps.getProperty("host.torrent.dest");
    	String pathToCreateTorrent = myProps.getProperty("path.to.createtorrent");
    	String pathToPython = myProps.getProperty("path.to.python");
    	
    	// get the file seperator from the system
    	String directorySeperator = System.getProperty("file.separator");
    	
    	String torrentFile = args[0] + ".torrent";
    	String trackerName = args[1];
    	// quotes added below for linux test
    	String directoryPath = args[2];
    	String pathToTorrent = torrentTempDir + directorySeperator + torrentFile;
    	String pathToWatch = torrentWatchDir + directorySeperator + torrentFile;
    	
    	log.info("pathToPython = " + pathToPython);
    	log.info("pathToCreateTorrent = " + pathToCreateTorrent);
    	log.info("torrentTempDir = " + torrentTempDir);
    	log.info("directoryPath = " + directoryPath);
    	log.info("trackerURL = " + trackerURL);
    	log.info("pathToTorrent = " + pathToTorrent);
    	
    	int indexfound = trackerName.indexOf(trackerURL);
    		
    	//if greater than -1, means we found it
    	if (indexfound > -1){
    		log.info("Our tracker was found, aborting.");
    		//here's where we bail if we find our own tracker
    		System.exit(0);
    	}
    	
    	log.info("Did not find our tracker, this torrent appears to be from an outside source.  Continuing...");
    	
    	//the tracker is not ours, so let's build the torrent!
    	ProcessBuilder pb = new ProcessBuilder(pathToPython, pathToCreateTorrent, "-o", torrentTempDir, directoryPath, trackerURL);
    	pb.redirectErrorStream(true);
    	
    	log.info("Starting python!");
    	Process p = pb.start();
    	    
    	InputStream is = p.getInputStream();
        InputStreamReader isr = new InputStreamReader(is);
        BufferedReader br = new BufferedReader(isr);
        String line;

        log.info("Output of running pycreatetorrent is: ");

        while ((line = br.readLine()) != null) {
        	log.info(line);
        }
        
        br.close();
        p.destroy();
        
       
        //
        //do ssh-y stuff
        //
               
        final SSHClient ssh = new SSHClient();
        ssh.loadKnownHosts();
        ssh.connect(hostname);
        
        
        try {
          
        	ssh.authPassword(username, password);
            final Session session = ssh.startSession();
            try {
            	//next we need to copy up the torrent file with scp
            	log.info("Copying file " + pathToTorrent + " to " + hostname + ":");
            	ssh.newSCPFileTransfer().upload(new FileSystemFile(pathToTorrent), torrentDest);
            	
            	//once transfer is complete, we want to update the rss feed
                final Command cmd = session.exec(serverCmd + " " + "\"" + torrentFile + "\"");
                log.info("Executing command: " + serverCmd + " on host " + hostname);
                log.info(IOUtils.readFully(cmd.getInputStream()).toString());
                cmd.join(5, TimeUnit.SECONDS);
                log.info("\n** exit status: " + cmd.getExitStatus());
            } finally {
                session.close();
            }
        } finally {
            ssh.disconnect();
        }
        
        //now move the torrent from the temp dir to the watch directory
        File sourceFile = new File(pathToTorrent);
        File destFile = new File (pathToWatch);
        
        log.info("Renaming file: " + sourceFile.getCanonicalPath() + " to: " + destFile.getCanonicalPath());
		sourceFile.renameTo(destFile);
		
    }
    
}
