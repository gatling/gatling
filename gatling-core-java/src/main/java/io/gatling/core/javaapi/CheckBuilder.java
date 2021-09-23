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

import com.fasterxml.jackson.databind.JsonNode;
import io.gatling.core.check.jmespath.JmesPathCheckType;
import io.gatling.core.check.jmespath.JsonpJmesPathCheckType;
import io.gatling.core.check.jsonpath.JsonFilter;
import io.gatling.core.check.jsonpath.JsonPathCheckType;
import io.gatling.core.check.jsonpath.JsonpJsonPathCheckType;
import io.gatling.core.check.regex.GroupExtractor;
import io.gatling.core.check.regex.RegexCheckType;
import io.gatling.core.javaapi.internal.CoreCheckType;
import net.jodah.typetools.TypeResolver;

import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;

import static io.gatling.core.javaapi.internal.ScalaHelpers.*;

public interface CheckBuilder {

  io.gatling.core.check.CheckBuilder<?, ?> asScala();
  CheckType type();

  interface CheckType {
  }

  interface Find<JavaX> extends Validate<JavaX> {
     Validate<JavaX> find();

    class Default<T, P, ScalaX, JavaX> implements Find<JavaX> {
      protected final io.gatling.core.check.CheckBuilder.Find<T, P, ScalaX> wrapped;
      protected final CheckType type;
      protected final Class<?> javaXClass;
      protected final Function<ScalaX, JavaX> scalaXToJavaX;

      public Default(io.gatling.core.check.CheckBuilder.Find<T, P, ScalaX> wrapped, CheckType type, Function<ScalaX, JavaX> scalaXToJavaX) {
        this.wrapped = wrapped;
        this.type = type;
        this.javaXClass = TypeResolver.resolveRawArguments(Function.class, scalaXToJavaX.getClass())[1];;
        this.scalaXToJavaX = scalaXToJavaX;
      }

      @Override
      public Validate<JavaX> find() {
        return new Validate.Default<>(wrapped.find().transform(toScalaFunction(scalaXToJavaX)), type, javaXClass);
      }

      @Override
      public <X2> Validate<X2> transform(Function<JavaX, X2> f) {
        return find().transform(f);
      }

      @Override
      public <X2> Validate<X2> transformWithSession(BiFunction<JavaX, Session, X2> f) {
        return find().transformWithSession(f);
      }

      @Override
      public Final isEL(String expected) {
        return find().isEL(expected);
      }

      @Override
      public Final is(JavaX expected) {
        return find().is(expected);
      }

      @Override
      public Final is(Function<Session, JavaX> expected) {
        return find().is(expected);
      }

      @Override
      public Final isNull() {
        return find().isNull();
      }

      @Override
      public Final notEL(String expected) {
        return find().notEL(expected);
      }

      @Override
      public Final not(JavaX expected) {
        return find().not(expected);
      }

      @Override
      public Final not(Function<Session, JavaX> expected) {
        return find().not(expected);
      }

      @Override
      public Final notNull() {
        return find().notNull();
      }

      @Override
      public Final in(JavaX... expected) {
        return find().in(expected);
      }

      @Override
      public Final in(List<JavaX> expected) {
        return find().in(expected);
      }

      @Override
      public Final inEL(String expected) {
        return find().inEL(expected);
      }

      @Override
      public Final in(Function<Session, List<JavaX>> expected) {
        return find().in(expected);
      }

      @Override
      public Final exists() {
        return find().exists();
      }

      @Override
      public Final notExists() {
        return find().notExists();
      }

      @Override
      public Final optional() {
        return find().optional();
      }

      @Override
      public Final lt(JavaX expected) {
        return find().lt(expected);
      }

      @Override
      public Final ltEL(String expected) {
        return find().ltEL(expected);
      }

      @Override
      public Final lt(Function<Session, JavaX> expected) {
        return find().lt(expected);
      }

      @Override
      public Final lte(JavaX expected) {
        return find().lte(expected);
      }

