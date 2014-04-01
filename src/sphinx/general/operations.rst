.. _operations:

##########
Operations
##########

Java version
============

Gatling is mostly tested against JDK7 packages provided by Oracle. Gatling 2 is compiled with JDK7, yet into JDK6 bytecode.

The launch scripts set up JVM options that are only available since Java 6u21, 64 bits version.
If you use an older version of Java, please consider upgrading, or edit the scripts and remove the unsupported options.
Versions of Java older than Java 6 are not supported.

Yet, we recommend that you use the latest JDK:

  * NIO is based on native code, so it depends on JVM implementation and bugs are frequently fixed.
    For example, NIO was broken on Oracle JDK7 until 7u10.
  * Gatling is tested against modern JDKs
  * Gatling has several optimizations that targets JDK7, e.g. based on new ``String`` implementation introduced in 7u6.

OS tuning
=========

Gatling can consume a very large number of open file handles during normal operation.
Typically, OSes limit this, so you will have to tweak a few options so you can massively open new sockets and reach heavy load.

Changing the limit
------------------

Most operating systems can change the open-files limit using the ``ulimit -n`` command. Example:

::

	$ ulimit -n 65536

However, this only changes the limit for the current shell session. Changing the limit on a system-wide, permanent basis varies more between systems.

Linux
^^^^^

Edit ``/etc/security/limits.conf`` and append the following two lines:

::

	*       soft    nofile  65535
	*       hard    nofile  65535

Save the file. Restart so that the limits take effect. You can now verify with ``ulimit -a`` that the limits are correctly set.

On Debian, if the above limits are ignored, you may want to add ``session required pam_limits.so`` in ``/etc/pam.d/common-session``. 
For remote users, you may want to add the same line in ``/etc/pam.d/sshd``.

Mac OS/X
^^^^^^^^

On Mac you need to run the following commands in order to *unbuckle the belts*:

::

	$ sudo sysctl -w kern.maxfilesperproc=200000
	$ sudo sysctl -w kern.maxfiles=200000
	$ sudo sysctl -w net.inet.ip.portrange.first=1024

You could also have to increase your ephemeral port range or tune your TCP time out so that they expire faster.