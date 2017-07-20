
import ast
import logging
import datetime

from pylons import config

from rdflib.namespace import Namespace, RDF, SKOS
from rdflib import URIRef, BNode, Literal

import ckan.logic as logic

from ckanext.dcat.profiles import RDFProfile, DCAT, LOCN, VCARD, DCT, FOAF, ADMS
from ckanext.dcat.utils import catalog_uri, dataset_uri, resource_uri

import ckanext.dcatapit.interfaces as interfaces
import ckanext.dcatapit.helpers as helpers


DCATAPIT = Namespace('http://dati.gov.it/onto/dcatapit#')

it_namespaces = {
    'dcatapit': DCATAPIT,
}

THEME_BASE_URI = 'http://publications.europa.eu/resource/authority/data-theme/'
LANG_BASE_URI = 'http://publications.europa.eu/resource/authority/language/'
FREQ_BASE_URI = 'http://publications.europa.eu/resource/authority/frequency/'
FORMAT_BASE_URI = 'http://publications.europa.eu/resource/authority/file-type/'
GEO_BASE_URI = 'http://publications.europa.eu/resource/authority/place/'

# vocabulary name, base URI
THEME_CONCEPTS = ('eu_themes', THEME_BASE_URI)
LANG_CONCEPTS = ('languages', LANG_BASE_URI)
GEO_CONCEPTS = ('places', GEO_BASE_URI)
FREQ_CONCEPTS = ('frequencies', FREQ_BASE_URI)
FORMAT_CONCEPTS = ('filetype', FORMAT_BASE_URI)

DEFAULT_VOCABULARY_KEY = 'OP_DATPRO'
DEFAULT_THEME_KEY = DEFAULT_VOCABULARY_KEY
DEFAULT_FORMAT_CODE = DEFAULT_VOCABULARY_KEY
DEFAULT_FREQ_CODE = 'UNKNOWN'

LOCALISED_DICT_NAME_BASE = 'DCATAPIT_MULTILANG_BASE'
LOCALISED_DICT_NAME_RESOURCES = 'DCATAPIT_MULTILANG_RESOURCES'

lang_mapping_ckan_to_voc = {
    'it': 'ITA',
    'de': 'DEU',
    'en': 'ENG',
    'en_GB': 'ENG',
}

lang_mapping_xmllang_to_ckan = {
    'it' : 'it',
    'de' : 'de',
    'en' : 'en_GB' ,
}

format_mapping = {
    'WMS': 'MAP_SRVC',
    'HTML': 'HTML_SIMPL',
    'CSV': 'CSV',
    'XLS': 'XLS',
    'ODS': 'ODS',
    'ZIP': 'OP_DATPRO', # requires to be more specific, can't infer

}


log = logging.getLogger(__name__)