      @Override
      public Final lteEL(String expected) {
        return find().lteEL(expected);
      }

      @Override
      public Final lte(Function<Session, JavaX> expected) {
        return find().lte(expected);
      }

      @Override
      public Final gt(JavaX expected) {
        return find().gt(expected);
      }

      @Override
      public Final gtEL(String expected) {
        return find().gtEL(expected);
      }

      @Override
      public Final gt(Function<Session, JavaX> expected) {
        return find().gt(expected);
      }

      @Override
      public Final gte(JavaX expected) {
        return find().gte(expected);
      }

      @Override
      public Final gteEL(String expected) {
        return find().gteEL(expected);
      }

      @Override
      public Final gte(Function<Session, JavaX> expected) {
        return find().gte(expected);
      }

      @Override
      public Final name(String n) {
        return find().name(n);
      }

      @Override
      public Final saveAs(String key) {
        return find().saveAs(key);
      }

      @Override
      public CheckType type() {
        return find().type();
      }

      @Override
      public io.gatling.core.check.CheckBuilder<?, ?> asScala() {
        return find().asScala();
      }
    }
  }

  interface MultipleFind<JavaX> extends Find<JavaX> {
    Validate<JavaX> find(int occurrence);
    Validate<List<JavaX>> findAll();
    Validate<JavaX> findRandom();
    Validate<List<JavaX>> findRandom(int num);
    Validate<List<JavaX>> findRandom(int num, boolean failIfLess);
    Validate<Integer> count();

    class Default<T, P, ScalaX, JavaX> extends Find.Default<T, P, ScalaX, JavaX> implements MultipleFind<JavaX> {
      protected final io.gatling.core.check.CheckBuilder.MultipleFind<T, P, ScalaX> wrapped;

      public Default(io.gatling.core.check.CheckBuilder.MultipleFind<T, P, ScalaX> wrapped, CheckType type, Function<ScalaX, JavaX> scalaXToJavaX) {
        super(wrapped, type, scalaXToJavaX);
        this.wrapped = wrapped;
      }

      private <X2> Validate<X2> makeValidate(io.gatling.core.check.CheckBuilder.Validate<T, P, X2> wrapped) {
        return new Validate.Default<>(wrapped, type, javaXClass);
      }

      @Override
      public Validate<JavaX> find() {
        return makeValidate(transformSingleCheck(wrapped.find(), scalaXToJavaX));
      }

      @Override
      public Validate<JavaX> find(int occurrence) {
        return makeValidate(transformSingleCheck(wrapped.find(occurrence), scalaXToJavaX));
      }

      @Override
      public Validate<List<JavaX>> findAll() {
        return makeValidate(transformSeqCheck(wrapped.findAll(), scalaXToJavaX));
      }

      @Override
      public Validate<JavaX> findRandom() {
        return makeValidate(transformSingleCheck(wrapped.findRandom(), scalaXToJavaX));
      }

      @Override
      public Validate<List<JavaX>> findRandom(int num) {
        return findRandom(num, false);
      }

      @Override
      public Validate<List<JavaX>> findRandom(int num, boolean failIfLess) {
        return makeValidate(transformSeqCheck(wrapped.findRandom(num, failIfLess), scalaXToJavaX));
      }

      @Override
      public Validate<Integer> count() {
        return makeValidate(toCountCheck(wrapped));
      }
    }
  }

