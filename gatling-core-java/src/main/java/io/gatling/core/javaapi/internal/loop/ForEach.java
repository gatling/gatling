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

package io.gatling.core.javaapi.internal.loop;

import io.gatling.commons.validation.Validation;
import io.gatling.core.javaapi.ChainBuilder;
import io.gatling.core.javaapi.Session;
import io.gatling.core.javaapi.internal.StructureBuilder;
import scala.Function1;
import scala.collection.immutable.Seq;

import java.util.List;
import java.util.UUID;
import java.util.function.Function;

import static io.gatling.core.javaapi.internal.ScalaHelpers.toGatlingSessionFunctionImmutableSeq;

public interface ForEach<T extends StructureBuilder<T, W>, W extends io.gatling.core.structure.StructureBuilder<W>> {

  T make(Function<W, W> f);

  default Loop<T> foreach(List<Object> seq, String attributeName) {
    return foreach(seq, attributeName, null);
  }

  default Loop<T> foreach(List<Object> seq, String attributeName, String counterName) {
    return foreach(unused -> seq, attributeName, counterName);
  }

  default Loop<T> foreach(Function<Session, List<Object>> seq, String attributeName) {
    return foreach(seq, attributeName, null);
  }

  default Loop<T> foreach(Function<Session, List<Object>> seq, String attributeName, String counterName) {
    return new Loop<>(this, toGatlingSessionFunctionImmutableSeq(seq), attributeName, counterName);
  }

  final class Loop<T extends StructureBuilder<T, ?>> {
    private final ForEach<T, ?> context;
    final Function1<io.gatling.core.session.Session, Validation<Seq<Object>>> seq;
    private final String attributeName;
    private final String counterName;

    Loop(ForEach<T, ?> context, Function1<io.gatling.core.session.Session, Validation<Seq<Object>>> seq, String attributeName, String counterName) {
      this.context = context;
      this.seq = seq;
      this.attributeName = attributeName;
      this.counterName = counterName == null ? UUID.randomUUID().toString() : counterName;
    }

    public T loop(ChainBuilder chain) {
      return context.make(wrapped -> wrapped.foreach(seq, attributeName, counterName, chain.wrapped));
    }
  }
}
