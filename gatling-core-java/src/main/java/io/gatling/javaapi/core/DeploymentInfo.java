/*
 * Copyright 2011-2024 GatlingCorp (https://gatling.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.gatling.javaapi.core;

import io.gatling.core.util.DeploymentInfo$;
import java.util.UUID;

/** Some deployment information when deploying on Gatling Enterprise */
public final class DeploymentInfo {

  public static DeploymentInfo INSTANCE = new DeploymentInfo();

  private DeploymentInfo() {}

  /** The UUID of the Gatling Enterprise Location where this Load Generator is deployed */
  public final UUID runId = DeploymentInfo$.MODULE$.runId().getOrElse(null);

  /** The name of the Gatling Enterprise Location where this Load Generator is deployed */
  public final String locationName = DeploymentInfo$.MODULE$.locationName().getOrElse(null);

  /** The number of Load Generators deployed on this Location */
  public final int numberOfLoadGeneratorsInLocation =
      DeploymentInfo$.MODULE$.numberOfLoadGeneratorsInLocation();

  /** The index of this Load Generator in this Location */
  public final int indexOfLoadGeneratorInLocation =
      DeploymentInfo$.MODULE$.indexOfLoadGeneratorInLocation();

  /** The total number of Load Generators deployed in this run, all Locations included */
  public final int numberOfLoadGeneratorsInRun =
      DeploymentInfo$.MODULE$.numberOfLoadGeneratorsInRun();

  /** The index of this Load Generator, all Locations included */
  public final int indexOfLoadGeneratorInRun = DeploymentInfo$.MODULE$.indexOfLoadGeneratorInRun();
}
