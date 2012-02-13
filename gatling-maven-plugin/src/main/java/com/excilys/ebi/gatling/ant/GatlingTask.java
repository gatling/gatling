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
package com.excilys.ebi.gatling.ant;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.taskdefs.Java;
import org.apache.tools.ant.types.Path;

/**
 * Java Ant Task to execute Gatling with default jvm arguments.
 * 
 * @author <a href="mailto:nicolas.huray@gmail.com">Nicolas Huray</a>
 */
public class GatlingTask extends Java {
	
	public static final String GATLING_CLASSPATH_REF_NAME = "gatling.classpath";

    private static final String DEFAULT_JVM_ARGS =
            "-server -XX:+UseThreadPriorities " +
                    "-XX:ThreadPriorityPolicy=42 " +
                    "-Xms512M -Xmx512M -Xmn100M -Xss1024k " +
                    "-XX:+HeapDumpOnOutOfMemoryError " +
                    "-XX:+AggressiveOpts " +
                    "-XX:+OptimizeStringConcat " +
                    "-XX:+UseFastAccessorMethods " + "" +
                    "-XX:+UseParNewGC " +
                    "-XX:+UseConcMarkSweepGC " +
                    "-XX:+CMSParallelRemarkEnabled " +
                    "-XX:SurvivorRatio=8 " +
                    "-XX:MaxTenuringThreshold=1 " +
                    "-XX:CMSInitiatingOccupancyFraction=75 " +
                    "-XX:+UseCMSInitiatingOccupancyOnly";

    public GatlingTask() {
        createJvmarg().setLine(DEFAULT_JVM_ARGS);
        setFork(true);
        setFailonerror(true);
    }
    
    private Path getGatlingClasspath() {
        Object gatlingClasspath = getProject().getReference(GATLING_CLASSPATH_REF_NAME);
        if (!(gatlingClasspath instanceof Path)) {
            throw new BuildException("Please create a path with id " + GATLING_CLASSPATH_REF_NAME);
        }
        return (Path) gatlingClasspath;
    }


    public void execute() {
        setClasspath(getGatlingClasspath());
        setClassname("com.excilys.ebi.gatling.app.Gatling");
        super.execute();
    }
}