  interface Validate<X> extends Final {
    <X2> Validate<X2> transform(Function<X, X2> f);
    <X2> Validate<X2> transformWithSession(BiFunction<X, Session, X2> f);
    //    def transformOption[X2](transformation: Option[X] => Validation[Option[X2]]): ValidatorCheckBuilder[T, P, X2]
//    def transformOptionWithSession[X2](transformation: (Option[X], Session) => Validation[Option[X2]]): ValidatorCheckBuilder[T, P, X2]
//    def validate(validator: Expression[Validator[X]]): CheckBuilder[T, P, X]
//    def validate(opName: String, validator: (Option[X], Session) => Validation[Option[X]]): CheckBuilder[T, P, X]
    Final isEL(String expected);
    Final is(X expected);
    Final is(Function<Session, X> expected);
    Final isNull();
    Final notEL(String expected);
    Final not(X expected);
    Final not(Function<Session, X> expected);
    Final notNull();
    Final in(X... expected);
    Final in(List<X> expected);
    Final inEL(String expected);
    Final in(Function<Session, List<X>> expected);
    Final exists();
    Final notExists();
    Final optional();
    Final lt(X expected);
    Final ltEL(String expected);
    Final lt(Function<Session, X> expected);
    Final lte(X expected);
    Final lteEL(String expected);
    Final lte(Function<Session, X> expected);
    Final gt(X expected);
    Final gtEL(String expected);
    Final gt(Function<Session, X> expected);
    Final gte(X expected);
    Final gteEL(String expected);
    Final gte(Function<Session, X> expected);

    final class Default<T, P, X> implements Validate<X> {
      private final io.gatling.core.check.CheckBuilder.Validate<T, P, X> wrapped;
      private final CheckType type;
      private final Class<?> xClass;

      public Default(io.gatling.core.check.CheckBuilder.Validate<T, P, X> wrapped, CheckType type, Class<?> xClass) {
        this.wrapped = wrapped;
        this.type = type;
        this.xClass = xClass;
      }

      private Final makeFinal(io.gatling.core.check.CheckBuilder.Final<T, P> wrapped) {
        return new Final.Default<>(wrapped, type);
      }

      @Override
      public <X2> Validate<X2> transform(Function<X, X2> f) {
        Class<?> x2Class = TypeResolver.resolveRawArguments(Function.class, f.getClass())[1];
        return new Validate.Default<>(wrapped.transform(f::apply), type, x2Class);
      }

      @Override
      public <X2> Validate<X2> transformWithSession(BiFunction<X, Session, X2> f) {
        Class<?> x2Class = TypeResolver.resolveRawArguments(BiFunction.class, f.getClass())[2];
        return new Validate.Default<>(wrapped.transformWithSession((x, session) -> f.apply(x, new Session(session))), type, x2Class);
      }

      @Override
      public Final is(X expected) {
        return makeFinal(wrapped.is(toStaticValueExpression(expected), equality(xClass)));
      }

      @Override
      public Final isEL(String expected) {
        return makeFinal(wrapped.is(toExpression(expected, xClass), equality(xClass)));
      }

      @Override
      public Final is(Function<Session, X> expected) {
        return makeFinal(wrapped.is(toTypedGatlingSessionFunction(expected), equality(xClass)));
      }

      @Override
      public Final isNull() {
        return makeFinal(wrapped.isNull());
      }

      @Override
      public Final not(X expected) {
        return makeFinal(wrapped.not(toStaticValueExpression(expected), equality(xClass)));
      }

      @Override
      public Final notEL(String expected) {
        return makeFinal(wrapped.not(toExpression(expected, xClass), equality(xClass)));
      }

      @Override
      public Final not(Function<Session, X> expected) {
        return makeFinal(wrapped.not(toTypedGatlingSessionFunction(expected), equality(xClass)));
      }

      @Override
      public Final notNull() {
        return makeFinal(wrapped.notNull());
      }

      @Override
      public Final in(X... expected) {
        return makeFinal(wrapped.in(toScalaSeq(expected)));
      }

      @Override
      public Final in(List<X> expected) {
        return makeFinal(wrapped.in(toScalaSeq(expected)));
      }

      @Override
      public Final inEL(String expected) {
        return makeFinal(wrapped.in(toSeqExpression(expected)));
      }

      @Override
      public Final in(Function<Session, List<X>> expected) {
        return makeFinal(wrapped.in(toGatlingSessionFunctionImmutableSeq(expected)));
      }

