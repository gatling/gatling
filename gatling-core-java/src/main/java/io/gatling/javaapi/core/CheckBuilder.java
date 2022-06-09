/*
 * Copyright 2011-2022 GatlingCorp (https://gatling.io)
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

import static io.gatling.javaapi.core.internal.Comparisons.*;
import static io.gatling.javaapi.core.internal.Converters.*;
import static io.gatling.javaapi.core.internal.CoreCheckBuilders.*;
import static io.gatling.javaapi.core.internal.Expressions.*;

import com.fasterxml.jackson.databind.JsonNode;
import io.gatling.core.check.css.CssCheckType;
import io.gatling.core.check.css.NodeConverter;
import io.gatling.core.check.jmespath.JmesPathCheckType;
import io.gatling.core.check.jmespath.JsonpJmesPathCheckType;
import io.gatling.core.check.jsonpath.JsonFilter;
import io.gatling.core.check.jsonpath.JsonPathCheckType;
import io.gatling.core.check.jsonpath.JsonpJsonPathCheckType;
import io.gatling.core.check.regex.GroupExtractor;
import io.gatling.core.check.regex.RegexCheckType;
import io.gatling.javaapi.core.internal.Converters;
import io.gatling.javaapi.core.internal.CoreCheckType;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import jodd.lagarto.dom.Node;
import jodd.lagarto.dom.NodeSelector;
import net.jodah.typetools.TypeResolver;

/**
 * Java wrapper of a Scala CheckBuilder. Builder of an Action in a Gatling scenario.
 *
 * <p>Immutable, so all methods return a new occurrence and leave the original unmodified.
 */
public interface CheckBuilder {

  /**
   * For internal use only
   *
   * @return the wrapped Scala instance
   */
  io.gatling.core.check.CheckBuilder<?, ?> asScala();

  /**
   * For internal use only
   *
   * @return the type of CheckBuilder
   */
  CheckType type();

  /** A type of check */
  interface CheckType {}

  /**
   * Step 1 of the Check DSL when the check can only return one single value Immutable, so all
   * methods return a new occurrence and leave the original unmodified.
   *
   * @param <JavaX> the type of Java values the check can extract
   */
  interface Find<JavaX> extends Validate<JavaX> {

    /**
     * Target a single/first value
     *
     * @return the next Check DSL step
     */
    Validate<JavaX> find();

    /**
     * Default implementation of {@link Find}
     *
     * @param <T> the check type
     * @param <P> the prepared input type
     * @param <ScalaX> the type of the extracted Scala value
     * @param <JavaX> the type of the presented Java value
     */
    class Default<T, P, ScalaX, JavaX> implements Find<JavaX> {
      protected final io.gatling.core.check.CheckBuilder.Find<T, P, ScalaX> wrapped;
      protected final CheckType type;
      protected final Class<?> javaXClass;
      protected final Function<ScalaX, JavaX> scalaXToJavaX;

      public Default(
          io.gatling.core.check.CheckBuilder.Find<T, P, ScalaX> wrapped,
          CheckType type,
          Class<?> javaXClass,
          Function<ScalaX, JavaX> scalaXToJavaX) {
        this.wrapped = wrapped;
        this.type = type;
        this.javaXClass = javaXClass;
        this.scalaXToJavaX = scalaXToJavaX;
      }

      @Override
      @Nonnull
      public Validate<JavaX> find() {
        return new Validate.Default<>(
            convertExtractedValueToJava(wrapped.find(), scalaXToJavaX), type, javaXClass);
      }

      @Override
      @Nonnull
      public <X2> Validate<X2> transform(@Nonnull Function<JavaX, X2> f) {
        return find().transform(f);
      }

      @Override
      @Nonnull
      public <X2> Validate<X2> transformWithSession(@Nonnull BiFunction<JavaX, Session, X2> f) {
        return find().transformWithSession(f);
      }

      @Override
      @Nonnull
      public Validate<JavaX> withDefault(@Nonnull JavaX value) {
        return find().withDefault(value);
      }

      @Override
      @Nonnull
      public Validate<JavaX> withDefaultEl(@Nonnull String value) {
        return find().withDefaultEl(value);
      }

      @Override
      @Nonnull
      public Validate<JavaX> withDefault(@Nonnull Function<Session, JavaX> defaultValue) {
        return find().withDefault(defaultValue);
      }

      @Override
      @Nonnull
      public Final validate(@Nonnull String opName, @Nonnull BiFunction<JavaX, Session, JavaX> f) {
        return find().validate(opName, f);
      }

      @Override
      @Nonnull
      public Final isEL(@Nonnull String expected) {
        return find().isEL(expected);
      }

      @Override
      @Nonnull
      public Final is(@Nonnull JavaX expected) {
        return find().is(expected);
      }

      @Override
      @Nonnull
      public Final is(@Nonnull Function<Session, JavaX> expected) {
        return find().is(expected);
      }

      @Override
      @Nonnull
      public Final isNull() {
        return find().isNull();
      }

      @Override
      @Nonnull
      public Final notEL(@Nonnull String expected) {
        return find().notEL(expected);
      }

      @Override
      @Nonnull
      public Final not(@Nonnull JavaX expected) {
        return find().not(expected);
      }

      @Override
      @Nonnull
      public Final not(@Nonnull Function<Session, JavaX> expected) {
        return find().not(expected);
      }

      @Override
      @Nonnull
      public Final notNull() {
        return find().notNull();
      }

      @Override
      @Nonnull
      public Final in(@Nonnull JavaX... expected) {
        return find().in(expected);
      }

      @Override
      @Nonnull
      public Final in(@Nonnull List<JavaX> expected) {
        return find().in(expected);
      }

      @Override
      @Nonnull
      public Final inEL(@Nonnull String expected) {
        return find().inEL(expected);
      }

      @Override
      @Nonnull
      public Final in(@Nonnull Function<Session, List<JavaX>> expected) {
        return find().in(expected);
      }

      @Override
      @Nonnull
      public Final exists() {
        return find().exists();
      }

      @Override
      @Nonnull
      public Final notExists() {
        return find().notExists();
      }

      @Override
      @Nonnull
      public Final optional() {
        return find().optional();
      }

      @Override
      @Nonnull
      public Final lt(@Nonnull JavaX expected) {
        return find().lt(expected);
      }

