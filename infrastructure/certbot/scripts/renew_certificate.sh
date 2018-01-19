#!/usr/bin/env bash

echo "Please enter your username: "
read -sr OS_USER

ssh $OS_USER@edge1 \
    sudo docker run -v /glusterfs/volume1/certbot/conf/:/etc/letsencrypt/ --network host -i -t 10.98.74.120:5000/daf-certbot:1.0.0 certbot renew

kubectl delete secret tls-secret

ssh $OS_USER@edge1 \
    sudo kubectl create secret tls tls-secret \
        --cert=/glusterfs/volume1/certbot/conf/live/daf-superset.teamdigitale.governo.it/fullchain.pem \
        --key=/glusterfs/volume1/certbot/conf/live/daf-superset.teamdigitale.governo.it/privkey.pem
