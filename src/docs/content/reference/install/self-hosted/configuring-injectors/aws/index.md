---
title: AWS EC2
seotitle: AWS EC2 injectors with Gatling Enterprise Self-Hosted
description: Learn how to configure AWS EC2 pools for Gatling Enterprise.
lead: Learn how to configure AWS EC2 pools for Gatling Enterprise.
date: 2021-03-26T09:40:35+01:00
lastmod: 2021-08-16T17:55:36+02:00
---

An AWS Pool is a reference to the AWS account you want to use to spawn injectors to run the simulation.

{{< alert warning >}}
Make sure your network configuration, in particular your Security Groups, will let Gatling Enterprise connect to your injectors on port 22 (SSH) and 9999 (HTTP).
{{< /alert >}}

To configure the type of instances you want to spawn, you need to fill the form below:

{{< img src="aws.png" alt="AWS Pool" >}}

- **Team**: Set if the pool is global or owned by a team
- **Profile Name**: Name of the AWS profile described in the AWS credentials file. If you want to use System or Environment properties instead of this file, choose `Use environment or system variables`
- **Region**: the region where to spawn your instances
- **AMI**: the AMI you want to use for your instances. You can use our certified AMIs or the ID of your custom AMI (the AMI should at least have JDK8 installed, a configured key pair without password and the port 22 & 9999 should be open)
- **VPC**: the VPC in which your instance will be created
- **Subnet**: the subnet in which your instance will be created
- **Security Group**: the security groups the instance will use
- **Instance Type**: the type of the instances you want to spawn
- **Key Pair**: the Key pair name used by your AMI
- **User Name**: the username used by your ssh command to connect to the instances. If you use one of our certified AMIs, the username will be ec2-user
- **Private Key**: the previously added [private key]({{< ref "../../../execute/self-hosted/admin/private-keys" >}}) used by your AMI
- **Use Elastic IP**: Allow instances to use predefined Elastic IP
- **Connect to private IP**: Gatling Enterprise will connect to the injectors' private IP instead of the public one. If unchecked, the private IP remains a fallback if a public IP is missing. This option should be used only when the Gatling Enterprise host and the injector are both in the same AWS network.
- **IAM Instance Profile**: optional step, you can specify an IAM instance profile to grant injectors permissions
- **AWS tags**: optional step, the tags will be visible in your AWS interface, hence you will be able to monitor them

{{< alert warning >}}
If you're using our certified AMIs, make sure that you add a security group allowing Internet access.
This is required for automatic critical security updates checks done by the OS.
{{< /alert >}}
