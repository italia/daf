#!/usr/bin/env bash

kubectl delete -f ./../../ingress/nginx-ingress-controller.yml

ssh root@edge1  \
    docker run -v /glusterfs/volume1/certbot/conf/:/etc/letsencrypt/ --network host -t 10.98.74.120:5000/daf-certbot:1.0.0 \
    certbot certonly --standalone --preferred-challenges tls-sni  --register-unsafely-without-email --agree-tos -d daf.teamdigitale.governo.it,daf-superset.teamdigitale.governo.it

kubectl delete secret tls-daf-secret

ssh root@edge1 \
    kubectl create secret tls tls-daf-secret \
        --cert=/glusterfs/volume1/certbot/conf/live/daf.teamdigitale.governo.it/fullchain.pem \
        --key=/glusterfs/volume1/certbot/conf/live/daf.teamdigitale.governo.it/privkey.pem

kubectl create -f ./../../ingress/nginx-ingress-controller.yml