      @Override
      @Nonnull
      public Final ltEL(@Nonnull String expected) {
        return find().ltEL(expected);
      }

      @Override
      @Nonnull
      public Final lt(@Nonnull Function<Session, JavaX> expected) {
        return find().lt(expected);
      }

      @Override
      @Nonnull
      public Final lte(@Nonnull JavaX expected) {
        return find().lte(expected);
      }

      @Override
      @Nonnull
      public Final lteEL(@Nonnull String expected) {
        return find().lteEL(expected);
      }

      @Override
      @Nonnull
      public Final lte(@Nonnull Function<Session, JavaX> expected) {
        return find().lte(expected);
      }

      @Override
      @Nonnull
      public Final gt(@Nonnull JavaX expected) {
        return find().gt(expected);
      }

      @Override
      @Nonnull
      public Final gtEL(@Nonnull String expected) {
        return find().gtEL(expected);
      }

      @Override
      @Nonnull
      public Final gt(@Nonnull Function<Session, JavaX> expected) {
        return find().gt(expected);
      }

      @Override
      @Nonnull
      public Final gte(@Nonnull JavaX expected) {
        return find().gte(expected);
      }

      @Override
      @Nonnull
      public Final gteEL(@Nonnull String expected) {
        return find().gteEL(expected);
      }

      @Override
      @Nonnull
      public Final gte(@Nonnull Function<Session, JavaX> expected) {
        return find().gte(expected);
      }

      @Override
      @Nonnull
      public Final name(@Nonnull String n) {
        return find().name(n);
      }

      @Override
      @Nonnull
      public Final saveAs(@Nonnull String key) {
        return find().saveAs(key);
      }

      @Override
      @Nonnull
      public CheckType type() {
        return find().type();
      }

      @Override
      public io.gatling.core.check.CheckBuilder<?, ?> asScala() {
        return find().asScala();
      }
    }
  }

  /**
   * Step 1 of the Check DSL when the check can return multiple values Immutable, so all methods
   * return a new occurrence and leave the original unmodified.
   *
   * @param <JavaX> the type of Java values the check can extract
   */
  interface MultipleFind<JavaX> extends Find<JavaX> {

    /**
     * Target the occurrence-th occurrence in the extracted values
     *
     * @param occurrence the rank of the target value in the extracted values list
     * @return the next Check DSL step
     */
    @Nonnull
    Validate<JavaX> find(int occurrence);

    /**
     * Target all the occurrences of the extracted values
     *
     * @return the next Check DSL step
     */
    @Nonnull
    Validate<List<JavaX>> findAll();

    /**
     * Target a random occurrence in the extracted values
     *
     * @return the next Check DSL step
     */
    @Nonnull
    Validate<JavaX> findRandom();

    /**
     * Target multiple random occurrences in the extracted values
     *
     * @param num the number of occurrences to collect
     * @return the next Check DSL step
     */
    @Nonnull
    Validate<List<JavaX>> findRandom(int num);

    /**
     * Target multiple random occurrences in the extracted values
     *
     * @param num the number of occurrences to collect
     * @param failIfLess fail if num is greater than the number of extracted values
     * @return the next Check DSL step
     */
    @Nonnull
    Validate<List<JavaX>> findRandom(int num, boolean failIfLess);

    /**
     * Target the count of extracted values
     *
     * @return the next Check DSL step
     */
    @Nonnull
    Validate<Integer> count();

    /**
     * Default implementation of {@link MultipleFind}
     *
     * @param <T> the check type
     * @param <P> the prepared input type
     * @param <ScalaX> the type of the extracted Scala value
     * @param <JavaX> the type of the presented Java value
     */
    class Default<T, P, ScalaX, JavaX> extends Find.Default<T, P, ScalaX, JavaX>
        implements MultipleFind<JavaX> {
      protected final io.gatling.core.check.CheckBuilder.MultipleFind<T, P, ScalaX> wrapped;

      public Default(
          @Nonnull io.gatling.core.check.CheckBuilder.MultipleFind<T, P, ScalaX> wrapped,
          @Nonnull CheckType type,
          @Nonnull Class<?> javaXClass,
          @Nullable Function<ScalaX, JavaX> scalaXToJavaX) {
        super(wrapped, type, javaXClass, scalaXToJavaX);
        this.wrapped = wrapped;
      }

      private <X2> Validate<X2> makeValidate(
          io.gatling.core.check.CheckBuilder.Validate<T, P, X2> wrapped, Class<?> javaXClass) {
        return new Validate.Default<>(wrapped, type, javaXClass);
      }

      @Override
      @Nonnull
      public Validate<JavaX> find() {
        return makeValidate(convertExtractedValueToJava(wrapped.find(), scalaXToJavaX), javaXClass);
      }

      @Override
      @Nonnull
      public Validate<JavaX> find(int occurrence) {
        return makeValidate(
            convertExtractedValueToJava(wrapped.find(occurrence), scalaXToJavaX), javaXClass);
      }

      @Override
      @Nonnull
      public Validate<List<JavaX>> findAll() {
        return makeValidate(
            convertExtractedSeqToJava(wrapped.findAll(), scalaXToJavaX), List.class);
      }

      @Override
      @Nonnull
      public Validate<JavaX> findRandom() {
        return makeValidate(
            convertExtractedValueToJava(wrapped.findRandom(), scalaXToJavaX), javaXClass);
      }

      @Override
      @Nonnull
      public Validate<List<JavaX>> findRandom(int num) {
        return findRandom(num, false);
      }

      @Override
      @Nonnull
      public Validate<List<JavaX>> findRandom(int num, boolean failIfLess) {
        return makeValidate(
            convertExtractedSeqToJava(wrapped.findRandom(num, failIfLess), scalaXToJavaX),
            List.class);
      }

      @Override
      @Nonnull
      public Validate<Integer> count() {
        return makeValidate(toCountCheck(wrapped), Integer.class);
      }
    }
  }

  /**
   * Step 2 of the Check DSL where we define how to validate the extracted value. Immutable, so all
   * methods return a new occurrence and leave the original unmodified.
   *
   * @param <X> the type of the extracted value
   */
  interface Validate<X> extends Final {

