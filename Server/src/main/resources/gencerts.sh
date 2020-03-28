#!/bin/bash

# KEY_PWD = Password for the key
# HOST = IP for the ca-cert.pem certificate
# TLS_CONNS = Accepted dns / ips to connect to
# CA_CC = Country code (C)
# CA_STATE = State (ST)
# CA_CITY = City (L)
# CA_ORG = Organization name (O)


KEY_PWD="root"
HOST="13.1.10.2"
TLS_CONNS="IP:13.1.10.2"

# Set default values
if [ "$CA_CC" = "" ]
then
  CA_CC="DE"
fi

if [ "$CA_STATE" = "" ]
then
  CA_STATE="Some-State"
fi

if [ "$CA_CITY" = "" ]
then
  CA_CITY="Some-City"
fi

if [ "$CA_ORG" = "" ]
then
  CA_ORG="Some-Ord"
fi


# Generate Key
openssl genrsa -aes256 -out ca-key.pem -passout "pass:${KEY_PWD}" 4096

# Generate ca-cert.pem
openssl req -new -x509 -days 365 -key ca-key.pem -sha256 -out ca-cert.pem \
  -subj "/C=${CA_CC}/ST=${CA_STATE}/L=${CA_CITY}/O=${CA_ORG}/CN=${HOST}" -passin "pass:${KEY_PWD}"

# Generate server-key.pem
openssl genrsa -out server-key.pem 4096

# Generate server.csr
openssl req -subj "/CN=${HOST}" -sha256 -new -key server-key.pem -out server.csr

# Specify dns / ips which are allowed as connection names
echo subjectAltName = "DNS:localhost,IP:127.0.0.1,${TLS_CONNS}" >> extfile.cnf
echo extendedKeyUsage = serverAuth >> extfile.cnf

openssl x509 -req -days 365 -sha256 -in server.csr -CA ca-cert.pem -CAkey ca-key.pem -CAcreateserial -out server-cert.pem -extfile extfile.cnf -passin "pass:${KEY_PWD}"

openssl genrsa -out key.pem 4096
openssl req -subj "/CN=client" -new -key key.pem -out client.csr

echo extendedKeyUsage = clientAuth > extfile-client.cnf

openssl x509 -req -days 365 -sha256 -in client.csr -CA ca-cert.pem -CAkey ca-key.pem -CAcreateserial -out cert.pem -extfile extfile-client.cnf -passin "pass:${KEY_PWD}"

# Remove obsolete files
rm -v client.csr server.csr extfile.cnf extfile-client.cnf
