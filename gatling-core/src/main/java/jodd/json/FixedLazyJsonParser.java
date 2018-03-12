/*
 * Copyright 2011-2018 GatlingCorp (http://gatling.io)
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

package jodd.json;

import io.advantageous.boon.core.SimpleLazyMap;

import java.util.Map;
import java.util.function.Supplier;

public class FixedLazyJsonParser extends JsonParser {

  private static final Supplier<Map> LAZYMAP_SUPPLIER = SimpleLazyMap::new;

  public void forceSuppliers() {
    mapSupplier = LAZYMAP_SUPPLIER;
    listSupplier = LAZYLIST_SUPPLIER;
  }
}
