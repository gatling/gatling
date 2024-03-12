---
title: Kubernetes locations configuration
menutitle: Configuration
seotitle: Configure Kubernetes locations in Gatling Enterprise Cloud
description: Load Generators on your private Kubernetes cluster.
lead: Private Locations on your Kubernetes cluster.
date: 2023-01-12T16:46:04+00:00
lastmod: 2023-10-13T08:10:39+00:00
---

## Instance specifications

We recommend that you use for your own load generators pods with at least 4 cores. See CPU requests and limits configuration below.

You might want to tune the `Xmx` JVM options to half of the memory request.
See `jvm-options` configuration below.
If you don't, the JVM will use a max heap size of 1/4th of the physical memory.

Also, if you're deploying your load generators in the same cluster as the application under test,
we recommend that you isolate the load generators on their dedicated nodes, using taints and tolerations.
See `tolerations` configuration below.

## Permissions

To use Kubernetes private locations, the control plane must have access to your Kubernetes cluster.

If the control plane is launched from outside the cluster, you have to give access to a valid Kubernetes file. See [Organizing Cluster Access Using kubeconfig Files](https://kubernetes.io/docs/concepts/configuration/organize-cluster-access-kubeconfig/).
The `.kube` folder can be mounted in `/app`, and absolute path must be set in a `KUBECONFIG` environment variable (e.g. `KUBECONFIG=/app/.kube/config`)

If the control plane is launched from inside the cluster, please refer to our [Kubernetes Control plane deployment documentation]({{< ref "installation" >}})

{{< alert tip >}}
When connecting to the cluster using HTTPS, if a custom truststore and/or keystore is needed, `KUBERNETES_TRUSTSTORE_FILE`,
 `KUBERNETES_TRUSTSTORE_PASSPHRASE` and/or `KUBERNETES_KEYSTORE_FILE`, `KUBERNETES_KEYSTORE_PASSPHRASE` environment variables should be set.
{{< /alert >}}

## System requirements

Kubernetes private locations image rely on some dependencies.

So when using a custom image, make sure following are available:

- [jq](https://jqlang.github.io/jq/download/) a lightweight and flexible command-line JSON processor.
- [curl](https://curl.se/download.html) a command line tool and library for transferring data with URLs
- [Java runtime environment](https://openjdk.org/install/): OpenJDK 64bits LTS versions: 11, 17 or 21 (see [Gatling prerequisites]({{< ref "../../oss#java-version" >}}))

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
      description = "Private Location on Kubernetes"
      # Private location type
      type = "kubernetes"
      # Namespace (optional, default based on kubernetes configuration)
      namespace = "gatling"
      # Certified image configuration
      # They are hosted on Docker Hub, and available for the linux/amd64 and linux/arm64 platforms
      image {
        type = certified
        java = latest # Possible values : 11, 17, 21 or latest
      }
      # Custom image configuration
      # You can build your own images from https://github.com/gatling/frontline-injector-docker-image
      # image {
      #   type = custom
      #   image = "gatlingcorp/classic-openjdk:latest"
      # }
      # Clean up finished jobs resources after given time (optional)
      ttl-after-finished = 10 minutes
      # Service account used for load generator pods (optional)
      # service-account-name = "myServiceAccount"
      # Labels of initiated resources (optional)
      labels {
        # ExampleKey = ExampleValue
      }
      # Annotations of initiated resources (optional)
      annotations {
        # ExampleKey = ExampleValue
      }
      # Environment variables of initiated pods (optional)
      environment-variables {
        # ExampleKey = ExampleValue
      }
      # Resources configuration for created pods (optional).
      # https://kubernetes.io/docs/concepts/configuration/manage-resources-containers/#resource-requests-and-limits-of-pod-and-container
      # Even though this configuration is optional (when undefined, the default values for your cluster will be used),
      # we STRONGLY recommend using specifications that are equal or above to the following values.
      # We also recommend to set both requests and limits to the same values.
      # 
      resources {
        limits {
          # memory = "1Gi"
          # cpu = "4.0"
        }
        requests {
          # memory = "1Gi"
          # cpu = "4.0"
        }
      }
      # Tolerations (optional)
      tolerations = [
      #  {
      #    key = key1
      #    operator = Equal
      #    # Value is not needed when effect is Exists (optional)
      #    value = value1 
      #    # An empty effect matches all effects with key (optional)
      #    effect = NoSchedule
      #  }
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

{{< alert info >}}
The service account is optional; the control-plane Kubernetes defaults will be utilized instead.
For example, in [Kubernetes installation]({{< ref "/reference/install/cloud/kubernetes/installation#example" >}}), the service account is configured on the control-plane container.
{{< /alert >}}

