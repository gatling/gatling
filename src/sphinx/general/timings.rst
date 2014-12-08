.. _timings:

#######
Timings
#######

As Gatling runs and executes requests, several timings are recorded, which make up the basis of all forms of reporting in Gatling: console, HTML reports, etc...

.. _users:

Active Users
============

At a given second, active users are computed as:

* the number of active users at the previous second
* \+ the number of users who started during this second
* \- the number of users who finished during the previous second

.. _request-timings:

Requests
========

Latency
-------

The latency is the elapsed time between the beginning of the request's sending and the start of the response's receiving:

* The end of request's sending is the instant when the last bytes of the request have been sent over the wire by Gatling.
* The beginning of the response's receiving is the instant when at least the response's HTTP status has been been received by Gatling.

Response Time
-------------

The response time is the elasped time between the beginning of the request's sending and the end of the response's receiving:

* The beginning of the request's sending is the instant when the connection to the target host has been established or grabbed from the pool.
* The end of the response's receiving is the instant when the whole response (status, headers and body) has been received by Gatling

.. _groups-timings:

Groups
======

Response Time
-------------

The response time of a group is the cumulated response times of each individual request in that group.

.. note::

  When dealing with embedded resources (inferred or explicitly set), the behaviour is slightly different : as resources are fetched asynchronously,
  the cumulated response time for embedded resources starts from the beginning of the first resource request to the end of the last resource request.

Cumulated Time
--------------

The cumulated time of a group is the elapsed time from the start of the group's first request to the end of the group's last request, including  pauses.
Groups' cumulated time are only reported in the "Cumulated response time" chart.
