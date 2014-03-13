##########
Operations
##########

Java version
############

Gatling has only been tested against Java packages provided by Oracle. Gatling 1 is compiled with JDK6, while Gatling 2 is compiled with JDK7, yet into JDK6 bytecode.

The launch scripts set up JVM options that are only available since Java 6u21. If you use an older version of Java, please consider upgrading, or edit the scripts for removing the unsupported options. Versions of Java older than Java 6 are not supported, please upgrade.

Yet, we recommend that you use the latest version of Java. NIO is based on native code, so it depends on JVM implementation and bugs are frequently fixed. For example, NIO have been broken on Oracle JDK7 until 1.7.0_10. Gatling is mostly tested on Oracle JDK7, OS X and Linux.

OS tuning
#########

Gatling can consume a very large number of open file handles during normal operation. Typically, OSes are limiting this, so you will have to tweak a few options so that you can massively open new sockets and reach heavy load.

Changing the limit
==================

Most operating systems can change the open-files limit using the ``ulimit -n`` command. Example:

::

	$ ulimit -n 65536

However, this only changes the limit for the current shell session. Changing the limit on a system-wide, permanent basis varies more between systems.

Linux
=====

Edit ``/etc/security/limits.conf`` and append the following two lines:

::

	*       soft    nofile  65535
	*       hard    nofile  65535

Save the file. Restart so that the limits take effect. You can now verify with ``ulimit -a`` that the limits are correctly set.


Mac OS/X
========

On Mac you need to run the following commands in order to "unbuckle the belts":

::

	$ sudo sysctl -w kern.maxfilesperproc=200000
	$ sudo sysctl -w kern.maxfiles=200000
	$ sudo sysctl -w net.inet.ip.portrange.first=1024

You could also have to increase your ephemeral port range or tune your TCP time out so that they expire faster. Here's also a good entry point: https://github.com/0xdata/h2o/wiki/EC2-hosts-configuration-and-tuning
