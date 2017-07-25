[![Build Status](https://travis-ci.org/geosolutions-it/ckanext-dcatapit.svg?branch=master)](https://travis-ci.org/geosolutions-it/ckanext-dcatapit)
[![Code Coverage](https://codecov.io/github/geosolutions-it/ckanext-dcatapit/coverage.svg?branch=master)](https://codecov.io/github/geosolutions-it/ckanext-dcatapit?branch=master)

# ckanext-dcatapit
CKAN extension for the Italian Open Data Portals (DCAT_AP-IT).

## Contents

- [Overview](#overview)
- [License](#license)
- [Demo Instance](#demo-instance)
- [Requirements](#requirements)
- [Installation](#installation)
- [Development Installation](#development-installation)
- [Running the Tests](#running-the-tests)
- [DCAT_AP-IT CSW Harvester](#dcat_ap-it-csw-harvester)
    - [CSW Metadata Guidelines](#csw-metadata-guidelines)
- [Test Instance and Validation](#test-instance-and-validation)
- [Extending the package schema in your own extension](#extending-the-package-schema-in-your-own-extension)
- [Managing translations](#managing-translations)
    - [Creating a new translation](#creating-a-new-translation)
    - [Updating an existing translation](#updating-an-existing-translation)
- [Contributing](#contributing)
- [Support, Communication and Credits](#support-communication-and-credits)

## License

**ckanext-dcatapit** is Free and Open Source software and is licensed under the GNU Affero General Public License (AGPL) v3.0 whose full text may be found at:

http://www.fsf.org/licensing/licenses/agpl-3.0.html

## Demo Instance

A demo instance can be found [here](http://dcatapit.geo-solutions.it/).

## Overview 

This extension provides plugins that allow CKAN to expose and consume metadata from other catalogs using RDF documents serialized according to the [Italian DCAT Application Profile](http://www.dati.gov.it/content/dcat-ap_it_v10). The Data Catalog Vocabulary (DCAT) is "an RDF vocabulary designed to facilitate interoperability between data catalogs published on the Web".

## Requirements

The ckanext-dcatapit extension has been developed for CKAN 2.4 or later and is based on the [ckanext-dcat plugin](https://github.com/ckan/ckanext-dcat).

In order to use the dcatapit CSW harvester functionalities, you need to install also the [ckanext-spatial](https://github.com/ckan/ckanext-spatial) extension.

If you want to manage localized fields, the ckanext-dcatapit extension requires also the [ckanext-multilang plugin](https://github.com/geosolutions-it/ckanext-multilang/tree/ckan-2.5.2) installed (see the [WIKI](https://github.com/geosolutions-it/ckanext-multilang/wiki) for more details about that).

## Installation

1. (**Optional**) Install the **ckanext-multilang** extension as described [here](https://github.com/geosolutions-it/ckanext-multilang/blob/master/README.rst#installation). Install this extension only if you need to manage multilingual fields for dataset, groups, tags and organizations

2. (**Optional**) Install the **ckanext-spatial** extension as described [here](https://github.com/ckan/ckanext-spatial). Install this extension only if you need to use the dcatapit CSW harvester (see below).

3. Install the **ckanext-dcat** extension as described [here](https://github.com/ckan/ckanext-dcat/blob/master/README.md#installation).

4. Activate your CKAN virtual environment, for example:

     `. /usr/lib/ckan/default/bin/activate`
     
5. Go into your CKAN path for extension (like /usr/lib/ckan/default/src):

    `git clone https://github.com/geosolutions-it/ckanext-dcatapit.git`
    
    `cd ckanext-dcatapit`
    
    `pip install -e .`

6. Add ``dcatapit_pkg`` and ``dcatapit_org`` and ``dcatapit_config`` to the ``ckan.plugins`` setting in your CKAN
   config file (by default the config file is located at ``/etc/ckan/default/production.ini``).

   * **dcatapit_pkg**: extends the package schema allowing to edit and visualize extra fields according to the DCAT_AP-IT specs.
   * **dcatapit_org**: extends the organization schema allowing to edit and visualize extra fields according to the DCAT_AP-IT specs.
   * **dcatapit_config**: extends the admin configuration schema allowing to edit and visualize extra fields according to the DCAT_AP-IT specs.

    The ckanext-dcatapit allows to localize the package fields (eg. title, description etc.) according to the schema definition, but to
    do that requires the ckanext-multilang installed.

7. In order to enable also the RDF harvester add ``dcatapit_harvester`` to the ``ckan.plugins`` setting in your CKAN. 
The ckanext-dcatapit RDF harvester also harvests localized fields in multiple languages, but to do that requires the ckanext-multilang installed.

8. In order to enable also the CSW harvester add ``dcatapit_csw_harvester`` to the ``ckan.plugins`` setting in your CKAN.

9. Enable the dcatapit profile adding the following configuration property in the ``production.ini`` file:

    `ckanext.dcat.rdf.profiles = euro_dcat_ap it_dcat_ap`

10. Configure the CKAN base URI as reported in the [dcat documentation](https://github.com/ckan/ckanext-dcat/blob/master/README.md#uris):
    `ckanext.dcat.base_uri = YOUR_BASE_URI`

11. Initialize the CKAN DB with the mandatory table needed for localized vocabulary voices:

    `paster --plugin=ckanext-dcatapit vocabulary initdb --config=/etc/ckan/default/production.ini`

12. Restart CKAN. For example if you've deployed CKAN with Apache on Ubuntu:

     `sudo service apache2 reload`
     
13. The EU controlled vocabularies must be populated before start using the dcatapit plugin. Execute in sequence these commands:

    `paster --plugin=ckanext-dcatapit vocabulary load --url http://publications.europa.eu/mdr/resource/authority/language/skos/languages-skos.rdf --name languages --config=/etc/ckan/default/production.ini`
    
    `paster --plugin=ckanext-dcatapit vocabulary load --url http://publications.europa.eu/mdr/resource/authority/data-theme/skos/data-theme-skos.rdf --name eu_themes --config=/etc/ckan/default/production.ini`
    
    `paster --plugin=ckanext-dcatapit vocabulary load --url http://publications.europa.eu/mdr/resource/authority/place/skos/places-skos.rdf --name places --config=/etc/ckan/default/production.ini`
    
    `paster --plugin=ckanext-dcatapit vocabulary load --url http://publications.europa.eu/mdr/resource/authority/frequency/skos/frequencies-skos.rdf --name frequencies --config=/etc/ckan/default/production.ini`
    
    `paster --plugin=ckanext-dcatapit vocabulary load --url http://publications.europa.eu/mdr/resource/authority/file-type/skos/filetypes-skos.rdf  --name filetype --config=/etc/ckan/default/production.ini`
    
## Development Installation

To install `ckanext-dcatapit` for development, activate your CKAN virtualenv and do:

    git clone https://github.com/geosolutions-it/ckanext-dcatapit.git
    
    cd ckanext-dcatapit
    
    python setup.py develop

    pip install -r dev-requirements.txt
    
## Running the Tests

To prepare the environment for running tests, follow instructions reported [here](http://docs.ckan.org/en/latest/contributing/test.html)
Then initialize the test database:

    cd /usr/lib/ckan/default/src/ckan
	paster db init -c test-core.ini

Finally to run tests, do:

    cd /usr/lib/ckan/default/src/ckanext-dcatapit
    
    . /usr/lib/ckan/default/bin/activate
    
    nosetests --ckan --nologcapture --with-pylons=test.ini --with-coverage --cover-package=ckanext.dcatapit --cover-inclusive --cover-erase --cover-tests ckanext/dcatapit

## DCAT_AP-IT CSW Harvester
The ckanext-dcatapit extension provides also a CSW harvester built on the **ckanext-spatial** extension, and inherits all of its functionalities. With this harvester you can harvest dcatapit dataset fields from the ISO metadata. The CSW harvester uses a default configuration usefull for populating mandatory fields into the source metadata, this json configuration can be customized into the harvest source form (please see the default one [here](https://github.com/geosolutions-it/ckanext-dcatapit/blob/master/ckanext/dcatapit/harvesters/csw_harvester.py#L54)). Below an example of the available configuration properties (for any configuration property not specified, the default one will be used):

    {
       "dcatapit_config":{
          "dataset_themes":"OP_DATPRO",
          "dataset_places":"ITA_BZO",
          "dataset_languages":"{ITA,DEU}",
          "frequency":"UNKNOWN",
          "agents":{
             "publisher":{
                "code":"p_bz",
                "role":"publisher",
                "code_regex":{
                   "regex":"\\(([^)]+)\\:([^)]+)\\)",
                   "groups":[2]
                },
                "name_regex":{
                   "regex":"([^(]*)(\\(IPa[^)]*\\))(.+)",
                   "groups":[1, 3]
                }
             },
             "owner":{
                "code":"p_bz",
                "role":"owner",
                "code_regex":{
                   "regex":"\\(([^)]+)\\:([^)]+)\\)",
                   "groups":[2]
                },
                "name_regex":{
                   "regex":"([^(]*)(\\(IPa[^)]*\\))(.+)",
                   "groups":[1, 3]
                }
             },
             "author":{
                "code":"p_bz",
                "role":"author",
                "code_regex":{
                   "regex":"\\(([^)]+)\\:([^)]+)\\)",
                   "groups":[2]
                },
                "name_regex":{
                   "regex":"([^(]*)(\\(IPa[^)]*\\))(.+)",
                   "groups":[1, 3]
                }
             }
          },
          "controlled_vocabularies":{
             "dcatapit_skos_theme_id":"theme.data-theme-skos",
             "dcatapit_skos_places_id":"theme.places-skos"
          }
       }
    }

* ``dataset_themes``: default value to use for the dataset themes field if the thesaurus keywords are missing in the ISO metadata. The source metadata should have thesaurus keywords from the EU controlled vocabulary (data-theme-skos.rdf). Multiple values must be set between braces and comma separated values.

* ``dataset_places``: default value to use for the dataset geographical name field if the thesaurus keywords are missing in the ISO metadata. The source metadata should have thesaurus keywords from the EU controlled vocabulary (places-skos.rdf). Multiple values must be set between braces and comma separated values.

* ``dataset_languages``: default value to use for the dataset languages field. Metadata languages are harvested by the che ckanext-spatial extension (see the 'dataset-language' in iso_values). Internally the harvester map the ISO languages to the mdr vocabulary languages. The default configuration for that can be overridden in harvest source configuration by using an additional configuration property, like:

        "mapping_languages_to_mdr_vocabulary": {
            "ita': "ITA",
            "ger': "DEU",
            "eng': "ENG"
        }
        
* ``frequency``: default value to use for the dataset frequency field. Metadata frequencies are harvested by the che ckanext-spatial extension (see the 'frequency-of-update' in iso_values). Internally the harvester automatically map the ISO frequencies to the mdr vocabulary frequencies.

* ``agents``: Configuration for harvesting the dcatapit dataset agents from the responsible party metadata element. Below more details on the agent configuration:

         "publisher":{
            "code":"p_bz",      --> the IPA/IVA code to use as default for the agent identifier
            "role":"publisher", --> the responsible party role to harvest for this agent
            "code_regex":{      --> a regular expression to extrapolate a substring from the responsible party organization name
               "regex":"\\(([^)]+)\\:([^)]+)\\)",
               "groups":[2]     --> optional, dependes by the regular expression
            },
            "name_regex":{      --> a regular expression to extrapolate the IPA/IVA code from the responsible party organization name
               "regex":"([^(]*)(\\(IPA[^)]*\\))(.+)",
               "groups":[1, 3]  --> optional, dependes by the regular expression
            }
         }
     
* ``controlled_vocabularies``: To harvest 'dataset_themes' and 'dataset_places' the harvester needs to know the thesaurus ID or TITLE as specified into the source metadata.

**NOTES**: The default IPA code to use is extrapolated by the metadata identifier in respect to the RNDT specifications (ipa_code:UUID).
This represents a last fallback if the agent regex does not match any code and if the agent code has not been specified in configuration.

#### Harvest source configuration
In order to set the dcatapit CSW harvester:

1. Specify a valid csw endpoint in the URL field 
2. Specify a title and a description for the harvest source
3. Select 'DCAT_AP-IT CSW Harvester' as source type
4. Provide your own configuration to override the default one

### CSW Metadata Guidelines

* The dataset unique identifier will be harvested from the metadata fileIdentifier (see the above paragraph for additional notes about the IPA code).

* In order to harvest dcatapit dataset themes, the source metadata should have thesaurus keywords from the EU controlled vocabulary (data-theme-skos.rdf). Then the thesaurus identifier or title must be specified into the controlled_vocabularies->dcatapit_skos_theme_id configuration property

* In order to harvest dcatapit dataset geographical names, the source metadata should have thesaurus keywords from the EU controlled vocabulary (places-skos.rdf). Then the thesaurus identifier or title must be specified into the controlled_vocabularies->dcatapit_skos_places_id configuration property

* The dcatapit agents (publisher, holder, creator) will be harvested from the responsible party with the role specified in configuration (see 'agents' configuration property explained above)

* The dataset languages are harvested using the xpaths reported [here](https://github.com/ckan/ckanext-spatial/blob/master/ckanext/spatial/model/harvested_metadata.py#L723)

* The dataset frequency of update is harvested using the xpath reported [here](https://github.com/ckan/ckanext-spatial/blob/master/ckanext/spatial/model/harvested_metadata.py#L597)

## Test Instance and Validation

We have a test instance available at [this link](http://dcatapit.geo-solutions.it/) with a few sample datasets; a specific test dataset is available [here](http://dcatapit.geo-solutions.it/dataset/dcatapit-test-dataset).

If you want to test validation please use the online validator available [here](http://52.50.205.146:3031/dcat-ap_validator.html). To get no errors you should validate the entire catalog (e.g. [this link](http://dcatapit.geo-solutions.it/catalog.rdf)) if you validate a single dataset (e.g. using [this link](http://dcatapit.geo-solutions.it/dataset/dcatapit-test-dataset.rdf)) you will always get an error for "...missing catalog...".

## Extending the package schema in your own extension

The dcatapit extension allows to define additional custom fields to the package schema by implementing the `ICustomSchema` interface 
in you CKAN extension. Below a sample:

    class ExamplePlugin(plugins.SingletonPlugin):

        # ICustomSchema
        plugins.implements(interfaces.ICustomSchema)

        def get_custom_schema(self):
            return [
                {
                    'name': 'custom_text',
                    'validator': ['ignore_missing'],
                    'element': 'input',
                    'type': 'text',
                    'label': _('Custom Text'),
                    'placeholder': _('custom texte here'),
                    'is_required': False,
                    'localized': False
                }
            ]

Through this an additional schema field named `custom_text` will be added to the package schema and automatically managed by the dcatapit extension. Below a brief description of the fields properties that can be used:

* ``name``: the name of the field
* ``validator``: array of validators to use for the field
* ``element``: the element type to use into the package edit form (ie. see the available ckan macros or macros defined into the dcatapit extension [here](https://github.com/geosolutions-it/ckanext-dcatapit/blob/master/ckanext/dcatapit/templates/macros/dcatapit_form_macros.html)
* ``type``: the type of input eg. email, url, date (default: text)
* ``label``: the human readable label
* ``placeholder``: some placeholder text
* ``is_required``: boolean of whether this input is requred for the form to validate
* ``localized``: True to enable the field localization by the dcatapit extension (default False). This need the ckanext-multilang installed.

## Managing translations

The dcatapit extension implements the ITranslation CKAN's interface so the translations procedure of the GUI elements is automatically covered using the translations files provided in the i18n directory.

    Pay attention that the usage of the ITranslation interface can work only in CKAN 2.5 
    or later, if you are using a minor version of CKAN the ITranslation's implementation 
    will be ignored.

### Creating a new translation

To create a new translation proceed as follow:

1. Extract new messages from your extension updating the pot file

     `python setup.py extract_messages`
     
2.  Create a translation file for your language (a po file) using the existing pot file in this plugin

     `python setup.py init_catalog --locale YOUR_LANGUAGE`

     Replace YOUR_LANGUAGE with the two-letter ISO language code (e.g. es, de).
     
3. Do the translation into the po file

4. Once the translation files (po) have been updated, either manually or via Transifex, compile them by running:

     `python setup.py compile_catalog --locale YOUR_LANGUAGE`
     
### Updating an existing translation

In order to update the existing translations proceed as follow:

1. Extract new messages from your extension updating the pot file

     `python setup.py extract_messages`
     
2. Update the strings in your po file, while preserving your po edits, by doing:

     `python setup.py update_catalog --locale YOUR-LANGUAGE`

3. Once the translation files (po) have been updated adding the new translations needed, compile them by running:

     `python setup.py compile_catalog --locale YOUR_LANGUAGE`

## Contributing

We welcome contributions in any form:

* pull requests for new features
* pull requests for bug fixes
* pull requests for documentation
* funding for any combination of the above

## Support, Communication and Credits
This work has been performed by [GeoSolutions](http://www.geo-solutions.it) with funding provided by Provincia Autonoma di Trento and Provincia Autonoma di Bolzano in a joint effort.

The work is provided as is and no warranty whatsoever is provided. There is a public Google Group for support questions available [here](https://groups.google.com/d/forum/ckanext-dcatapit) but beware that support through this channel will be given on a best effort basis.

Professional Support is available through our [Enterprise Support Services](http://www.geo-solutions.it/enterprise-support-services) offer. 
