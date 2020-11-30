############
Handling JSF
############

Basic JSF
=========

JSF requires a parameter named ``javax.faces.ViewState`` to be captured on every page and be passed in every POST request.

Adding a check for capturing the value and a param on very request would be very cumbersome.
Hopefully, we can factor out these operations.

Define factory methods for building JSF requests that would automatically perform those operations:

.. includecode:: code/HandlingJsfSample.scala#factory-methods

You can then build your requests just like you're used to:

.. includecode:: code/HandlingJsfSample.scala#example-scenario

.. note:: The sample above is taken from the `Primefaces demo <http://www.primefaces.org/showcase-labs>`_.

See Rafael Pestano's `demo project <https://github.com/rmpestano/gatling-jsf-demo>`_ for a complete sample.

Trinidad
========

Trinidad's ``_afPfm`` query parameter can be handled in a similar fashion:

.. includecode:: code/HandlingJsfSample.scala#trinidad
