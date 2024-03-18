---
title: Installation with Ansible
seotitle: Install Gatling Enterprise Self-Hosted with Ansible
description: Learn how to install Gatling Enterprise with Ansible.
lead: Download our Ansible playbook and run it to install easily Gatling Enterprise and Cassandra.
date: 2021-03-26T16:58:37+01:00
lastmod: 2022-07-05T14:35:37+00:00
---

{{< alert tip >}}
Images of Gatling Enterprise published to the AWS Marketplace are made using this installer.
The directory layout will be the same.
{{< /alert >}}

## Requirements

The installer can be run from anywhere.

Ansible will be used to perform the installation. You'll need:

* Python 2.7.7+ or 3.5+
* Ansible 2.7.6+
* An instance/VM (at least `c5.large` or equivalent) running on supported Linux distributions

Supported Linux distributions are:

* Amazon Linux 1/2
* CentOS 7
* Debian 8/9/10
* RHEL (RedHat Enterprise Linux) 7

In case you don't already use Ansible, you can download it from [here](https://docs.ansible.com/ansible/latest/installation_guide/intro_installation.html). You do not need any Ansible knowledge to use this installer.

If you do want to know more about Ansible, you can check its [user guide](https://docs.ansible.com/ansible/latest/user_guide/quickstart.html).

## Configuring a proxy

Ansible will use the shell's proxy when running the script in your computer.

If you need to specify a proxy for the remote machine on which Gatling Enterprise will be installed, you can add environment variables at the installer level, in `frontline.yml`:

```diff
 - hosts: all
+  environment:
+    http_proxy: http://proxy.bos.example.com:8080
+    https_proxy: http://proxy.bos.example.com:8080
   roles:
     # ...
```

Don't put these in `configuration.yml`, you should see a warning like this one if you do:

```
[WARNING]: Skipping unexpected key (environment) in group (all), only "vars", "children" and "hosts" are valid
```

In case you decide to [run ansible locally]({{< ref "#running-the-installer-locally" >}}), it is necessary to use global environment variables before running the installer:

```bash
export HTTP_PROXY=http://proxy.bos.example.com:8080
export HTTPS_PROXY=http://proxy.bos.example.com:8080
```

## Using the installer

### Downloading and integrity checking

You can download the installer here:

```
REPLACE_WITH_YOUR_CUSTOM_GATLING_ENTERPRISE_BUNDLE_DOWNLOAD_LINK/frontline-installer/{{< var selfHostedVersion >}}/frontline-installer-{{< var selfHostedVersion >}}.zip
```

{{< alert tip >}}
Check the [documentation conventions]({{< ref "../introduction#custom-bundle-download-link" >}}) for ways to find your custom Gatling Enterprise bundle download link.
{{< /alert >}}

We suggest you download and check the integrity of the installer by doing the following:

```bash
#!/bin/bash

# The two variables you must change
version={{< var selfHostedVersion >}}
custom_url=REPLACE_WITH_YOUR_CUSTOM_GATLING_ENTERPRISE_BUNDLE_DOWNLOAD_LINK

archive_name=frontline-installer-${version}.zip
archive_url=${repo_url}/frontline-installer/${version}/${archive_name}

curl -O ${archive_url}
curl -O ${archive_url}.sha1

echo "$(cat ${archive_name}.sha1)  ${archive_name}" | sha1sum     -c -
# For MacOS users:                                    shasum -a 1 -c -
```

If you have aliases on `echo` and/or `cat`, you can prefix them with an anti-slash to make sure you are using the original command instead, as such: `\echo`, `\cat`.

### Configuring the installer

After unzipping the installer, you'll need to fill in your instance's SSH connection info and provided UUID in `configuration.yml`.

{{< include-code "configuration.yml" yaml >}}

You can also modify in `configuration.yml` whether you want to install build tools (Maven, Gradle, sbt), Nginx or kubectl (required for Kubernetes pools).

### Running the installer

Type in `./installer.sh` and wait for the installation to end.

The script will ask for a sudo password. On Amazon Linux 1 and 2, the default user `ec2-user` is able to sudo without any password, so you can just type in `<Enter>` twice.

The script is idempotent. It means you can run it multiple times without compromising your previous installation. It also means you can start over a previous failed run and continue on.

{{< alert warning >}}
The installer cannot be used to upgrade to a new version yet.
{{< /alert >}}

#### Running the installer locally

{{< alert warning >}}
This is only meant for scenarios in which you can't run Ansible remotely. Running Ansible locally isn't the common use case.
{{< /alert >}}

In case you don't have the necessary tools to run Ansible remotely, i.e. running Ansible on your machine in order to install Gatling Enterprise on another machine, you can launch Ansible directly on the machine that will host Gatling Enterprise.

First, you need to copy the inventory `configuration.yml` file vars inside the playbook `frontline.yml` file, as such:

```yaml
- hosts: all

  vars: # part you need to copy
    ...

  roles:
    ...
```

Then, you will be able to run Ansible directly on the host you intend to install Gatling Enterprise in:

```shell
ansible-playbook \
  -b --ask-become-pass \
  -c local \
  -i localhost, \
  frontline.yml
```

### Running Gatling Enterprise

Services will be configured for each installed components of Gatling Enterprise. They will automatically start on boot.

You can control them with the `service`/`systemctl` command:

```shell
# On SysV-based distributions
sudo service {cassandra|frontline|nginx} {start|stop}
# On systemd-based distributions
sudo systemctl {start|stop} {cassandra|frontline|nginx}
```

{{< alert tip >}}
Gatling Enterprise depends on Cassandra, it will wait on its availability when starting.
{{< /alert >}}

{{< alert tip >}}
Nginx reverse proxy to Gatling Enterprise, but will still start if it is not available.
{{< /alert >}}

## Installation Layout

Two users will be created, `cassandra` and `frontline`, that will be used by, respectively, Cassandra and Gatling Enterprise.

{{< alert tip >}}
If you want a file to be access by Gatling Enterprise (E.g.: private keys), make sure to properly modify its `group:user` to `frontline:frontline`.
{{< /alert >}}

**Installation and configuration directories:**
```
/opt/cassandra
/opt/frontline
```

Nginx is installed using the packager of the distribution.

All other dependencies (I.e.: builders), are also installed in `/opt`.

Versions are installed in their own directories and linked to `/opt/cassandra` and/or `/opt/frontline`. Previous configuration files won't be overwritten on update.

**Home and data directories:**
```
/var/lib/cassandra
/var/frontline
```

**SystemV configuration files:**
```
/etc/sysconfig/cassandra
/etc/sysconfig/frontline
```

Any changes to the `PATH` of each service can be pushed in these files.

**SystemV services files:**
```
/etc/init.d/cassandra
/etc/init.d/frontline
```

**Systemd unit files:**
```
/etc/systemd/system/cassandra.service
/etc/systemd/system/frontline.service
```

**Logging directories:**
```
/var/log/cassandra
/var/log/frontline
```

## Troubleshooting

If anything goes wrong during the installation. You can turn on Ansible logging by modifying the following line in the `frontline.yml` file, switching the value of `no_log` from `True` to `False`:

```yaml
no_log: False
```