    /**
     * Transform the extracted value
     *
     * @param f the transformation function
     * @param <X2> the transformed value
     * @return a new Validate
     */
    @Nonnull
    <X2> Validate<X2> transform(@Nonnull Function<X, X2> f);

    /**
     * Transform the extracted value, whith access to the current {@link Session}
     *
     * @param f the transformation function
     * @param <X2> the transformed value
     * @return a new Validate
     */
    @Nonnull
    <X2> Validate<X2> transformWithSession(@Nonnull BiFunction<X, Session, X2> f);

    /**
     * Provide a default value if the check wasn't able to extract anything
     *
     * @param value the default value
     * @return a new Validate
     */
    @Nonnull
    Validate<X> withDefault(@Nonnull X value);

    /**
     * Provide a default Gatling Expression Language value if the check wasn't able to extract
     * anything
     *
     * @param value the default value as a Gatling Expression Language String
     * @return a new Validate
     */
    @Nonnull
    Validate<X> withDefaultEl(@Nonnull String value);

    /**
     * Provide a default Gatling Expression Language value if the check wasn't able to extract
     * anything
     *
     * @param defaultValue the default value as a function
     * @return a new Validate
     */
    @Nonnull
    Validate<X> withDefault(@Nonnull Function<Session, X> defaultValue);

    /**
     * Provide a custom validation strategy
     *
     * @param name the name of the validation, in case of a failure
     * @param f the custom validation function, must throw to trigger a failure
     * @return a new Final
     */
    @Nonnull
    Final validate(@Nonnull String name, @Nonnull BiFunction<X, Session, X> f);

    /**
     * Validate the extracted value is equal to an expected value
     *
     * @param expected the expected value
     * @return a new Final
     */
    @Nonnull
    Final is(X expected);

    /**
     * Alias for {@link Validate#is(Object)} as `is` is a reserved keyword in Kotlin
     *
     * @param expected the expected value
     * @return a new Final
     */
    @Nonnull
    default Final shouldBe(X expected) {
      return is(expected);
    }

    /**
     * Validate the extracted value is equal to an expected value, passed as a Gatling Expression
     * Language String
     *
     * @param expected the expected value as a Gatling Expression Language String
     * @return a new Final
     */
    @Nonnull
    Final isEL(String expected);

    /**
     * Validate the extracted value is equal to an expected value, passed as a function
     *
     * @param expected the expected value as a function
     * @return a new Final
     */
    @Nonnull
    Final is(Function<Session, X> expected);

    /**
     * Alias for {@link Validate#is(Function)} as `is` is a reserved keyword in Kotlin
     *
     * @param expected the expected value
     * @return a new Final
     */
    @Nonnull
    default Final shouldBe(Function<Session, X> expected) {
      return is(expected);
    }

    /**
     * Validate the extracted value is null
     *
     * @return a new Final
     */
    Final isNull();

    /**
     * Validate the extracted value is not an expected value
     *
     * @param expected the unexpected value
     * @return a new Final
     */
    @Nonnull
    Final not(@Nonnull X expected);

    /**
     * Validate the extracted value is not an expected value, passed as a Gatling Expression
     * Language String
     *
     * @param expected the unexpected value as a Gatling Expression Language String
     * @return a new Final
     */
    @Nonnull
    Final notEL(@Nonnull String expected);

    /**
     * Validate the extracted value is not an expected value, passed as a function
     *
     * @param expected the unexpected value as a function
     * @return a new Final
     */
    @Nonnull
    Final not(@Nonnull Function<Session, X> expected);

    /**
     * Validate the extracted value is not null
     *
     * @return a new Final
     */
    Final notNull();

    /**
     * Validate the extracted value belongs to an expected set
     *
     * @param expected the set of possible values
     * @return a new Final
     */
    @Nonnull
    Final in(@Nonnull X... expected);

    /**
     * Alias for `in` that's a reserved keyword in Kotlin
     *
     * @param expected the set of possible values
     * @return a new Final
     */
    @Nonnull
    default Final within(@Nonnull X... expected) {
      return in(expected);
    }

    /**
     * Validate the extracted value belongs to an expected set
     *
     * @param expected the set of possible values
     * @return a new Final
     */
    @Nonnull
    Final in(@Nonnull List<X> expected);

    /**
     * Alias for `in` that's a reserved keyword in Kotlin
     *
     * @param expected the set of possible values
     * @return a new Final
     */
    @Nonnull
    default Final within(@Nonnull List<X> expected) {
      return in(expected);
    }

    /**
     * Validate the extracted value belongs to an expected set, passed as a Gatling Expression
     * Language String
     *
     * @param expected the set of possible values, as a Gatling Expression Language String
     * @return a new Final
     */
    @Nonnull
    Final inEL(@Nonnull String expected);

    /**
     * Alias for `in` that's a reserved keyword in Kotlin
     *
     * @param expected the set of possible values, as a Gatling Expression Language String
     * @return a new Final
     */
    @Nonnull
    default Final withinEL(@Nonnull String expected) {
      return inEL(expected);
    }

    /**
     * Validate the extracted value belongs to an expected set, passed as a function
     *
     * @param expected the set of possible values, as a function
     * @return a new Final
     */
    @Nonnull
    Final in(@Nonnull Function<Session, List<X>> expected);

    /**
     * Alias for `in` that's a reserved keyword in Kotlin
     *
     * @param expected the set of possible values, as a function
     * @return a new Final
     */
    @Nonnull
    default Final within(@Nonnull Function<Session, List<X>> expected) {
      return in(expected);
    }

    /**
     * Validate the check was able to extract any value
     *
     * @return a new Final
     */
    @Nonnull
    Final exists();

    /**
     * Validate the check was not able to extract any value
     *
     * @return a new Final
     */
    @Nonnull
    Final notExists();

    /**
     * Make the check is successful whenever it was able to extract something or not
     *
     * @return a new Final
     */
    @Nonnull
    Final optional();

    /**
     * Validate the extracted value is less than a given value
     *
     * @param value the value
     * @return a new Final
     */
    @Nonnull
    Final lt(@Nonnull X value);

    /**
     * Validate the extracted value is less than a given value, passed as a Gatling Expression
     * Language String
     *
     * @param value the value, as a Gatling Expression Language String
     * @return a new Final
     */
    @Nonnull
    Final ltEL(@Nonnull String value);

