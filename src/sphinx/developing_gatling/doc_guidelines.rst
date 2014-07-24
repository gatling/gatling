.. _doc-guidelines:

########################
Documentation Guidelines
########################

Gatling's documentation is built using `Sphinx <http://sphinx-doc.org/>`__, which relies on the `reStructuredText markup language <http://docutils.sourceforge.net/rst.html>`__.

reStructuredText
================

Sphinx documentation provides the `reStructuredText Primer <http://sphinx-doc.org/rest.html>`__, a nice introduction to reST syntax and Sphinx *directives*.

Sections
--------

As reStructuredText allows any scheme for section headings (as long as it's consistent), the Gatling documentation use the following convention:

* ``#`` (over and under) for module headings
* ``=`` for sections
* ``-`` for subsections
* ``^`` for subsubsections
* ``~`` for subsubsubsections

Building the Documentation
==========================

You'll need to install Sphinx and some additional Python packages first.

Installing Sphinx
-----------------

Linux
^^^^^

* Install Python with your distribution's package manager
* If ``pip`` wasn't installed along Python, please `install it <http://pip.readthedocs.org/en/latest/installing.html>`__
* Install both Sphinx and PIL (`Python Imaging Library <http://www.pythonware.com/products/pil/>`__) using `pip`: ``pip install sphinx PIL``


Mac OS X
^^^^^^^^

* Install Python (`pip` is included) using `Homebrew <http://brew.sh/>`__ : ``brew install python``
* Install both Sphinx and PIL (`Python Imaging Library <http://www.pythonware.com/products/pil/>`__) using `pip`: ``pip install sphinx PIL``

For other plateforms, please refer to `Sphinx installation guide <http://sphinx-doc.org/install.html>`__.

Running Sphinx
--------------

Gatling relies on the `sbt-site plugin <https://github.com/sbt/sbt-site>`__ to manage the Sphinx documentation build.

* ``sbt makeSite`` generates the Sphinx documentation in ``<project-dir>/target/sphinx/html/index.html``.
* ``sbt previewSite`` start a web server at ``localhost:4000`` which points to the documentation's index.
