# Contributing to Gatling

## About Github's issue tracker

We use Github's issue tracker as a TODO list or, and as far as you are concerned, for filing bug reports and asking for new features.
 
If you need help or simply want to ask a question, please DON'T use this issue tracker!
Search [Gatling's Google Group](https://groups.google.com/forum/#!forum/gatling) or post your questions there if they haven't been asked and answered previously.

## Preliminaries 

* Whenever possible, use the latest Gatling version.
* Search [Issues](https://github.com/gatling/gatling/issues) beforehand, your bug may have been already reported by another user.
* Open one issue for each problem.


## Filing bug reports

In order to get the bugs fixed as fast as possible, we need a few things from you first :
 
* The environment
* The steps
* The problem

### The environment

In order to narrow down the search, we need to know first :

* The Gatling's version you're using 
* Which OS you're running Gatling on
* Your method of running Gatling (Bundle, plugins, Jenkins, etc...)


### The steps

We'll also need to know **exactly** what you were doing.
To do so, please provide a complete description of what you were trying to achieve, with code samples, or even better : provide a [Gist](https://gist.github.com/) (or anything similar) of your simulation.

### The problem

Finally, describe the problem you're facing : the more information you give, the better.
If there is any error message or stacktrace available, include it in your bug report.

## Submitting Pull Requests

### Requirements

Before you submit a pull request, make sure that:

1. New features or API changes are properly documented (documentation sources are in `/src/sphynx`)
2. You provided tests for the code changes you made
3. The code follows Gatling's code guidelines formatting rules (code will be auomatically formatted if you compile it locally)
4. The pull request's commits must follow our guidelines (see the **Commits and commit messages** below)
5. Source files have the appropriate copyright header license :

	```
	/**
   * Copyright 2011-2018 GatlingCorp (https://gatling.io)
   *
   * Licensed under the Apache License, Version 2.0 (the "License");
   * you may not use this file except in compliance with the License.
   * You may obtain a copy of the License at
   *
   *  http://www.apache.org/licenses/LICENSE-2.0
   *
   * Unless required by applicable law or agreed to in writing, software
   * distributed under the License is distributed on an "AS IS" BASIS,
   * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   * See the License for the specific language governing permissions and
   * limitations under the License.
   */
 	``` 

Pull requests are automatically validated by Travis CI and pull requests resulting in a build failure won't obviously be merged.

#### Commits and commit messages

The commit message must be explicit and states what the commit changes. It must also references the Github issue it's closing.
A good example is : `Disable IPv6 by default, close #2013`.