      @Override
      public Final exists() {
        return makeFinal(wrapped.exists());
      }

      @Override
      public Final notExists() {
        return makeFinal(wrapped.notExists());
      }

      @Override
      public Final optional() {
        return makeFinal(wrapped.optional());
      }

      @Override
      public Final lt(X expected) {
        return makeFinal(wrapped.lt(toStaticValueExpression(expected), ordering(xClass)));
      }

      @Override
      public Final ltEL(String expected) {
        return makeFinal(wrapped.lt(toExpression(expected, xClass), ordering(xClass)));
      }

      @Override
      public Final lt(Function<Session, X> expected) {
        return makeFinal(wrapped.lt(toTypedGatlingSessionFunction(expected), ordering(xClass)));
      }

      @Override
      public Final lte(X expected) {
        return makeFinal(wrapped.lte(toStaticValueExpression(expected), ordering(xClass)));
      }

      @Override
      public Final lteEL(String expected) {
        return makeFinal(wrapped.lte(toExpression(expected, xClass), ordering(xClass)));
      }

      @Override
      public Final lte(Function<Session, X> expected) {
        return makeFinal(wrapped.lte(toTypedGatlingSessionFunction(expected), ordering(xClass)));
      }

      @Override
      public Final gt(X expected) {
        return makeFinal(wrapped.gt(toStaticValueExpression(expected), ordering(xClass)));
      }

      @Override
      public Final gtEL(String expected) {
        return makeFinal(wrapped.gt(toExpression(expected, xClass), ordering(xClass)));
      }

      @Override
      public Final gt(Function<Session, X> expected) {
        return makeFinal(wrapped.gt(toTypedGatlingSessionFunction(expected), ordering(xClass)));
      }

      @Override
      public Final gte(X expected) {
        return makeFinal(wrapped.gte(toStaticValueExpression(expected), ordering(xClass)));
      }

      @Override
      public Final gteEL(String expected) {
        return makeFinal(wrapped.gte(toExpression(expected, xClass), ordering(xClass)));
      }

      @Override
      public Final gte(Function<Session, X> expected) {
        return makeFinal(wrapped.gte(toTypedGatlingSessionFunction(expected), ordering(xClass)));
      }

      @Override
      public Final name(String n) {
        return exists().name(n);
      }

      @Override
      public Final saveAs(String key) {
        return exists().saveAs(key);
      }

      @Override
      public CheckType type() {
        return exists().type();
      }

      @Override
      public io.gatling.core.check.CheckBuilder<?, ?> asScala() {
        return exists().asScala();
      }
    }
  }

  interface Final extends CheckBuilder {
    Final name(String n);
    Final saveAs(String key);

    final class Default<T, P> implements Final {
      private final io.gatling.core.check.CheckBuilder.Final<T, P> wrapped;
      private final CheckType type;

      public Default(io.gatling.core.check.CheckBuilder.Final<T, P> wrapped, CheckType type) {
        this.wrapped = wrapped;
        this.type = type;
      }

      @Override
      public Final name(String n) {
        return new Default<>(wrapped.name(n), type);
      }

      @Override
      public Final saveAs(String key) {
        return new Default<>(wrapped.saveAs(key), type);
      }

      @Override
      public CheckType type() {
        return type;
      }

      @Override
      public io.gatling.core.check.CheckBuilder<?, ?> asScala() {
        return wrapped;
      }
    }
  }

  interface CaptureGroupCheckBuilder extends MultipleFind<String> {
    MultipleFind<RegexGroups.Tuple2> capture2();
    MultipleFind<RegexGroups.Tuple3> capture3();
    MultipleFind<RegexGroups.Tuple4> capture4();
    MultipleFind<RegexGroups.Tuple5> capture5();
    MultipleFind<RegexGroups.Tuple6> capture6();
    MultipleFind<RegexGroups.Tuple7> capture7();
    MultipleFind<RegexGroups.Tuple8> capture8();