    /**
     * Validate the extracted value is less than a given value, passed as a function
     *
     * @param value the value, as a function
     * @return a new Final
     */
    @Nonnull
    Final lt(@Nonnull Function<Session, X> value);

    /**
     * Validate the extracted value is less than or equal to a given value
     *
     * @param value the value
     * @return a new Final
     */
    @Nonnull
    Final lte(@Nonnull X value);

    /**
     * Validate the extracted value is less than or equal to a given value, passed as a Gatling
     * Expression Language String
     *
     * @param value the value, as a Gatling Expression Language String
     * @return a new Final
     */
    @Nonnull
    Final lteEL(@Nonnull String value);

    /**
     * Validate the extracted value is less than or equal to a given value, passed as a function
     *
     * @param value the value, as a function
     * @return a new Final
     */
    @Nonnull
    Final lte(@Nonnull Function<Session, X> value);

    /**
     * Validate the extracted value is greater than a given value
     *
     * @param value the value
     * @return a new Final
     */
    @Nonnull
    Final gt(@Nonnull X value);

    /**
     * Validate the extracted value is greater than a given value, passed as a Gatling Expression
     * Language String
     *
     * @param value the value, as a Gatling Expression Language String
     * @return a new Final
     */
    @Nonnull
    Final gtEL(@Nonnull String value);

    /**
     * Validate the extracted value is greater than a given value, passed as a function
     *
     * @param value the value, as a function
     * @return a new Final
     */
    @Nonnull
    Final gt(@Nonnull Function<Session, X> value);

    /**
     * Validate the extracted value is greater than or equal to a given value
     *
     * @param value the value
     * @return a new Final
     */
    @Nonnull
    Final gte(@Nonnull X value);

    /**
     * Validate the extracted value is greater than or equal to a given value, passed as a Gatling
     * Expression Language String
     *
     * @param value the value, as a Gatling Expression Language String
     * @return a new Final
     */
    @Nonnull
    Final gteEL(@Nonnull String value);

    /**
     * Validate the extracted value is greater than or equal to a given value, passed as a function
     *
     * @param value the value, as a function
     * @return a new Final
     */
    @Nonnull
    Final gte(@Nonnull Function<Session, X> value);

    /**
     * Default implementation of {@link Validate}
     *
     * @param <T> the check type
     * @param <P> the prepared input type
     * @param <X> the type of the extracted value
     */
    final class Default<T, P, X> implements Validate<X> {
      private final io.gatling.core.check.CheckBuilder.Validate<T, P, X> wrapped;
      private final CheckType type;
      private final Class<?> xClass;

      public Default(
          io.gatling.core.check.CheckBuilder.Validate<T, P, X> wrapped,
          CheckType type,
          Class<?> xClass) {
        this.wrapped = wrapped;
        this.type = type;
        this.xClass = xClass;
      }

      private Final makeFinal(io.gatling.core.check.CheckBuilder.Final<T, P> wrapped) {
        return new Final.Default<>(wrapped, type);
      }

      @Override
      @Nonnull
      public <X2> Validate<X2> transform(@Nonnull Function<X, X2> f) {
        Class<?> x2Class = TypeResolver.resolveRawArguments(Function.class, f.getClass())[1];
        return new Validate.Default<>(
            wrapped.transformOption(
                optX -> {
                  X x = optX.isDefined() ? optX.get() : null;
                  return validation(() -> scala.Option.apply(f.apply(x)));
                }),
            type,
            x2Class);
      }

      @Override
      @Nonnull
      public <X2> Validate<X2> transformWithSession(@Nonnull BiFunction<X, Session, X2> f) {
        Class<?> x2Class = TypeResolver.resolveRawArguments(BiFunction.class, f.getClass())[2];
        return new Validate.Default<>(
            wrapped.transformOptionWithSession(
                (optX, session) -> {
                  X x = optX.isDefined() ? optX.get() : null;
                  return validation(() -> scala.Option.apply(f.apply(x, new Session(session))));
                }),
            type,
            x2Class);
      }

      @Override
      @Nonnull
      public Validate<X> withDefault(@Nonnull X value) {
        return new Validate.Default<>(
            wrapped.withDefault(toStaticValueExpression(value)), type, xClass);
      }

      @Override
      @Nonnull
      public Validate<X> withDefaultEl(@Nonnull String value) {
        return new Validate.Default<>(
            wrapped.withDefault(toExpression(value, xClass)), type, xClass);
      }

      @Override
      @Nonnull
      public Validate<X> withDefault(@Nonnull Function<Session, X> defaultValue) {
        return new Validate.Default<>(
            wrapped.withDefault(javaFunctionToExpression(defaultValue)), type, xClass);
      }

      @Override
      @Nonnull
      public Final is(@Nonnull X expected) {
        return makeFinal(wrapped.is(toStaticValueExpression(expected), equality(xClass)));
      }

      @Override
      @Nonnull
      public Final validate(@Nonnull String opName, @Nonnull BiFunction<X, Session, X> f) {
        return makeFinal(
            wrapped.validate(
                opName,
                (optX, session) -> {
                  X x = optX.isDefined() ? optX.get() : null;
                  return validation(() -> scala.Option.apply(f.apply(x, new Session(session))));
                }));
      }

      @Override
      @Nonnull
      public Final isEL(@Nonnull String expected) {
        return makeFinal(wrapped.is(toExpression(expected, xClass), equality(xClass)));
      }

      @Override
      @Nonnull
      public Final is(@Nonnull Function<Session, X> expected) {
        return makeFinal(wrapped.is(javaFunctionToExpression(expected), equality(xClass)));
      }

      @Override
      @Nonnull
      public Final isNull() {
        return makeFinal(wrapped.isNull());
      }

      @Override
      @Nonnull
      public Final not(@Nonnull X expected) {
        return makeFinal(wrapped.not(toStaticValueExpression(expected), equality(xClass)));
      }

      @Override
      @Nonnull
      public Final notEL(@Nonnull String expected) {
        return makeFinal(wrapped.not(toExpression(expected, xClass), equality(xClass)));
      }

      @Override
      @Nonnull
      public Final not(@Nonnull Function<Session, X> expected) {
        return makeFinal(wrapped.not(javaFunctionToExpression(expected), equality(xClass)));
      }

