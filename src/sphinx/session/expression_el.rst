#################
Expression and EL
#################

.. _gatling_el:

Expression Language
===================

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

.. _expression:

Expression
==========

Most Gatling DSL methods actually takes ``Expression[T]`` parameters, which is a type alias for ``Session => Validation[T]``.

How is it that one can also pass Strings then?

The reason is that there is an implicit conversion that automagically parses those Strings when the Simulation is instanciated and turn them into Expressions.

.. warning::
    This implicit conversion is only triggered when trying to pass a String to a method that expects an Expression instead.

.. note::
    For more information about ``Validation``, please check out :ref:`reference page <validation>`.
