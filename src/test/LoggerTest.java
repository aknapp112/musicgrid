package test;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LoggerTest {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Logger log = LoggerFactory.getLogger(LoggerTest.class);
		log.info("Hello world");

	}

}
