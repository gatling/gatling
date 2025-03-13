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

package io.gatling.javaapi.jms;

import edu.umd.cs.findbugs.annotations.NonNull;
import io.gatling.javaapi.core.CheckBuilder;
import io.gatling.javaapi.jms.internal.JmsCheckType;
import io.gatling.jms.check.JmsPropertyCheckType;
import io.gatling.jms.check.JmsPropertyFilter;
import jakarta.jms.Message;

public interface JmsCheckBuilder {

  /** A special {@link CheckBuilder.Find <String>} that works on JMS Message properties */
  interface JmsPropertyOfTypeFind extends CheckBuilder.Find<String> {

    /**
     * Define that the extracted value is a String
     *
     * @return a new Find
     */
    @NonNull
    Find<String> ofString();

    /**
     * Define that the extracted value is a Boolean
     *
     * @return a new Find
     */
    @NonNull
    Find<Boolean> ofBoolean();

    /**
     * Define that the extracted value is an Integer
     *
     * @return a new Find
     */
    @NonNull
    Find<Integer> ofInt();

    /**
     * Define that the extracted value is a Long
     *
     * @return a new Find
     */
    @NonNull
    Find<Long> ofLong();

    /**
     * Define that the extracted value is a Double
     *
     * @return a new Find
     */
    @NonNull
    Find<Double> ofDouble();

    /**
     * Define that the extracted value is an untyped object
     *
     * @return a new Find
     */
    @NonNull
    Find<Object> ofObject();

    /**
     * Default implementation of {@link JmsPropertyOfTypeFind}
     *
     * @param <T> the check type
     */
    abstract class Default<T> extends Find.Default<T, Message, String, String>
        implements JmsPropertyOfTypeFind {
      public Default(
          io.gatling.core.check.CheckBuilder.Find<T, Message, String> wrapped, CheckType type) {
        super(wrapped, type, String.class, null);
      }

      @NonNull
      protected abstract <X> io.gatling.core.check.CheckBuilder.Find<T, Message, X> ofType(
          JmsPropertyFilter<X> filter);

      @Override
      @NonNull
      public Find<String> ofString() {
        return new Find.Default<>(
            ofType(JmsPropertyFilter.stringJmsPropertyFilter()), type, String.class, null);
      }

      @Override
      @NonNull
      public Find<Boolean> ofBoolean() {
        return new Find.Default<>(
            ofType(JmsPropertyFilter.jBooleanJmsPropertyFilter()),
            type,
            Boolean.class,
            Boolean.class::cast);
      }

      @Override
      @NonNull
      public Find<Integer> ofInt() {
        return new Find.Default<>(
            ofType(JmsPropertyFilter.jIntegerJmsPropertyFilter()),
            type,
            Integer.class,
            Integer.class::cast);
      }

      @Override
      @NonNull
      public Find<Long> ofLong() {
        return new Find.Default<>(
            ofType(JmsPropertyFilter.jLongJmsPropertyFilter()), type, Long.class, Long.class::cast);
      }

      @NonNull
      public Find<Byte> ofByte() {
        return new Find.Default<>(
            ofType(JmsPropertyFilter.jByteJmsPropertyFilter()), type, Byte.class, Byte.class::cast);
      }

      @NonNull
      public Find<Short> ofShort() {
        return new Find.Default<>(
            ofType(JmsPropertyFilter.jShortJmsPropertyFilter()),
            type,
            Short.class,
            Short.class::cast);
      }

      @Override
      @NonNull
      public Find<Double> ofDouble() {
        return new Find.Default<>(
            ofType(JmsPropertyFilter.jDoubleJmsPropertyFilter()),
            type,
            Double.class,
            Double.class::cast);
      }

      @Override
      @NonNull
      public Find<Object> ofObject() {
        return new Find.Default<>(
            ofType(JmsPropertyFilter.jAnyJmsPropertyFilter()), type, Object.class, null);
      }
    }
  }

  /** An implementation of {@link JmsCheckBuilder.JmsPropertyOfTypeFind} for JMS Properties. */
  final class JmsProperty
      extends JmsCheckBuilder.JmsPropertyOfTypeFind.Default<JmsPropertyCheckType> {

    public JmsProperty(
        @NonNull
            io.gatling.core.check.CheckBuilder.Find<JmsPropertyCheckType, Message, String>
                wrapped) {
      super(wrapped, JmsCheckType.JmsProperty);
    }

    @Override
    @NonNull
    protected <X> io.gatling.core.check.CheckBuilder.Find<JmsPropertyCheckType, Message, X> ofType(
        JmsPropertyFilter<X> filter) {
      io.gatling.jms.check.JmsPropertyCheckBuilder<String> actual =
          (io.gatling.jms.check.JmsPropertyCheckBuilder<String>) wrapped;
      return new io.gatling.jms.check.JmsPropertyCheckBuilder<>(actual.propertyName(), filter);
    }
  }
}
