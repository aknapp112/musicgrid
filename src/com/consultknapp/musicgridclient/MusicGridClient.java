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
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import static java.nio.file.StandardCopyOption.*;


import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.common.IOUtils;
import net.schmizz.sshj.connection.channel.direct.Session;
import net.schmizz.sshj.connection.channel.direct.Session.Command;
import net.schmizz.sshj.xfer.FileSystemFile;

import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.audio.AudioHeader;
import org.jaudiotagger.audio.exceptions.CannotReadException;
import org.jaudiotagger.audio.exceptions.CannotWriteException;
import org.jaudiotagger.audio.exceptions.InvalidAudioFrameException;
import org.jaudiotagger.audio.exceptions.ReadOnlyFileException;
import org.jaudiotagger.tag.FieldKey;
import org.jaudiotagger.tag.Tag;
import org.jaudiotagger.tag.TagException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;



/** This examples demonstrates how a remote command can be executed. */
public class MusicGridClient {

	static Logger log;
	static String username;
	static String password;
	static String hostname;
	static String serverCmd;
	static String trackerURL;
	static String torrentWatchDir;
	static String torrentTempDir;
	static String torrentDest;
	static String pathToCreateTorrent;
	static String pathToPython;
	static String pathToFlac;
	static String pathToLame;
	static String userMP3Lib;
	static String directorySeperator;
	static String torrentName;
	static boolean createMP3;
	
