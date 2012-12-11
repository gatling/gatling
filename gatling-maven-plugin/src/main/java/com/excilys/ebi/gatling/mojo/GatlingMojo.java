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
package com.excilys.ebi.gatling.mojo;

import static java.util.Arrays.asList;
import static org.codehaus.plexus.util.StringUtils.*;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.apache.maven.toolchain.Toolchain;
import scala_maven_executions.JavaMainCaller;
import scala_maven_executions.JavaMainCallerByFork;
import scala_maven_executions.JavaMainCallerInProcess;
import scala_maven_executions.MainHelper;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.toolchain.ToolchainManager;
import org.apache.tools.ant.DirectoryScanner;
import com.excilys.ebi.gatling.app.CommandLineConstants;

/**
 * Mojo to execute Gatling.
 *
 * @goal execute
 * @phase integration-test
 * @description Gatling Maven Plugin
 * @requiresDependencyResolution test
 */
public class GatlingMojo extends AbstractMojo {

	public static final String[] DEFAULT_INCLUDES = { "**/*.scala" };
	public static final String GALTING_MAIN_CLASS = "com.excilys.ebi.gatling.app.Gatling";

	public static final String[] JVM_ARGS = new String[] {"-server","-XX:+UseThreadPriorities","-XX:ThreadPriorityPolicy=42",
			"-Xms512M","-Xmx512M","-Xmn100M","-Xss1024k","-XX:+HeapDumpOnOutOfMemoryError","-XX:+AggressiveOpts","-XX:+OptimizeStringConcat",
			"-XX:+UseFastAccessorMethods","-XX:+UseParNewGC","-XX:+UseConcMarkSweepGC","-XX:+CMSParallelRemarkEnabled","-XX:+CMSClassUnloadingEnabled",
			"-XX:CMSInitiatingOccupancyFraction=75","-XX:+UseCMSInitiatingOccupancyOnly","-XX:SurvivorRatio=8","-XX:MaxTenuringThreshold=1"};

	/**
	 * Runs simulation but does not generate reports. By default false.
	 *
	 * @parameter expression="${gatling.noReports}" alias="nr"
	 *            default-value="false"
	 * @description Runs simulation but does not generate reports
	 */
	protected boolean noReports;

	/**
	 * Generates the reports for the simulation in this folder.
	 *
	 * @parameter expression="${gatling.reportsOnly}" alias="ro"
	 * @description Generates the reports for the simulation in this folder
	 */
	protected File reportsOnly;

	/**
	 * Uses this file as the configuration file.
	 *
	 * @parameter expression="${gatling.configDir}" alias="cd"
	 *            default-value="${basedir}/src/test/resources"
	 * @description Uses this file as the configuration directory
	 */
	protected File configDir;

	/**
	 * Uses this folder to discover simulations that could be run
	 *
	 * @parameter expression="${gatling.simulationsFolder}" alias="sf"
	 *            default-value="${basedir}/src/test/scala"
	 * @description Uses this folder to discover simulations that could be run
	 */
	protected File simulationsFolder;

	/**
	 * Sets the list of include patterns to use in directory scan for
	 * simulations. Relative to simulationsFolder.
	 *
	 * @parameter
	 * @description Include patterns to use in directory scan for simulations
	 */
	protected List<String> includes;

	/**
	 * Sets the list of exclude patterns to use in directory scan for
	 * simulations. Relative to simulationsFolder.
	 *
	 * @parameter
	 * @description Exclude patterns to use in directory scan for simulations
	 */
	protected List<String> excludes;

	/**
	 * A name of a Simulation class to run. This takes precedence over the
	 * includes / excludes parameters.
	 *
	 * @parameter expression="${gatling.simulation}" alias="s"
	 * @description The name of the Simulation class to run
	 */
	protected String simulation;

	/**
	 * Uses this folder as the folder where feeders are stored
	 *
	 * @parameter expression="${gatling.dataFolder}" alias="df"
	 *            default-value="${basedir}/src/test/resources/data"
	 * @description Uses this folder as the folder where feeders are stored
	 */
	protected File dataFolder;

	/**
	 * Uses this folder as the folder where request bodies are stored
	 *
	 * @parameter expression="${gatling.requestBodiesFolder}" alias="bf"
	 *            default-value="${basedir}/src/test/resources/request-bodies"
	 * @description Uses this folder as the folder where request bodies are
	 *              stored
	 */
	protected File requestBodiesFolder;

	/**
	 * Uses this folder as the folder where results are stored
	 *
	 * @parameter expression="${gatling.resultsFolder}" alias="rf"
	 *            default-value="${basedir}/target/gatling/results"
	 * @description Uses this folder as the folder where results are stored
	 */
	protected File resultsFolder;

	/**
	 * Extra JVM arguments to pass when running Gatling.
	 *
	 * @parameter
	 */
	protected List<String> jvmArgs;

