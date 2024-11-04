#!/usr/bin/env bash

set -ex

DAYS="365"
# openssl ecparam -list_curves
CURVE="prime256v1"

# Generate root key and certificate
openssl ecparam -name "$CURVE" -genkey -noout -out ca-key.pem
openssl req -new -x509 -nodes -days "$DAYS" \
        -subj "/C=NO/ST=Hordaland/L=Bergen/O=Sikt/CN=root" \
        -key ca-key.pem -out ca-cert.pem

# Generate server key and certificate request
openssl req -newkey ec -pkeyopt ec_paramgen_curve:"$CURVE" -nodes -days "$DAYS" \
   -keyout server-key.pem \
   -subj "/C=NO/ST=Hordaland/L=Bergen/O=Sikt/CN=server" -out server-req.pem

# Generate server certificate
openssl x509 -req -days "$DAYS" -set_serial 01 \
   -in server-req.pem \
   -out server-cert.pem \
   -CA ca-cert.pem \
   -CAkey ca-key.pem

# Client key and cert request
openssl req -newkey ec -pkeyopt ec_paramgen_curve:"$CURVE" -nodes -days "$DAYS" \
   -keyout client-key.pem \
   -subj "/C=NO/ST=Hordaland/L=Bergen/O=Sikt/CN=client" -out client-req.pem

# Client cert
openssl x509 -req -days "$DAYS" -set_serial 01 \
   -in client-req.pem \
   -out client-cert.pem \
   -CA ca-cert.pem \
   -CAkey ca-key.pem

# Write files in format of nREPL:
rm -fv ./client.keys ./server.keys
cat ca-cert.pem server-cert.pem server-key.pem > server.keys
cat ca-cert.pem client-cert.pem client-key.pem > client.keys
chmod 0400 server.keys client.keys

# Clean up:
rm ca-key.pem ca-cert.pem \
   server-key.pem server-req.pem server-cert.pem \
   client-key.pem client-req.pem client-cert.pem
