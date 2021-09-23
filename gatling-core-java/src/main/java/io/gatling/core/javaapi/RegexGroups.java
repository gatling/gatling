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

import java.util.Objects;

public final class RegexGroups {
  private RegexGroups() {
  }

  public static final class Tuple2 {
    private final String v1;
    private final String v2;

    public static Tuple2 fromScala(scala.Tuple2<String, String> t) {
      return new Tuple2(t._1(), t._2());
    }

    public Tuple2(String v1, String v2) {
      this.v1 = v1;
      this.v2 = v2;
    }

    public String getV1() {
      return v1;
    }

    public String getV2() {
      return v2;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      Tuple2 tuple2 = (Tuple2) o;
      return v1.equals(tuple2.v1) && v2.equals(tuple2.v2);
    }

    @Override
    public int hashCode() {
      return Objects.hash(v1, v2);
    }
  }

  public static final class Tuple3 {
    private final String v1;
    private final String v2;
    private final String v3;

    public static Tuple3 fromScala(scala.Tuple3<String, String, String> t) {
      return new Tuple3(t._1(), t._2(), t._3());
    }

    public Tuple3(String v1, String v2, String v3) {
      this.v1 = v1;
      this.v2 = v2;
      this.v3 = v3;
    }

    public String getV1() {
      return v1;
    }

    public String getV2() {
      return v2;
    }

    public String getV3() {
      return v3;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      Tuple3 tuple3 = (Tuple3) o;
      return v1.equals(tuple3.v1) && v2.equals(tuple3.v2) && v3.equals(tuple3.v3);
    }

    @Override
    public int hashCode() {
      return Objects.hash(v1, v2, v3);
    }
  }

  public static final class Tuple4 {
    private final String v1;
    private final String v2;
    private final String v3;
    private final String v4;

    public static Tuple4 fromScala(scala.Tuple4<String, String, String, String> t) {
      return new Tuple4(t._1(), t._2(), t._3(), t._4());
    }

    public Tuple4(String v1, String v2, String v3, String v4) {
      this.v1 = v1;
      this.v2 = v2;
      this.v3 = v3;
      this.v4 = v4;
    }

    public String getV1() {
      return v1;
    }

    public String getV2() {
      return v2;
    }

    public String getV3() {
      return v3;
    }

    public String getV4() {
      return v4;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      Tuple4 tuple4 = (Tuple4) o;
      return v1.equals(tuple4.v1) && v2.equals(tuple4.v2) && v3.equals(tuple4.v3) && v4.equals(tuple4.v4);
    }

    @Override
    public int hashCode() {
      return Objects.hash(v1, v2, v3, v4);
    }
  }

  public static final class Tuple5 {
    private final String v1;
    private final String v2;
    private final String v3;
    private final String v4;
    private final String v5;

    public static Tuple5 fromScala(scala.Tuple5<String, String, String, String, String> t) {
      return new Tuple5(t._1(), t._2(), t._3(), t._4(), t._5());
    }

    public Tuple5(String v1, String v2, String v3, String v4, String v5) {
      this.v1 = v1;
      this.v2 = v2;
      this.v3 = v3;
      this.v4 = v4;
      this.v5 = v5;
    }

    public String getV1() {
      return v1;
    }

    public String getV2() {
      return v2;
    }

    public String getV3() {
      return v3;
    }

    public String getV4() {
      return v4;
    }

    public String getV5() {
      return v5;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      Tuple5 tuple5 = (Tuple5) o;
      return v1.equals(tuple5.v1) && v2.equals(tuple5.v2) && v3.equals(tuple5.v3) && v4.equals(tuple5.v4) && v5.equals(tuple5.v5);
    }

    @Override
    public int hashCode() {
      return Objects.hash(v1, v2, v3, v4, v5);
    }
  }

  public static final class Tuple6 {
    private final String v1;
    private final String v2;
    private final String v3;
    private final String v4;
    private final String v5;
    private final String v6;

    public static Tuple6 fromScala(scala.Tuple6<String, String, String, String, String, String> t) {
      return new Tuple6(t._1(), t._2(), t._3(), t._4(), t._5(), t._6());
    }

