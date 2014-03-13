##################
Passing Parameters
##################

One might want to pass parameters from the command line to the Simulation, for example the number of users, the duration of the ramp...

This can be done very easily with additional JAVA_OPTIONS in the launch script:

``JAVA_OPTS="-Dusers=500 -Dramp=3600"``

::

	val nbUsers = Integer.getInteger("users", 1)
	val myRamp  = java.lang.Long.getLong("ramp", 0L)
	setUp(scn.inject(ramp(nbUsers users) over (ramp seconds))

Of course, passing a String is just as easy as:

``JAVA_OPTS="-Dfoo=bar"``

::

	val foo = System.getProperty("foo")
