
=================
ckanext-multilang
=================

The ckanext-multilang CKAN's extension provides a way to localize your CKAN's title and description 
contents for: Dataset, Resources, Tags, Organizations and Groups. This extension creates some new DB tables for this purpose 
containing localized contents in base of the configured CKAN's locales in configuration (the production.ini file).
So,  accessing the CKAN's GUI in 'en', for example, the User can create a new Dataset and automatically new localized records 
for that language will be created  in the multilang tables. In the same way, changing the GUI's language, from the CKAN's language 
dropdown, the user will be able to edit again the same Dataset in order to specify 'title' and 'description' of the Dataset for the 
new selected language.
In this way Dataset's title and description will automatically change simply switching the language from the CKAN's dropdonw.
 
The ckanext-multilang provides also an harvester built on top of the ckanext-spatial extension, and inherits all of its functionalities.
With this harvester, localized content for Dataset in CKAN can be retrieved form CSW metadata that contains the gmd:PT_FreeText XML 
element (see the `WIKI <https://github.com/geosolutions-it/ckanext-multilang/wiki>`_ for more details).	

----
WIKI
----

The WIKI page of this CKAN extension can be found `here <https://github.com/geosolutions-it/ckanext-multilang/wiki>`_ in this repository.

-------
License
-------

**ckanext-multilang** is Free and Open Source software and is licensed under the GNU Affero General Public License (AGPL) v3.0 whose full text may be found at:

http://www.fsf.org/licensing/licenses/agpl-3.0.html


------------
Requirements
------------

The ckanext-multilang extension has been developed for CKAN 2.4 or later. In addition:

* The CSW multilingual harvester provided by the ckanext-multilang extension requires the `ckanext-spatial plugin <https://github.com/ckan/ckanext-spatial>`_ installed on CKAN (see the `WIKI <https://github.com/geosolutions-it/ckanext-multilang/wiki>`_ for more details about that).

* The CSW multilingual harvester provided by the ckanext-multilang extension requires the `ckanext-geonetwork plugin <https://github.com/geosolutions-it/ckanext-geonetwork>`_ installed on CKAN if you want to leverage on the advanced harvesting functionalities (see the `WIKI <https://github.com/geosolutions-it/ckanext-multilang/wiki#features>`_ for more details about the multilang harvester).

------------
Installation
------------

To install ckanext-multilang:


1. Activate your CKAN virtual environment, for example::

     . /usr/lib/ckan/default/bin/activate
     
2. Go into your CKAN path for extension (like /usr/lib/ckan/default/src)::

    git clone https://github.com/geosolutions-it/ckanext-multilang.git
    
    cd ckanext-multilang
    
    pip install -e .

3. Initialize the DB with the mandatory Tables needed for localized records::

      paster --plugin=ckanext-multilang multilangdb initdb --config=/etc/ckan/default/production.ini

4. Add ``multilang`` and ``multilang_harvester`` to the ``ckan.plugins`` setting in your CKAN
   config file (by default the config file is located at ``/etc/ckan/default/production.ini``).
   
5. Update the Solr schema.xml file used by CKAN introducing the following elements.
   
   **Inside the 'fields' Tag**::
   
          <dynamicField name="package_multilang_localized_*" type="text" indexed="true" stored="true" multiValued="false"/>
   
   **A new 'copyField' to append**::
   
          <copyField source="package_multilang_localized_*" dest="text"/>
      

6. Restart Solr.

7. Restart CKAN. For example if you've deployed CKAN with Apache on Ubuntu::

     sudo service apache2 reload

------------------------
Development Installation
------------------------

To install `ckanext-multilang` for development, activate your CKAN virtualenv and do::

    git clone https://github.com/geosolutions-it/ckanext-multilang.git
    
    cd ckanext-multilang
    
    python setup.py develop

    pip install -r dev-requirements.txt

------------
Contributing
------------

We welcome contributions in any form:

* pull requests for new features
* pull requests for bug fixes
* pull requests for documentation
* funding for any combination of the above

--------------------
Professional Support
--------------------

The **ckanext-multilang** is provided as is and no warranty whatsoever is provided. Professional Support is available through our `Enterprise Support Services <http://www.geo-solutions.it/enterprise-support-services>`_ offer.
