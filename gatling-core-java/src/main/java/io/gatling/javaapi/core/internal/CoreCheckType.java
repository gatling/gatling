/*
 * Copyright 2011-2025 GatlingCorp (https://gatling.io)
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

package io.gatling.javaapi.core.internal;

import io.gatling.javaapi.core.CheckBuilder;

public enum CoreCheckType implements CheckBuilder.CheckType {
  BodyBytes,
  BodyLength,
  BodyString,
  BodyStream,
  Regex,
  Substring,
  XPath,
  Css,
  JsonPath,
  JsonpJsonPath,
  JmesPath,
  JsonpJmesPath,
  Md5,
  Sha1,
  ResponseTime
}
