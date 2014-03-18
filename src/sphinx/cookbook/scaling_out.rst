.. _scaling-out:

###########
Scaling Out
###########

Sometimes, generating some very heavy load from a single machine might lead to saturating the OS or the network interface controller.

In this case, you might want to use several Gatling instances hosted on multiple machines.

Gatling doesn't have a cluster mode yet, but you can achieve similar results manually:

* deploy Gatling on several machines along with the Simulation classes and the associated resources (data, request-bodies, etc..)
* launch them remotely from a script, with the ``-nr`` (no reports) option
* retrieve all the simulation.log files
* rename them so they don't clash
* place them into a folder in the results folder of a Gatling instance
* generate the reports with Gatling with the ``-ro name-of-the-simulation-folder`` (reports only), Gatling will pick all the files that match ``.*\.log``

.. note::
    If you want to use assertions on the consolidated results, you have to deploy the simulation on the consoldating node and force it through the ``-s`` option
