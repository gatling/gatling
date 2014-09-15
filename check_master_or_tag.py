#!/usr/bin/env python

from os import getenv 
from sys import exit 

is_master = getenv('TRAVIS_BRANCH') == "master"
is_tag = getenv('TRAVIS_TAG') != ""
is_not_pr = getenv('TRAVIS_PULL_REQUEST') == "false"

if((is_master or is_tag) and is_not_pr):
  exit(0)
else:
  exit(1)
