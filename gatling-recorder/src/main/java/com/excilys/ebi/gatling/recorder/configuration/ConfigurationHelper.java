/**
 * Copyright 2011-2012 eBusiness Information, Groupe Excilys (www.excilys.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * 		http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.excilys.ebi.gatling.recorder.configuration;

import static com.excilys.ebi.gatling.recorder.ui.Constants.GATLING_RECORDER_FILE_NAME;
import static org.apache.commons.io.IOUtils.closeQuietly;
import static org.kohsuke.args4j.ExampleMode.ALL;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.excilys.ebi.gatling.recorder.ui.enumeration.ResultType;
import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.DomDriver;

import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;

public final class ConfigurationHelper {

	private static final Logger logger = LoggerFactory.getLogger(ConfigurationHelper.class);

	private static final XStream XSTREAM = new XStream(new DomDriver());

	private static final File CONFIGURATION_FILE = new File(System.getProperty("user.home"), GATLING_RECORDER_FILE_NAME);

	static {
		XSTREAM.alias("resultType", ResultType.class);
		XSTREAM.alias("configuration", Configuration.class);
		XSTREAM.alias("pattern", Pattern.class);
		XSTREAM.alias("proxy", ProxyConfig.class);
	}

	private ConfigurationHelper() {
		throw new UnsupportedOperationException();
	}

	public static void initConfiguration(String[] args) {
		initFromDisk();
		initFromCommandLine(args);
	}

	private static void initFromCommandLine(String[] args) {
		CommandLineOptions cliOpts = new CommandLineOptions();
		CmdLineParser parser = new CmdLineParser(cliOpts);
		try {
			parser.parseArgument(args);
			if (!cliOpts.isResultText() && !cliOpts.isResultScala())
				cliOpts.setResultText(true);

			if (cliOpts.isRunningFrame()) {
				if (cliOpts.getOutputFolder() == null)
					throw new CmdLineException(parser, "'-run' must be used with '-of'\n");
			}

			Configuration.initFromCommandLineOptions(cliOpts);
		} catch (CmdLineException e) {
			System.err.println(e.getMessage() + "\n");
			parser.printUsage(System.err);
			System.err.println("\n\tExample: gatling-recorder " + parser.printExample(ALL));
			System.exit(0);
		}
	}

	private static void initFromDisk() {
		if (CONFIGURATION_FILE.exists()) {
			try {
				Configuration c = (Configuration) XSTREAM.fromXML(CONFIGURATION_FILE);
				Configuration.initFromExistingConfig(c);
			} catch (Exception e) {
				logger.error(e.getMessage());
			}
		}
	}

	public static void saveToDisk() {

		FileWriter fw = null;
		try {
			fw = new FileWriter(CONFIGURATION_FILE);
			XSTREAM.toXML(Configuration.getInstance(), fw);
		} catch (IOException e) {
			logger.error(e.getMessage());
		} finally {
			closeQuietly(fw);
		}
	}
}
