import logging
import ckanext.dcatapit.harvesters.utils as utils

from ckan.plugins.core import SingletonPlugin
from ckanext.spatial.harvesters.csw import CSWHarvester

from ckanext.spatial.model import ISODocument
from ckanext.spatial.model import ISOElement
from ckanext.spatial.model import ISOKeyword
from ckanext.spatial.model import ISOResponsibleParty

log = logging.getLogger(__name__)

ISODocument.elements.append(
    ISOResponsibleParty(
        name="cited-responsible-party",
        search_paths=[
            "gmd:identificationInfo/gmd:MD_DataIdentification/gmd:citation/gmd:CI_Citation/gmd:citedResponsibleParty/gmd:CI_ResponsibleParty",
            "gmd:identificationInfo/srv:SV_ServiceIdentification/gmd:citation/gmd:CI_Citation/gmd:citedResponsibleParty/gmd:CI_ResponsibleParty"
        ],
        multiplicity="1..*",
    )
)

ISODocument.elements.append(
    ISOElement(
        name="conformity-specification-title",
        search_paths=[
            "gmd:dataQualityInfo/gmd:DQ_DataQuality/gmd:report/gmd:DQ_DomainConsistency/gmd:result/gmd:DQ_ConformanceResult/gmd:specification/gmd:CI_Citation/gmd:title/gco:CharacterString/text()"
        ],
        multiplicity="1",
     ))

ISOKeyword.elements.append(
    ISOElement(
        name="thesaurus-title",
        search_paths=[
            "gmd:thesaurusName/gmd:CI_Citation/gmd:title/gco:CharacterString/text()",
        ],
        multiplicity="1",
    ))

ISOKeyword.elements.append(
    ISOElement(
        name="thesaurus-identifier",
        search_paths=[
            "gmd:thesaurusName/gmd:CI_Citation/gmd:identifier/gmd:MD_Identifier/gmd:code/gco:CharacterString/text()",
        ],
        multiplicity="1",
    ))

