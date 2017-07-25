
import json

import logging
import ckan.lib.search as search
import ckan.lib.dictization.model_dictize as model_dictize

from ckanext.harvest.model import HarvestObject

from ckan.lib.base import model
from ckan.model import Session

from ckanext.multilang.model import PackageMultilang, GroupMultilang, TagMultilang
from ckan.model import Package

from ckan.plugins.core import SingletonPlugin

from ckanext.spatial.lib.csw_client import CswService
from ckanext.spatial.harvesters.csw import CSWHarvester

from ckanext.spatial.model import ISODocument
from ckanext.spatial.model import ISOElement
from ckanext.spatial.model import ISOResponsibleParty
from ckanext.spatial.model import ISOKeyword

from ckan.logic import ValidationError, NotFound, get_action

from pylons import config
from datetime import datetime

log = logging.getLogger(__name__)

# Extend the ISODocument definitions by adding some more useful elements

log.info('CSW Multilang harvester: extending ISODocument with PT_FreeText')

## ISO Document xpath definition for localized title and description

class ISOTextGroup(ISOElement):
    elements = [
        ISOElement(
            name="text",
            search_paths=[
                "gmd:LocalisedCharacterString/text()"
            ],
            multiplicity="1",
        ),
        ISOElement(
            name="locale",
            search_paths=[
                "gmd:LocalisedCharacterString/@locale"
            ],
            multiplicity="1",
        )
    ]

class ISOKeywordTextGroup(ISOElement):
    elements = [
        ISOElement(
            name="name",
            search_paths=[
                "../../gco:CharacterString/text()"
            ],
            multiplicity="1",
        ),
        ISOElement(
            name="text",
            search_paths=[
                "gmd:LocalisedCharacterString/text()"
            ],
            multiplicity="1",
        ),
        ISOElement(
            name="locale",
            search_paths=[
                "gmd:LocalisedCharacterString/@locale"
            ],
            multiplicity="1",
        )
    ]

ISODocument.elements.append(
    ISOTextGroup(
        name="title-text",
        search_paths=[
            "gmd:identificationInfo/gmd:MD_DataIdentification/gmd:citation/gmd:CI_Citation/gmd:title/gmd:PT_FreeText/gmd:textGroup",
            "gmd:identificationInfo/srv:SV_ServiceIdentification/gmd:citation/gmd:CI_Citation/gmd:title/gmd:PT_FreeText/gmd:textGroup"
        ],
        multiplicity="1..*",
    )
)

ISODocument.elements.append(
    ISOTextGroup(
        name="abstract-text",
        search_paths=[
            "gmd:identificationInfo/gmd:MD_DataIdentification/gmd:abstract/gmd:PT_FreeText/gmd:textGroup",
            "gmd:identificationInfo/srv:SV_ServiceIdentification/gmd:abstract/gmd:PT_FreeText/gmd:textGroup"
        ],
        multiplicity="1..*",
    )
)

## ISO Document xpath definition for localized responsible party

ISOResponsibleParty.elements.append(
    ISOTextGroup(
        name="organisation-name-localized",
        search_paths=[
            "gmd:organisationName/gmd:PT_FreeText/gmd:textGroup"
        ],
        multiplicity="1..*",
    )
)

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

## ISO Document xpath definition for localized keywords

ISOKeyword.elements.append(
    ISOKeywordTextGroup(
        name="keyword-name-localized",
        search_paths=[
            "gmd:keyword/gmd:PT_FreeText/gmd:textGroup"
        ],
        multiplicity="1..*",
    )
)

