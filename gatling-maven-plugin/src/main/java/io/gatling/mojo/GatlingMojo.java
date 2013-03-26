/**
 * Copyright 2011-2013 eBusiness Information, Groupe Excilys (www.excilys.com)
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
package io.gatling.mojo;

import static java.util.Arrays.asList;
import static org.codehaus.plexus.util.StringUtils.trim;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.exec.ExecuteException;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.toolchain.Toolchain;
import org.apache.maven.toolchain.ToolchainManager;
import org.codehaus.plexus.util.DirectoryScanner;

import scala_maven_executions.JavaMainCaller;
import scala_maven_executions.MainHelper;
import scala_maven_executions.MainWithArgsInFile;

import io.gatling.app.CommandLineConstants;
import io.gatling.app.Gatling;

/**
 * Mojo to execute Gatling.
 *
 * @goal execute
 * @phase integration-test
 * @description Gatling Maven Plugin
 * @requiresDependencyResolution test
 */
public class GatlingMojo extends AbstractMojo {

	public static final String[] DEFAULT_INCLUDES = {"**/*.scala"};
	public static final String GATLING_MAIN_CLASS = "io.gatling.app.Gatling";

	public static final String[] JVM_ARGS = new String[]{"-server", "-XX:+UseThreadPriorities", "-XX:ThreadPriorityPolicy=42",
			"-Xms512M", "-Xmx512M", "-Xmn100M", "-Xss2M", "-XX:+HeapDumpOnOutOfMemoryError", "-XX:+AggressiveOpts", "-XX:+OptimizeStringConcat",
			"-XX:+UseFastAccessorMethods", "-XX:+UseParNewGC", "-XX:+UseConcMarkSweepGC", "-XX:+CMSParallelRemarkEnabled", "-XX:+CMSClassUnloadingEnabled",
			"-XX:CMSInitiatingOccupancyFraction=75", "-XX:+UseCMSInitiatingOccupancyOnly", "-XX:SurvivorRatio=8", "-XX:MaxTenuringThreshold=1"};

	/**
	 * Runs simulation but does not generate reports. By default false.
	 *
	 * @parameter expression="${gatling.noReports}" alias="nr"
	 * default-value="false"
	 * @description Runs simulation but does not generate reports
	 */
	private boolean noReports;

	/**
	 * Generates the reports for the simulation in this folder.
	 *
	 * @parameter expression="${gatling.reportsOnly}" alias="ro"
	 * @description Generates the reports for the simulation in this folder
	 */
	private String reportsOnly;

	/**
	 * Uses this file as the configuration file.
	 *
	 * @parameter expression="${gatling.configDir}" alias="cd"
	 * default-value="${basedir}/src/test/resources"
	 * @description Uses this file as the configuration directory
	 */
	private File configDir;

	/**
	 * Uses this folder to discover simulations that could be run
	 *
	 * @parameter expression="${gatling.simulationsFolder}" alias="sf"
	 * default-value="${basedir}/src/test/scala"
	 * @description Uses this folder to discover simulations that could be run
	 */
	private File simulationsFolder;

	/**
	 * Sets the list of include patterns to use in directory scan for
	 * simulations. Relative to simulationsFolder.
	 *
	 * @parameter
	 * @description Include patterns to use in directory scan for simulations
	 */
	private List<String> includes;

	/**
	 * Sets the list of exclude patterns to use in directory scan for
	 * simulations. Relative to simulationsFolder.
	 *
	 * @parameter
	 * @description Exclude patterns to use in directory scan for simulations
	 */
	private List<String> excludes;

	/**
	 * A name of a Simulation class to run. This takes precedence over the
	 * includes / excludes parameters.
	 *
	 * @parameter expression="${gatling.simulationClass}" alias="s"
	 * @description The name of the Simulation class to run
	 */
	private String simulationClass;

	/**
	 * Uses this folder as the folder where feeders are stored
	 *
	 * @parameter expression="${gatling.dataFolder}" alias="df"
	 * default-value="${basedir}/src/test/resources/data"
	 * @description Uses this folder as the folder where feeders are stored
	 */
	private File dataFolder;

