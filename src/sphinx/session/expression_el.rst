#################
Expression and EL
#################

For the Impatient
=================

Most Gatling DSL methods can be passed *Expression Language* Strings.

This is a very convenient feature to write dynamic values.

Gatling automagically parses those Strings and turn them into functions that will compute a result based on the data stored into the Session.

Yet it's very limited, don't expect a full blown dynamic language!

Gatling EL uses a ``${attributeName}`` syntax, where *attributeName* is the name of an attribute in the Session.

For example::

    request("page").get("/foo?${bar}")

Moreover, Gatling EL provide the builtin functions::

	"${foo.size}"   // returns the size of foo if foo is a Seq
	"${foo.random}" // returns a random element of foo if foo is a Seq
	"${foo(5)}"     // returns the 5th element of foo if foo is a Seq
	"${foo(bar)}"   // returns the barth element of foo if bar is an Int and foo is a Seq

.. warning::
    This Expression Language only works on the final value that is passed to the DSL method when the Simulation is instanciated.

    For example, ``queryParam("latitude", "${latitude}".toInt + 24)`` won't work,
    the program will blow on ``"${latitude}".toInt`` as this String can't be parsed into an Int.

    The solution here would be to pass a function:

    ``session => session("latitude").validate[Int].map(i => i + 24)``.

In Depth
========

Expression
----------

Most Gatling DSL methods actually takes ``Expression[T]`` parameters, which is a type alias for ``Session => Validation[T]``.

How is it that one can also pass Strings then?

The reason is that there is an implicit conversion that automagically parses those Strings when the Simulation is instanciated and turn them into Expressions.

.. warning::
This implicit conversion is only triggered when trying to pass a String to a method that expects an Expression instead.

Validation
----------

``Validation`` is an abstraction for describing something that can either be a valid result, or an error message.
Scalaz has a great implementation, but Gatling has its own, both less powerful yet much more simple.

The benefit of using this abstraction is that it's composable, so one can chain operations that consume and produce validations without having to determine on every operation if it's actually dealing with a succeeding operation or not.

``Validation[T]`` has a type parameter ``T`` that is the type of the value in case of a success.

It has 2 implementations:

* ``Success[T](value: T)`` that wraps a value in case of a success
* ``Failure(message: String)`` that wraps a String error message

The goal of such an abstraction is to deal with "unexpected results" in a composable and cheap way instead of using Exceptions.

``Validation`` has the standard Scala "monadic" methods such as ``map`` and ``flatMap`` so that you can compose and use Scala *"for comprehension"* syntactic sugar.

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


You can also use Scala *"for comprehension"*.

For the impatient, just consider it's like a super loop that can iterate other multiple objects of the same kind (like embedded loops) and can iterate over other things that collections, such as ``Validation``\ s or ``Option``\ s.

Here's what the last example would look like with *"for comprehension"*::

    val baz: Validation[String] = for {
      fooValue <- foo
      barValue <- bar
    } yield fooValue + barValue

For more information, check the Scaladoc.
