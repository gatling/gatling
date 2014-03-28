.. _validation:

##########
Validation
##########

.. _validation-concept:

Concept
=======

``Validation`` is an abstraction for describing something that can either be a valid result, or an error message.
Scalaz has a great implementation, but Gatling has its own, both less powerful yet much more simple.

The benefit of using this abstraction is that it's composable, so one can chain operations that consume and produce validations without having to determine on every operation if it's actually dealing with a succeeding operation or not.

``Validation[T]`` has a type parameter ``T`` that is the type of the value in case of a success.

It has 2 implementations:

* ``Success[T](value: T)`` that wraps a value in case of a success
* ``Failure(message: String)`` that wraps a String error message

The goal of such an abstraction is to deal with "unexpected results" in a composable and cheap way instead of using Exceptions.

.. _validation-usage:

Usage
=====

Creating instances
------------------

First, import the ``validation`` package::

  import io.gatling.core.validation._

Then, you can either directly create new instance of the case classes::

	val foo: Validation[String] = Success("foo")
	val bar: Validation[String] = Failure("errorMessage")

or use the helpers::

	val foo: Validation[String] = "foo".success
	val bar: Validation[String] = "errorMessage".failure

Manipulating
------------

``Validation`` can be used with pattern matching::

  def display(v: Validation[String] = v match {
      case Success(string) => println("success: " + string)
      case Failure(error)  => println("failure: " + error)
  }

  val foo = Success("foo")
  display(foo) // will print success: foo

  val bar = Failure("myErrorMessage")
  display(bar) // will print failure: myErrorMessage

``Validation`` has the standard Scala "monadic" methods such as:

  * ``map``:expects a function that takes the value if it's a success and return a value.
  * ``flatMap``: expects a function that takes the value if it's a success and return a new ``Validation``

Basically, ``map`` is used to **chain with an operation that can't fail**, hence return a raw value::

	val foo = Success(1)
	val bar = foo.map(value => value + 2)
	println(bar) // will print Success(3)

``flatMap`` is used to **chain with an operation that can fail**, hence return a ``Validation``::

	val foo = Success("foo")
	val bar = foo.flatMap(value => Success("bar"))
	println(bar)) // will print Success("bar")

	val baz = foo.flatMap(value => Failure("error")
	println(baz)) // will print Failure("error")

In both case, the chained function is not called if the original ``Validation`` was a ``Failure``::

	val foo: Validation[Int] = Failure("error")
  val bar = baz.map(value => value + 2)
	println(qix) // will print Failure("error")

You can also use Scala *"for comprehension"* syntactic sugar.

For the impatient, just consider it's like a super loop that can iterate other multiple objects of the same kind (like embedded loops) and can iterate over other things that collections, such as ``Validation``\ s or ``Option``\ s.

Here's what the above example would look like with *"for comprehension"*::

  val foo: Validation[Int] = ???
  val bar: Validation[Int] = ???

  val baz: Validation[String] = for {
    fooValue <- foo
    barValue <- bar
  } yield fooValue + barValue
