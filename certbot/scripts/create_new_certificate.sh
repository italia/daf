#!/usr/bin/env bash

kubectl delete -f ./../../ingress/nginx-ingress-controller.yml

ssh root@edge1  \
    docker run -v /glusterfs/volume1/certbot/conf/:/etc/letsencrypt/ --network host -t 10.98.74.120:5000/daf-certbot:1.0.0 \
    certbot certonly --standalone --preferred-challenges tls-sni  --register-unsafely-without-email --agree-tos -d daf-superset.teamdigitale.governo.it

kubectl delete secret tls-secret

ssh root@edge1 \
    kubectl create secret tls tls-secret \
        --cert=/glusterfs/volume1/certbot/conf/live/daf-superset.teamdigitale.governo.it/fullchain.pem \
        --key=/glusterfs/volume1/certbot/conf/live/daf-superset.teamdigitale.governo.it/privkey.pem

kubectl create -f ./../../ingress/nginx-ingress-controller.yml
