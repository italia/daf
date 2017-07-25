#!/bin/sh -e

nosetests --ckan --nologcapture --with-pylons=subdir/test.ini --with-coverage --cover-package=ckanext.dcatapit --cover-inclusive --cover-erase --cover-tests ckanext/dcatapit