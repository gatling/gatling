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

package io.gatling.core.javaapi;

import java.util.List;

import static io.gatling.core.javaapi.internal.ScalaHelpers.*;

public abstract class Filter<W extends io.gatling.core.filter.Filter> {

  private final W wrapped;

  private Filter(W wrapped) {
    this.wrapped = wrapped;
  }

  public W asScala() {
    return wrapped;
  }

  public static final class AllowList extends Filter<io.gatling.core.filter.AllowList> {
    public AllowList(List<String> patterns) {
      super(new io.gatling.core.filter.AllowList(toScalaSeq(patterns)));
    }
  }

  public static final class DenyList extends Filter<io.gatling.core.filter.DenyList> {
    public DenyList(List<String> patterns) {
      super(new io.gatling.core.filter.DenyList(toScalaSeq(patterns)));
    }
  }
}
