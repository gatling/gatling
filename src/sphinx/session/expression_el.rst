#################
Expression and EL
#################

Preleminary: Validation
=======================

Validation is an abstraction for describing something that can either be a valid result, or an error message. Scalaz has a great implementation, but Gatling has its own, both less powerful and much more simple.

The benefit of using this abstraction is that it's composable, so one can chain operations that consume and producude validations without having to handle on every operation if it's actually dealing with a succeeding operation or not.

Gatling APIs heavily use Validation.

``Validation[T]`` has a type parameter `T` that is the type of the value in case of a success.

It has 2 implementations:

* ``Success[T](value: T)`` that wraps a value in case of a success
* ``Failure(message: String)`` that wraps a String error message

The goal of such an abstraction is to deal with "unexpected results" in a composable and cheap way instead of using Exceptions.

Validation has the standard Scala "monadic" methods such as ``map`` and ``flatMap`` so that you can compose and use Scala "for comprehension" syntaxic sugar.

For example::

	val foo: Validation[String] = Success("foo")
	val bar: Validation[String] = Success("bar")
	val baz: Validation[String] = foo.flatMap(value => value + bar)
	println(baz) // will print "foobar"

::

	val foo: Validation[String] = Success("foo")
	val bar: Validation[String] = Failure("error")
	val baz: Validation[String] = foo.flatMap(value => bar)
	println(baz) // will print "error"

::

	val foo: Validation[String] = Failure("error")
	val bar: Validation[String] = Success("bar")
	val baz: Validation[String] = foo.flatMap(value => value + bar)
	println(baz) // will print "error"


You can also use Scala "for comprehension".

For the impatient, just consider it's like a super loop that can iterate other multiple objects of the same kind (like embedded loops) and can iterate over other things that collections, such as Validations or Options.

Here's what the last example would look like with "for comprehension"::

    val baz: Validation[String] = for {
      fooValue <- foo
      barValue <- bar
    } yield fooValue + barValue


For more information, check the Scaladoc.

.. _expression:

Expression
==========

Most Gatling DSL methods takes ``Expression[T]`` parameters, which is a type alias for ``Session => Validation[T]``.

.. _gatling_el:

Expression Language
===================

Most Gatling DSL methods actually take ``Expression[T]`` parameters, where Expression is a type alias for Session => Expression[T].

But one can also pass a String. What happens here is that there's an implicit conversion that compiles this String into an Expression.

Gatling EL use a ``${attributeName}`` syntax, very similar to the Java JSTL one, but much more limited. Don't expect a full blown dynamic language!

The Expression will return a Failure if:
* the type of the result doesn't match the expected one (of course, everything can be turned into a String)
* the Session doesn't contained an attribute named "attributeName"

Moreover, Gatling EL provide the builtin functions::

	"${foo.size}"   // returns the size of foo if foo is a Seq
	"${foo.random}" // returns a random element of foo if foo is a Seq
	"${foo(5)}"     // returns the 5th element of foo if foo is a Seq
	"${foo(bar)}"   // returns the barth element of foo if bar is an Int and foo is a Seq
