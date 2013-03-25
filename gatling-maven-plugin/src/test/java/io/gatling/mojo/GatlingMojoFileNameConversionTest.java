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

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.io.File;
import java.util.Arrays;
import java.util.Collection;

import static io.gatling.mojo.GatlingMojo.fileNameToClassName;
import static org.junit.Assert.assertEquals;

@RunWith(value = Parameterized.class)
public class GatlingMojoFileNameConversionTest {

    private static final String SEP = File.separator;
    private String className;
    private String fileName;

    public GatlingMojoFileNameConversionTest(String className, String fileName) {
        this.className = className;
        this.fileName = fileName;
    }

    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        Object[][] data = new Object[][]{
                {"MySimulation", "MySimulation.scala"}
                , {"MySimulation", "MySimulation.customFileExtension"}
                , {"45000Email", "45000Email.scala"}
                , {"TestScala", "TestScala.scala"}
                , {"test_a", "test_a.scala"}
                , {"t123456", "t123456.scala"}
                , {"A1", "A1.scala"}
                , {"mypackage.MySimulation", "mypackage" + SEP + "MySimulation.scala"}
                , {"my.deeply.nested.package.MySimulation",
                    "my" + SEP + "deeply" + SEP + "nested" + SEP + "package" + SEP + "MySimulation.scala"}
                , {"leading.whitespace.MySimulation",
                    " \n\t " + "leading" + SEP + "whitespace" + SEP + "MySimulation.scala"}
                , {"trailing.whitespace.MySimulation",
                    "trailing" + SEP + "whitespace" + SEP + "MySimulation.scala" + " \n\t "}
        };
        return Arrays.asList(data);
    }

    @Test
    public void test_fileNametoClassName() {
        assertEquals(className, fileNameToClassName(fileName));
    }

}
