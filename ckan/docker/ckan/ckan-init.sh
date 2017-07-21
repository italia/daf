#!/bin/bash

# Add user "ckanadmin" with password "ckanpassword". Add user "ckanadmin" to sysadmin group. Change password at first login.
#paster --plugin=ckan user remove ckanadmin --config /etc/ckan/default/ckan.ini
paster --plugin=ckan user add ckanadmin email=admin@mail.com password=ckanpassword --config /etc/ckan/default/ckan.ini
paster --plugin=ckan sysadmin add ckanadmin --config /etc/ckan/default/ckan.ini

# Vocabulary Load
paster --plugin=ckanext-dcatapit vocabulary load --url http://publications.europa.eu/mdr/resource/authority/language/skos/languages-skos.rdf --name languages --config /etc/ckan/default/ckan.ini
paster --plugin=ckanext-dcatapit vocabulary load --url http://publications.europa.eu/mdr/resource/authority/data-theme/skos/data-theme-skos.rdf --name eu_themes --config /etc/ckan/default/ckan.ini
paster --plugin=ckanext-dcatapit vocabulary load --url http://publications.europa.eu/mdr/resource/authority/place/skos/places-skos.rdf --name places --config /etc/ckan/default/ckan.ini
paster --plugin=ckanext-dcatapit vocabulary load --url http://publications.europa.eu/mdr/resource/authority/frequency/skos/frequencies-skos.rdf --name frequencies --config /etc/ckan/default/ckan.ini
paster --plugin=ckanext-dcatapit vocabulary load --url http://publications.europa.eu/mdr/resource/authority/file-type/skos/filetypes-skos.rdf --name filetype --config /etc/ckan/default/ckan.ini
