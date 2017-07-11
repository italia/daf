FROM ckan/ckan:latest

RUN ckan-pip install -e git+https://github.com/ckan/ckanext-harvest.git#egg=ckanext-harvest

RUN ckan-pip install -e git+https://github.com/ckan/ckanext-dcat.git#egg=ckanext-dcat

RUN ckan-pip install -e git+https://github.com/geosolutions-it/ckanext-dcatapit.git#egg=ckanext-dcatapit

RUN ckan-pip install rdflib

RUN ckan-pip install geomet

RUN apt-get install -y vim

ADD ckan.ini /etc/ckan/default/ckan.ini

#RUN ckan-paster --plugin=ckanext-dcatapit vocabulary initdb --config=/etc/ckan/default/ckan.ini

#RUN ckan-paster --plugin=ckanext-dcatapit vocabulary load --url http://publications.europa.eu/mdr/resource/authority/language/skos/languages-skos.rdf --name languages --config=/etc/ckan/default/ckan.ini

#RUN ckan-paster --plugin=ckanext-dcatapit vocabulary load --url http://publications.europa.eu/mdr/resource/authority/data-theme/skos/data-theme-skos.rdf --name eu_themes --config=/etc/ckan/default/ckan.ini

#RUN ckan-paster --plugin=ckanext-dcatapit vocabulary load --url http://publications.europa.eu/mdr/resource/authority/place/skos/places-skos.rdf --name places --config=/etc/ckan/default/development.ini

#RUN ckan-paster --plugin=ckanext-dcatapit vocabulary load --url http://publications.europa.eu/mdr/resource/authority/frequency/skos/frequencies-skos.rdf --name frequencies --config=/etc/ckan/default/development.ini

#RUN ckan-paster --plugin=ckanext-dcatapit vocabulary load --url http://publications.europa.eu/mdr/resource/authority/file-type/skos/filetypes-skos.rdf --name filetype --config=/etc/ckan/default/development.ini
