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
- [Java runtime environment](https://openjdk.org/install/): OpenJDK 64bits LTS versions: 11, 17 or 21 (see [Gatling prerequisites]({{< ref "../../../oss#java-version" >}}))

{{< alert tip >}}
Learn how to tune the OS for more performance, configure the open files limit, the kernel and the network [here]({{< ref "../../../../script/core/operations#os-tuning" >}}).
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
      # Engine (optional, default classic)
      engine = "classic" # Possible values: classic or javascript
      # Certified image configuration
      # They are hosted on Docker Hub, and available for the linux/amd64 and linux/arm64 platforms
      image {
        type = certified
        java = latest # See engine section
      }
      # Custom image configuration
      # You can build your own images from https://github.com/gatling/frontline-injector-docker-image
      # image {
      #   type = custom
      #   image = "gatlingcorp/classic-openjdk:latest"
      # }
      # Job definition (optional)
      # See https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.30/#job-v1-batch for properties
      job = { include "job.json" }
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

### Engine

The engine specified for a location determines the compatible package formats (JavaScript or JVM) for Gatling packages.

Each engine (`classic` or `javascript`) supports specific Java versions, where `latest` is defaulted.

The table below outlines the supported Java versions for certified Gatling images:

| Engine      | Supported Java Versions |
|-------------|-------------------------|
| classic     | 21 or latest            |
| javascript  | latest                  |

{{< alert info >}}
For the `javascript` engine, only the latest Java version is supported, which corresponds to the GraalVM version used to run Gatling with JavaScript.
{{< /alert >}}

### Example JSON Job Definition

When configuring the control plane, you may include a job definition that adheres to the [Kubernetes Job API schema](https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.30/#job-v1-batch). 
This job definition manages the deployment and execution of Gatling simulations on Kubernetes.

Several aspects of this job schema are enforced by the control plane:
* `spec.parallelism`: Automatically set to match the number of instances configured for the simulation at the specified location, ensuring the workload is distributed as intended.
* `spec.backoffLimit`: Set to 0 to disable any retries, ensuring that failed simulations do not automatically restart.
* `spec.template.spec.restartPolicy`: Enforced as Never to prevent the creation of unintended instances.
* `spec.template.spec.containers`: The job is restricted to a single container in the pod specification, which will run the Gatling simulation.
* `spec.template.spec.containers[0].image`: The container image is determined by your configuration (certified or custom) and cannot be set within the job definition itself.
* `spec.template.spec.containers[0].command`: Reserved for initiating the Gatling script and should not be modified within the job definition.

Here is an example of a basic JSON job definition:
```json
{
  "apiVersion": "batch/v1",
  "kind": "Job",
  "metadata": {
    "generateName": "gatling-job-",
    "namespace": "gatling"
  },
  "spec": {
    "template": {
      "metadata": {
        "annotations": {
          "example-key": "example-value"
        },
        "labels": {
          "example-key": "example-value"
        },
        "namespace": "gatling"
      },
      "spec": {
        "containers": [
          {
            "env": [
              {
                "name": "env-key",
                "value": "env-value"
              }
            ],
            "name": "gatling-container",
            "resources": {
              "limits": {
                "memory": "512Mi",
                "cpu": "4"
              },
              "requests": {
                "memory": "512Mi",
                "cpu": "4"
              }
            }
          }
        ],
        "securityContext": {
          "sysctls": [
            {
              "name": "net.ipv4.tcp_tw_reuse",
              "value": "1"
            }
          ]
        }
      }
    },
    "ttlSecondsAfterFinished": 60
  }
}
```

#### Key Elements of the Example Job Definition

* `apiVersion`: Specifies the API version for the Job resource, which is batch/v1 in this case.
* `kind`: Indicates that this is a Job resource.
* `metadata.generateName`: A prefix for the name of the job. Kubernetes will append a unique suffix to ensure the job name is unique.
* `metadata.namespace`: The namespace in which the job will be created. Ensure this matches your configuration.
* `spec.template.metadata`: Contains metadata such as annotations and labels for the job template.
* `spec.template.spec.containers`: The container definition, including environment variables and resource limits.
* `resources.limits and resources.requests`: Specify the resources (memory and CPU) the container can use and the minimum resources it is guaranteed.
* `securityContext.sysctls`: Configures kernel parameters for the container, such as enabling TCP connection time wait reuse.
* `ttlSecondsAfterFinished`: The job's time-to-live after completion. After this time, the job will be automatically deleted by Kubernetes.

{{< alert info >}}
This job definition is based on the Kubernetes Job API schema.
For more details on the available properties and their configurations, please refer to the [Kubernetes Job API schema](https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.30/#job-v1-batch).
{{< /alert >}}
