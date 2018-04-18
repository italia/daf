#!/usr/bin/env bash

echo "Please enter your username: "
read -sr OS_USER

kubectl delete -f ./../../ingress/nginx-ingress-controller.yml

ssh $OS_USER@edge1 \
    sudo docker run -v /glusterfs/volume1/certbot/confTeamdigitale/:/etc/letsencrypt/ --network host -i -t 10.98.74.120:5000/daf-certbot:1.0.0 certbot renew

kubectl delete secret tls-daf-teamdigitale-secret

ssh $OS_USER@edge1 \
    sudo kubectl create secret tls tls-daf-teamdigitale-secret \
        --cert=/glusterfs/volume1/certbot/confTeamdigitale/live/daf.teamdigitale.it/fullchain.pem \
        --key=/glusterfs/volume1/certbot/confTeamdigitale/live/daf.teamdigitale.it/privkey.pem

kubectl create -f ./../../ingress/nginx-ingress-controller.yml
