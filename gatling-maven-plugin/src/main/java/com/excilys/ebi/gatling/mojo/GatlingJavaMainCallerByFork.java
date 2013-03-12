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

import java.util.Map.Entry;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.toolchain.Toolchain;

import scala_maven_executions.JavaMainCallerByFork;

public class GatlingJavaMainCallerByFork extends JavaMainCallerByFork {

	public GatlingJavaMainCallerByFork(AbstractMojo requester1, String mainClassName1, String classpath, String[] jvmArgs1, String[] args1, boolean forceUseArgFile,
			Toolchain toolchain, boolean propagateSystemProperties) throws Exception {
		super(requester1, mainClassName1, classpath, jvmArgs1, args1, forceUseArgFile, toolchain);

		if (propagateSystemProperties) {
			for (Entry<Object, Object> systemProp : System.getProperties().entrySet()) {
				String key = systemProp.getKey().toString();
				if (!key.startsWith("java.") //
						&& !key.startsWith("sun.") //
						&& !key.startsWith("maven.") //
						&& !key.startsWith("file.") //
						&& !key.startsWith("awt.") //
						&& !key.startsWith("os.") //
						&& !key.startsWith("user.") //
						&& !key.equals("line.separator")) {
					addJvmArgs("-D" + key + "=" + systemProp.getValue());
				}
			}
		}
	}
}
