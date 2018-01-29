# Digital Certificates Documentation #

This documentation explains how to generate and install the digital certificates needed to operate the public DAF endpoints under HTTPS:

## HowTo

### create new certificates

To create new certificates run the script `sh create_new_certificate_teamdigitale.sh`. It will ask your username to connect to edge1 and renew the certificates.
This script does the following steps:

In general, the script executes the following steps:
 * Delete the pre-existing Kubernetes Ingress controller
 * Open a ssh session to the server to be validated
 * Launch a docker run command to generate the certificates through Certbot
 * Delete the pre-existings secret on kubernetes
 * Create the new secret with the fresh certificates
 * Recreate the Kubernetes Ingress controller

### Renew the certificates [NOT Sure Revised by Fabio Fumarola but now sure if it works]

1. execute the script `./scripts/renew_certificate.sh`


## Notes ##

The final domain used for the DAF will be ``teamdigitale.governo.it``, but while in preview version, it is exposed under the ``teamdigitale.it`` domain.

For this reason there are two different scripts to generate the certificates and two different Kubernets secrets.

Both the scripts have been run and the certificates are stored in two different folders in Glusterfs.

Everything is summarized in the following table:

| status       | domain                      | script                                     | secret                          | folder                                          |
|--------------|-----------------------------|--------------------------------------------|---------------------------------|-------------------------------------------------|
| Alpha / Beta | ``teamdigitale.it``         | ``create_new_certificate_teamdigitale.sh`` | ``tls-daf-teamdigitale-secret`` | ``/glusterfs/volume1/certbot/confTeamdigitale`` |
| Stable       | ``teamdigitale.governo.it`` | ``create_new_certificate.sh``              | ``tls-daf-secret``              | ``/glusterfs/volume1/certbot/conf``             |
|              |                             |                                            |                                 |                                                 |


## Certificate authority ##

DAF uses [Let’s Encrypt](https://letsencrypt.org "Let’s Encrypt") as Certificate Authority and [Certbot](https://certbot.eff.org) as ACME client to generate and deploy the certificates.
