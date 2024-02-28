---
title: Dedicated locations configuration
menutitle: Configuration
seotitle: Configure dedicated locations in Gatling Enterprise Cloud
description: Deploy load generators on dedicated machines that you manage.
lead: Deploy load generators on dedicated machines that you manage.
date: 2023-01-12T16:46:04+00:00
lastmod: 2023-10-13T08:10:39+00:00
---

## Instance specifications

We recommend that you use load generator instances with at least 4 cores.

You might want to tune the `Xmx` JVM options to half of the physical memory.
See `jvm-options` configuration below.
If you don't, the JVM will use a max heap size of 1/4th of the physical memory.

## Permissions

You can configure private locations with pre-existing servers. 
The control plane will be able to use configured dedicated machines as load generators during your simulations.

The control plane must have access to your dedicated machines. Ensure that each host is reachable through SSH on a specific port.

The control plane possesses a private key for establishing connections, while the corresponding public key is shared and configured on every host in the location.

## System requirements

Dedicated Machines private locations rely on some dependencies.

So, make sure following are available on the location configured hosts:

- `bash` the GNU Project's shell
- [jq](https://jqlang.github.io/jq/download/) a lightweight and flexible command-line JSON processor.
- [curl](https://curl.se/download.html) a command line tool and library for transferring data with URLs
- [Java runtime environment](https://openjdk.org/install/): OpenJDK 64bits LTS versions: 11, 17 or 21 (see [Gatling prerequisites]({{< ref "../../oss#java-version" >}}))
- `~/.ssh/authorized_keys` with control plane public key

{{< alert tip >}}
Learn how to tune the OS for more performance, configure the open files limit, the kernel and the network [here]({{< ref "../../../script/core/operations#os-tuning" >}}).
{{< /alert >}}

## Control plane configuration file

```bash
control-plane {
  # Control plane token
  token = "cpt_example_c7oze5djp3u14a5xqjanh..."
  # Control plane token with an environment variable
  token = ${?CONTROL_PLANE_TOKEN}
  # Control plane description (optional)
  description = "my control plane description"
  # Locations configurations
  locations = [
    {
      # Private location ID, must be prefixed by prl_, only consist of numbers 0-9, 
      # lowercase letters a-z, and underscores, with a max length of 30 characters
      id = "prl_private_location_example"
      # Private location description (optional)
      description = "Private Location on Dedicated Machines"
      # Private location type
      type = "dedicated"
      ssh {
        # SSH user
        user = "gatling"
        ## SSH private key
        private-key {
          # SSH private key path used to secure connections
          path = "private/key/path"
          # SSH private key password (optional, default: none)
          password = "password"
           # SSH private key password with an environment variable
          password = ${?PRIVATE_KEY_PASSWORD}
        }
        # SSH port (optional, default: 22)
        # port = 22
        # SSH connection timeout (optional, default 15 seconds)
        # connection-timeout = 15 seconds
      }
      # Working directory on hosts, must be executable (optional, default: /tmp)
      # working-directory = "/tmp
      
      # Hosts accessible with the SSH private key, on configured port (hostnames or IP addresses)
      hosts = [
        # "my.domain.example.com",
        # "1.2.3.4"
      ]
      
      # Java configuration (following configuration properties are optional)
      # System properties (optional)
      system-properties {
        # ExampleKey = ExampleValue
      }
      # Overwrite JAVA_HOME definition (optional)
      # java-home = "/usr/lib/jvm/zulu"
      # JVM Options (optional)
      # Default ones, that can be overridden with precedence:
      # [
      #   "-XX:MaxInlineLevel=20", 
      #   "-XX:MaxTrivialSize=12", 
      #   "-XX:+IgnoreUnrecognizedVMOptions", 
      #   "--add-opens=java.base/java.nio=ALL-UNNAMED", 
      #   "--add-opens=java.base/jdk.internal.misc=ALL-UNNAMED"
      # ]
      # Based on your instance configuration, you may want to update Xmx and Xms values.
      # jvm-options = ["-Xmx4G", "-Xms512M"]
    }
  ]
}
```

{{< alert warning >}}
Working directory must be executable
{{< /alert >}}

## Troubleshooting

This section contains tips for troubleshooting common issues with dedicated machines. 
If you have an issue not covered here, consider posting a question on the [Community Forum](https://community.gatling.io/c/enterprise/6).

### SSH keys

Execute the command `ssh-keygen -t ed25519 -f control-plane` to generate your key pair. _(You have the option to configure a password during this process)_

This will generate the `control-plane` private key and `control-plane.pub` public key in the current directory where the command was executed.

Next, you'll need to configure the authorized keys on the host for a user named, for example, `gatling`.

To configure the authorized keys on the host:

1. Create the authorized_keys file in the gatling/.ssh directory on the host:
  ```bash
  touch gatling/.ssh/authorized_keys
  chmod 600 gatling/.ssh/authorized_keys
  ```

2. Add the public key to gatling/.ssh/authorized_keys on the host. You can do this either by using the ssh-copy-id command:
  ```bash
  ssh-copy-id -i control-plane.pub gatling@host
  ```
  Or, manually by appending the public key to the file with:
  ```bash
  cat control-plane.pub >> gatling/.ssh/authorized_keys
  ```

3. Add the private key to the control plane. Refer to your control plane config file for the expected private key file path: for your location, the value of `ssh.private-key.path`.

### Timeout

If your execution results in a timeout without any additional information, it indicates that the script failed unexpectedly. 
To investigate this issue, you can configure a location to remain active by using the following settings:

```
locations = [
  {
    id = "prl_dedicated"
    type = "dedicated"
    ...
    debug {
      keep-load-generator-alive = true
    }
  }
]
```

Afterward, you can examine the logs via systemd logs using the identifier `gatling` with `journalctl --identifier gatling`.