      @Override
      @Nonnull
      public Final notNull() {
        return makeFinal(wrapped.notNull());
      }

      @Override
      @Nonnull
      public Final in(@Nonnull X... expected) {
        return makeFinal(wrapped.in(toScalaSeq(expected)));
      }

      @Override
      @Nonnull
      public Final in(@Nonnull List<X> expected) {
        return makeFinal(wrapped.in(toScalaSeq(expected)));
      }

      @Override
      @Nonnull
      public Final inEL(@Nonnull String expected) {
        return makeFinal(wrapped.in(toSeqExpression(expected)));
      }

      @Override
      @Nonnull
      public Final in(@Nonnull Function<Session, List<X>> expected) {
        return makeFinal(wrapped.in(javaListFunctionToExpression(expected)));
      }

      @Override
      @Nonnull
      public Final exists() {
        return makeFinal(wrapped.exists());
      }

      @Override
      @Nonnull
      public Final notExists() {
        return makeFinal(wrapped.notExists());
      }

      @Override
      @Nonnull
      public Final optional() {
        return makeFinal(wrapped.optional());
      }

      @Override
      @Nonnull
      public Final lt(@Nonnull X expected) {
        return makeFinal(wrapped.lt(toStaticValueExpression(expected), ordering(xClass)));
      }

      @Override
      @Nonnull
      public Final ltEL(@Nonnull String expected) {
        return makeFinal(wrapped.lt(toExpression(expected, xClass), ordering(xClass)));
      }

      @Override
      @Nonnull
      public Final lt(@Nonnull Function<Session, X> expected) {
        return makeFinal(wrapped.lt(javaFunctionToExpression(expected), ordering(xClass)));
      }

      @Override
      @Nonnull
      public Final lte(@Nonnull X expected) {
        return makeFinal(wrapped.lte(toStaticValueExpression(expected), ordering(xClass)));
      }

      @Override
      @Nonnull
      public Final lteEL(@Nonnull String expected) {
        return makeFinal(wrapped.lte(toExpression(expected, xClass), ordering(xClass)));
      }

      @Override
      @Nonnull
      public Final lte(@Nonnull Function<Session, X> expected) {
        return makeFinal(wrapped.lte(javaFunctionToExpression(expected), ordering(xClass)));
      }

      @Override
      @Nonnull
      public Final gt(@Nonnull X expected) {
        return makeFinal(wrapped.gt(toStaticValueExpression(expected), ordering(xClass)));
      }

      @Override
      @Nonnull
      public Final gtEL(@Nonnull String expected) {
        return makeFinal(wrapped.gt(toExpression(expected, xClass), ordering(xClass)));
      }

      @Override
      @Nonnull
      public Final gt(@Nonnull Function<Session, X> expected) {
        return makeFinal(wrapped.gt(javaFunctionToExpression(expected), ordering(xClass)));
      }

      @Override
      @Nonnull
      public Final gte(@Nonnull X expected) {
        return makeFinal(wrapped.gte(toStaticValueExpression(expected), ordering(xClass)));
      }

      @Override
      @Nonnull
      public Final gteEL(@Nonnull String expected) {
        return makeFinal(wrapped.gte(toExpression(expected, xClass), ordering(xClass)));
      }

      @Override
      @Nonnull
      public Final gte(@Nonnull Function<Session, X> expected) {
        return makeFinal(wrapped.gte(javaFunctionToExpression(expected), ordering(xClass)));
      }

      @Override
      @Nonnull
      public Final name(@Nonnull String n) {
        return exists().name(n);
      }

      @Override
      @Nonnull
      public Final saveAs(@Nonnull String key) {
        return exists().saveAs(key);
      }

      @Override
      @Nonnull
      public CheckType type() {
        return exists().type();
      }

      @Override
      public io.gatling.core.check.CheckBuilder<?, ?> asScala() {
        return exists().asScala();
      }
    }
  }

  /**
   * Last step of the Check DSL Immutable, so all methods return a new occurrence and leave the
   * original unmodified.
   */
  interface Final extends CheckBuilder {

    /**
     * Provide a custom name for the check, to be used in case of a failure
     *
     * @param n the name
     * @return a new Final
     */
    @Nonnull
    Final name(@Nonnull String n);

    /**
     * Save the extracted value in the virtual user's {@link Session}
     *
     * @param key the key to store the extracted value in the {@link Session}
     * @return a new Final
     */
    @Nonnull
    Final saveAs(@Nonnull String key);

    /**
     * Default implementation of {@link Final}
     *
     * @param <T> the check type
     * @param <P> the prepared input type
     */
    final class Default<T, P> implements Final {
      private final io.gatling.core.check.CheckBuilder.Final<T, P> wrapped;
      private final CheckType type;

      public Default(io.gatling.core.check.CheckBuilder.Final<T, P> wrapped, CheckType type) {
        this.wrapped = wrapped;
        this.type = type;
      }

      @Override
      @Nonnull
      public Final name(@Nonnull String n) {
        return new Default<>(wrapped.name(n), type);
      }

      @Override
      @Nonnull
      public Final saveAs(@Nonnull String key) {
        return new Default<>(wrapped.saveAs(key), type);
      }

      @Override
      @Nonnull
      public CheckType type() {
        return type;
      }

      @Override
      public io.gatling.core.check.CheckBuilder<?, ?> asScala() {
        return wrapped;
      }
    }
  }

  /** A special {@link MultipleFind<String>} that can define regex capture groups */
  interface CaptureGroupCheckBuilder extends MultipleFind<String> {

    /**
     * Define that the check extracts an expected number of values from capture groups
     *
     * @param count the number of capture groups in the regular expression pattern
     * @return a new MultipleFind
     */
    @Nonnull
    MultipleFind<List<String>> captureGroups(int count);