	/**
	 * Uses this folder as the folder where request bodies are stored
	 *
	 * @parameter expression="${gatling.requestBodiesFolder}" alias="bf"
	 * default-value="${basedir}/src/test/resources/request-bodies"
	 * @description Uses this folder as the folder where request bodies are
	 * stored
	 */
	private File requestBodiesFolder;

	/**
	 * Uses this folder as the folder where results are stored
	 *
	 * @parameter expression="${gatling.resultsFolder}" alias="rf"
	 * default-value="${basedir}/target/gatling/results"
	 * @description Uses this folder as the folder where results are stored
	 */
	private File resultsFolder;

	/**
	 * Extra JVM arguments to pass when running Gatling.
	 *
	 * @parameter
	 */
	private List<String> jvmArgs;

	/**
	 * Forks the execution of Gatling plugin into a separate JVM.
	 *
	 * @parameter expression="${gatling.fork}" default-value="true"
	 * @description Forks the execution of Gatling plugin into a separate JVM
	 */
	private boolean fork;

	/**
	 * Will cause the project build to look successful, rather than fail, even
	 * if there are Gatling test failures. This can be useful on a continuous
	 * integration server, if your only option to be able to collect output
	 * files, is if the project builds successfully.
	 *
	 * @parameter expression="${gatling.failOnError}" default-value="true"
	 */
	private boolean failOnError;

	/**
	 * Force the name of the directory generated for the results of the run
	 *
	 * @parameter expression="${gatling.outputName}" alias="on"
	 * @description Uses this as the base name of the results folder
	 */
	private String outputDirectoryBaseName;
	
	/**
	 * Propagates System properties in fork mode to forked process
	 *
	 * @parameter expression="${gatling.propagateSystemProperties}" default-value="true"
	 * @description Propagates System properties in fork mode to forked process
	 */
	private boolean propagateSystemProperties;

	/**
	 * The Maven Project
	 *
	 * @parameter expression="${project}"
	 * @required
	 * @readonly
	 */
	private MavenProject mavenProject;

	/**
	 * The Maven Session Object
	 *
	 * @parameter expression="${session}"
	 * @required
	 * @readonly
	 */
	private MavenSession session;

	/**
	 * The toolchain manager to use.
	 *
	 * @component
	 * @required
	 * @readonly
	 */
	private ToolchainManager toolchainManager;

	/**
	 * Executes Gatling simulations.
	 */
	@Override
	public void execute() throws MojoExecutionException {
		// Create results directories
		resultsFolder.mkdirs();
		try {
			executeGatling(jvmArgs().toArray(new String[0]), gatlingArgs().toArray(new String[0]));
		} catch (Exception e) {
			if (failOnError) {
				throw new MojoExecutionException("Gatling failed.", e);
			} else {
				getLog().warn("There was some errors while running your simulation, but failOnError set to false won't fail your build.");
			}
		}
	}

	private void executeGatling(String[] jvmArgs, String[] gatlingArgs) throws Exception {
		// Setup classpath
		String testClasspath = buildTestClasspath();
		// Setup toolchain
		Toolchain toolchain = toolchainManager.getToolchainFromBuildContext("jdk", session);
		if (fork) {
			JavaMainCaller caller = new GatlingJavaMainCallerByFork(this, GATLING_MAIN_CLASS, testClasspath, jvmArgs, gatlingArgs, false, toolchain, propagateSystemProperties);
			try {
				caller.run(false);
			} catch (ExecuteException e) {
				if (e.getExitValue() == Gatling.SIMULATION_ASSERTIONS_FAILED()) {
					throw new GatlingSimulationAssertionsFailedException(e);
				}
			}
		} else {
			GatlingJavaMainCallerInProcess caller = new GatlingJavaMainCallerInProcess(this, GATLING_MAIN_CLASS, testClasspath, gatlingArgs);
			int returnCode = caller.run();
			if (returnCode == Gatling.SIMULATION_ASSERTIONS_FAILED()) {
				throw new GatlingSimulationAssertionsFailedException();
			}
		}
	}