class ItalianDCATAPProfile(RDFProfile):
    '''
    An RDF profile for the Italian DCAT-AP recommendation for data portals
    It requires the European DCAT-AP profile (`euro_dcat_ap`)
    '''

    def parse_dataset(self, dataset_dict, dataset_ref):

        # check the dataset type
        if (dataset_ref, RDF.type, DCATAPIT.Dataset) not in self.g:
            # not a DCATAPIT dataset
            return dataset_dict

        # date info
        for predicate, key, logf in (
                (DCT.issued, 'issued', log.debug),
                (DCT.modified, 'modified', log.warn),
                ):
            value = self._object_value(dataset_ref, predicate)
            if value:
                self._remove_from_extra(dataset_dict, key)

                value = helpers.format(value, '%Y-%m-%d', 'date')
                dataset_dict[key] = value
            else:
                logf('No %s found for dataset "%s"', predicate, dataset_dict.get('title', '---'))

        # 0..1 predicates
        for predicate, key, logf in (
                (DCT.identifier, 'identifier', log.warn),
                ):
            value = self._object_value(dataset_ref, predicate)
            if value:
                self._remove_from_extra(dataset_dict, key)
                dataset_dict[key] = value
            else:
                logf('No %s found for dataset "%s"', predicate, dataset_dict.get('title', '---'))

        # 0..n predicates list
        for predicate, key, logf in (
                (ADMS.identifier, 'alternate_identifier', log.debug),
                (DCT.isVersionOf, 'is_version_of', log.debug),
                ):
            valueList = self._object_value_list(dataset_ref, predicate)
            if valueList:
                self._remove_from_extra(dataset_dict, key)
                value = ','.join(valueList)
                dataset_dict[key] = value
            else:
                logf('No %s found for dataset "%s"', predicate, dataset_dict.get('title', '---'))

        # conformsTo
        self._remove_from_extra(dataset_dict, 'conforms_to')
        conform_list = []
        for conforms_to in self.g.objects(dataset_ref, DCT.conformsTo):
            conform_list.append(self._object_value(conforms_to, DCT.identifier))
        if conform_list:
            value = ','.join(conform_list)
            dataset_dict['conforms_to'] = value
        else:
            log.debug('No DCT.conformsTo found for dataset "%s"', dataset_dict.get('title', '---'))

        # Temporal
        start, end = self._time_interval(dataset_ref, DCT.temporal)
        for v, key, logf in (
                (start, 'temporal_start', log.debug),
                (end, 'temporal_end', log.debug),
                ):
            if v:
                self._remove_from_extra(dataset_dict, key)

                value = helpers.format(v, '%Y-%m-%d', 'date')
                dataset_dict[key] = value
            else:
                log.warn('No %s Date found for dataset "%s"', key, dataset_dict.get('title', '---'))

        # URI 0..1
        for predicate, key, base_uri in (
                (DCT.accrualPeriodicity, 'frequency', FREQ_BASE_URI),
                ):
            valueRef = self._object_value(dataset_ref, predicate)
            if valueRef:
                self._remove_from_extra(dataset_dict, key)
                value = self._strip_uri(valueRef, base_uri)
                dataset_dict[key] = value
            else:
                log.warn('No %s found for dataset "%s"', predicate, dataset_dict.get('title', '---'))

        # URI lists
        for predicate, key, base_uri in (
                (DCT.language, 'language', LANG_BASE_URI),
                (DCAT.theme, 'theme', THEME_BASE_URI),
                ):
            self._remove_from_extra(dataset_dict, key)
            valueRefList = self._object_value_list(dataset_ref, predicate)
            valueList = [self._strip_uri(valueRef, base_uri) for valueRef in valueRefList]
            value = ','.join(valueList)
            if len(valueList) > 1:
                value = '{'+value+'}'
            dataset_dict[key] = value

        # Spatial
        spatial_tags = []
        geonames_url = None

        for spatial in self.g.objects(dataset_ref, DCT.spatial):
           for spatial_literal in self.g.objects(spatial, DCATAPIT.geographicalIdentifier):
               spatial_value = spatial_literal.value
               if GEO_BASE_URI in spatial_value:
                   spatial_tags.append(self._strip_uri(spatial_value, GEO_BASE_URI))
               else:
                   if geonames_url:
                       log.warn("GeoName URL is already set to %s, value %s will not be imported", geonames_url, spatial_value)
                   else:
                       geonames_url = spatial_value

        if len(spatial_tags) > 0:
            value = ','.join(spatial_tags)
            if len(spatial_tags) > 1:
                value = '{'+value+'}'
            dataset_dict['geographical_name'] = value

        if geonames_url:
            dataset_dict['geographical_geonames_url'] = geonames_url

        ### Collect strings from multilang fields

        # { 'field_name': {'it': 'italian loc', 'de': 'german loc', ...}, ...}
        localized_dict = {}

        for key, predicate in (
                ('title', DCT.title),
                ('notes', DCT.description),
                ):
            self._collect_multilang_strings(dataset_dict, key, dataset_ref, predicate, localized_dict)

        # Agents
        for predicate, basekey in (
                (DCT.publisher, 'publisher'),
                (DCT.rightsHolder, 'holder'),
                (DCT.creator, 'creator'),
                ):
            agent_dict, agent_loc_dict = self._parse_agent(dataset_ref, predicate, basekey)
            for key,v in agent_dict.iteritems():
                self._remove_from_extra(dataset_dict, key)
                dataset_dict[key] = v
            localized_dict.update(agent_loc_dict)

        # when all localized data have been parsed, check if there really any and add it to the dict
        if len(localized_dict) > 0:
            log.debug('Found multilang metadata')
            dataset_dict[LOCALISED_DICT_NAME_BASE] = localized_dict

        ### Resources

        resources_loc_dict = {}

        # In ckan, the license is a dataset property, not resource's
        # We'll collect all of the resources' licenses, then we will postprocess them
        licenses = [] #  contains tuples (url, name)

        for resource_dict in dataset_dict.get('resources', []):
            resource_uri = resource_dict['uri']
            if not resource_uri:
                log.warn("URI not defined for resource %s", resource_dict['name'])
                continue

            distribution = URIRef(resource_uri)
            if not (dataset_ref, DCAT.distribution, distribution) in self.g:
                log.warn("Distribution not found in dataset %s", resource_uri)
                continue

            # URI 0..1
            for predicate, key, base_uri in (
                    (DCT['format'], 'format', FORMAT_BASE_URI), # Format
                    ):
                valueRef = self._object_value(distribution, predicate)
                if valueRef:
                    value = self._strip_uri(valueRef, base_uri)
                    resource_dict[key] = value
                else:
                    log.warn('No %s found for resource "%s"::"%s"',
                             predicate,
                             dataset_dict.get('title', '---'),
                             resource_dict.get('name', '---'))

            # License
            license = self._object(distribution, DCT.license)
            if license:
                # just add this info in the resource extras
                resource_dict['license_url'] = str(license)
                license_name = self._object_value(license, FOAF.name) # may be either the title or the id
                if(license_name):
                    # just add this info in the resource extras
                    resource_dict['license_name'] = license_name
                else:
                    license_name = "unknown"
                licenses.append((str(license), license_name))
            else:
                log.warn('No license found for resource "%s"::"%s"',
                         dataset_dict.get('title', '---'),
                         resource_dict.get('name', '---'))

            # Multilang
            loc_dict = {}

            for key, predicate in (
                    ('name', DCT.title),
                    ('description', DCT.description),
                    ):
                self._collect_multilang_strings(resource_dict, key, distribution, predicate, loc_dict)

            if len(loc_dict) > 0:
                log.debug('Found multilang metadata in resource %s', resource_dict['name'])
                resources_loc_dict[resource_uri] = loc_dict

        if len(resources_loc_dict) > 0:
            log.debug('Found multilang metadata in resources')
            dataset_dict[LOCALISED_DICT_NAME_RESOURCES] = resources_loc_dict

        # postprocess licenses
        # license_ids = {id for url,id in licenses}  # does not work in python 2.6
        license_ids = set()
        for url,id in licenses:
           license_ids.add(id)

        if license_ids:
            if len(license_ids) > 1:
                log.warn('More than one license found for dataset "%s"', dataset_dict.get('title', '---'))
            dataset_dict['license_id'] = license_ids.pop() # take a random one

        return dataset_dict

    def _collect_multilang_strings(self, base_dict, key, subj, pred, loc_dict):
        '''
        Search for multilang Literals matching (subj, pred).
        - Literals not localized will be stored as source_dict[key] -- possibly replacing the value set by the EURO parser
        - Localized literals will be stored into target_dict[key][lang]
        '''

        for obj in self.g.objects(subj, pred):
            value = obj.value
            lang = obj.language
            if not lang:
                # force default value in dataset
                base_dict[key] = value
            else:
                # add localized string
                lang_dict = loc_dict.setdefault(key, {})
                lang_dict[lang_mapping_xmllang_to_ckan.get(lang)] = value

    def _remove_from_extra(self, dataset_dict, key):

        #  search and replace
        for extra in dataset_dict.get('extras', []):
            if extra['key'] == key:
                dataset_dict['extras'].pop(dataset_dict['extras'].index(extra))
                return

    def _add_or_replace_extra(self, dataset_dict, key, value):

        #  search and replace
        for extra in dataset_dict.get('extras', []):
            if extra['key'] == key:
                extra['value'] = value
                return

        # add if not found
        dataset_dict['extras'].append({'key': key, 'value': value})

    def _parse_agent(self, subject, predicate, base_name):

        agent_dict = {}
        loc_dict= {}

        for agent in self.g.objects(subject, predicate):
            agent_dict[base_name + '_identifier'] = self._object_value(agent, DCT.identifier)
            self._collect_multilang_strings(agent_dict, base_name + '_name', agent, FOAF.name, loc_dict)

        return agent_dict, loc_dict

    def _strip_uri(self, value, base_uri):
        return value.replace(base_uri, '')


    def graph_from_dataset(self, dataset_dict, dataset_ref):

        title = dataset_dict.get('title')
        
        g = self.g

        for prefix, namespace in it_namespaces.iteritems():
            g.bind(prefix, namespace)

        ### add a further type for the Dataset node
        g.add((dataset_ref, RDF.type, DCATAPIT.Dataset))

        ### replace themes
        value = self._get_dict_value(dataset_dict, 'theme')
        if value:
            for theme in value.split(','):
                self.g.remove((dataset_ref, DCAT.theme, URIRef(theme)))
                theme = theme.replace('{','').replace('}','')
                self.g.add((dataset_ref, DCAT.theme, URIRef(THEME_BASE_URI + theme)))
                self._add_concept(THEME_CONCEPTS, theme)
        else:
                self.g.add((dataset_ref, DCAT.theme, URIRef(THEME_BASE_URI + DEFAULT_THEME_KEY)))
                self._add_concept(THEME_CONCEPTS, DEFAULT_THEME_KEY)

        ### replace languages
        value = self._get_dict_value(dataset_dict, 'language')
        if value:
            for lang in value.split(','):
                self.g.remove((dataset_ref, DCT.language, Literal(lang)))
                lang = lang.replace('{','').replace('}','')
                self.g.add((dataset_ref, DCT.language, URIRef(LANG_BASE_URI + lang)))
                # self._add_concept(LANG_CONCEPTS, lang)

        ### add spatial (EU URI)
        value = self._get_dict_value(dataset_dict, 'geographical_name')
        if value:
            for gname in value.split(','):
                gname = gname.replace('{','').replace('}','')

                dct_location = BNode()
                self.g.add((dataset_ref, DCT.spatial, dct_location))

                self.g.add((dct_location, RDF['type'], DCT.Location))

                # Try and add a Concept from the spatial vocabulary
                if self._add_concept(GEO_CONCEPTS, gname):
                    self.g.add((dct_location, DCATAPIT.geographicalIdentifier, Literal(GEO_BASE_URI + gname)))

                    # geo concept is not really required, but may be a useful adding
                    self.g.add((dct_location, LOCN.geographicalName, URIRef(GEO_BASE_URI + gname)))
                else:
                    # The dataset field is not a controlled tag, let's create a Concept out of the label we have
                    concept = BNode()
                    self.g.add((concept, RDF['type'], SKOS.Concept))
                    self.g.add((concept, SKOS.prefLabel, Literal(gname)))
                    self.g.add((dct_location, LOCN.geographicalName, concept))

        ### add spatial (GeoNames)
        value = self._get_dict_value(dataset_dict, 'geographical_geonames_url')
        if value:
            dct_location = BNode()
            self.g.add((dataset_ref, DCT.spatial, dct_location))

            self.g.add((dct_location, RDF['type'], DCT.Location))
            self.g.add((dct_location, DCATAPIT.geographicalIdentifier, Literal(value)))

        ### replace periodicity
        self._remove_node(dataset_dict, dataset_ref,  ('frequency', DCT.accrualPeriodicity, None, Literal))
        self._add_uri_node(dataset_dict, dataset_ref, ('frequency', DCT.accrualPeriodicity, DEFAULT_FREQ_CODE, URIRef), FREQ_BASE_URI)
        # self._add_concept(FREQ_CONCEPTS, dataset_dict.get('frequency', DEFAULT_VOCABULARY_KEY))

        ### replace landing page
        self._remove_node(dataset_dict, dataset_ref,  ('url', DCAT.landingPage, None, URIRef))
        landing_page_uri = None
        if dataset_dict.get('name'):
            landing_page_uri = '{0}/dataset/{1}'.format(catalog_uri().rstrip('/'), dataset_dict['name'])
        else:
            landing_page_uri = dataset_uri(dataset_dict)  # TODO: preserve original URI if harvested

        self.g.add((dataset_ref, DCAT.landingPage, URIRef(landing_page_uri)))

        ### conformsTo
        self.g.remove((dataset_ref, DCT.conformsTo, None))
        value = self._get_dict_value(dataset_dict, 'conforms_to')
        if value:
            for item in value.split(','):

                standard = BNode()
                self.g.add((dataset_ref, DCT.conformsTo, standard))

                self.g.add((standard, RDF['type'], DCT.Standard))
                self.g.add((standard, RDF['type'], DCATAPIT.Standard))
                self.g.add((standard, DCT.identifier, Literal(item)))

        ### publisher

        # DCAT by default creates this node
        # <dct:publisher>
        #   <foaf:Organization rdf:about="http://10.10.100.75/organization/55535226-f82a-4cf7-903a-3e10afeaa79a">
        #     <foaf:name>orga2_test</foaf:name>
        #   </foaf:Organization>
        # </dct:publisher>

        for s,p,o in g.triples( (dataset_ref, DCT.publisher, None) ):
            #log.info("Removing publisher %r", o)
            g.remove((s, p, o))

        self._add_agent(dataset_dict, dataset_ref, 'publisher', DCT.publisher)

        ### Rights holder : Agent
        holder_ref = self._add_agent(dataset_dict, dataset_ref, 'holder', DCT.rightsHolder)

        ### Autore : Agent
        self._add_agent(dataset_dict, dataset_ref, 'creator', DCT.creator)

        ### Point of Contact

        # <dcat:contactPoint rdf:resource="http://dati.gov.it/resource/PuntoContatto/contactPointRegione_r_liguri"/>

        # <!-- http://dati.gov.it/resource/PuntoContatto/contactPointRegione_r_liguri -->
        # <dcatapit:Organization rdf:about="http://dati.gov.it/resource/PuntoContatto/contactPointRegione_r_liguri">
        #    <rdf:type rdf:resource="&vcard;Kind"/>
        #    <rdf:type rdf:resource="&vcard;Organization"/>
        #    <vcard:hasEmail rdf:resource="mailto:infoter@regione.liguria.it"/>
        #    <vcard:fn>Regione Liguria - Sportello Cartografico</vcard:fn>
        # </dcatapit:Organization>


        # TODO: preserve original info if harvested

        # retrieve the contactPoint added by the euro serializer
        euro_poc = g.value(subject=dataset_ref, predicate=DCAT.contactPoint, object=None, any=False)

        # euro poc has this format:
        # <dcat:contactPoint>
        #    <vcard:Organization rdf:nodeID="Nfcd06f452bcd41f48f33c45b0c95979e">
        #       <vcard:fn>THE ORGANIZATION NAME</vcard:fn>
        #       <vcard:hasEmail>THE ORGANIZATION EMAIL</vcard:hasEmail>
        #    </vcard:Organization>
        # </dcat:contactPoint>

        if euro_poc:
            g.remove((dataset_ref, DCAT.contactPoint, euro_poc))

        org_id = dataset_dict.get('organization',{}).get('id')

        # get orga info
        org_show = logic.get_action('organization_show')

        try:
            org_dict = org_show({}, 
            	{'id': org_id,
            	'include_datasets': False,
            	'include_tags': False,
            	'include_users': False,
            	'include_groups': False,
            	'include_extras': True,
            	'include_followers': False})
        except Exception, e:
            org_dict = {}

        org_uri = organization_uri(org_dict)

        poc = URIRef(org_uri)
        g.add((dataset_ref, DCAT.contactPoint, poc))
        g.add((poc, RDF.type, DCATAPIT.Organization))
        g.add((poc, RDF.type, VCARD.Kind))
        g.add((poc, RDF.type, VCARD.Organization))

        g.add((poc, VCARD.fn, Literal(org_dict.get('name'))))

        if 'email' in org_dict.keys():  # this element is mandatory for dcatapit, but it may not have been filled for imported datasets
            g.add((poc, VCARD.hasEmail, URIRef(org_dict.get('email'))))
        if 'telephone' in org_dict.keys():
            g.add((poc, VCARD.hasTelephone, Literal(org_dict.get('telephone'))))
        if 'site' in org_dict.keys():
            g.add((poc, VCARD.hasURL, Literal(org_dict.get('site'))))

        ### Multilingual
        # Add localized entries in dataset
        # TODO: should we remove the non-localized nodes?

        loc_dict = interfaces.get_for_package(dataset_dict['id'])
        #  The multilang fields
        loc_package_mapping = {
            'title': (dataset_ref, DCT.title),
            'notes': (dataset_ref, DCT.description),
            'holder_name': (holder_ref, FOAF.name)
        }

        self._add_multilang_values(loc_dict, loc_package_mapping)

        ### Resources
        for resource_dict in dataset_dict.get('resources', []):

            distribution = URIRef(resource_uri(resource_dict))  # TODO: preserve original info if harvested

            # Add the DCATAPIT type
            g.add((distribution, RDF.type, DCATAPIT.Distribution))

            ### format
            self._remove_node(resource_dict, distribution,  ('format', DCT['format'], None, Literal))
            if not self._add_uri_node(resource_dict, distribution, ('distribution_format', DCT['format'], None, URIRef), FORMAT_BASE_URI):
                guessed_format = guess_format(resource_dict)
                if guessed_format:
                    self.g.add((distribution, DCT['format'], URIRef(FORMAT_BASE_URI + guessed_format)))
                else:
                    log.warn('No format for resource: %s / %s', dataset_dict.get('title', 'N/A'), resource_dict.get('description', 'N/A') )
                    self.g.add((distribution, DCT['format'], URIRef(FORMAT_BASE_URI + DEFAULT_FORMAT_CODE)))

            ### license
            # <dct:license rdf:resource="http://creativecommons.org/licenses/by/3.0/it/"/>
            #
            # <dcatapit:LicenseDocument rdf:about="http://creativecommons.org/licenses/by/3.0/it/">
            #    <rdf:type rdf:resource="&dct;LicenseDocument"/>
            #    <owl:versionInfo>3.0 ITA</owl:versionInfo>
            #    <foaf:name>CC BY</foaf:name>
            #    <dct:type rdf:resource="http://purl.org/adms/licencetype/Attribution"/>
            # </dcatapit:LicenseDocument>

            # "license_id" : "cc-zero"
            # "license_title" : "Creative Commons CCZero",
            # "license_url" : "http://www.opendefinition.org/licenses/cc-zero",

            license_url = dataset_dict.get('license_url', '')
            license_id = dataset_dict.get('license_id', '')
            license_title = dataset_dict.get('license_title', '')

            if license_url:
                license = URIRef(license_url)
                g.add((license, RDF['type'], DCATAPIT.LicenseDocument))
                g.add((license, RDF['type'], DCT.LicenseDocument))
                g.add((license, DCT['type'], URIRef('http://purl.org/adms/licencetype/Attribution'))) # TODO: infer from CKAN license

                g.add((distribution, DCT.license, license))

                if license_id:
                    # log.debug('Adding license id: %s', license_id)
                    g.add((license, FOAF.name, Literal(license_id)))
                elif license_title:
                    # log.debug('Adding license title: %s', license_title)
                    g.add((license, FOAF.name, Literal(license_title)))
                else:
                    g.add((license, FOAF.name, Literal('unknown')))
                    log.warn('License not found for dataset: %s', title)

            ### Multilingual
            # Add localized entries in resource
            # TODO: should we remove the not-localized nodes?

            loc_dict = interfaces.get_for_resource(resource_dict['id'])

            #  The multilang fields
            loc_resource_mapping = {
                'name': (distribution, DCT.title),
                'description': (distribution, DCT.description),
            }
            self._add_multilang_values(loc_dict, loc_resource_mapping)

    def _add_multilang_values(self, loc_dict, loc_mapping):
        if loc_dict:
            for field_name, lang_dict in loc_dict.iteritems():
                ref, pred = loc_mapping.get(field_name, (None, None))
                if not pred:
                    log.warn('Multilang field not mapped "%s"', field_name)
                    continue

                for lang, value in lang_dict.iteritems():
                   lang = lang.split('_')[0]  # rdflib is quite picky in lang names
                   self.g.add((ref, pred, Literal(value, lang=lang)))


    def _add_agent(self, _dict, ref, basekey, _type):
        ''' Stores the Agent in this format:
                <dct:publisher rdf:resource="http://dati.gov.it/resource/Amministrazione/r_liguri"/>
                    <dcatapit:Agent rdf:about="http://dati.gov.it/resource/Amministrazione/r_liguri">
                        <rdf:type rdf:resource="&foaf;Agent"/>
                        <dct:identifier>r_liguri</dct:identifier>
                        <foaf:name>Regione Liguria</foaf:name>
                    </dcatapit:Agent>

            Returns the ref to the agent node
        '''

        agent_name = self._get_dict_value(_dict, basekey + '_name', 'N/A')
        agent_id = self._get_dict_value(_dict, basekey + '_identifier','N/A')

        agent = BNode()

        self.g.add((agent, RDF['type'], DCATAPIT.Agent))
        self.g.add((agent, RDF['type'], FOAF.Agent))
        self.g.add((ref, _type, agent))

        self.g.add((agent, FOAF.name, Literal(agent_name)))
        self.g.add((agent, DCT.identifier, Literal(agent_id)))

        return agent

    def _add_uri_node(self, _dict, ref, item, base_uri=''):

        key, pred, fallback, _type = item

        value = self._get_dict_value(_dict, key)
        if value:
            self.g.add((ref, pred, _type(base_uri + value)))
            return True
        elif fallback:
            self.g.add((ref, pred, _type(base_uri + fallback)))
            return False
        else:
            return False

    def _remove_node(self, _dict, ref, item):

        key, pred, fallback, _type = item

        value = self._get_dict_value(_dict, key)
        if value:
            self.g.remove((ref, pred, _type(value)))

    def _add_concept(self, concepts, tag):

        # Localized concepts should be serialized as:
        #
        # <dcat:theme rdf:resource="http://publications.europa.eu/resource/authority/data-theme/ENVI"/>
        #
        # <skos:Concept rdf:about="http://publications.europa.eu/resource/authority/data-theme/ENVI">
        #     <skos:prefLabel xml:lang="it">Ambiente</skos:prefLabel>
        # </skos:Concept>
        #
        # Return true if Concept has been added

        voc, base_uri = concepts

        loc_dict = interfaces.get_all_localized_tag_labels(tag)

        if loc_dict and len(loc_dict) > 0:

            concept = URIRef(base_uri + tag)
            self.g.add((concept, RDF['type'], SKOS.Concept))

            for lang, label in loc_dict.iteritems():
                lang = lang.split('_')[0]  # rdflib is quite picky in lang names
                self.g.add((concept, SKOS.prefLabel, Literal(label, lang=lang)))

            return True

        return False

    def graph_from_catalog(self, catalog_dict, catalog_ref):

        g = self.g

        for prefix, namespace in it_namespaces.iteritems():
            g.bind(prefix, namespace)

        ### Add a further type for the Catalog node
        g.add((catalog_ref, RDF.type, DCATAPIT.Catalog))

        ### Replace homepage
        # Try to avoid to have the Catalog URIRef identical to the homepage URI
        g.remove((catalog_ref, FOAF.homepage, URIRef(config.get('ckan.site_url'))))
        g.add((catalog_ref, FOAF.homepage, URIRef(catalog_uri() + '/#')))

        ### publisher
        pub_agent_name = config.get('ckanext.dcatapit_configpublisher_name', 'unknown')
        pub_agent_id = config.get('ckanext.dcatapit_configpublisher_code_identifier', 'unknown')

        agent = BNode()
        self.g.add((agent, RDF['type'], DCATAPIT.Agent))
        self.g.add((agent, RDF['type'], FOAF.Agent))
        self.g.add((catalog_ref, DCT.publisher, agent))
        self.g.add((agent, FOAF.name, Literal(pub_agent_name)))
        self.g.add((agent, DCT.identifier, Literal(pub_agent_id)))

        ### issued date
        issued = config.get('ckanext.dcatapit_config.catalog_issued', '1900-01-01')
        if issued:
            self._add_date_triple(catalog_ref, DCT.issued, issued)

        ### theme taxonomy

        # <dcat:themeTaxonomy rdf:resource="http://publications.europa.eu/resource/authority/data-theme"/>

        # <skos:ConceptScheme rdf:about="http://publications.europa.eu/resource/authority/data-theme">
        #    <dct:title xml:lang="it">Il Vocabolario Data Theme</dct:title>
        # </skos:ConceptScheme>

        taxonomy = URIRef(THEME_BASE_URI.rstrip('/'))
        self.g.add((catalog_ref, DCAT.themeTaxonomy, taxonomy))
        self.g.add((taxonomy, RDF.type, SKOS.ConceptScheme))
        self.g.add((taxonomy, DCT.title, Literal('Il Vocabolario Data Theme', lang='it')))

        ### language
        langs = config.get('ckan.locales_offered', 'it')

        for lang_offered in langs.split():
            lang_code = lang_mapping_ckan_to_voc.get(lang_offered)
            self.g.add((catalog_ref, DCT.language, URIRef(LANG_BASE_URI + lang_code)))

        self.g.remove((catalog_ref, DCT.language, Literal(config.get('ckan.locale_default', 'en'))))


def organization_uri(orga_dict):
    '''
    Returns an URI for the organization

    This will be used to uniquely reference the organization on the RDF serializations.

    The value will be

        `catalog_uri()` + '/organization/' + `orga_id`

    Check the documentation for `catalog_uri()` for the recommended ways of
    setting it.

    Returns a string with the resource URI.
    '''

    uri = '{0}/organization/{1}'.format(catalog_uri().rstrip('/'), orga_dict.get('id', None))

    return uri


def guess_format(resource_dict):
    f = resource_dict.get('format')

    if not f:
        log.info('No format found')
        return None

    ret = format_mapping.get(f, None)

    if not ret:
       log.info('Mapping not found for format %s', f)

    return ret