    /**
     * Default implementation of {@link CaptureGroupCheckBuilder}
     *
     * @param <T> the check type
     * @param <P> the prepared input type
     */
    abstract class Default<T, P> extends MultipleFind.Default<T, P, String, String>
        implements CaptureGroupCheckBuilder {
      public Default(
          io.gatling.core.check.CheckBuilder.MultipleFind<T, P, String> wrapped,
          CheckBuilder.CheckType type) {
        super(wrapped, type, String.class, null);
      }

      protected abstract <X> io.gatling.core.check.CheckBuilder.MultipleFind<T, P, X> extract(
          io.gatling.core.check.regex.GroupExtractor<X> groupExtractor);

      @Override
      @Nonnull
      public MultipleFind<List<String>> captureGroups(int count) {
        switch (count) {
          case 2:
            return new MultipleFind.Default<>(
                extract(io.gatling.core.check.regex.GroupExtractor.groupExtractor2()),
                type,
                List.class,
                Converters::toJavaList);
          case 3:
            return new MultipleFind.Default<>(
                extract(io.gatling.core.check.regex.GroupExtractor.groupExtractor3()),
                type,
                List.class,
                Converters::toJavaList);
          case 4:
            return new MultipleFind.Default<>(
                extract(io.gatling.core.check.regex.GroupExtractor.groupExtractor4()),
                type,
                List.class,
                Converters::toJavaList);
          case 5:
            return new MultipleFind.Default<>(
                extract(io.gatling.core.check.regex.GroupExtractor.groupExtractor5()),
                type,
                List.class,
                Converters::toJavaList);
          case 6:
            return new MultipleFind.Default<>(
                extract(io.gatling.core.check.regex.GroupExtractor.groupExtractor6()),
                type,
                List.class,
                Converters::toJavaList);
          case 7:
            return new MultipleFind.Default<>(
                extract(io.gatling.core.check.regex.GroupExtractor.groupExtractor7()),
                type,
                List.class,
                Converters::toJavaList);
          case 8:
            return new MultipleFind.Default<>(
                extract(io.gatling.core.check.regex.GroupExtractor.groupExtractor8()),
                type,
                List.class,
                Converters::toJavaList);
          default:
            throw new IllegalArgumentException(
                "captureGroups only supports between 2 and 8 capture groups, included, not "
                    + count);
        }
      }
    }
  }

  /** An implementation of {@link CaptureGroupCheckBuilder} for regex applied on Strings */
  final class Regex
      extends CaptureGroupCheckBuilder.Default<io.gatling.core.check.regex.RegexCheckType, String> {

    public Regex(
        io.gatling.core.check.CheckBuilder.MultipleFind<
                io.gatling.core.check.regex.RegexCheckType, String, String>
            wrapped) {
      super(wrapped, CoreCheckType.Regex);
    }

    @Override
    @Nonnull
    protected <X>
        io.gatling.core.check.CheckBuilder.MultipleFind<RegexCheckType, String, X> extract(
            @Nonnull GroupExtractor<X> groupExtractor) {
      io.gatling.core.check.regex.RegexCheckBuilder<String> actual =
          (io.gatling.core.check.regex.RegexCheckBuilder<String>) wrapped;
      return new io.gatling.core.check.regex.RegexCheckBuilder<>(
          actual.pattern(), actual.patterns(), groupExtractor);
    }
  }

  /** A special {@link Find<String>} that works on JSON */
  interface JsonOfTypeFind extends Find<String> {

    /**
     * Define that the extracted value is a String
     *
     * @return a new Find
     */
    @Nonnull
    Find<String> ofString();

    /**
     * Define that the extracted value is a Boolean
     *
     * @return a new Find
     */
    @Nonnull
    Find<Boolean> ofBoolean();

    /**
     * Define that the extracted value is an Integer
     *
     * @return a new Find
     */
    @Nonnull
    Find<Integer> ofInt();

    /**
     * Define that the extracted value is a Long
     *
     * @return a new Find
     */
    @Nonnull
    Find<Long> ofLong();

    /**
     * Define that the extracted value is a Double
     *
     * @return a new Find
     */
    @Nonnull
    Find<Double> ofDouble();

    /**
     * Define that the extracted value is a List (a JSON array)
     *
     * @return a new Find
     */
    @Nonnull
    Find<List<Object>> ofList();

    /**
     * Define that the extracted value is a Map (a JSON object)
     *
     * @return a new Find
     */
    @Nonnull
    Find<Map<String, Object>> ofMap();

    /**
     * Define that the extracted value is an untyped object
     *
     * @return a new Find
     */
    @Nonnull
    Find<Object> ofObject();

    /**
     * Default implementation of {@link JsonOfTypeFind}
     *
     * @param <T> the check type
     */
    abstract class Default<T> extends Find.Default<T, JsonNode, String, String>
        implements JsonOfTypeFind {
      public Default(
          io.gatling.core.check.CheckBuilder.Find<T, JsonNode, String> wrapped, CheckType type) {
        super(wrapped, type, String.class, null);
      }

      @Nonnull
      protected abstract <X> io.gatling.core.check.CheckBuilder.Find<T, JsonNode, X> ofType(
          io.gatling.core.check.jsonpath.JsonFilter<X> filter);

      @Override
      @Nonnull
      public Find<String> ofString() {
        return new Find.Default<>(
            ofType(io.gatling.core.check.jsonpath.JsonFilter.stringJsonFilter()),
            type,
            String.class,
            null);
      }

      @Override
      @Nonnull
      public Find<Boolean> ofBoolean() {
        return new Find.Default<>(
            ofType(io.gatling.core.check.jsonpath.JsonFilter.jBooleanJsonFilter()),
            type,
            Boolean.class,
            Boolean.class::cast);
      }

      @Override
      @Nonnull
      public Find<Integer> ofInt() {
        return new Find.Default<>(
            ofType(io.gatling.core.check.jsonpath.JsonFilter.jIntegerJsonFilter()),
            type,
            Integer.class,
            Integer.class::cast);
      }

      @Override
      @Nonnull
      public Find<Long> ofLong() {
        return new Find.Default<>(
            ofType(io.gatling.core.check.jsonpath.JsonFilter.jLongJsonFilter()),
            type,
            Long.class,
            Long.class::cast);
      }

      @Override
      @Nonnull
      public Find<Double> ofDouble() {
        return new Find.Default<>(
            ofType(io.gatling.core.check.jsonpath.JsonFilter.jDoubleJsonFilter()),
            type,
            Double.class,
            Double.class::cast);
      }

      @Override
      @Nonnull
      public Find<List<Object>> ofList() {
        return new Find.Default<>(
            ofType(io.gatling.core.check.jsonpath.JsonFilter.jListJsonFilter()),
            type,
            List.class,
            null);
      }

      @Override
      @Nonnull
      public Find<Map<String, Object>> ofMap() {
        return new Find.Default<>(
            ofType(io.gatling.core.check.jsonpath.JsonFilter.jMapJsonFilter()),
            type,
            Map.class,
            null);
      }

      @Override
      @Nonnull
      public Find<Object> ofObject() {
        return new Find.Default<>(
            ofType(io.gatling.core.check.jsonpath.JsonFilter.jObjectJsonFilter()),
            type,
            Object.class,
            null);
      }
    }
  }

