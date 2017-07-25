import json
import logging
import ckan.lib.search as search

from pylons import config
from ckan.lib.base import model
from ckan.model import Session
from pylons.i18n.translation import get_lang

from ckan.plugins.interfaces import Interface
from ckanext.dcatapit.model import DCATAPITTagVocabulary

log = logging.getLogger(__name__)


class ICustomSchema(Interface):
    '''
    Allows extensions to provide their own schema fields.
    '''
    def get_custom_schema(self):
        '''gets the array containing the custom schema fields'''
        return []


def get_language():
    lang = get_lang()

    if lang is not None:
        lang = unicode(lang[0])

    return lang

def update_solr_package_indexes(package_dict):
    # Updating Solr Index
    if package_dict:
        log.debug("::: UPDATING SOLR INDEX :::")

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

def save_extra_package_multilang(pkg, lang, field_type):
    try:
        from ckanext.multilang.model import PackageMultilang
    except ImportError:
        log.warn('DCAT-AP_IT: multilang extension not available.')
        return

    log.debug('Creating create_loc_field for package ID: %r', str(pkg.get('id')))
    PackageMultilang.persist(pkg, lang, field_type)
    log.info('Localized field created successfully')

def upsert_package_multilang(pkg_id, field_name, field_type, lang, text):
    try:
        from ckanext.multilang.model import PackageMultilang
    except ImportError:
        log.warn('DCAT-AP_IT: multilang extension not available.')
        return

    pml = PackageMultilang.get(pkg_id, field_name, lang, field_type)
    if not pml and text:
        PackageMultilang.persist({'id':pkg_id, 'field':field_name, 'text':text}, lang, field_type)
    elif pml and not text:
        pml.purge()
    elif pml and not pml.text == text:
        pml.text = text
        pml.save()

def upsert_resource_multilang(res_id, field_name, lang, text):
    try:
        from ckanext.multilang.model import ResourceMultilang
    except ImportError:
        log.warn('DCAT-AP_IT: multilang extension not available.')
        return

    ml = ResourceMultilang.get_for_pk(res_id, field_name, lang)
    if not ml and text:
        ResourceMultilang.persist_resources([ResourceMultilang(res_id, field_name, lang, text)])
    elif ml and not text:
        ml.purge()
    elif ml and not ml.text == text:
        ml.text = text
        ml.save()

def update_extra_package_multilang(extra, pkg_id, field, lang, field_type='extra'):
    try:
        from ckanext.multilang.model import PackageMultilang
    except ImportError:
        log.warn('DCAT-AP_IT: multilang extension not available.')
        return

    if extra.get('key') == field.get('name', None) and field.get('localized', False) == True:
        log.debug(':::::::::::::::Localizing schema field: %r', field['name'])

        f = PackageMultilang.get(pkg_id, field['name'], lang, field_type)
        if f:
            if extra.get('value') == '':
                f.purge()
            elif f.text != extra.get('value'):
                # Update the localized field value for the current language
                f.text = extra.get('value')
                f.save()

                log.info('Localized field updated successfully')

        elif extra.get('value') != '':
            # Create the localized field record
            save_extra_package_multilang({'id': pkg_id, 'text': extra.get('value'), 'field': extra.get('key')}, lang, 'extra')

def get_localized_field_value(field=None, pkg_id=None, field_type='extra'):
    try:
        from ckanext.multilang.model import PackageMultilang
    except ImportError:
        log.warn('DCAT-AP_IT: multilang extension not available.')
        return

    if field and pkg_id:
        lang = get_language()
        if lang:
            localized_value = PackageMultilang.get(pkg_id, field, lang, field_type)
            if localized_value:
                return localized_value.text
            else:
                return None
        else:
            return None
    else:
        return None

def get_for_package(pkg_id):
    '''
    Returns all the localized fields of a dataset, in a dict of dicts, i.e.:
        {FIELDNAME:{LANG:label,...},...}

    Returns None if multilang extension not loaded.
    '''

    try:
        from ckanext.multilang.model import PackageMultilang
    except ImportError:
        log.warn('DCAT-AP_IT: multilang extension not available.')

        # TODO: if no multilang, return the dataset in a single language in the same format of the multilang data
        return None

    records = PackageMultilang.get_for_package(pkg_id)
    return _multilang_to_dict(records)

def get_for_resource(res_id):
    '''
    Returns all the localized fields of a dataset's resources, in a dict of dicts, i.e.:
         {FIELDNAME:{LANG:label, ...}, ...}

    Returns None if multilang extension not loaded.
    '''

    try:
        from ckanext.multilang.model import ResourceMultilang
    except ImportError:
        log.warn('DCAT-AP_IT: multilang extension not available.')

        return None

    records = ResourceMultilang.get_for_resource_id(res_id)
    return _multilang_to_dict(records)

def _multilang_to_dict(records):
    fields_dict = {}

    for r in records:
        fieldname = r.field
        lang = r.lang
        value = r.text

        lang_dict = fields_dict.get(fieldname, {})
        if len(lang_dict) == 0:
            fields_dict[fieldname] = lang_dict

        lang_dict[lang] = value

    return fields_dict

def persist_tag_multilang(name, lang, localized_text, vocab_name):
    log.info('DCAT-AP_IT: persisting tag multilang for tag %r ...', name)

    tag = DCATAPITTagVocabulary.by_name(name, lang)

    if tag:
        # Update the existing record
        if localized_text and localized_text != tag.text:
            tag.text = localized_text

            try:
                tag.save()
                log.info('::::::::: OBJECT TAG UPDATED SUCCESSFULLY :::::::::')
                pass
            except Exception, e:
                # on rollback, the same closure of state
                # as that of commit proceeds.
                Session.rollback()

                log.error('Exception occurred while persisting DB objects: %s', e)
                raise
    else:
        # Create a new localized record
        vocab = model.Vocabulary.get(vocab_name)
        existing_tag = model.Tag.by_name(name, vocab)

        if existing_tag:
            DCATAPITTagVocabulary.persist({'id': existing_tag.id, 'name': name, 'text': localized_text}, lang)
            log.info('::::::::: OBJECT TAG PERSISTED SUCCESSFULLY :::::::::')

def get_localized_tag_name(tag_name=None, fallback_lang=None):
    if tag_name:
        lang = get_language()

        localized_tag_name = DCATAPITTagVocabulary.by_name(tag_name, lang)

        if localized_tag_name:
            return localized_tag_name.text
        else:
            if fallback_lang:
                fallback_name = DCATAPITTagVocabulary.by_name(tag_name, fallback_lang)

                if fallback_name:
                    fallback_name = fallback_name.text
                    return fallback_name
                else:
                    return tag_name
            else:
                return tag_name
    else:
        return None

def get_all_localized_tag_labels(tag_name):
    return DCATAPITTagVocabulary.all_by_name(tag_name)
