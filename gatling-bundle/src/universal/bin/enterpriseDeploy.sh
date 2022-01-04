#!/bin/bash
#
# Copyright 2011-2021 GatlingCorp (http://gatling.io)
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
# 		http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

set -e
clean_up_on_error() {
      ARG=$?
      if [ $ARG != 0 ] && [ -n "$GATLING_HOME" ]; then
        rm -f "${GATLING_HOME}/target/package.jar"
      fi
      exit $ARG
}
trap clean_up_on_error EXIT

GATLING_ENTERPRISE_CLOUD_DOMAIN="https://cloud.gatling.io"
usage() {
cat << EOF
usage: $0 --packageId <packageId> [--apiToken <apiToken>]

Options:
  --packageId string  Specify a packageId, Retrieve your package id here: ${GATLING_ENTERPRISE_CLOUD_DOMAIN}/#/admin/artifacts
  --apiToken string   Specify a apiToken, create an API token with 'Packages' permission here: ${GATLING_ENTERPRISE_CLOUD_DOMAIN}/#/api-tokens
                      Can also be set with the environment variable GATLING_ENTERPRISE_API_TOKEN
EOF
}

OLDDIR=$(pwd)
BIN_DIR=$(dirname "$0")
cd "${BIN_DIR}/.." && DEFAULT_GATLING_HOME=$(pwd) && cd "${OLDDIR}"
GATLING_HOME="${GATLING_HOME:=${DEFAULT_GATLING_HOME}}"

unset GATLING_ENTERPRISE_PACKAGE_ID

if [[ -z "${GATLING_ENTERPRISE_API_TOKEN}" ]]; then
  unset API_TOKEN
else
  API_TOKEN="${GATLING_ENTERPRISE_API_TOKEN}"
fi

while [[ $# -gt 0 ]]; do
    case "$1" in
        --packageId)
            GATLING_ENTERPRISE_PACKAGE_ID=$2
            shift 2
            ;;

        --apiToken)
            API_TOKEN=$2
            shift 2
            ;;

        *)
            usage
            exit
            ;;
    esac
done

#API token can be set as env variable as example given
# `export GATLING_ENTERPRISE_API_TOKEN=tokenApi`

if [ -z "${GATLING_ENTERPRISE_PACKAGE_ID}" ] || [ -z "${API_TOKEN}" ]; then
  usage
  exit 1
fi

#Check curl installation
if ! curl -V &> /dev/null
then
    echo "Curl is required to launch the script"
    exit
fi

# Create jar package
bash "${BIN_DIR}/enterprisePackage.sh"
# Create jar package done

# Upload the package.jar
echo "Uploading package.jar..."

UPLOAD_PACKAGE_FILE="${GATLING_HOME}/target/package.jar"
GATLING_ENTERPRISE_UPLOAD_PACKAGE_API="${GATLING_ENTERPRISE_CLOUD_DOMAIN}/api/public/artifacts/${GATLING_ENTERPRISE_PACKAGE_ID}/content?filename=package.jar"

HTTP_RESPONSE=$(
  curl --request PUT --upload-file "${UPLOAD_PACKAGE_FILE}" \
    "${GATLING_ENTERPRISE_UPLOAD_PACKAGE_API}" \
    --header "Authorization:${API_TOKEN}" \
    --write-out "HTTPSTATUSCODE:%{http_code} " \
    --silent
)
# extract the body
HTTP_RESPONSE_BODY=$(echo "$HTTP_RESPONSE" | sed -e 's/HTTPSTATUSCODE\:.*//g')

# extract the status
HTTP_RESPONSE_STATUS=$(echo "$HTTP_RESPONSE" | tr -d '\n' | sed -e 's/.*HTTPSTATUSCODE://')

if [ "${HTTP_RESPONSE_STATUS}" == 200 ]; then
  echo "Package successfully uploaded to Gatling Enterprise with id ${GATLING_ENTERPRISE_PACKAGE_ID}"
else
  echo "Upload failed"
  echo "http response code: $HTTP_RESPONSE_STATUS"
  echo "error: $HTTP_RESPONSE_BODY"
  exit 1
fi