  /** A special {@link MultipleFind<String>} that works on JSON */
  interface JsonOfTypeMultipleFind extends MultipleFind<String> {

    /**
     * Define that the extracted value is a String
     *
     * @return a new MultipleFind
     */
    @Nonnull
    MultipleFind<String> ofString();

    /**
     * Define that the extracted value is a Boolean
     *
     * @return a new MultipleFind
     */
    @Nonnull
    MultipleFind<Boolean> ofBoolean();

    /**
     * Define that the extracted value is an Integer
     *
     * @return a new MultipleFind
     */
    @Nonnull
    MultipleFind<Integer> ofInt();

    /**
     * Define that the extracted value is a Long
     *
     * @return a new MultipleFind
     */
    @Nonnull
    MultipleFind<Long> ofLong();

    /**
     * Define that the extracted value is a Double
     *
     * @return a new MultipleFind
     */
    @Nonnull
    MultipleFind<Double> ofDouble();

    /**
     * Define that the extracted value is a List (a JSON array)
     *
     * @return a new MultipleFind
     */
    @Nonnull
    MultipleFind<List<Object>> ofList();

    /**
     * Define that the extracted value is a Map (a JSON object)
     *
     * @return a new MultipleFind
     */
    @Nonnull
    MultipleFind<Map<String, Object>> ofMap();

    /**
     * Define that the extracted value is an untyped object
     *
     * @return a new MultipleFind
     */
    @Nonnull
    MultipleFind<Object> ofObject();

    /**
     * Default implementation of {@link JsonOfTypeMultipleFind}
     *
     * @param <T> the check type
     */
    abstract class Default<T> extends MultipleFind.Default<T, JsonNode, String, String>
        implements JsonOfTypeMultipleFind {
      public Default(
          io.gatling.core.check.CheckBuilder.MultipleFind<T, JsonNode, String> wrapped,
          CheckType type) {
        super(wrapped, type, String.class, null);
      }

      @Nonnull
      protected abstract <X> io.gatling.core.check.CheckBuilder.MultipleFind<T, JsonNode, X> ofType(
          io.gatling.core.check.jsonpath.JsonFilter<X> filter);

      @Override
      @Nonnull
      public MultipleFind<String> ofString() {
        return new MultipleFind.Default<>(
            ofType(io.gatling.core.check.jsonpath.JsonFilter.stringJsonFilter()),
            type,
            String.class,
            null);
      }

      @Override
      @Nonnull
      public MultipleFind<Boolean> ofBoolean() {
        return new MultipleFind.Default<>(
            ofType(io.gatling.core.check.jsonpath.JsonFilter.jBooleanJsonFilter()),
            type,
            Boolean.class,
            Boolean.class::cast);
      }

      @Override
      @Nonnull
      public MultipleFind<Integer> ofInt() {
        return new MultipleFind.Default<>(
            ofType(io.gatling.core.check.jsonpath.JsonFilter.jIntegerJsonFilter()),
            type,
            Integer.class,
            Integer.class::cast);
      }

      @Override
      @Nonnull
      public MultipleFind<Long> ofLong() {
        return new MultipleFind.Default<>(
            ofType(io.gatling.core.check.jsonpath.JsonFilter.jLongJsonFilter()),
            type,
            Long.class,
            Long.class::cast);
      }

      @Override
      @Nonnull
      public MultipleFind<Double> ofDouble() {
        return new MultipleFind.Default<>(
            ofType(io.gatling.core.check.jsonpath.JsonFilter.jDoubleJsonFilter()),
            type,
            Double.class,
            Double.class::cast);
      }

      @Override
      @Nonnull
      public MultipleFind<List<Object>> ofList() {
        return new MultipleFind.Default<>(
            ofType(io.gatling.core.check.jsonpath.JsonFilter.jListJsonFilter()),
            type,
            List.class,
            null);
      }

      @Override
      @Nonnull
      public MultipleFind<Map<String, Object>> ofMap() {
        return new MultipleFind.Default<>(
            ofType(io.gatling.core.check.jsonpath.JsonFilter.jMapJsonFilter()),
            type,
            Map.class,
            null);
      }

      @Override
      @Nonnull
      public MultipleFind<Object> ofObject() {
        return new MultipleFind.Default<>(
            ofType(io.gatling.core.check.jsonpath.JsonFilter.jObjectJsonFilter()),
            type,
            Object.class,
            null);
      }
    }
  }

  /**
   * An implementation of {@link JsonOfTypeFind} for JMESPath.
   *
   * @see <a href="https://jmespath.org/">https://jmespath.org/</a>
   */
  final class JmesPath
      extends JsonOfTypeFind.Default<io.gatling.core.check.jmespath.JmesPathCheckType> {

    public JmesPath(
        io.gatling.core.check.CheckBuilder.Find<
                io.gatling.core.check.jmespath.JmesPathCheckType, JsonNode, String>
            wrapped) {
      super(wrapped, CoreCheckType.JmesPath);
    }

    @Override
    @Nonnull
    protected <X> io.gatling.core.check.CheckBuilder.Find<JmesPathCheckType, JsonNode, X> ofType(
        JsonFilter<X> filter) {
      io.gatling.core.check.jmespath.JmesPathCheckBuilder<String> actual =
          (io.gatling.core.check.jmespath.JmesPathCheckBuilder<String>) wrapped;
      return new io.gatling.core.check.jmespath.JmesPathCheckBuilder<>(
          actual.path(), actual.jmesPaths(), filter);
    }
  }