    abstract class Default<T, P> extends MultipleFind.Default<T, P, String, String> implements CaptureGroupCheckBuilder {
      public Default(io.gatling.core.check.CheckBuilder.MultipleFind<T, P, String> wrapped, CheckBuilder.CheckType type) {
        super(wrapped, type, Function.identity());
      }

      protected abstract <X> io.gatling.core.check.CheckBuilder.MultipleFind<T, P, X> extract(io.gatling.core.check.regex.GroupExtractor<X> groupExtractor);

      @Override
      public MultipleFind<RegexGroups.Tuple2> capture2() {
        return new MultipleFind.Default<>(extract(io.gatling.core.check.regex.GroupExtractor.groupExtractor2()), type, RegexGroups.Tuple2::fromScala);
      }

      @Override
      public MultipleFind<RegexGroups.Tuple3> capture3() {
        return new MultipleFind.Default<>(extract(io.gatling.core.check.regex.GroupExtractor.groupExtractor3()), type, RegexGroups.Tuple3::fromScala);
      }

      @Override
      public MultipleFind<RegexGroups.Tuple4> capture4() {
        return new MultipleFind.Default<>(extract(io.gatling.core.check.regex.GroupExtractor.groupExtractor4()), type, RegexGroups.Tuple4::fromScala);
      }

      @Override
      public MultipleFind<RegexGroups.Tuple5> capture5() {
        return new MultipleFind.Default<>(extract(io.gatling.core.check.regex.GroupExtractor.groupExtractor5()), type, RegexGroups.Tuple5::fromScala);
      }

      @Override
      public MultipleFind<RegexGroups.Tuple6> capture6() {
        return new MultipleFind.Default<>(extract(io.gatling.core.check.regex.GroupExtractor.groupExtractor6()), type, RegexGroups.Tuple6::fromScala);
      }

      @Override
      public MultipleFind<RegexGroups.Tuple7> capture7() {
        return new MultipleFind.Default<>(extract(io.gatling.core.check.regex.GroupExtractor.groupExtractor7()), type, RegexGroups.Tuple7::fromScala);
      }

      @Override
      public MultipleFind<RegexGroups.Tuple8> capture8() {
        return new MultipleFind.Default<>(extract(io.gatling.core.check.regex.GroupExtractor.groupExtractor8()), type, RegexGroups.Tuple8::fromScala);
      }
    }
  }

  final class Regex extends CaptureGroupCheckBuilder.Default<io.gatling.core.check.regex.RegexCheckType, String> {

    public Regex(io.gatling.core.check.CheckBuilder.MultipleFind<io.gatling.core.check.regex.RegexCheckType, String, String> wrapped) {
      super(wrapped, CoreCheckType.Regex);
    }

    @Override
    protected <X> io.gatling.core.check.CheckBuilder.MultipleFind<RegexCheckType, String, X> extract(GroupExtractor<X> groupExtractor) {
      io.gatling.core.check.regex.RegexCheckBuilder<String> actual = (io.gatling.core.check.regex.RegexCheckBuilder<String>) wrapped;
      return new io.gatling.core.check.regex.RegexCheckBuilder<>(actual.pattern(), actual.patterns(), groupExtractor);
    }
  }

  interface JsonOfTypeFind extends Find<String> {
    Find<Boolean> ofBoolean();
    Find<Integer> ofInt();
    Find<Long> ofLong();
    Find<Double> ofDouble();
    Find<List<Object>> ofList();
    Find<Map<String, Object>> ofMap();
    Find<Object> ofObject();

    abstract class Default<T> extends Find.Default<T, JsonNode, String, String> implements JsonOfTypeFind {
      public Default(io.gatling.core.check.CheckBuilder.Find<T, JsonNode, String> wrapped, CheckType type) {
        super(wrapped, type, Function.identity());
      }

      protected abstract <X> io.gatling.core.check.CheckBuilder.Find<T, JsonNode, X> ofType(io.gatling.core.check.jsonpath.JsonFilter<X> filter);

