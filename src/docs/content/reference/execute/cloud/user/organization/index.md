---
title: Organizations
seotitle: Organizations in Gatling Enterprise Cloud
description: Learn how to access your organization details in Gatling Enterprise Cloud.
date: 2021-08-05T13:13:30+00:00
lastmod: 2021-08-05T13:13:30+00:00
---

To view information about your organization, click on the **Organization settings** button.

{{< img src="menu.png" alt="Organization settings" >}}

## Switch between multiple organizations

If you've been invited to multiple organizations, 
you can switch between them by clicking on the organization name in the top right corner.

{{< img src="multiple-organizations.png" alt="Multiple organizations" >}}

## Profile

{{< img src="profile.png" alt="Organization profile information" >}}

* **Avatar** - Composed by default from the two first characters of your **Organization name**.
* **Organization Name** - The display name for your organization.
* **Organization Slug** - Unique string name, in lowercase and spaced by dashes `-`.


{{< alert tip >}}
Click on the pen icon to edit the **Organization name**.
{{< /alert >}}

## Credit consumption

{{< img src="credits-consumption.png" alt="Organization credit informations" >}}

{{< alert info >}}
A credit represents a minute of usage of one Gatling load generator.
{{< /alert >}}

* **Blue** - Available credits.
* **Orange** - Consumed credits.

## Admin users

Shows all System Admins in your organization.

{{< img src="admin-users.png" alt="Organization users admin" >}}

For each System Admin, you will find their GitHub username, first name, and last name.

## Credits

{{< alert warning >}}
This section is only available to System Admins.
{{< /alert >}}

Credits consumption history.

{{< img src="credits.png" alt="Organization credits view in row" >}}

By clicking on a row, you will see all the details of the credit consumption for each month.

{{< img src="credits-detail.png" alt="Organization credits view in detail" >}}

* **Type** - Which type of event occurred, with a link redirecting to the run when credits were used to run a simulation.
* **Date** - The day the event took effect.
* **Credits** - Number of credits gained or used from the event.

### Extra credits

{{< alert warning >}}
This section is only available to System Admins & payment made by Stripe.
{{< /alert >}}

When you start to hit the credit limits in your plan:

{{< img src="credit_empty.png" alt="Empty credits" >}}

And you don't want to wait the next filling of credit, you can activate extra credits by clicking button **Edit spending limit**

{{< img src="credit_edit_limit.png" alt="Edit extra credits limit" >}}

And set new extra credit limit.

{{< img src="credit_extra_used.png" alt="Extra credits used" >}}

Now simulations -- you couldn't launch before -- can run consuming extra credits.


## Plans

{{< alert warning >}}
This section is only available to System Admins.
{{< /alert >}}

Plans view history.

{{< img src="plans.png" alt="Organization plan" >}}

* **Status** - Current status of the payment plan: **Terminated**, **Active**, or **PaymentFailure**.
* **From** - Start date of the plan.
* **To** - End date of the plan, if there is one.
* **Credits** - Number of credits awarded each month by the plan.

## Offers

{{< alert warning >}}
This section is only available to Organization System Admins.
{{< /alert >}}

### payment via Stripe

This page shows all available offers for your organization. You can choose the number of credits for your offer. A credit represents a minute of usage of one Gatling load generator.

{{< img src="offers.png" alt="Available Offers" >}}

Click on the **Subscribe now** button in order to buy the desired offer via stripe. If you want to change your current offer, or buy the **Custom** one, please click on **Contact us**.


### payment via AWS Marketplace

After Organization and Admin user of this organization created, 
you can choose to subscribe to an offer via the [AWS marketplace](https://aws.amazon.com/marketplace/pp/prodview-6bhi2464rfmzq):  

{{<img src="aws_marketplace.png" alt="AWS marketplace offer" >}}

select, among other options, the contract option:

{{<img src="aws_contract_option.png" alt="Contract option" >}}

and click on **Create contract**:

{{<img src="aws_create_contract.png" alt="Create contract" >}}

to finish setup, fill the subscription form with current users and organization information:

{{<img src="aws_subscription_form.png" alt="setup subscription" >}}

## Billing
After the first payment, you can access invoices and update payment information by clicking on the **Customer portal** button in the **Billing** tab. 

{{< img src="customer-portal.png" alt="Customer portal" >}}

{{< alert info >}}
If you subscribe through the AWS marketplace, your billing information and invoices are available through AWS.
{{< /alert >}}