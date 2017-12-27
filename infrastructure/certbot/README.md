# Digital Certificates Documentation #

This documentation explains how to generate and install the digital certificates needed to operate the public DAF endpoints under HTTPS

## Certificate authority ##

DAF uses [Let’s Encrypt](https://letsencrypt.org "Let’s Encrypt") as Certificate Authority and [Certbot](https://certbot.eff.org) as ACME client to generate and deploy the certificates.

## HowTo ##

Certbot needs to be launched from the server associated to the domain to validate.

To ease this activity, a Docker image has been created containing the Certbot tool and a Bash script is used to automate the operation.

The script executes the following steps:
 * Delete the pre-existing Kubernetes Ingress controller
 * Open a ssh session to the server to be validated
 * Launch a docker run command to generate the certificates through Certbot
 * Delete the pre-existings secret on kubernetes
 * Create the new secret with the fresh certificates
 * Recreate the Kubernetes Ingress controller

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

Given that certificates are already generated and stored in Kubernetes, when the domain need to be changed the following steps will be needed:
  * Delete the preexisting Kubernetes Ingress controller
  * Adapt the ``daf/ingress/nginx-ingress-controller.yml`` file changing the secret name to be used (at the file bottom)
  * Recreate the Kubernetes Ingress controller
