#!/bin/bash

SRC_DIR=/sphinx-doc
BUILD_DIR="${SRC_DIR}/_build"
HTML_DIR="$BUILD_DIR/html"
SRC_FILE="${SRC_DIR}/index.rst"

if [ ! -d "$HTML_DIR" ]; then
  mkdir -p "$HTML_DIR"
fi

cd "$HTML_DIR"
python -m SimpleHTTPServer 55242 &
while true ; do 
  sphinx-autogen -o "$BUILD_DIR" "$SRC_FILE"
  sleep 5
done