	private String buildTestClasspath() throws Exception {
		@SuppressWarnings("unchecked")
        List<String> testClasspathElements = (List<String>) mavenProject.getTestClasspathElements();
		testClasspathElements.add(configDir.getPath());
		// Find plugin jar and add it to classpath
		testClasspathElements.add(MainHelper.locateJar(GatlingMojo.class));
		// Jenkins seems to need scala-maven-plugin in the test classpath in order to work
		testClasspathElements.add(MainHelper.locateJar(MainWithArgsInFile.class));
		return MainHelper.toMultiPath(testClasspathElements);
	}

	private List<String> jvmArgs() {
		List<String> jvmArguments = (jvmArgs != null) ? jvmArgs : new ArrayList<String>();
		jvmArguments.addAll(Arrays.asList(JVM_ARGS));
		return jvmArguments;
	}

	private List<String> gatlingArgs() throws Exception {
		// Solves the simulations, if no simulation file is defined
		if (simulationClass == null) {
			List<String> simulations = resolveSimulations(simulationsFolder, includes, excludes);

			if (simulations.isEmpty()) {
				getLog().error("No simulations to run");
				throw new MojoFailureException("No simulations to run");

			} else if (simulations.size() > 1) {
				getLog().error("More than 1 simulation to run, need to specify one");
				throw new MojoFailureException("More than 1 simulation to run, need to specify one");

			} else {
				simulationClass = simulations.get(0);
			}
		}

		// Arguments
		List<String> args = new ArrayList<String>();
		args.addAll(asList("-" + CommandLineConstants.CLI_DATA_FOLDER(), dataFolder.getCanonicalPath(),//
				"-" + CommandLineConstants.CLI_RESULTS_FOLDER(), resultsFolder.getCanonicalPath(),// ;
				"-" + CommandLineConstants.CLI_REQUEST_BODIES_FOLDER(), requestBodiesFolder.getCanonicalPath(),//
				"-" + CommandLineConstants.CLI_SIMULATIONS_FOLDER(), simulationsFolder.getCanonicalPath(),//
				"-" + CommandLineConstants.CLI_SIMULATION(), simulationClass));

		if (noReports) {
			args.add("-" + CommandLineConstants.CLI_NO_REPORTS());
		}

		if (reportsOnly != null) {
			args.addAll(asList("-" + CommandLineConstants.CLI_REPORTS_ONLY(), reportsOnly));
		}

		if (outputDirectoryBaseName != null) {
			args.addAll(asList("-" + CommandLineConstants.CLI_OUTPUT_DIRECTORY_BASE_NAME(), outputDirectoryBaseName));
		}

		return args;
	}

	public static String fileNameToClassName(String fileName) {
		String trimmedFileName = trim(fileName);

		int lastIndexOfExtensionDelim = trimmedFileName.lastIndexOf(".");
		String strippedFileName = lastIndexOfExtensionDelim > 0 ? trimmedFileName.substring(0, lastIndexOfExtensionDelim) : trimmedFileName;

		return strippedFileName.replace(File.separatorChar, '.');
	}

	/**
	 * Resolve simulation files to execute from the simulation folder and
	 * includes/excludes.
	 *
	 * @return a comma separated String of simulation class names.
	 */
	private List<String> resolveSimulations(File simulationsFolder, List<String> includes, List<String> excludes) {
		DirectoryScanner scanner = new DirectoryScanner();

		// Set Base Directory
		getLog().debug("effective simulationsFolder: " + simulationsFolder.getPath());
		scanner.setBasedir(simulationsFolder);

		// Resolve includes
		if (includes != null && !includes.isEmpty()) {
			scanner.setIncludes(includes.toArray(new String[includes.size()]));
		} else {
			scanner.setIncludes(DEFAULT_INCLUDES);
		}

		// Resolve excludes
		if (excludes != null && !excludes.isEmpty()) {
			scanner.setExcludes(excludes.toArray(new String[excludes.size()]));
		}

		// Resolve simulations to execute
		scanner.scan();

		String[] includedFiles = scanner.getIncludedFiles();

		List<String> includedClassNames = new ArrayList<String>();
		for (String includedFile : includedFiles) {
			includedClassNames.add(fileNameToClassName(includedFile));
		}

		getLog().debug("resolved simulation classes: " + includedClassNames);
		return includedClassNames;
	}
}
