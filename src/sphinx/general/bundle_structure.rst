################
Bundle structure
################

The bundle structure is as following:

  * ``bin`` contains the launch scripts for Gatling and the Recorder.
  * ``conf`` contains the configuration files for Gatling, Akka and Logback.
  * ``lib`` contains the binaries used by Gatling
  * ``user-files``:

    * ``simulations`` contains your Simulations scala files. Please respect package folder hierarchy.
    * ``data`` contains feeder files.
    * ``request-bodies`` contains templates for request bodies.

  * ``results`` contains ``simulation.log`` and reports generated in a sub directory.