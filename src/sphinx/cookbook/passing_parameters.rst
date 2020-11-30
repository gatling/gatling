##################
Passing Parameters
##################

You might want to pass parameters from the command line to the Simulation, for example the number of users, the duration of the ramp...

This can be done very easily with additional ``JAVA_OPTS`` in the launch script:

``JAVA_OPTS="-Dusers=500 -Dramp=3600"``

.. includecode:: code/PassingParametersSample.scala#injection-from-props

Of course, passing a String is just as easy as:

``JAVA_OPTS="-Dfoo=bar"``

.. includecode:: code/PassingParametersSample.scala#string-property
