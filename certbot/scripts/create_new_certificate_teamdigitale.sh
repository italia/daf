#!/usr/bin/env bash

kubectl delete -f ./../../ingress/nginx-ingress-controller.yml

ssh root@edge1  \
    docker run -v /glusterfs/volume1/certbot/confTeamdigitale/:/etc/letsencrypt/ --network host -t 10.98.74.120:5000/daf-certbot:1.0.0 \
    certbot certonly --standalone --preferred-challenges tls-sni  --register-unsafely-without-email --agree-tos -d daf.teamdigitale.it,datascience.daf.teamdigitale.it,graph.daf.teamdigitale.it,dataportal-private.daf.teamdigitale.it,dataportal.daf.teamdigitale.it,api.daf.teamdigitale.it,bi.daf.teamdigitale.it,daf-superset.teamdigitale.it

kubectl delete secret tls-daf-secret

ssh root@edge1 \
    kubectl create secret tls tls-daf-teamdigitale-secret \
        --cert=/glusterfs/volume1/certbot/confTeamdigitale/live/daf.teamdigitale.it/fullchain.pem \
        --key=/glusterfs/volume1/certbot/confTeamdigitale/live/daf.teamdigitale.it/privkey.pem

kubectl create -f ./../../ingress/nginx-ingress-controller.yml
