---
menutitle: Dedicated IPs 
title: Dedicated IP addresses
seotitle: Use dedicated IP addresses in Gatling Enterprise Cloud
description: Request and use dedicated IP addresses for your load generator locations in Gatling Enterprise Cloud.
lead: Dedicated IP addresses for your locations.
date: 2021-03-10T14:29:04+00:00
lastmod: 2021-08-05T13:13:30+00:00
---

Dedicated IP Addresses allow you to have control over the load generators' IP addresses.
This is useful, for example, if your target system performs some sort of IP address filtering.

{{< img src="dedicated-ips.png" alt="Dedicated IPs" caption="Dedicated IPs" >}}

## Managing

To access the dedicated IP addresses section, click on **Dedicated IP Addresses** in the navigation bar.

{{< alert tip >}}
You can request dedicated IP addresses through [technical support](https://gatlingcorp.atlassian.net/servicedesk/customer/portal/8/group/12/create/59).

Please provide:
- Organization SLUG
- Desired number of dedicated IP addresses per location
- Contact Email
- GitHub username
A sales person will contact you.
{{< /alert >}}

The Dedicated IP addresses table shows your available dedicated IP addresses. Each one belongs to a specific location.

## Usage

You can enable the use of dedicated IP addresses when [configuring simulation locations]({{< ref "simulations#step-2-locations-configuration" >}}).

When starting a run of a simulation configured to use dedicated IP addresses,
if you have enough dedicated IP addresses available to satisfy the size of the configured locations,
they will be reserved for the run duration.  If you don’t have enough dedicated IP addresses available, the run won't start.