	/**
	 * Forks the execution of Gatling plugin into a separate JVM.
	 *
	 * @parameter expression="${gatling.fork}" default-value="true"
	 * @description Forks the execution of Gatling plugin into a separate JVM
	 */
	protected boolean fork;

	/**
	 * Will cause the project build to look successful, rather than fail, even
	 * if there are Gatling test failures. This can be useful on a continuous
	 * integration server, if your only option to be able to collect output
	 * files, is if the project builds successfully.
	 *
	 * @parameter expression="${gatling.failOnError}" default-value="true"
	 */
	protected boolean failOnError;

	/**
	 * Force the name of the directory generated for the results of the run
	 *
	 * @parameter expression="${gatling.outputName}" alias="on"
	 * @description Uses this as the base name of the results folder
	 */
	protected String outputDirectoryBaseName;

	/**
	 * The Maven Project
	 *
	 * @parameter expression="${project}"
	 * @required
	 * @readonly
	 */
	protected MavenProject mavenProject;

	/**
	 * The Maven Session Object
	 *
	 * @parameter expression="${session}"
	 * @required
	 * @readonly
	 */
	protected MavenSession session;

	/**
	 * The toolchain manager to use.
	 *
	 * @component
	 * @required
	 * @readonly
	 */
	protected ToolchainManager toolchainManager;

	/**
	 * Executes Gatling simulations.
	 */
	@Override
	public void execute() throws MojoExecutionException {
		// Prepare environment
		prepareEnvironment();
		try {
			executeGatling(jvmArgs().toArray(new String[0]),gatlingArgs().toArray(new String[0]));
		} catch (Exception e) {
			if (failOnError) {
				throw new MojoExecutionException("Gatling failed.", e);
			}
		}
	}

	protected void prepareEnvironment() {
		// Create results directories
		resultsFolder.mkdirs();
	}

	protected void executeGatling(String[] jvmArgs,String[] gatlingArgs) throws Exception {
		// Setup classpath
		List<String> testClasspathElements = (List<String>) mavenProject.getTestClasspathElements();
		testClasspathElements.add(configDir.getPath());
		String testClasspath = MainHelper.toMultiPath((List<String>) mavenProject.getTestClasspathElements());
		// Setup toolchain
		Toolchain toolchain = toolchainManager.getToolchainFromBuildContext("jdk",session);
		JavaMainCaller caller;
		if(fork) {
			caller = new JavaMainCallerByFork(this,GALTING_MAIN_CLASS,testClasspath,jvmArgs,gatlingArgs,false,toolchain);
		} else {
			caller = new JavaMainCallerInProcess(this,GALTING_MAIN_CLASS,testClasspath,null,gatlingArgs);
		}
		caller.run(true,true);
	}

	protected List<String> jvmArgs() {
		List<String> jvmArguments = (jvmArgs != null) ? jvmArgs : new ArrayList<String>();
		jvmArguments.addAll(Arrays.asList(JVM_ARGS));
		return jvmArguments;
	}

	protected List<String> gatlingArgs() throws MojoExecutionException {
		try {
			// Solves the simulations, if no simulation file is defined
			if (simulation == null) {
				List<String> simulations = resolveSimulations(simulationsFolder, includes, excludes);

				if (simulations.isEmpty()) {
					getLog().error("No simulations to run");
					throw new MojoFailureException("No simulations to run");

				} else if (simulations.size() > 1) {
					getLog().error("More than 1 simulation to run, need to specify one");
					throw new MojoFailureException("More than 1 simulation to run, need to specify one");

				} else {
					simulation = simulations.get(0);
				}
			}

			// Arguments
			List<String> args = new ArrayList<String>();
			args.addAll(asList("-" + CommandLineConstants.CLI_DATA_FOLDER(), dataFolder.getCanonicalPath(),//
					"-" + CommandLineConstants.CLI_RESULTS_FOLDER(), resultsFolder.getCanonicalPath(),// ;
					"-" + CommandLineConstants.CLI_REQUEST_BODIES_FOLDER(), requestBodiesFolder.getCanonicalPath(),//
					"-" + CommandLineConstants.CLI_SIMULATIONS_FOLDER(), simulationsFolder.getCanonicalPath(),//
					"-" + CommandLineConstants.CLI_SIMULATION(), simulation));

			if (noReports) {
				args.add("-" + CommandLineConstants.CLI_NO_REPORTS());
			}

			if (reportsOnly != null) {
				args.addAll(asList("-" + CommandLineConstants.CLI_REPORTS_ONLY(), reportsOnly.getCanonicalPath()));
			}

			if (outputDirectoryBaseName != null) {
				args.addAll(asList("-" + CommandLineConstants.CLI_OUTPUT_DIRECTORY_BASE_NAME(), outputDirectoryBaseName));
			}

			return args;
		} catch (Exception e) {
			throw new MojoExecutionException("Gatling failed.", e);
		}
	}

	protected String fileNameToClassName(String fileName) {
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
	protected List<String> resolveSimulations(File simulationsFolder, List<String> includes, List<String> excludes) {
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
