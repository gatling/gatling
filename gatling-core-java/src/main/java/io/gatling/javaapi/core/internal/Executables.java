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

package io.gatling.javaapi.core.internal;

import io.gatling.javaapi.core.ChainBuilder;
import io.gatling.javaapi.core.exec.Executable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public final class Executables {

  private Executables() {}

  public static List<ChainBuilder> toChainBuilders(Executable[] tail) {
    return Arrays.stream(tail).map(Executable::toChainBuilder).collect(Collectors.toList());
  }

  public static List<ChainBuilder> toChainBuilders(Executable head, Executable[] tail) {
    List<ChainBuilder> chainBuilders = new ArrayList<>();
    chainBuilders.add(head.toChainBuilder());
    chainBuilders.addAll(toChainBuilders(tail));
    return chainBuilders;
  }

  public static ChainBuilder toChainBuilder(Executable head, Executable[] tail) {
    return head.toChainBuilder().exec(toChainBuilders(tail));
  }
}