      @Override
      public Find<Boolean> ofBoolean() {
        return new Find.Default<>(ofType(io.gatling.core.check.jsonpath.JsonFilter.jBooleanJsonFilter()), type, Boolean.class::cast);
      }

      @Override
      public Find<Integer> ofInt() {
        return new Find.Default<>(ofType(io.gatling.core.check.jsonpath.JsonFilter.jIntegerJsonFilter()), type, Integer.class::cast);
      }

      @Override
      public Find<Long> ofLong() {
        return new Find.Default<>(ofType(io.gatling.core.check.jsonpath.JsonFilter.jLongJsonFilter()), type, Long.class::cast);
      }

      @Override
      public Find<Double> ofDouble() {
        return new Find.Default<>(ofType(io.gatling.core.check.jsonpath.JsonFilter.jDoubleJsonFilter()), type, Double.class::cast);
      }

      @Override
      public Find<List<Object>> ofList() {
        return new Find.Default<>(ofType(io.gatling.core.check.jsonpath.JsonFilter.jListJsonFilter()), type, Function.identity());
      }

      @Override
      public Find<Map<String, Object>> ofMap() {
        return new Find.Default<>(ofType(io.gatling.core.check.jsonpath.JsonFilter.jMapJsonFilter()), type, Function.identity());
      }

      @Override
      public Find<Object> ofObject() {
        return new Find.Default<>(ofType(io.gatling.core.check.jsonpath.JsonFilter.jObjectJsonFilter()), type, Function.identity());
      }
    }
  }

  interface JsonOfTypeMultipleFind extends MultipleFind<String> {
    MultipleFind<Boolean> ofBoolean();
    MultipleFind<Integer> ofInt();
    MultipleFind<Long> ofLong();
    MultipleFind<Double> ofDouble();
    MultipleFind<List<Object>> ofList();
    MultipleFind<Map<String, Object>> ofMap();
    MultipleFind<Object> ofObject();

    abstract class Default<T> extends MultipleFind.Default<T, JsonNode, String, String> implements JsonOfTypeMultipleFind {
      public Default(io.gatling.core.check.CheckBuilder.MultipleFind<T, JsonNode, String> wrapped, CheckType type) {
        super(wrapped, type, Function.identity());
      }

      protected abstract <X> io.gatling.core.check.CheckBuilder.MultipleFind<T, JsonNode, X> ofType(io.gatling.core.check.jsonpath.JsonFilter<X> filter);

      @Override
      public MultipleFind<Boolean> ofBoolean() {
        return new MultipleFind.Default<>(ofType(io.gatling.core.check.jsonpath.JsonFilter.jBooleanJsonFilter()), type, Boolean.class::cast);
      }

      @Override
      public MultipleFind<Integer> ofInt() {
        return new MultipleFind.Default<>(ofType(io.gatling.core.check.jsonpath.JsonFilter.jIntegerJsonFilter()), type, Integer.class::cast);
      }

      @Override
      public MultipleFind<Long> ofLong() {
        return new MultipleFind.Default<>(ofType(io.gatling.core.check.jsonpath.JsonFilter.jLongJsonFilter()), type, Long.class::cast);
      }

      @Override
      public MultipleFind<Double> ofDouble() {
        return new MultipleFind.Default<>(ofType(io.gatling.core.check.jsonpath.JsonFilter.jDoubleJsonFilter()), type, Double.class::cast);
      }

      @Override
      public MultipleFind<List<Object>> ofList() {
        return new MultipleFind.Default<>(ofType(io.gatling.core.check.jsonpath.JsonFilter.jListJsonFilter()), type, Function.identity());
      }

      @Override
      public MultipleFind<Map<String, Object>> ofMap() {
        return new MultipleFind.Default<>(ofType(io.gatling.core.check.jsonpath.JsonFilter.jMapJsonFilter()), type, Function.identity());
      }