class MultilangHarvester(CSWHarvester, SingletonPlugin):

    _package_dict = {} 

    _ckan_locales_mapping = {
        'ita': 'it',
        'ger': 'de',
        'eng': 'en_GB'
    }

    def info(self):
        return {
            'name': 'multilang',
            'title': 'CSW server (Multilang)',
            'description': 'Harvests CWS with Multilang',
            'form_config_interface': 'Text'
        }

    ##
    ## Saves in memory the package_dict localised texts 
    ##
    def get_package_dict(self, iso_values, harvest_object):
        package_dict = super(MultilangHarvester, self).get_package_dict(iso_values, harvest_object)      

        self._package_dict = {} 
        
        harvester_config = self.source_config.get('ckan_locales_mapping', {})
        if harvester_config:
            self._ckan_locales_mapping = harvester_config
            log.info('::::: ckan_locales_mapping entry found in harvester configuration :::::')

        if iso_values["abstract-text"] and iso_values["title-text"]:
            log.debug('::::: Collecting localised data from the metadata abstract :::::')
            localised_abstracts = []
            for abstract_entry in iso_values["abstract-text"]:
                if abstract_entry['text'] and abstract_entry['locale'].lower()[1:]:
                    if self._ckan_locales_mapping[abstract_entry['locale'].lower()[1:]]:
                        localised_abstracts.append({
                            'text': abstract_entry['text'],
                            'locale': self._ckan_locales_mapping[abstract_entry['locale'].lower()[1:]]
                        })
                    else:
                        log.warning('Locale Mapping not found for metadata abstract, entry skipped!')
                else:
                    log.warning('TextGroup data not available for metadata abstract, entry skipped!')

            log.debug('::::: Collecting localized data from the metadata title :::::')
            localised_titles = []
            for title_entry in iso_values["title-text"]:
                if title_entry['text'] and title_entry['locale'].lower()[1:]:
                    if self._ckan_locales_mapping[title_entry['locale'].lower()[1:]]:
                        localised_titles.append({
                            'text': title_entry['text'],
                            'locale': self._ckan_locales_mapping[title_entry['locale'].lower()[1:]]
                        })
                    else:
                        log.warning('Locale Mapping not found for metadata title, entry skipped!')
                else:
                    log.warning('TextGroup data not available for metadata title, entry skipped!')
            
            localised_titles.append({
                'text': iso_values['title'],
                'locale': self._ckan_locales_mapping[iso_values["metadata-language"].lower()]
            })

            localised_abstracts.append({
                'text': iso_values['abstract'],
                'locale': self._ckan_locales_mapping[iso_values["metadata-language"].lower()]
            })

            self._package_dict = {
                'localised_titles': localised_titles,
                'localised_abstracts': localised_abstracts
            }

            log.info('::::::::: Localised _package_dict saved in memory :::::::::')

        if iso_values["keywords"]:
            log.info('::::: Collecting localised data from the metadata keywords :::::')
            localized_tags = []

            for tag_entry in iso_values["keywords"]:
                #Getting other locales
                tag_localized_entry = tag_entry["keyword-name-localized"]

                #Getting keyword for default metadata locale
                for keyword in tag_entry['keyword']:
                    if iso_values["metadata-language"].lower() in self._ckan_locales_mapping: 
                        localized_tags.append({
                            'text': keyword,
                            'localized_text': keyword,
                            'locale': self._ckan_locales_mapping[iso_values["metadata-language"].lower()]
                        })

                for tag_localized in tag_localized_entry:
                    if tag_localized['text'] and tag_localized['locale'].lower()[1:]:
                        if self._ckan_locales_mapping[tag_localized['locale'].lower()[1:]]:
                            localized_tags.append({
                                'text': tag_localized['name'],
                                'localized_text': tag_localized['text'],
                                'locale': self._ckan_locales_mapping[tag_localized['locale'].lower()[1:]]
                            })
                        else:
                            log.warning('Locale Mapping not found for metadata keyword: %r, entry skipped!', tag_localized['name'])
                    else:
                        log.warning('TextGroup data not available for metadata keyword: %r, entry skipped!', tag_localized['name'])

            self._package_dict['localized_tags'] = localized_tags

        ## Configuring package organizations         
        organisation_mapping = self.source_config.get('organisation_mapping', [])

        if organisation_mapping:
            log.info('::::::::: Checking for the Organization :::::::::')

            organisation = self.handle_organization(harvest_object, organisation_mapping, iso_values)
            
            if organisation:
                package_dict['owner_org'] = organisation

        # End of processing, return the modified package
        return package_dict

    def handle_organization(self, harvest_object, organisation_mapping, values):
        citedResponsiblePartys = values["cited-responsible-party"]
        
        organisation = None
        for party in citedResponsiblePartys:
            if party["role"] == "owner":                
                for org in organisation_mapping:
                    if org.get("value") in party["organisation-name"]:
                        existing_org = model.Session.query(model.Group).filter(model.Group.name == org.get("key")).first()

                        if not existing_org:
                            log.warning('::::::::: Organisation not found in CKAN: %r: assigning default :::::::::', org.get("key"))
                        else:
                            organisation = existing_org.id

                            log.debug('::::: Collecting localized data from the organization name :::::')
                            localized_org = []

                            localized_org.append({
                                'text': org.get("value_" + self._ckan_locales_mapping[values["metadata-language"].lower()]) or org.get("value"),
                                'locale': self._ckan_locales_mapping[values["metadata-language"].lower()]
                            })

                            for entry in party["organisation-name-localized"]:
                                if entry['text'] and entry['locale'].lower()[1:]:
                                    if self._ckan_locales_mapping[entry['locale'].lower()[1:]]:
                                        localized_org.append({
                                            'text': org.get("value_" + self._ckan_locales_mapping[entry['locale'].lower()[1:]]) or entry['text'],
                                            'locale': self._ckan_locales_mapping[entry['locale'].lower()[1:]]
                                        })
                                    else:
                                        log.warning('Locale Mapping not found for organization name, entry skipped!')
                                else:
                                    log.warning('TextGroup data not available for organization name, entry skipped!')

                            log.debug("::::::::::::: Persisting localized ORG :::::::::::::")      
                            
                            # rows = session.query(GroupMultilang).filter(GroupMultilang.group_id == organisation).all()
                            rows = GroupMultilang.get_for_group_id(organisation)

                            for localized_entry in localized_org:
                                insert = True
                                for row in rows:
                                    if row.lang == localized_entry.get("locale") and row.field == 'title':
                                        # Updating the org localized record
                                        row.text = localized_entry.get("text")
                                        row.save()
                                        insert = False

                                        log.debug("::::::::::::: ORG locale successfully updated :::::::::::::") 

                                if insert:
                                    # Inserting the missing org localized record
                                    org_dict = {
                                        'id': organisation,
                                        'name': existing_org.name,
                                        'title': localized_entry.get("text"),
                                        'description': localized_entry.get("text")
                                    }

                                    GroupMultilang.persist(org_dict, localized_entry.get("locale"))
                                    
                                    log.debug("::::::::::::: ORG locale successfully added :::::::::::::")

        return organisation

    def import_stage(self, harvest_object):
        import_stage = super(MultilangHarvester, self).import_stage(harvest_object)

        if import_stage == True:
            # Get the existing HarvestObject
            existing_object = model.Session.query(HarvestObject) \
                    .filter(HarvestObject.guid==harvest_object.guid) \
                    .filter(HarvestObject.current==True) \
                    .first()

            session = Session
            
            harvested_package = session.query(Package).filter(Package.id == existing_object.package_id).first()
            
            if harvested_package:
                package_dict = model_dictize.package_dictize(harvested_package, {'model': model, 'session': session})
                if package_dict:
                    self.after_import_stage(package_dict)
        
        return import_stage

    def after_import_stage(self, package_dict):        
        log.info('::::::::: Performing after_import_stage  persist operation for localised dataset content :::::::::')

        if bool(self._package_dict):
            session = Session

            package_id = package_dict.get('id')
            
            # Persisting localized packages 
            try:
                # rows = session.query(PackageMultilang).filter(PackageMultilang.package_id == package_id).all()
                rows = PackageMultilang.get_for_package(package_id) 

                if not rows:
                    log.info('::::::::: Adding new localised object to the package_multilang table :::::::::')
                    
                    log.debug('::::: Persisting default metadata locale :::::')

                    loc_titles = self._package_dict.get('localised_titles')
                    if loc_titles:
                        log.debug('::::: Persisting title locales :::::')
                        for title in loc_titles:
                            PackageMultilang.persist({'id': package_id, 'text': title.get('text'), 'field': 'title'}, title.get('locale'))

                    loc_abstracts = self._package_dict.get('localised_abstracts')
                    if loc_abstracts:
                        log.debug('::::: Persisting abstract locales :::::')
                        for abstract in loc_abstracts:
                            PackageMultilang.persist({'id': package_id, 'text': abstract.get('text'), 'field': 'notes'}, abstract.get('locale'))

                    log.info('::::::::: OBJECT PERSISTED SUCCESSFULLY :::::::::')

                else:
                    log.info('::::::::: Updating localised object in the package_multilang table :::::::::')
                    for row in rows:
                        if row.field == 'title': 
                            titles = self._package_dict.get('localised_titles')
                            if titles:
                                for title in titles:
                                    if title.get('locale') == row.lang:
                                        row.text = title.get('text')
                        elif row.field == 'notes': 
                            abstracts = self._package_dict.get('localised_abstracts')
                            if abstracts:
                                for abstract in abstracts:
                                    if abstract.get('locale') == row.lang:
                                        row.text = abstract.get('text')

                        row.save()

                    log.info('::::::::: OBJECT UPDATED SUCCESSFULLY :::::::::') 

                pass
            except Exception, e:
                # on rollback, the same closure of state
                # as that of commit proceeds. 
                session.rollback()

                log.error('Exception occurred while persisting DB objects: %s', e)
                raise

            # Persisting localized Tags
            
            loc_tags = self._package_dict.get('localized_tags')
            if loc_tags:
                log.debug('::::: Persisting tag locales :::::')
                for tag in loc_tags:
                    tag_name = tag.get('text')
                    tag_lang = tag.get('locale')
                    tag_localized_name = tag.get('localized_text')

                    tag = TagMultilang.by_name(tag_name, tag_lang)

                    if tag:
                        # Update the existing record                        
                        if tag_localized_name and tag_localized_name != tag.text:
                            tag.text = tag_localized_name

                            try:
                                tag.save()
                                log.info('::::::::: OBJECT TAG UPDATED SUCCESSFULLY :::::::::') 
                                pass
                            except Exception, e:
                                # on rollback, the same closure of state
                                # as that of commit proceeds. 
                                session.rollback()

                                log.error('Exception occurred while persisting DB objects: %s', e)
                                raise
                    else:
                        # Create a new localized record
                        existing_tag = model.Tag.by_name(tag_name)

                        if existing_tag:
                            TagMultilang.persist({'id': existing_tag.id, 'name': tag_name, 'text': tag_localized_name}, tag_lang)
                            log.info('::::::::: OBJECT TAG PERSISTED SUCCESSFULLY :::::::::')

        # Updating Solr Index
        if package_dict:
            log.info("::: UPDATING SOLR INDEX :::")
            # solr update here
            psi = search.PackageSearchIndex()

            # update the solr index in batches
            BATCH_SIZE = 50

            def process_solr(q):
                # update the solr index for the query
                query = search.PackageSearchQuery()
                q = {
                    'q': q,
                    'fl': 'data_dict',
                    'wt': 'json',
                    'fq': 'site_id:"%s"' % config.get('ckan.site_id'),
                    'rows': BATCH_SIZE
                }

                for result in query.run(q)['results']:
                    data_dict = json.loads(result['data_dict'])
                    if data_dict['owner_org'] == package_dict.get('owner_org'):
                        psi.index_package(data_dict, defer_commit=True)

            count = 0
            q = []
            
            q.append('id:"%s"' % (package_dict.get('id')))
            count += 1
            if count % BATCH_SIZE == 0:
                process_solr(' OR '.join(q))
                q = []

            if len(q):
                process_solr(' OR '.join(q))
            # finally commit the changes
            psi.commit()
        else:
            log.warning("::: package_dict is None: SOLR INDEX CANNOT BE UPDATED! :::")


        return
