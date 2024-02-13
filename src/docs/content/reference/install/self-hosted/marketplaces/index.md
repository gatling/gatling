---
title: Installation on a Marketplace
seotitle: Marketplace installation of Gatling Enterprise Self-Hosted
description: Install or upgrade Gatling Enterprise on a Marketplace, and learn how to configure it.
lead: Install or upgrade Gatling Enterprise on a Marketplace, and learn how to configure it.
date: 2022-06-23T12:53:30+00:00
lastmod: 2022-09-07T08:06:55+00:00
---

## AWS Marketplace

### Installing on the AWS Marketplace {#aws-installation}

We provide three offers on the AWS Marketplace:

* Continuous Load Testing
* Distributed Load Testing
* High-Scale Load Testing

The links could be updated by AWS so it is best to search for `gatling enterprise` directly, see the following [search results](https://aws.amazon.com/marketplace/search/results?searchTerms=gatling+enterprise).

After selecting an offer, the AWS Marketplace will guide you through the installation on your own AWS account.

### Upgrading on the AWS Marketplace {#aws-upgrade}

You'll need two running instances to perform a migration. In order to avoid paying the subscription fee of two Gatling Enterprise instances at the same, we recommended starting the migration in the original instance, download every backup you made, shutdown the instance down, and finally start a fresh one with the latest version. The idea behing this process is that you only pay the AWS Marketplace fee when an instance is running.

On the former instance:

* Stop Gatling Enterprise with the command `sudo systemctl stop frontline`
* Stop the Cassandra database with the command `sudo systemctl stop cassandra`
* Copy the `/var/lib/cassandra` folder holding Cassandra's data
* Copy the `/opt/frontline/conf` and `/opt/frontline/keys` folders holding Gatling Enterprise's configuration

On the new instance:

* Create a new Gatling Enterprise instance with the latest version
* Stop Gatling Enterprise with the command `sudo systemctl stop frontline`
* Stop the Cassandra database with the command `sudo systemctl stop cassandra`
* Delete content inside the `/var/lib/cassandra` folder
* Copy the previously saved Cassandra data onto the new one
* Check the owner and the permissions `sudo chown cassandra:cassandra -R /var/lib/cassandra`
* Delete the content of the `/opt/frontline/conf` and `/opt/frontline/keys` directories
* Copy the previously saved Gatling Enterprise onto the new one
* Check the owner and the permissions `sudo chown frontline:frontline -R /opt/frontline/conf /opt/frontline/keys`
* Start the Cassandra database `sudo systemctl start cassandra`
* Wait a few minutes
* Start Gatling Enterprise `sudo systemctl start frontline`
* Wait a few minutes

## Azure Marketplace

### Installation {#azure-installation}

We provide a single offer on the Azure Marketplace with multiple pricing ranges:

* Continuous Load Testing
* Distributed Load Testing
* High-Scale Load Testing

You can see the offer itself [here](https://azuremarketplace.microsoft.com/en-us/marketplace/apps/gatlingcorp.gatling-frontline).

After selecting a plan, the Azure Marketplace will guide you through the installation on your own Azure subscription.

### Upgrading on the Azure Marketplace {#azure-upgrade}

Binaries are stored on the OS Disk in `/opt`.

Data are stored on the Data Disk in `/data`.

Upgrading consists in keeping the data disk, reusing it on the new instance, while copying the original configuration of Gatling Enterprise on the OS Disk.

On the former instance:

* Stop Gatling Enterprise with the command `sudo systemctl stop frontline`
* Stop the Cassandra database with the command `sudo systemctl stop cassandra`
* Copy the content of the `/opt/frontline/conf` and `/opt/frontline/keys` folders

On the new instance:

* Create a new Azure Gatling Enterprise instance with the latest version reusing the data disk of the previous version
* Stop Gatling Enterprise with the command `sudo systemctl stop frontline`
* Stop the Cassandra database with the command `sudo systemctl stop cassandra`
* Delete the content of the `/opt/frontline/conf` and `/opt/frontline/keys` folders
* Copy your previous back of the Gatling Enterprise configuration keys content from the former instance
* Check the owner and the permissions of these backups in the new instance: `sudo chown frontline:frontline -R /opt/frontline/conf /opt/frontline/keys`
* Start the Cassandra database with the command `sudo systemctl start cassandra`
* Wait a few minutes
* Start Gatling Enterprise with the command `sudo systemctl start frontline`
* Wait a few minutes