  /**
   * An implementation of {@link JsonOfTypeFind} for JSONP + JMESPath.
   *
   * @see <a href="https://jmespath.org/">https://jmespath.org/</a>
   */
  final class JsonpJmesPath
      extends JsonOfTypeFind.Default<io.gatling.core.check.jmespath.JsonpJmesPathCheckType> {

    public JsonpJmesPath(
        @Nonnull
            io.gatling.core.check.CheckBuilder.Find<
                    io.gatling.core.check.jmespath.JsonpJmesPathCheckType, JsonNode, String>
                wrapped) {
      super(wrapped, CoreCheckType.JsonpJmesPath);
    }

    @Override
    @Nonnull
    protected <X>
        io.gatling.core.check.CheckBuilder.Find<JsonpJmesPathCheckType, JsonNode, X> ofType(
            JsonFilter<X> filter) {
      io.gatling.core.check.jmespath.JsonpJmesPathCheckBuilder<String> actual =
          (io.gatling.core.check.jmespath.JsonpJmesPathCheckBuilder<String>) wrapped;
      return new io.gatling.core.check.jmespath.JsonpJmesPathCheckBuilder<>(
          actual.path(), actual.jmesPaths(), filter);
    }
  }

  /**
   * An implementation of {@link JsonOfTypeFind} for JsonPath.
   *
   * @see <a
   *     href="https://goessner.net/articles/JsonPath/">https://goessner.net/articles/JsonPath/</a>
   */
  final class JsonPath
      extends JsonOfTypeMultipleFind.Default<io.gatling.core.check.jsonpath.JsonPathCheckType> {

    public JsonPath(
        @Nonnull
            io.gatling.core.check.CheckBuilder.MultipleFind<
                    io.gatling.core.check.jsonpath.JsonPathCheckType, JsonNode, String>
                wrapped) {
      super(wrapped, CoreCheckType.JsonPath);
    }

    @Override
    @Nonnull
    protected <X>
        io.gatling.core.check.CheckBuilder.MultipleFind<JsonPathCheckType, JsonNode, X> ofType(
            JsonFilter<X> filter) {
      io.gatling.core.check.jsonpath.JsonPathCheckBuilder<String> actual =
          (io.gatling.core.check.jsonpath.JsonPathCheckBuilder<String>) wrapped;
      return new io.gatling.core.check.jsonpath.JsonPathCheckBuilder<>(
          actual.path(), actual.jsonPaths(), filter);
    }
  }

  /**
   * An implementation of {@link JsonOfTypeFind} for JSONP + JsonPath.
   *
   * @see <a
   *     href="https://goessner.net/articles/JsonPath/">https://goessner.net/articles/JsonPath/</a>
   */
  final class JsonpJsonPath
      extends JsonOfTypeMultipleFind.Default<
          io.gatling.core.check.jsonpath.JsonpJsonPathCheckType> {

    public JsonpJsonPath(
        @Nonnull
            io.gatling.core.check.CheckBuilder.MultipleFind<
                    io.gatling.core.check.jsonpath.JsonpJsonPathCheckType, JsonNode, String>
                wrapped) {
      super(wrapped, CoreCheckType.JsonpJsonPath);
    }

    @Override
    @Nonnull
    protected <X>
        io.gatling.core.check.CheckBuilder.MultipleFind<JsonpJsonPathCheckType, JsonNode, X> ofType(
            JsonFilter<X> filter) {
      io.gatling.core.check.jsonpath.JsonpJsonPathCheckBuilder<String> actual =
          (io.gatling.core.check.jsonpath.JsonpJsonPathCheckBuilder<String>) wrapped;
      return new io.gatling.core.check.jsonpath.JsonpJsonPathCheckBuilder<>(
          actual.path(), actual.jsonPaths(), filter);
    }
  }

  /** A special {@link MultipleFind<String>} that works on Css selectors */
  interface CssOfTypeMultipleFind extends MultipleFind<String> {

    /**
     * Define that the extracted value is a String
     *
     * @return a new MultipleFind
     */
    @Nonnull
    MultipleFind<String> ofString();

    /**
     * Define that the extracted value is a Lagarto DOM Node
     *
     * @return a new MultipleFind
     */
    @Nonnull
    MultipleFind<Node> ofNode();

    /**
     * Default implementation of {@link JsonOfTypeMultipleFind}
     *
     * @param <T> the check type
     */
    abstract class Default<T> extends MultipleFind.Default<T, NodeSelector, String, String>
        implements CssOfTypeMultipleFind {
      public Default(
          io.gatling.core.check.CheckBuilder.MultipleFind<T, NodeSelector, String> wrapped,
          CheckType type) {
        super(wrapped, type, String.class, null);
      }

      @Nonnull
      protected abstract <X>
          io.gatling.core.check.CheckBuilder.MultipleFind<T, NodeSelector, X> ofType(
              io.gatling.core.check.css.NodeConverter<X> nodeConverter);

      @Override
      @Nonnull
      public MultipleFind<String> ofString() {
        return new MultipleFind.Default<>(
            ofType(io.gatling.core.check.css.NodeConverter.stringNodeConverter()),
            type,
            String.class,
            null);
      }

      @Override
      @Nonnull
      public MultipleFind<Node> ofNode() {
        return new MultipleFind.Default<>(
            ofType(io.gatling.core.check.css.NodeConverter.nodeNodeConverter()),
            type,
            Node.class,
            null);
      }
    }
  }

  /** An implementation of {@link CssOfTypeMultipleFind} for css selectors applied on Strings */
  final class Css extends CssOfTypeMultipleFind.Default<io.gatling.core.check.css.CssCheckType> {

    public Css(
        io.gatling.core.check.CheckBuilder.MultipleFind<
                io.gatling.core.check.css.CssCheckType, NodeSelector, String>
            wrapped) {
      super(wrapped, CoreCheckType.Css);
    }

    @Nonnull
    @Override
    protected <X>
        io.gatling.core.check.CheckBuilder.MultipleFind<CssCheckType, NodeSelector, X> ofType(
            NodeConverter<X> nodeConverter) {
      io.gatling.core.check.css.CssCheckBuilder<String> actual =
          (io.gatling.core.check.css.CssCheckBuilder<String>) wrapped;
      return new io.gatling.core.check.css.CssCheckBuilder<>(
          actual.expression(), actual.nodeAttribute(), actual.selectors(), nodeConverter);
    }
  }
}
