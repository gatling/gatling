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

import java.io.File;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;

import org.apache.maven.plugin.AbstractMojo;
import org.codehaus.plexus.util.StringUtils;
import scala_maven_executions.JavaMainCallerSupport;
import scala_maven_executions.SpawnMonitor;

/**
 * This class will call a java main method via reflection.
 * Modified to suit Gatling's needs.
 *
 * @author J. Suereth
 *         <p/>
 *         Note: a -classpath argument *must* be passed into the jvmargs.
 */
public class JavaMainCallerInProcess extends JavaMainCallerSupport {

	private ClassLoader oldClassLoader = null;

	public JavaMainCallerInProcess(AbstractMojo requester, String mainClassName, String classpath, String[] jvmArgs, String[] args) throws Exception {
		super(requester, mainClassName, "", jvmArgs, args);

		//Pull out classpath and create class loader
		ArrayList<URL> urls = new ArrayList<URL>();

		for (String path : classpath.split(File.pathSeparator)) {
			try {
				urls.add(new File(path).toURI().toURL());
			} catch (MalformedURLException e) {
				//TODO - Do something useful here...
				requester.getLog().error(e);
			}
		}

		oldClassLoader = Thread.currentThread().getContextClassLoader();
		Thread.currentThread().setContextClassLoader(new URLClassLoader(urls.toArray(new URL[urls.size()])));
	}


	@Override
	// In process, ignore jvm args
	public void addJvmArgs(String... args) {}


	@Override
	// Not used, @see #run()
	public boolean run(boolean displayCmd, boolean throwFailure) throws Exception {
		return false;
	}

	public int run() throws Exception {
		try {
			return runInternal(false);
		} catch (Exception e) {
				throw e;
		}
	}

	/**
	 * spawns a thread to run the method
	 */
	public SpawnMonitor spawn(final boolean displayCmd) throws Exception {
		final Thread t = new Thread() {
			@Override
			public void run() {
				try {
					runInternal(displayCmd);
				} catch (Exception e) {
					// Ignore
				}
			}
		};
		t.start();
		return new SpawnMonitor() {
			public boolean isRunning() throws Exception {
				return t.isAlive();
			}
		};
	}

	/**
	 * Runs the main method of a java class
	 */
	private int runInternal(boolean displayCmd) throws Exception {
		String[] argArray = args.toArray(new String[args.size()]);
		if (displayCmd) {
			requester.getLog().info("cmd : " + mainClassName + "(" + StringUtils.join(argArray, ",") + ")");
		}
		int returnCode = runGatling(mainClassName, args, null);
		Thread.currentThread().setContextClassLoader(oldClassLoader);
		return returnCode;
	}

	private int runGatling(String mainClassName, List<String> args, ClassLoader cl) throws Exception {
		if(cl == null) {
			cl = Thread.currentThread().getContextClassLoader();
		}
		Class<?> mainClass = cl.loadClass(mainClassName);
		Method runGatlingMethod = mainClass.getMethod("runGatling", String[].class);
		String[] argArray = args.toArray(new String[args.size()]);

		return (Integer) runGatlingMethod.invoke(null, new Object[] {argArray});
	}

	public void redirectToLog() {
		requester.getLog().warn("redirection to log is not supported for 'inProcess' mode");
	}


}