    public Tuple6(String v1, String v2, String v3, String v4, String v5, String v6) {
      this.v1 = v1;
      this.v2 = v2;
      this.v3 = v3;
      this.v4 = v4;
      this.v5 = v5;
      this.v6 = v6;
    }

    public String getV1() {
      return v1;
    }

    public String getV2() {
      return v2;
    }

    public String getV3() {
      return v3;
    }

    public String getV4() {
      return v4;
    }

    public String getV5() {
      return v5;
    }

    public String getV6() {
      return v6;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      Tuple6 tuple6 = (Tuple6) o;
      return v1.equals(tuple6.v1) && v2.equals(tuple6.v2) && v3.equals(tuple6.v3) && v4.equals(tuple6.v4) && v5.equals(tuple6.v5) && v6.equals(tuple6.v6);
    }

    @Override
    public int hashCode() {
      return Objects.hash(v1, v2, v3, v4, v5, v6);
    }
  }

  public static final class Tuple7 {
    private final String v1;
    private final String v2;
    private final String v3;
    private final String v4;
    private final String v5;
    private final String v6;
    private final String v7;

    public static Tuple7 fromScala(scala.Tuple7<String, String, String, String, String, String, String> t) {
      return new Tuple7(t._1(), t._2(), t._3(), t._4(), t._5(), t._6(), t._7());
    }

    public Tuple7(String v1, String v2, String v3, String v4, String v5, String v6, String v7) {
      this.v1 = v1;
      this.v2 = v2;
      this.v3 = v3;
      this.v4 = v4;
      this.v5 = v5;
      this.v6 = v6;
      this.v7 = v7;
    }

    public String getV1() {
      return v1;
    }

    public String getV2() {
      return v2;
    }

    public String getV3() {
      return v3;
    }

    public String getV4() {
      return v4;
    }

    public String getV5() {
      return v5;
    }

    public String getV6() {
      return v6;
    }

    public String getV7() {
      return v7;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      Tuple7 tuple7 = (Tuple7) o;
      return v1.equals(tuple7.v1) && v2.equals(tuple7.v2) && v3.equals(tuple7.v3) && v4.equals(tuple7.v4) && v5.equals(tuple7.v5) && v6.equals(tuple7.v6) && v7.equals(tuple7.v7);
    }

    @Override
    public int hashCode() {
      return Objects.hash(v1, v2, v3, v4, v5, v6, v7);
    }
  }

  public static final class Tuple8 {
    private final String v1;
    private final String v2;
    private final String v3;
    private final String v4;
    private final String v5;
    private final String v6;
    private final String v7;
    private final String v8;

    public static Tuple8 fromScala(scala.Tuple8<String, String, String, String, String, String, String, String> t) {
      return new Tuple8(t._1(), t._2(), t._3(), t._4(), t._5(), t._6(), t._7(), t._8());
    }

    public Tuple8(String v1, String v2, String v3, String v4, String v5, String v6, String v7, String v8) {
      this.v1 = v1;
      this.v2 = v2;
      this.v3 = v3;
      this.v4 = v4;
      this.v5 = v5;
      this.v6 = v6;
      this.v7 = v7;
      this.v8 = v8;
    }

    public String getV1() {
      return v1;
    }

    public String getV2() {
      return v2;
    }

    public String getV3() {
      return v3;
    }

    public String getV4() {
      return v4;
    }

    public String getV5() {
      return v5;
    }

    public String getV6() {
      return v6;
    }

    public String getV7() {
      return v7;
    }

    public String getV8() {
      return v8;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      Tuple8 tuple8 = (Tuple8) o;
      return v1.equals(tuple8.v1) && v2.equals(tuple8.v2) && v3.equals(tuple8.v3) && v4.equals(tuple8.v4) && v5.equals(tuple8.v5) && v6.equals(tuple8.v6) && v7.equals(tuple8.v7) && v8.equals(tuple8.v8);
    }

    @Override
    public int hashCode() {
      return Objects.hash(v1, v2, v3, v4, v5, v6, v7, v8);
    }
  }
}
