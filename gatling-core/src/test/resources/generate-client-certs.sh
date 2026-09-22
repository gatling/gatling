#!/usr/bin/env bash
# Generates NB_CERTS client key/certificate pairs signed by the root CA
# (run generate-ca-and-truststore.sh first) and merges them into a single
# PKCS12 keystore, one alias per client: mykey0001, mykey0002, ...
#
# openssl alone cannot merge several independent key+cert pairs into one
# PKCS12 (it only keeps the last private key found), so each client is
# first exported to its own single-entry p12 with openssl, then merged
# into the final keystore with `keytool -importkeystore`.
#
# Output: target/test-classes/ssl/clients-keystore.p12
#
# Env overrides: PASSWORD (default changeit), CLIENT_DAYS (default 730), NB_CERTS (default 20)
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/../../.." && pwd)"
SSL_DIR="$PROJECT_ROOT/target/test-classes/ssl"
PASSWORD="${PASSWORD:-changeit}"
CLIENT_DAYS="${CLIENT_DAYS:-730}"
NB_CERTS="${NB_CERTS:-20}"

if [[ ! -f "$SSL_DIR/ca-cert.pem" || ! -f "$SSL_DIR/ca-key.pem" ]]; then
  echo "[generate-client-certs] no CA found in $SSL_DIR - run generate-ca-and-truststore.sh first" >&2
  exit 1
fi

if (( NB_CERTS > 9999 )); then
  echo "[generate-client-certs] NB_CERTS=$NB_CERTS exceeds the mykeyNNNN 4-digit alias format (max 9999)" >&2
  exit 1
fi

WORK_DIR="$(mktemp -d)"
trap 'rm -rf "$WORK_DIR"' EXIT

rm -f "$SSL_DIR/clients-keystore.p12"

echo "[generate-client-certs] generating $NB_CERTS client certificates (RSA 2048, ${CLIENT_DAYS} days)"
for i in $(seq 1 "$NB_CERTS"); do
  alias=$(printf "mykey%04d" "$i")

  openssl genrsa -out "$WORK_DIR/${alias}-key.pem" 2048 2>/dev/null
  openssl req -new -key "$WORK_DIR/${alias}-key.pem" \
    -subj "/O=Gatling Demo/CN=${alias}" \
    -out "$WORK_DIR/${alias}.csr"
  openssl x509 -req -in "$WORK_DIR/${alias}.csr" \
    -CA "$SSL_DIR/ca-cert.pem" -CAkey "$SSL_DIR/ca-key.pem" -CAcreateserial \
    -days "$CLIENT_DAYS" -sha256 \
    -out "$WORK_DIR/${alias}-cert.pem"

  openssl pkcs12 -export \
    -inkey "$WORK_DIR/${alias}-key.pem" \
    -in "$WORK_DIR/${alias}-cert.pem" \
    -certfile "$SSL_DIR/ca-cert.pem" \
    -name "$alias" \
    -out "$WORK_DIR/${alias}.p12" \
    -passout "pass:$PASSWORD"

  keytool -importkeystore -noprompt \
    -srckeystore "$WORK_DIR/${alias}.p12" -srcstoretype PKCS12 -srcstorepass "$PASSWORD" \
    -destkeystore "$SSL_DIR/clients-keystore.p12" -deststoretype PKCS12 -deststorepass "$PASSWORD" \
    >/dev/null

  echo "[generate-client-certs] added $alias ($i/$NB_CERTS)"
done

echo "[generate-client-certs] done -> $SSL_DIR/clients-keystore.p12 ($NB_CERTS entries)"
