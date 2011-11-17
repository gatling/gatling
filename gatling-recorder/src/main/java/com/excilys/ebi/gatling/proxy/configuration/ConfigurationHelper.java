package com.excilys.ebi.gatling.proxy.configuration;

import static com.excilys.ebi.gatling.proxy.ui.Constants.GATLING_RECORDER_FILE_NAME;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import com.excilys.ebi.gatling.proxy.ui.enumeration.ResultType;
import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.DomDriver;

public final class ConfigurationHelper {

	private static final XStream XSTREAM = new XStream(new DomDriver());

	private static final File CONFIGURATION_FILE = new File(System.getProperty("user.home"), GATLING_RECORDER_FILE_NAME);

	static {
		XSTREAM.alias("resultType", ResultType.class);
		XSTREAM.alias("configuration", Configuration.class);
	}

	private ConfigurationHelper() {
		throw new UnsupportedOperationException();
	}

	public static Configuration readFromDisk() {

		if (CONFIGURATION_FILE.exists()) {
			try {
				return (Configuration) XSTREAM.fromXML(CONFIGURATION_FILE);
			} catch (Exception e) {
				System.err.println(e);
				return null;
			}
		} else {
			return null;
		}
	}

	public static void saveToDisk(Configuration configuration) {

		FileWriter fw = null;
		try {
			fw = new FileWriter(CONFIGURATION_FILE);
			XSTREAM.toXML(configuration, fw);

		} catch (IOException e) {
			System.err.println(e);

		} finally {
			if (fw != null) {
				try {
					fw.close();
				} catch (IOException e) {
					System.err.println(e);
				}
			}
		}
	}
}