      @Override
      public MultipleFind<Object> ofObject() {
        return new MultipleFind.Default<>(ofType(io.gatling.core.check.jsonpath.JsonFilter.jObjectJsonFilter()), type, Function.identity());
      }
    }
  }

  final class JmesPath extends JsonOfTypeFind.Default<io.gatling.core.check.jmespath.JmesPathCheckType> {

    public JmesPath(io.gatling.core.check.CheckBuilder.Find<io.gatling.core.check.jmespath.JmesPathCheckType, JsonNode, String> wrapped) {
      super(wrapped, CoreCheckType.JmesPath);
    }

    @Override
    protected <X> io.gatling.core.check.CheckBuilder.Find<JmesPathCheckType, JsonNode, X> ofType(JsonFilter<X> filter) {
      io.gatling.core.check.jmespath.JmesPathCheckBuilder<String> actual = (io.gatling.core.check.jmespath.JmesPathCheckBuilder<String>) wrapped;
      return new io.gatling.core.check.jmespath.JmesPathCheckBuilder<>(actual.path(), actual.jmesPaths(), filter);
    }
  }

  final class JsonpJmesPath extends JsonOfTypeFind.Default<io.gatling.core.check.jmespath.JsonpJmesPathCheckType> {

    public JsonpJmesPath(io.gatling.core.check.CheckBuilder.Find<io.gatling.core.check.jmespath.JsonpJmesPathCheckType, JsonNode, String> wrapped) {
      super(wrapped, CoreCheckType.JsonpJmesPath);
    }

    @Override
    protected <X> io.gatling.core.check.CheckBuilder.Find<JsonpJmesPathCheckType, JsonNode, X> ofType(JsonFilter<X> filter) {
      io.gatling.core.check.jmespath.JsonpJmesPathCheckBuilder<String> actual = (io.gatling.core.check.jmespath.JsonpJmesPathCheckBuilder<String>) wrapped;
      return new io.gatling.core.check.jmespath.JsonpJmesPathCheckBuilder<>(actual.path(), actual.jmesPaths(), filter);
    }
  }

  final class JsonPath extends JsonOfTypeMultipleFind.Default<io.gatling.core.check.jsonpath.JsonPathCheckType> {

    public JsonPath(io.gatling.core.check.CheckBuilder.MultipleFind<io.gatling.core.check.jsonpath.JsonPathCheckType, JsonNode, String> wrapped) {
      super(wrapped, CoreCheckType.JsonPath);
    }

    @Override
    protected <X> io.gatling.core.check.CheckBuilder.MultipleFind<JsonPathCheckType, JsonNode, X> ofType(JsonFilter<X> filter) {
      io.gatling.core.check.jsonpath.JsonPathCheckBuilder<String> actual = (io.gatling.core.check.jsonpath.JsonPathCheckBuilder<String>) wrapped;
      return new io.gatling.core.check.jsonpath.JsonPathCheckBuilder<>(actual.path(), actual.jsonPaths(), filter);
    }
  }

  final class JsonpJsonPath extends JsonOfTypeMultipleFind.Default<io.gatling.core.check.jsonpath.JsonpJsonPathCheckType> {

    public JsonpJsonPath(io.gatling.core.check.CheckBuilder.MultipleFind<io.gatling.core.check.jsonpath.JsonpJsonPathCheckType, JsonNode, String> wrapped) {
      super(wrapped, CoreCheckType.JsonpJsonPath);
    }

    @Override
    protected <X> io.gatling.core.check.CheckBuilder.MultipleFind<JsonpJsonPathCheckType, JsonNode, X> ofType(JsonFilter<X> filter) {
      io.gatling.core.check.jsonpath.JsonpJsonPathCheckBuilder<String> actual = (io.gatling.core.check.jsonpath.JsonpJsonPathCheckBuilder<String>) wrapped;
      return new io.gatling.core.check.jsonpath.JsonpJsonPathCheckBuilder<>(actual.path(), actual.jsonPaths(), filter);
    }
  }
}
