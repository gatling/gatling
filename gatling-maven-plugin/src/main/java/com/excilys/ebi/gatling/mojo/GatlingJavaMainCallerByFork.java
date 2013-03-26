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

import java.util.List;
import java.util.Map.Entry;

import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.ExecuteException;
import org.apache.commons.exec.Executor;
import org.apache.commons.exec.PumpStreamHandler;
import org.apache.commons.exec.ShutdownHookProcessDestroyer;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.toolchain.Toolchain;
import org.codehaus.plexus.util.StringUtils;

import scala_maven_executions.JavaMainCallerByFork;

public class GatlingJavaMainCallerByFork extends JavaMainCallerByFork {

	public GatlingJavaMainCallerByFork(AbstractMojo requester1, String mainClassName1, String classpath, String[] jvmArgs1, String[] args1, boolean forceUseArgFile, Toolchain toolchain, boolean propagateSystemProperties) throws Exception {
		super(requester1, mainClassName1, classpath, jvmArgs1, args1, forceUseArgFile, toolchain);

		if (propagateSystemProperties) {
			for (Entry<Object, Object> systemProp : System.getProperties().entrySet()) {
				String name = systemProp.getKey().toString();
				String value = systemProp.getValue().toString();
				if (isPropagatableProperty(name)) {
					addJvmArgs("-D" + name + "=" + StringUtils.quoteAndEscape(value, '\"'));
				}
			}
		}
	}
	
    @Override
    public boolean run(boolean displayCmd, boolean throwFailure) throws Exception {
        List<String> cmd = buildCommand();
        displayCmd(displayCmd, cmd);
        Executor exec = new DefaultExecutor();

        //err and out are redirected to out
        exec.setStreamHandler(new PumpStreamHandler(System.out, System.err, System.in));
        
        /* fix for issue Issue #1047*/
    	exec.setProcessDestroyer(new ShutdownHookProcessDestroyer());
        
        CommandLine cl = new CommandLine(cmd.get(0));
        for (int i = 1; i < cmd.size(); i++) {
            cl.addArgument(cmd.get(i), false);
        }
        try {
        	int exitValue = exec.execute(cl);
            if (exitValue != 0) {
                if (throwFailure) {
                    throw new MojoFailureException("command line returned non-zero value:" + exitValue);
                }
                return false;
            }
            return true;
        } catch (ExecuteException exc) {
            if (throwFailure) {
                throw exc;
            }
            return false;
        }
    }
    
    public void displayCmd(boolean displayCmd, List<String> cmd) {
        if (displayCmd) {
            requester.getLog().info("cmd: " + " " + StringUtils.join(cmd.iterator(), " "));
        } else if (requester.getLog().isDebugEnabled()) {
            requester.getLog().debug("cmd: " + " " + StringUtils.join(cmd.iterator(), " "));
        }
    }

	private boolean isPropagatableProperty(String name) {
		return !name.startsWith("java.") //
				&& !name.startsWith("sun.") //
				&& !name.startsWith("maven.") //
				&& !name.startsWith("file.") //
				&& !name.startsWith("awt.") //
				&& !name.startsWith("os.") //
				&& !name.startsWith("user.") //
				&& !name.equals("line.separator") //
				&& !name.equals("path.separator");
	}
}
