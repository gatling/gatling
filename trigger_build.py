#!/usr/bin/env python
import json, urllib2, os, sys

def api_call(url, token=None, data=None):
    if data:
        data = json.dumps(data)
    req = urllib2.Request(url, data)
    if data:
        req.add_header('Content-Type', 'application/json; charset=UTF-8')
    if token:
        req.add_header('Authorization', 'token ' + token)
    p = urllib2.urlopen(req)
    return json.loads(p.read())

def travis_ping(travis_token, repository):
    last_build_id = api_call('https://api.travis-ci.org/repos/{}/builds'.format(repository), travis_token)[0]['id']
    return api_call('https://api.travis-ci.org/requests', travis_token, { 'build_id': last_build_id })['result']

travis_ping(os.environ["API_TOKEN"], sys.argv[1])