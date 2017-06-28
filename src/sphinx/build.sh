#!/bin/bash

set -e
set -x

version=2.2

sphinx-build . ${version}
rm -rf ../../../../site/wordpress/wp-includes/gatling/docs/${version}
mv ${version} ../../../../site/wordpress/wp-includes/gatling/docs/${version}
