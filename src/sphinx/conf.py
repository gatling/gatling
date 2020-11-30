# -*- coding: utf-8 -*-
#
# Gatling documentation build configuration

import sys, os, re, docutils

##############
# Extensions #
##############

sys.path.append(os.path.abspath("_sphinx/exts"))
extensions = ['sphinx.ext.todo', 'sphinx.ext.extlinks', 'includecode']
todo_include_todos = True
extlinks = {
  'issue' : ('https://github.com/gatling/gatling/issues/%s', '#'),
  'version' : ('https://github.com/gatling/gatling/issues?milestone=%s&state=closed', None)
}

################
# Project info #
################

project = u'Gatling'
copyright = u'2018 Gatling Corp 2000-2018'
version = '3.0.0-SNAPSHOT'
release = version

####################
# General settings #
####################

master_doc = 'index'
add_function_parentheses = False
highlight_language = 'scala'
exclude_patterns = ['_build']
nitpicky = True

###############
# HTML output #
###############

html_theme = 'gatling'
html_theme_path = ['_sphinx/themes']
html_extra_path = ['cheat-sheet.html']
html_theme_options = { 'github' : 'https://github.com/gatling/gatling/edit/master/src/sphinx/' }
html_title = 'Gatling documentation'
html_domain_indices = False
html_use_index = False
html_show_sourcelink = False
html_show_sphinx = False
htmlhelp_basename = 'Gatlingdoc'

def regex_replace(s, find, replace):
  return re.sub(find, replace, s)

def add_jinja_filters(app):
  app.builder.templates.environment.filters['regex_replace'] = regex_replace

def setup(app):
  app.connect('builder-inited', add_jinja_filters)
