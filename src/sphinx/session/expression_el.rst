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

  "${foo.size()}"             // expects `foo` to point to a Scala or Java collection attribute and returns its size
  "${foo.random()}"           // expects `foo` to point to an indexed collection and returns a random element
  "${foo.exists()}"           // returns true if the session contains a `foo` attribute, false otherwise
  "${foo.isUndefined()}"      // returns true if the session doesn't contains a `foo` attribute, false otherwise
  "${foo(5)}"                 // expects `foo` to point to an indexed collection or a Tuple and returns the 5-th element
  "${foo(n)}"                 // expects `foo` to point to an indexed collection or a Tuple and `n` to point to an Int and returns the n-th element
  "${foo.bar}"                // expects foo to point to a Map and returns the value associated with key `bar`
  "${foo._2}"                 // expects `foo` points to a Tuple and returns the second element (identical to idiomatic Scala Tuple syntax, 1 based index)
  "${foo.jsonStringify()}"    // properly formats into a JSON value (wrap Strings with double quotes, deal with null)
  "${currentTimeMillis()}"    // System.currentTimeMillis
  "${currentDate(<pattern>)}" // new Date() formatted with a java.text.SimpleDateFormat pattern

You can also combine different Gatling EL builtin functions. For example if ``foo`` is a List of Lists ``${foo(0)(0)}`` will return first element of the first list in ``foo``. ``${foo.list.random()}`` will return random element from an indexed collection associated with key ``list`` in a map ``foo``.
 
Gatling EL supports the following indexed collections: java.util.List, Seq and Array. It also supports both Scala and Java maps. Function ``.size`` supports any Scala or Java collection.

.. warning::
  This Expression Language only works on String values being passed to Gatling DSL methods.
  Such Strings are parsed only once, when the Gatling simulation is being instantiated.

  For example ``queryParam("latitude", session => "${latitude}")`` wouldn't work because the parameter is not a String, but a function that returns a String.

  Also, ``queryParam("latitude", "${latitude}".toInt)`` wouldn't because the ``toInt`` would happen before passing the parameter to the ``queryParam`` method.

  The solution here would be to pass a function:

  ``session => session("latitude").validate[Int]``.

.. warning::
  By default, IntelliJ will automatically prepend your String with an ``s`` as soon as you start typing ``${``
  because it thinks you want to use `Scala's String interpolation <https://docs.scala-lang.org/overviews/core/string-interpolation.html>`_.
  You need to remove this ``s`` to use Gatling EL.

Escaping ``${``
---------------

To prevent ``"${"`` from being interpreted by the EL compiler, add a ``$`` before it. ``"$${foo}"`` will be turned into ``"${foo}"``.

If you want a ``$`` before the placeholder, add another ``$``.
Assuming the session attribute ``foo`` holds ``"FOO"``, ``"$$${foo}"`` will be turned into ``"$FOO"``.

This can go on and on. In general, if there are 2n-1 ``$`` characters before ``${`` -- an even number of ``$`` characters totally --
there will be n ``$`` before ``{`` in the final string;
if there are 2n ``$`` before ``${`` -- an odd number totally -- there will be n ``$`` before the placeholder.

.. _expression:

Expression
==========

Most Gatling DSL methods actually take ``Expression[T]`` parameters, which is a type alias for ``Session => Validation[T]``.
This way, one can pass functions to generate parameters, possibly based on the Session's content.

.. includecode:: code/ExpressionSample.scala#inline-expression

How is it that one can also pass Strings and other values then?

The reason is that there are implicit conversions:

* when passing a String, it gets automagically parsed turn them into Expressions thanks to Gatling EL compiler.
* when passing a value of another type, it gets automagically wrapped into an Expression that will always return this static value.

.. warning::
  Implicit conversions are only triggered when expected type and passed parameter type don't match, for example trying to pass a String to a method that expects an Expression instead.
  Those implicit conversions are triggered at compile time.

.. note::
  For more information about ``Validation``, please check out the :ref:`Validation reference <validation>`.
