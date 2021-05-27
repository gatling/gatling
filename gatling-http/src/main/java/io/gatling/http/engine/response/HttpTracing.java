/*
 * Copyright 2011-2021 GatlingCorp (https://gatling.io)
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

package io.gatling.http.engine.response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HttpTracing {
  private static final Logger LOGGER = LoggerFactory.getLogger(HttpTracing.class.getPackage().getName());
  public static final boolean IS_HTTP_DEBUG_ENABLED = LOGGER.isDebugEnabled();
  public static final boolean IS_HTTP_TRACE_ENABLED = LOGGER.isTraceEnabled();
}
