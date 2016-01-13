.. _operations:

##########
Operations
##########

Java version
============

Gatling is mostly tested against JDK8 packages provided by Oracle.
Gatling requires **JDK8**. We recommend that you use an up-to-date JDK.

If some launch scripts options are not available on your JVM, e.g. because you run a 32 bit version, please edit the scripts and remove the unsupported options.

IPv4 vs IPv6
============

IPv6 (enabled by default on Java) was found to sometimes cause some performance issues, so the launch scripts disable it with the following options::

  -Djava.net.preferIPv4Stack=true
  -Djava.net.preferIPv6Addresses=false

If you really need to prefer IPv6, please edit the launch scripts.

OS tuning
=========

Gatling can consume a very large number of open file handles during normal operation.
Typically, operating systems limit this number, so you may have to tweak a few options in your chosen OS so that you can open *many* new sockets and achieve heavy load.

Changing the limit
------------------

Most operating systems can change the open-files limit using the ``ulimit -n`` command. Example:

::

  $ ulimit -n 65536

However, this only changes the limit for the current shell session. Changing the limit on a system-wide, permanent basis varies more between systems.

Linux
^^^^^

To permanently set the soft and hard values *for all users of the system* to allow for up to 65536 open files ; edit ``/etc/security/limits.conf`` and append the following two lines:

::

  *       soft    nofile  65535
  *       hard    nofile  65535

Save the file. Start a new session so that the limits take effect. You can now verify with ``ulimit -a`` that the limits are correctly set.

For Debian and Ubuntu, you should enable PAM user limits. To do so, add ``session required pam_limits.so`` in:

* ``/etc/pam.d/common-session``
* ``/etc/pam.d/common-session-noninteractive`` if the file exists
* ``/etc/pam.d/sshd`` if you access the machine via SSH

Also, if accessing the machine via SSH, be sure to have ``UseLogin yes`` in ``/etc/ssh/sshd_config``

For more tuning, you may want to do the following:

::

  # more ports for testing
  sudo sysctl -w net.ipv4.ip_local_port_range="1025 65535"

  # increase the maximum number of possible open file descriptors:
  echo 300000 | sudo tee /proc/sys/fs/nr_open
  echo 300000 | sudo tee /proc/sys/fs/file-max

Mac OS/X
^^^^^^^^

On Mac you need to run the following commands in order to *unbuckle the belts*:

::

  $ sudo sysctl -w kern.maxfilesperproc=300000
  $ sudo sysctl -w kern.maxfiles=300000
  $ sudo sysctl -w net.inet.ip.portrange.first=1024

You may also increase your ephemeral port range or tune your TCP timeout so that they expire faster.