class DCATAPITCSWHarvester(CSWHarvester, SingletonPlugin):

    _dcatapit_config = {
        'dataset_themes': 'OP_DATPRO',
        'dataset_places': None,
        'dataset_languages': 'ITA',
        'frequency': 'UNKNOWN',
        'agents': {
            'publisher': {
                'code': 'temp_ipa',
                'role': 'publisher',
                'code_regex': {
                    'regex': '\(([^)]+)\:([^)]+)\)',
                    'groups': [2]  # optional, dependes by the regular expression
                },
                'name_regex': {
                    'regex': '([^(]*)(\(IPA[^)]*\))(.+)',
                    'groups': [1, 3]  # optional, dependes by the regular expression
                }
            },
            'owner': {
                'code': 'temp_ipa',
                'role': 'owner',
                'code_regex': {
                    'regex': '\(([^)]+)\:([^)]+)\)',
                    'groups': [2]  # optional, dependes by the regular expression
                },
                'name_regex': {
                    'regex': '([^(]*)(\(IPA[^)]*\))(.+)',
                    'groups': [1, 3]  # optional, dependes by the regular expression
                }
            },
            'author': {
                'code': 'temp_ipa',
                'role': 'author',
                'code_regex': {
                    'regex': '\(([^)]+)\:([^)]+)\)',
                    'groups': [2]  # optional, dependes by the regular expression
                },
                'name_regex': {
                    'regex': '([^(]*)(\(IPA[^)]*\))(.+)',
                    'groups': [1, 3]  # optional, dependes by the regular expression
                }
            }
        },
        'controlled_vocabularies': {
            'dcatapit_skos_theme_id': 'theme.data-theme-skos',
            'dcatapit_skos_places_id': 'theme.places-skos'
        }
    }

    def info(self):
        return {
            'name': 'DCAT_AP-IT CSW Harvester',
            'title': 'DCAT_AP-IT CSW Harvester',
            'description': 'DCAT_AP-IT Harvester for harvesting dcatapit fields from CWS',
            'form_config_interface': 'Text'
        }

    def get_package_dict(self, iso_values, harvest_object):
        package_dict = super(DCATAPITCSWHarvester, self).get_package_dict(iso_values, harvest_object)

        mapping_frequencies_to_mdr_vocabulary = self.source_config.get('mapping_frequencies_to_mdr_vocabulary', \
            utils._mapping_frequencies_to_mdr_vocabulary)
        mapping_languages_to_mdr_vocabulary = self.source_config.get('mapping_languages_to_mdr_vocabulary', \
            utils._mapping_languages_to_mdr_vocabulary)

        dcatapit_config = self.source_config.get('dcatapit_config', self._dcatapit_config)

        #if dcatapit_config and not all(name in dcatapit_config for name in self._dcatapit_config):
        #    dcatapit_config = self._dcatapit_config
        #    log.warning('Some keys are missing in dcatapit_config configuration property, \
        #        keyes to use are: dataset_theme, dataset_language, agent_code, frequency, \
        #        agent_code_regex, org_name_regex and dcatapit_skos_theme_id. Using defaults')
        #elif not dcatapit_config:
        #    dcatapit_config = self._dcatapit_config

        controlled_vocabularies = dcatapit_config.get('controlled_vocabularies', \
            self._dcatapit_config.get('controlled_vocabularies'))
        agents = dcatapit_config.get('agents', self._dcatapit_config.get('agents'))

        #
        # Increase the tag name max length limit to 100 as set at DB level (instead 50 as did by the ckanext-spatial)
        #
        tags = []
        if 'tags' in iso_values:
            for tag in iso_values['tags']:
                tag = tag[:100] if len(tag) > 100 else tag
                tags.append({'name': tag})

        # Add default_tags from config
        default_tags = self.source_config.get('default_tags',[])
        if default_tags:
           for tag in default_tags:
              tags.append({'name': tag})

        package_dict['tags'] = tags

        # ------------------------------#
        #    MANDATORY FOR DCAT-AP_IT   #
        # ------------------------------#

        #  -- identifier -- #
        identifier = iso_values["guid"]
        package_dict['extras'].append({'key': 'identifier', 'value': identifier})

        default_agent_code = identifier.split(':')[0] if ':' in identifier else None

        #  -- theme -- #
        dataset_themes = []
        if iso_values["keywords"]:
            default_vocab_id = self._dcatapit_config.get('controlled_vocabularies').get('dcatapit_skos_theme_id')
            dataset_themes = utils.get_controlled_vocabulary_values('eu_themes', \
                controlled_vocabularies.get('dcatapit_skos_theme_id', default_vocab_id), iso_values["keywords"])

        if dataset_themes and len(dataset_themes) > 1:
            dataset_themes = list(set(dataset_themes))
            dataset_themes = '{' + ','.join(str(l) for l in dataset_themes) + '}'
        else:
            dataset_themes = dataset_themes[0] if dataset_themes and len(dataset_themes) > 0 else dcatapit_config.get('dataset_themes', \
                self._dcatapit_config.get('dataset_themes'))

        log.info("Medatata harvested dataset themes: %r", dataset_themes)
        package_dict['extras'].append({'key': 'theme', 'value': dataset_themes})

        #  -- publisher -- #
        citedResponsiblePartys = iso_values["cited-responsible-party"]
        agent_name, agent_code = utils.get_responsible_party(citedResponsiblePartys, agents.get('publisher', \
            self._dcatapit_config.get('agents').get('publisher')))
        package_dict['extras'].append({'key': 'publisher_name', 'value': agent_name})
        package_dict['extras'].append({'key': 'publisher_identifier', 'value': agent_code or default_agent_code})

        #  -- modified -- #
        revision_date = iso_values["date-updated"] or iso_values["date-released"]
        package_dict['extras'].append({'key': 'modified', 'value': revision_date})

        #  -- frequency -- #
        updateFrequency = iso_values["frequency-of-update"]
        package_dict['extras'].append({'key': 'frequency', 'value': \
            mapping_frequencies_to_mdr_vocabulary.get(updateFrequency, \
            dcatapit_config.get('frequency', self._dcatapit_config.get('frequency')))})

        #  -- rights_holder -- #
        citedResponsiblePartys = iso_values["cited-responsible-party"]
        agent_name, agent_code = utils.get_responsible_party(citedResponsiblePartys, \
            agents.get('owner', self._dcatapit_config.get('agents').get('owner')))
        package_dict['extras'].append({'key': 'holder_name', 'value': agent_name})
        package_dict['extras'].append({'key': 'holder_identifier', 'value': agent_code or default_agent_code})

        # -----------------------------------------------#
        #    OTHER FIELDS NOT MANDATORY FOR DCAT_AP-IT   #
        # -----------------------------------------------#

        #  -- alternate_identifier nothing to do  -- #

        #  -- issued -- #
        publication_date = iso_values["date-released"]
        package_dict['extras'].append({'key': 'issued', 'value': publication_date})

        #  -- geographical_name  -- #
        dataset_places = []
        if iso_values["keywords"]:
            default_vocab_id = self._dcatapit_config.get('controlled_vocabularies').get('dcatapit_skos_theme_id')
            dataset_places = utils.get_controlled_vocabulary_values('places', \
                controlled_vocabularies.get('dcatapit_skos_places_id', default_vocab_id), iso_values["keywords"])

        if dataset_places and len(dataset_places) > 1:
            dataset_places = list(set(dataset_places))
            dataset_places = '{' + ','.join(str(l) for l in dataset_places) + '}'
        else:
            dataset_places = dataset_places[0] if dataset_places and len(dataset_places) > 0 else dcatapit_config.get('dataset_places', \
                self._dcatapit_config.get('dataset_places'))

        if dataset_places:
            log.info("Medatata harvested dataset places: %r", dataset_places)
            package_dict['extras'].append({'key': 'geographical_name', 'value': dataset_places})

        #  -- geographical_geonames_url nothing to do  -- #

        #  -- language -- #
        dataset_languages = iso_values["dataset-language"]
        language = None
        if dataset_languages and len(dataset_languages) > 0:
            languages = []
            for language in dataset_languages:
                lang = mapping_languages_to_mdr_vocabulary.get(language, None)
                if lang:
                    languages.append(lang)

            if len(languages) > 1:
                language = '{' + ','.join(str(l) for l in languages) + '}'
            else:
                language = languages[0] if len(languages) > 0 else dcatapit_config.get('dataset_languages', \
                    self._dcatapit_config.get('dataset_languages'))

            log.info("Medatata harvested dataset languages: %r", language)
        else:
            language = dcatapit_config.get('dataset_language')

        package_dict['extras'].append({'key': 'language', 'value': language})

        #  -- temporal_coverage -- #
        for key in ['temporal-extent-begin', 'temporal-extent-end']:
            if len(iso_values[key]) > 0:
                temporal_extent_value = iso_values[key][0]
                if key == 'temporal-extent-begin':
                    package_dict['extras'].append({'key': 'temporal_start', 'value': temporal_extent_value})
                if key == 'temporal-extent-end':
                    package_dict['extras'].append({'key': 'temporal_end', 'value': temporal_extent_value})

        #  -- conforms_to -- #
        conforms_to = iso_values["conformity-specification-title"]
        package_dict['extras'].append({'key': 'conforms_to', 'value': conforms_to})

        #  -- creator -- #
        citedResponsiblePartys = iso_values["cited-responsible-party"]
        agent_name, agent_code = utils.get_responsible_party(citedResponsiblePartys, \
            agents.get('author', self._dcatapit_config.get('agents').get('author')))
        package_dict['extras'].append({'key': 'creator_name', 'value': agent_name})
        package_dict['extras'].append({'key': 'creator_identifier', 'value': agent_code or default_agent_code})

        # End of processing, return the modified package
        return package_dict