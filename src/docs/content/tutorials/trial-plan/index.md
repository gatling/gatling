---
menutitle: Evaluating Gatling Enterprise
title: Evaluating Gatling Enterprise with a trial plan
seotitle: Terms and conditions for a Gatling Enterprise trial account
description: "Learn about Gatling Enterprise’s free trial, including features to test, trial limitations, advanced features to test, and post-trial options."
lead: Learn about Gatling Enterprise’s free trial, including features to test, trial limitations, advanced features to test, and post-trial options.
---

The trial offers an opportunity to explore the key features of Gatling Enterprise as a load testing platform for free during the trial period.

Gatling Enterprise offers [3 plan tiers](https://gatling.io/pricing): Basic, Team, and Enterprise.

When you create an account on Gatling Enterprise Cloud to start a product trial, you can benefit from the same plan limits as the Basic plan (except for minutes of testing).

## Product evaluation options

You have 2 options to evaluate the capabilities of Gatling Enterprise:

### Self-service

- You do everything on your end, following the different guidelines provided by Gatling in the documentation,

- It does not require any payment method at sign-up,

- Do not automatically convert to a paid plan when the trial ends.

### Sales-assisted 

- The Sales team supports you with a professional process and customized terms and conditions,

- It does not require any payment method at sign-up,

- Do not automatically convert to a paid plan when the trial ends.


## Information visible in the trial

On the Homepage, you will see a **Subscribe** button, which disappears as soon as you switch to a paid plan.

{{< img src="trial-1.png" alt="Gatling Enterprise subscribe button" >}}

On the homepage, in the right-hand corner, you will see a Plan & Credits section with the number of trial free credits and credits remaining.

{{< img src="trial-2.png" alt="credit metering" >}}

In the menu, if you select **Organization**, then the **Billing** tab, you can see that your account has no plan activated.

{{< img src="trial-3.png" alt="no plans activated warning message" >}}

## Features to test with your trial account

Load testing is a way of ensuring that your digital service (website, web app, API…, etc.) works well in real-world scenarios.

We recommend that you quickly create a straightforward load test simulating the activity of a small part of your desired virtual users.

- Create a test in 1-min [specifying a website ]({{< ref "/quickstart/" >}}) URL

- [Use your existing Gatling script]({{< ref "/scripting-intro/#run-the-simulation-on-gatling-enterprise-cloud" >}})

To test the following features:

### Phase 1

#### Protocol support
  
You should consider the protocol your digital service uses and test Gatling Enterprise's ability to generate the required workload for virtual users. The protocol version can matter.

#### Advanced reporting

When your test run ends, you always proceed with the topmost performance engineering activity: understanding and analyzing results. Gatling Enterprise is a leader of advanced metrics and user-friendly reports on load testing. See our dedicated page on [Reports]({{< ref "../reference/stats/" >}})

#### Compare run metrics

#### Trends reports 

When you have multiple runs of your trial test, you can evaluate the test results over time. Trends reports are helpful to monitor for regressions or performance changes over longer time periods. Learn more in the [Trends documentation]({{< ref "../reference/stats/trends/" >}})

#### Share a report with a public link

Gatling Enterprise simplifies sharing test results with your teammates or management. One way to share results is by generating and sharing a public link to a test report. 

### Phase 2

#### Load-scheme distribution and customization

Testing the ability to elevate the number of concurrent users and set up the scheme for applying the load is more important than being able to run your full test at scale. As Gatling Enterprise scales linearly, we don’t recommend high-scale load testing on trial, except if you’re considering a sales-assisted trial.

#### Integrations 

Explore how to integrate Gatling into your [CI/CD pipeline]({{< ref "../reference/integrations/" >}})

#### Community assistance

Regarding software support, the people behind the performance testing platform you choose are more important than the tool itself. The opportunity to receive guidance throughout the trial or help when troubleshooting any issues from the Gatling team will save you time and money.

The features you can test are unlimited, but remember that you only have 14 days. So, dedicate your time solely to evaluating the Gatling Enterprise product capabilities and carry out your test at scale after you make your decision.

## Trial limitations

There are some limitations to the trial.

The self-service trial has the following limits:

|                          |                       |                                                                                                         |
| :----------------------: | :-------------------: | :-----------------------------------------------------------------------------------------------------: |
|           Item           |      Trial Limit      |                                                 Comment                                                 |
|          Period          |        14 days        |                          The remaining credits are reset to 0 after this period                         |
|    Minutes of testing    |           30          |  If you need more minutes of testing, please [contact the Sales team](https://gatling.io/book-a-demo).  |
|           Users          |           2           |                 Inviting your team members to test the product with you is always best.                 |
|           Team           |           1           |                                                                                                         |
|      Load generators     |           1           |    If you need more load generators, please [contact the Sales team](https://gatling.io/book-a-demo).   |
|  Load generators hosting |       AWS, Paris      |                                                                                                         |
| Distributed Load Testing |           No          |                                                                                                         |
|       Dedicated IP       |           No          |          It can be activated, please [contact the Sales team](https://gatling.io/book-a-demo).          |
|       Integrations       |           No          |                                                                                                         |
|       Notifications      |           No          |                                                                                                         |
|      Data retention      |        14 days        |                                                                                                         |
|          Support         | Community & Live chat | If you need more or different support, please [contact the Sales team](https://gatling.io/book-a-demo). |
|            SLA           |      No agreement     |                                                                                                         |

The sales-assisted trial as the following limits:

|                          |                      |                                                                                                      |
| :----------------------: | :------------------: | :--------------------------------------------------------------------------------------------------: |
|           Item           |         Limit        |                                                Comment                                               |
|           Users          |       Unlimited      |                                                                                                      |
|           Team           |       Unlimited      |                                                                                                      |
|      Load generators     |       Up to 20       |                                      It depends on the use case                                      |
|  Load generators hosting |      AWS, Paris      | Private Locations can be activated, please [contact the Sales team](https://gatling.io/book-a-demo). |
| Distributed Load Testing |          Yes         |                                                                                                      |
|       Dedicated IP       |          Yes         |                                                                                                      |
|       Integrations       |          Yes         |                                                                                                      |
|       Notifications      |          Yes         |                                                                                                      |
|    Minutes of testing    |       Up to 300      |          If you need more, please [contact the Sales team](https://gatling.io/book-a-demo).          |
|      Data retention      |    Several months    |                                                                                                      |
|          Support         |      Sales Team      |                                                                                                      |
|            SLA           | Evaluation agreement |                                                                                                      |

With 1 load generator, you can simulate up to 40,000 virtual users per second or the equivalent of 300,000 requests per second. See our [FAQ]({{< ref "/faq/#how-much-load-can-1-load-injector-generate-with-gatling" >}}).

So you can run tests with many concurrent virtual users during the trial.

## Why go on a sales-assisted trial?

You want to talk to a human to help you understand Gatling Enterprise product's capabilities.

You need customized limits to test Gatling Enterprise.

You are already using the open-source version of Gatling and want to understand the differences in detail and the benefits of Gatling Enterprise for your team.

You want to compare your current tool with Gatling Enterprise.

The software is important, as are the contractual options, terms and conditions, support, and professional services.

You need help finding the answer to your question in the documentation.

Because you like our sales team and that's reason enough.

## Activation of advanced features and add-ons

The following features are not available on the trial by default:

- High-scale Load Testing with more than 1 Load Generator

- [Distributed Load Testing]({{< ref "../reference/execute/cloud/user/simulations/#step-2-locations-configuration" >}})

- [Dedicated IP]({{< ref "../reference/execute/cloud/user/dedicated-ips/" >}})

- [Privates Locations]({{< ref "../reference/install/cloud/private-locations/introduction/" >}})

- [Privates Packages]({{< ref "../reference/install/cloud/private-locations/private-packages/" >}})

You can contact the sales team to request access. Our teams will develop your use case together to find the most appropriate solutions.

## Examples of credit use

30 credits correspond to 30 minutes of testing using 1 Load generator.

For example, you will be able to achieve the following:

|      |     |                |                         |         |
| :--: | :-: | :------------: | :---------------------: | :-----: |
| Test | Run | Load Generator | Test Duration (minutes) | Credits |
|   1  |  1  |        1       |            30           |    30   |
|   1  |  6  |        1       |            5            |    30   |


## Post-Trial decision

Your trial period finishes after 14 days.

Regarding the value of a load test platform, as long as you identify problems (back-end, network, infrastructure…, etc.) to be solved, we can say that the ‘trial’ ends once your team exceeds the credit limit.

But you will still have access to your account and data history even if you have used up all your credits.

You can upgrade to a paid plan anytime later.

There is no commitment on your part in terms of volume or amount.

You can always start small. The important thing is to get started!

See how our plans compare <https://gatling.io/pricing>.

After that period, you can opt for one of the three following paths:

### Upgrade to a paid monthly plan

Continue your tests with superior limits and support.

If you wish to continue on a paid plan, you must add a payment method to ensure a seamless transition from the trial to the paid plan when your trial ends. To add a payment method:

1. Click on the **Subscribe to a plan** button on the Gatling Enterprise home page. 
2. Select **Monthly** at the top.
3. Select between the Basic and Team plans and click **Subscribe now**.
4. Fill in your payment details and click **Save payment method**.
5. Click the **Pay (amount)** button on the Payment screen to complete your purchase.

### Upgrade to a paid annual plan

If you wish to continue on a paid plan, you must add a payment method to ensure a seamless transition from the trial to the paid plan when your trial ends. To add a payment method:

1. Click on the **Subscribe to a plan** button on the Gatling Enterprise home page. 
2. Select **Annually** at the top.
3. Select between the Basic and Team plans and click **Subscribe now**.
4. Fill in your payment details and click **Save payment method**.
5. Click the **Pay (amount)** button on the Payment screen to complete your purchase.

### Request a sales-assisted trial

We evaluate each use case to make sure that a sales-assisted trial is the right choice for you and Gatling. [contact the Sales team](https://gatling.io/book-a-demo) to find out if this is the right option for you. 

## Return for a second trial

If your trial account credits expire, you can create a new account with another email address (such as your business email address) and start a new trial.
