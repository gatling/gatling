/*
 * Copyright 2011-2019 GatlingCorp (https://gatling.io)
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

package io.gatling.core.jodd;

import jodd.json.JsonParser;

import java.util.Map;
import java.util.function.Supplier;

public class FixedJoddJsonParser extends JsonParser {

  private static final Supplier<Map> FIXED_LAZYMAP_SUPPLIER = FixedLazyMap::new;

  public FixedJoddJsonParser() {
    lazy(true);
    mapSupplier = FIXED_LAZYMAP_SUPPLIER;
  }
}
