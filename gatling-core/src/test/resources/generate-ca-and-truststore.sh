#!/usr/bin/env bash
# Generates a self-signed root CA and a PKCS12 truststore containing its certificate.
#
# Output (in target/test-classes/ssl, already on the test classpath):
#   ca-key.pem     - CA private key (kept around, needed to sign client/server certs)
#   ca-cert.pem    - CA certificate
#   truststore.p12 - PKCS12 truststore, alias "root-ca"
#
# Env overrides: PASSWORD (default changeit), CA_DAYS (default 3650)
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/../../.." && pwd)"
SSL_DIR="$PROJECT_ROOT/target/test-classes/ssl"
PASSWORD="${PASSWORD:-changeit}"
CA_DAYS="${CA_DAYS:-3650}"

mkdir -p "$SSL_DIR"

echo "[generate-ca-and-truststore] generating root CA (RSA 2048, ${CA_DAYS} days) in $SSL_DIR"
openssl genrsa -out "$SSL_DIR/ca-key.pem" 2048
openssl req -x509 -new -nodes -key "$SSL_DIR/ca-key.pem" -sha256 -days "$CA_DAYS" \
  -subj "/O=Gatling Demo/CN=Gatling Demo Root CA" \
  -out "$SSL_DIR/ca-cert.pem"

# Note: openssl's own "pkcs12 -export -nokeys" cert-only bags are NOT reliably
# recognized as trustedCertEntry by Java's PKCS12 KeyStore implementation, so
# the truststore is built with keytool instead.
echo "[generate-ca-and-truststore] building truststore.p12 (alias root-ca)"
rm -f "$SSL_DIR/truststore.p12"
keytool -importcert -noprompt \
  -alias root-ca \
  -file "$SSL_DIR/ca-cert.pem" \
  -keystore "$SSL_DIR/truststore.p12" \
  -storetype PKCS12 \
  -storepass "$PASSWORD"

echo "[generate-ca-and-truststore] done -> $SSL_DIR/truststore.p12"
