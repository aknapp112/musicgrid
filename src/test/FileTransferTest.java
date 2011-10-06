package test;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.channels.WritableByteChannel;


public class FileTransferTest {

	/**
	 * @param args
	 */
	public static void main(String[] args) throws IOException {
		
		String pathToSource = "C:\\users\\andy\\test.txt";
		String pathToDest = "C:\\users\\andy\\temp\\text.txt";
		
		File sourceFile = new File(pathToSource);
		File destFile = new File (pathToDest);
	    
		sourceFile.renameTo(destFile);
		
		/*
		if(!destFile.exists()) {
			  destFile.createNewFile();
			 }
	  
	        FileChannel source = null;
	        FileChannel destination = null;
	        
	        try {
	        	source = new FileInputStream(sourceFile).getChannel();
	        	destination = new FileInputStream(destFile).getChannel();
	        	destination.transferFrom(source, 0, source.size());
	        	sourceFile.delete();
	        	        	
	        }
	        finally {
	        	if (source != null){
	        		source.close();
	        	}
	        	if (destination != null){
	        		destination.close();
	        	}
	        }
	    */   
	}

}
