.. _scaling-out:

###########
Scaling Out
###########

Sometimes, generating some very heavy load from a single machine might lead to saturating the OS or the network interface controller.

In this case, you might want to use several Gatling instances hosted on multiple machines.

Gatling doesn't have a cluster mode yet, but you can achieve similar results manually:

* deploy Gatling on several machines along with the Simulation classes and the associated resources (data, bodies, etc..)
* launch them remotely from a script, with the ``-nr`` (no reports) option
* retrieve all the simulation.log files
* rename them so they don't clash
* place them into a folder in the results folder of a Gatling instance
* generate the reports with Gatling with the ``-ro name-of-the-simulation-folder`` (reports only), Gatling will pick all the files that match ``.*\.log``

Example script using the above commands: (the script will run the same simulation parralel on given hosts, gather logs and generate a single report)

.. code-block:: shell

   #!/bin/bash
   ##################################################################################################################
   #Gatling scale out/cluster run script:
   #Before running this script some assumptions are made:
   #1) Public keys were exchange inorder to ssh with no password promot (ssh-copy-id on all remotes)
   #2) Check  read/write permissions on all folders declared in this script.
   #3) Gatling installation (GATLING_HOME variable) is the same on all hosts
   #4) Assuming all hosts has the same user name (if not change in script)
   ##################################################################################################################
   
   #Assuming same user name for all hosts
   USER_NAME='nimrod'
   
   #Remote hosts list
   HOSTS=( 192.168.28.24 192.123.123.12 180.123.98.1)
   
   #Assuming all Gatling installation in same path (with write permissions)
   GATLING_HOME=/gatling/gatling-charts-highcharts-1.5.6
   GATLING_SIMULATIONS_DIR=$GATLING_HOME/user-files/simulations
   GATLING_RUNNER=$GATLING_HOME/bin/gatling.sh
   
   #Change to your simulation class name
   SIMULATION_NAME='nimrodstech.GatlingClusterTest'
   
   #No need to change this
   GATLING_REPORT_DIR=$GATLING_HOME/results/
   GATHER_REPORTS_DIR=/gatling/reports/
   
   echo "Starting Gatling cluster run for simulation: $SIMULATION_NAME"
   
   echo "Cleaning previous runs from localhost"
   rm -rf $GATHER_REPORTS_DIR
   mkdir $GATHER_REPORTS_DIR
   rm -rf $GATLING_REPORT_DIR
   
   for HOST in "${HOSTS[@]}"
   do
     echo "Cleaning previous runs from host: $HOST"
     ssh -n -f $USER_NAME@$HOST "sh -c 'rm -rf $GATLING_REPORT_DIR'"
   done
   
   for HOST in "${HOSTS[@]}"
   do
     echo "Copying simulations to host: $HOST"
     scp -r $GATLING_SIMULATIONS_DIR/* $USER_NAME@$HOST:$GATLING_SIMULATIONS_DIR
   done
   
   for HOST in "${HOSTS[@]}"
   do
     echo "Running simulation on host: $HOST"
     ssh -n -f $USER_NAME@$HOST "sh -c 'nohup $GATLING_RUNNER -nr -s $SIMULATION_NAME > /gatling/run.log 2>&1 &'"
   done
   
   echo "Running simulation on localhost"
   $GATLING_RUNNER -nr -s $SIMULATION_NAME
   
   echo "Gathering result file from localhost"
   ls -t $GATLING_REPORT_DIR | head -n 1 | xargs -I {} mv ${GATLING_REPORT_DIR}{} ${GATLING_REPORT_DIR}report
   cp ${GATLING_REPORT_DIR}report/simulation.log $GATHER_REPORTS_DIR
   
   
   for HOST in "${HOSTS[@]}"
   do
     echo "Gathering result file from host: $HOST"
     ssh -n -f $USER_NAME@$HOST "sh -c 'ls -t $GATLING_REPORT_DIR | head -n 1 | xargs -I {} mv ${GATLING_REPORT_DIR}{} ${GATLING_REPORT_DIR}report'"
     scp $USER_NAME@$HOST:${GATLING_REPORT_DIR}report/simulation.log ${GATHER_REPORTS_DIR}simulation-$HOST.log
   done
   
   mv $GATHER_REPORTS_DIR $GATLING_REPORT_DIR
   echo "Aggregating simulations"
   $GATLING_RUNNER -ro reports
   
   #using macOSX
   open ${GATLING_REPORT_DIR}reports/index.html
   
   #using ubuntu
   #google-chrome ${GATLING_REPORT_DIR}reports/index.html


