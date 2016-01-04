/**
 * Copyright 2011-2016 GatlingCorp (http://gatling.io)
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
//#import

import io.gatling.commons.validation._
//#import

class ValidationSpec {

  object Classes {
    //#with-classes
    val foo: Validation[String] = Success("foo")
    val bar: Validation[String] = Failure("errorMessage")
    //#with-classes
  }

  object Helpers {
    //#with-helpers
    val foo: Validation[String] = "foo".success
    val bar: Validation[String] = "errorMessage".failure
    //#with-helpers
  }

  object PatternMatching {
    //#pattern-matching
    def display(v: Validation[String]) = v match {
      case Success(string) => println("success: " + string)
      case Failure(error)  => println("failure: " + error)
    }

    val foo = Success("foo")
    display(foo) // will print success: foo

    val bar = Failure("myErrorMessage")
    display(bar) // will print failure: myErrorMessage
    //#pattern-matching
  }

  object Map {
    //#map
    val foo = Success(1)
    val bar = foo.map(value => value + 2)
    println(bar) // will print Success(3)
    //#map
  }

  object FlatMap {
    //#flatMap
    val foo = Success("foo")
    val bar = foo.flatMap(value => Success("bar"))
    println(bar) // will print Success("bar")

    val baz = foo.flatMap(value => Failure("error"))
    println(baz) // will print Failure("error")
    //#flatMap
  }

  object MapFailure {
    //#map-failure
    val foo: Validation[Int] = Failure("error")
    val bar = foo.map(value => value + 2)
    println(bar) // will print Failure("error")
    //#map-failure
  }

  object ForComp {
    //#for-comp
    val foo: Validation[Int] = ???
    val bar: Validation[Int] = ???

    val baz: Validation[Int] = for {
      fooValue <- foo
      barValue <- bar
    } yield fooValue + barValue
    //#for-comp
  }
}
