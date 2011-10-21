package test;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.common.IOUtils;
import net.schmizz.sshj.connection.channel.direct.Session;
import net.schmizz.sshj.connection.channel.direct.Session.Command;
import net.schmizz.sshj.userauth.keyprovider.KeyProvider;

public class SshTest {
	
	
	public static void main(String[] args) throws IOException {
		
		
		
		
    	Properties myProps = new Properties();
    	FileInputStream MyInputStream = new FileInputStream("mgc.props");
    	myProps.load(MyInputStream);
    	
    	String username = myProps.getProperty("user.name");
    	String password = myProps.getProperty("user.password");
    	String hostname = myProps.getProperty("host.name");
    	String serverCmd = myProps.getProperty("host.command");
    	String trackerURL = myProps.getProperty("host.tracker.url");
    	String torrentWatchDir = myProps.getProperty("torrent.watch.dir");
    	String torrentTempDir = myProps.getProperty("torrent.temp.dir");
    	String trackerDest = myProps.getProperty("host.trackerdest");
    	String pathToCreateTorrent = myProps.getProperty("path.to.createtorrent");
    	String pathToPython = myProps.getProperty("path.to.python");
    	
		final SSHClient ssh = new SSHClient();
        ssh.loadKnownHosts();
        File privateKey = new File("C:\\users\\andy\\.ssh\\musicman_rsa");
        KeyProvider keys = ssh.loadKeys(privateKey.getPath());
        
       // ssh.authPublickey(username);
        
        //ssh.connect(hostname);
        
        
        try {
          
        	ssh.authPublickey(System.getProperty("user.name"), keys);
        	
        	
            final Session session = ssh.startSession();
            try {
            	//next we need to copy up the torrent file with scp
            	//System.out.println("Copying file " + pathToTorrent + " to " + hostname + ":");
            	//ssh.newSCPFileTransfer().upload(new FileSystemFile(pathToTorrent), trackerDest);
            	
            	//once transfer is complete, we want to update the rss feed
                //final Command cmd = session.exec(serverCmd + " " + "\"" + torrentFile + "\"");
            	Command cmd = session.exec("ping -c1 www.google.com");
                System.out.println("Executing command: " + cmd + " on host " + hostname);
                System.out.println(IOUtils.readFully(cmd.getInputStream()).toString());
                cmd.join(5, TimeUnit.SECONDS);
                System.out.println("\n** exit status: " + cmd.getExitStatus());
            } finally {
               // session.close();
            }
        } finally {
           // ssh.disconnect();
        }
        
	}

}
