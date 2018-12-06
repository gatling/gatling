#################
Expression and EL
#################

.. _el:

Expression Language
===================

Most Gatling DSL methods can be passed *Expression Language* Strings.

This is a very convenient feature to write dynamic values.

Gatling parses Strings parameter values and turn them into functions that will compute a result based on the data stored into the Session when they will be evaluated.

The Gatling EL is not a dynamic language, but just placeholders with a few additional helpers.

If you need a full blown templating engine, you can use our Pebble support.

Gatling EL uses a ``${attributeName}`` syntax, where *attributeName* is the name of an attribute in the Session.

For example::

  request("page").get("/foo?${bar}")

Moreover, Gatling EL provide the following built-in functions::

  "${foo.size()}"          // returns the size of `foo` if `foo` points to a Scala or Java collection attribute
  "${foo.random()}"        // returns a random element of `foo` if `foo` points to an indexed collection
  "${foo.exists()}"        // returns true if the session contains a `foo` attribute, false otherwise
  "${foo.isUndefined()}"   // returns true if the session doesn't contains a `foo` attribute, false otherwise
  "${foo(5)}"              // returns the 5th element of `foo` if `foo` points to an indexed collection
  "${foo(n)}"              // returns the n-th element of `foo` if `n` points to an Int and `foo` to an indexed collection
  "${foo.bar}"             // returns the value associated with key `bar` if `foo` points to a map
  "${foo._2}"              // returns the second element if `foo` points to a Tuple object
  "${foo.jsonStringify()}" // properly formats into a JSON value (wrap Strings with double quotes, deal with null)

You can also combine different Gatling EL builtin functions. For example if ``foo`` is a List of Lists ``${foo(0)(0)}`` will return first element of the first list in ``foo``. ``${foo.list.random()}`` will return random element from an indexed collection associated with key ``list`` in a map ``foo``.
 
Gatling EL supports the following indexed collections: java.util.List, Seq and Array. It also supports both Scala and Java maps. Function ``.size`` supports any Scala or Java collection.

.. warning::
  This Expression Language only works on String values being passed to Gatling DSL methods.
  Such Strings are parsed only once, when the Gatling simulation is being instanciated.

  For example ``queryParam("latitude", session => "${latitude}")`` wouldn't work because the parameter is not a String, but a function that returns a String.

  Also, ``queryParam("latitude", "${latitude}".toInt)`` wouldn't because the ``toInt`` would happen before passing the parameter to the ``queryParam`` method.

  The solution here would be to pass a function:

  ``session => session("latitude").validate[Int]``.

.. warning::
  By default, IntelliJ will automatically prepend your String with an ``s`` as soon as you start typing ``${``
  because it thinks you want to use `Scala's String interpolation <https://docs.scala-lang.org/overviews/core/string-interpolation.html>`_.
  You need to remove this ``s`` to use Gatling EL.

.. _expression:

Expression
==========

Most Gatling DSL methods actually takes ``Expression[T]`` parameters, which is a type alias for ``Session => Validation[T]``.

How is it that one can also pass Strings and other values then?

The reason is that there are implicit conversions:

* when passing a String, it gets automagically parsed turn them into Expressions thanks to Gatling EL compiler.
* when passing a value of another type, it gets automagically wrapped into an Expression that will always return this static value.

.. warning::
  Implicit conversions are only triggered when expected type and passed parameter type don't match, for example trying to pass a String to a method that expects an Expression instead.
  Those implicit conversions are triggered at compile time.

.. note::
  For more information about ``Validation``, please check out the :ref:`Validation reference <validation>`.