    public static void main(String... args) throws IOException {
    	
    	log = LoggerFactory.getLogger(MusicGridClient.class);
    	Properties myProps = new Properties();
    	FileInputStream MyInputStream = new FileInputStream("mgc.props");
    	myProps.load(MyInputStream);
    	
    	log.info("Starting MusicGridClient!");
    	log.info("loading properties from property file");
    	
    	username = myProps.getProperty("user.name");
    	password = myProps.getProperty("user.password");
    	hostname = myProps.getProperty("host.name");
    	serverCmd = myProps.getProperty("host.command");
    	trackerURL = myProps.getProperty("host.tracker.url");
    	torrentWatchDir = myProps.getProperty("torrent.watch.dir");
    	torrentTempDir = myProps.getProperty("torrent.temp.dir");
    	torrentDest = myProps.getProperty("host.torrent.dest");
    	pathToCreateTorrent = myProps.getProperty("path.to.createtorrent");
    	pathToPython = myProps.getProperty("path.to.python");
    	pathToFlac = myProps.getProperty("path.to.flac");
    	pathToLame = myProps.getProperty("path.to.lame");
    	//String test = myProps.getProperty("user.create.mp3");
    	createMP3 = Boolean.parseBoolean(myProps.getProperty("user.create.mp3"));
    	userMP3Lib = myProps.getProperty("user.mp3.lib");
    	
    	
    	// get the file seperator from the system
    	directorySeperator = System.getProperty("file.separator");
    	
    	torrentName = args[0];
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
    	log.info("pathToFlac = " + pathToFlac);
    	log.info("pathToLame = " + pathToLame);
    	log.info("createMP3 = " +  Boolean.toString(createMP3));
    	
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
               
        SSHClient ssh;
        ssh = new SSHClient();
        
		try {	
			ssh.loadKnownHosts();
			ssh.connect(hostname);
		} catch (Exception e) {
			log.error(e.getMessage());
		}
               
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
            } catch (Exception e){
            	
            	
            }
            finally {
                session.close();
            }
        } finally {
            ssh.disconnect();
        }
        
        //now move the torrent from the temp dir to the watch directory
        File sourceFile = new File(pathToTorrent);
        File destFile = new File (pathToWatch);
        
        log.info("Renaming file: " + sourceFile.getCanonicalPath() + " to: " + destFile.getCanonicalPath());
		try {
			sourceFile.renameTo(destFile);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			log.error(e.getMessage());
		}
		
		//convert to MP3 if you dare!
		log.debug("Should I create mp3?");
		if (createMP3){
			log.info("Converting mp3s...");		
			MP3Convert(directoryPath);
		} else {
			log.info("NOT Converting mp3s");
		}
		
		log.info("MusicGridClient exiting...");
    }
 
    
	public static void MP3Convert(String directory)   {
		
		log.info("Entering MP3Convert...");
		log.debug("directory = " + directory);
		//File testFile = new File("C:\\Users\\Andy\\flac_test\\test.flac");
		AudioFile af = new AudioFile();
		// get the file seperator from the system
    	// String directorySeperator = System.getProperty("file.separator");
		
		// argument is direcotry name
		File dir = new File(directory);
		
		FilenameFilter filter = new FilenameFilter() {
		    public boolean accept(File dir, String name) {
		        return name.endsWith(".flac");
		    }
		};
		
		String[] children = dir.list(filter);
		String[] mp3List = null;
		//String[] children = dir.list();
		
		if (children == null) {
		    // Either dir does not exist or is not a directory
					
		} else {
		    for (int i=0; i<children.length; i++) {
		        // Get filename of file or directory
		        String filename = children[i];
		        //mp3List[i] = filename.replaceAll("flac", "mp3");
		        
		        log.info("Processing: " + filename);
		        File testFile = new File(dir + directorySeperator + filename);
		        
		        try {
					af = AudioFileIO.read(testFile);
					
				} catch (CannotReadException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (TagException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (ReadOnlyFileException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (InvalidAudioFrameException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
				
		        //do interesting things
		        //parseFLAC(af);
		        log.info(changeFileExt(filename, "mp3"));
		        //encodeFLACtoWAV("c:" + directorySeperator + "Users" + directorySeperator + "Andy" + directorySeperator + "flac_test" + directorySeperator + filename);
		        //encodeFLACtoMP3("c:" + directorySeperator + "Users" + directorySeperator + "Andy" + directorySeperator + "flac_test" + directorySeperator + filename);
		        if (filename.indexOf("flac") != -1 ){
		        	log.info("FLAC file detected, calling mp3 encoding procedure!");
		        	encodeFLACtoMP3(dir + directorySeperator + filename);
		        } else {
		        	log.info("Not a a flac, skipping - " + filename);
		        }
		        
		        
		    }
		}
		
		
		
		
		
		
		
	}
	
	public static boolean parseFLAC(AudioFile f){
		
		Tag t = f.getTag();
		AudioHeader ah = f.getAudioHeader();
		
	    log.info(f.toString());
		
		return true;
	}
	
	public static String changeFileExt(String filename, String newExt){
		
		int dotPos = filename.lastIndexOf(".");
		//String strExtension = filename.substring(dotPos + 1);
		String strFilename = filename.substring(0, dotPos);
		String newfileExtension = newExt;
		String strNewFileName = strFilename + "." + newfileExtension;
		
		return strNewFileName;	
		
	}

	public static void encodeFLACtoWAV(String flacFile){
		
		
		
		ProcessBuilder pb = new ProcessBuilder(pathToFlac, "-d","-f", flacFile);
    	pb.redirectErrorStream(true);
    	
    	log.info("Starting flac!");
    	Process p;
		BufferedReader br;
		try {
			p = pb.start();
			
			InputStream is = p.getInputStream();
			InputStreamReader isr = new InputStreamReader(is);
			br = new BufferedReader(isr);
			String line;

			log.info("Output of running flac is: ");

			while ((line = br.readLine()) != null) {
				log.info(line);
			}
			
			br.close();
	        p.destroy();
	        
	      

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
		
						
		}
        
        
		
		
	}

	public static void encodeWAVtoMP3(String wavFile){
		
		
		
		ProcessBuilder pb = new ProcessBuilder(pathToLame, "-V0", "-q","-f","--silent", wavFile);
		
    	pb.redirectErrorStream(true);
    	
    	log.info("Starting lame!");
    	Process p;
		BufferedReader br;
		try {
			p = pb.start();
			
			InputStream is = p.getInputStream();
			InputStreamReader isr = new InputStreamReader(is);
			br = new BufferedReader(isr);
			String line;

			log.info("Output of running lame is: ");

			while ((line = br.readLine()) != null) {
				log.info(line);
			}
			
			br.close();
	        p.destroy();
	        
	        
	        
	        

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
		
						
		}
        
        
		
		
	}

	public static void encodeFLACtoMP3(String flacFile){
		
		String wavFile = "";
		
		encodeFLACtoWAV(flacFile);
		
		//TODO add code to transport tags
		
		wavFile = changeFileExt(flacFile, "wav");
		String mp3File = changeFileExt(flacFile, "mp3");
		
		encodeWAVtoMP3(wavFile);
		
		File f = new File(wavFile);
		f.delete();
		
		copyTags(flacFile, mp3File);
		moveToLib(mp3File);
	}

	public static void copyTags(String flacFile, String mp3File){
		
		File flacF = new File(flacFile);
		File mp3F = new File(mp3File);
		
		
		try {
			AudioFile flac = AudioFileIO.read(flacF);
			AudioFile mp3 = AudioFileIO.read(mp3F);
			
			Tag flacTag = flac.getTag();
			Tag mp3Tag = mp3.getTagOrCreateAndSetDefault();
			
			log.info("Artist: " + flacTag.getFirst(FieldKey.ARTIST));
			log.info("Album: " + flacTag.getFirst(FieldKey.ALBUM));
			log.info("Title: " + flacTag.getFirst(FieldKey.TITLE));
			log.info("Comment: " + flacTag.getFirst(FieldKey.COMMENT));
			log.info("Year: " + flacTag.getFirst(FieldKey.YEAR));
			log.info("Track: " + flacTag.getFirst(FieldKey.TRACK));
			//log.info("Disc No.: " + flacTag.getFirst(FieldKey.DISC_NO));
			log.info("Composer: " + flacTag.getFirst(FieldKey.COMPOSER));
			log.info("Artist Sort: " + flacTag.getFirst(FieldKey.ARTIST_SORT));
			log.info("Genre: " + flacTag.getFirst(FieldKey.GENRE));
			
			mp3Tag.setField(FieldKey.ARTIST, flacTag.getFirst(FieldKey.ARTIST));
			mp3Tag.setField(FieldKey.ALBUM, flacTag.getFirst(FieldKey.ALBUM));
			mp3Tag.setField(FieldKey.TITLE, flacTag.getFirst(FieldKey.TITLE));
			mp3Tag.setField(FieldKey.COMMENT, flacTag.getFirst(FieldKey.COMMENT));
			mp3Tag.setField(FieldKey.YEAR, flacTag.getFirst(FieldKey.YEAR));
			mp3Tag.setField(FieldKey.TRACK, flacTag.getFirst(FieldKey.TRACK));
			//mp3Tag.setField(FieldKey.DISC_NO, flacTag.getFirst(FieldKey.DISC_NO));
			mp3Tag.setField(FieldKey.COMPOSER, flacTag.getFirst(FieldKey.COMPOSER));
			mp3Tag.setField(FieldKey.ARTIST_SORT, flacTag.getFirst(FieldKey.ARTIST_SORT));
			mp3Tag.setField(FieldKey.GENRE, flacTag.getFirst(FieldKey.GENRE));
			
			
			
			AudioFileIO.write(mp3);
			
			
			
			
			
			
		} catch (CannotReadException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (TagException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ReadOnlyFileException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InvalidAudioFrameException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (CannotWriteException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	
		
		
		
	}

	public static void moveToLib(String mp3File){
		
		log.info("Moving " + "'" + mp3File +"'" + " to " + "'" + userMP3Lib + directorySeperator + torrentName + "'" ); 
		boolean success = false;;
		File f = new File(userMP3Lib + directorySeperator + torrentName);
		
		if (!f.exists()){
			 success = f.mkdirs();
		}
		
		
		
		if (success){
			log.info("Directories created: " + userMP3Lib + directorySeperator + torrentName);
		}
		
		String filename = mp3File.substring(mp3File.lastIndexOf("\\"), mp3File.length()); 
		Path sourceFile = Paths.get(mp3File);
	    Path destFile = Paths.get(userMP3Lib + directorySeperator + torrentName + directorySeperator + filename);
	    
	    
	    
		try {
			log.info("Renaming file: " + sourceFile.toString() + " to: " + destFile.toString());
			//sourceFile.renameTo(destFile);
			Files.move(sourceFile, destFile, REPLACE_EXISTING);
			
		} catch (Exception e) {
			// TODO Auto-generated catch block
			log.error(e.getMessage());
		}
			
		
	}

}
