import logging
import cgi

import ckan 

import ckan.logic as logic
import ckan.lib.base as base
import ckan.plugins as p
import ckan.lib.maintain as maintain
import ckanext.multilang.helpers as helpers

from pylons import config

from pylons.i18n.translation import get_lang

from urllib import urlencode
from paste.deploy.converters import asbool
from ckan.lib.base import request
from ckan.lib.base import c, g, h
from ckan.lib.base import model
from ckan.lib.base import render
from ckan.lib.base import _

import ckan.lib.navl.dictization_functions as dict_fns

from ckan.lib.navl.validators import not_empty

from ckan.common import OrderedDict, _, json, request, c, g, response

from ckan.controllers.package import PackageController
from ckanext.multilang.model import PackageMultilang, GroupMultilang, ResourceMultilang, TagMultilang

from ckan.controllers.home import CACHE_PARAMETERS

log = logging.getLogger(__name__)

render = base.render
abort = base.abort
redirect = base.redirect

NotFound = logic.NotFound
NotAuthorized = logic.NotAuthorized
ValidationError = logic.ValidationError
get_action = logic.get_action
check_access = logic.check_access
clean_dict = logic.clean_dict
tuplize_dict = logic.tuplize_dict
parse_params = logic.parse_params

def _encode_params(params):
    return [(k, v.encode('utf-8') if isinstance(v, basestring) else str(v))
            for k, v in params]

def url_with_params(url, params):
    params = _encode_params(params)
    return url + u'?' + urlencode(params)

def search_url(params, package_type=None):
    if not package_type or package_type == 'dataset':
        url = h.url_for(controller='package', action='search')
    else:
        url = h.url_for('{0}_search'.format(package_type))
    return url_with_params(url, params)

class MultilangPackageController(PackageController):

    ## Managed localized fields for Package in package_multilang table
    pkg_localized_fields = [
        'title',
        'notes',
        'author',
        'maintainer',
        'url'
    ]

    """
       This controller overrides the core PackageController 
       for dataset list view search and dataset details page
    """

    def localized_tags_persist(self, extra_tag, pkg_dict, lang):
        if extra_tag:
            for tag in extra_tag:
                localized_tag = TagMultilang.by_name(tag.get('key'), lang)

                if localized_tag and localized_tag.text != tag.get('value'):
                    localized_tag.text = tag.get('value')
                    localized_tag.save()
                elif localized_tag is None:
                    # Find the tag id from the existing tags in dict
                    tag_id = None
                    for dict_tag in pkg_dict.get('tags'):
                        if dict_tag.get('name') == tag.get('key'):
                            tag_id = dict_tag.get('id')

                    if tag_id:
                        TagMultilang.persist({'id': tag_id, 'name': tag.get('key'), 'text': tag.get('value')}, lang)

    def _save_new(self, context, package_type=None):
        # The staged add dataset used the new functionality when the dataset is
        # partially created so we need to know if we actually are updating or
        # this is a real new.
        is_an_update = False
        ckan_phase = request.params.get('_ckan_phase')
        from ckan.lib.search import SearchIndexError
        try:
            data_dict = clean_dict(dict_fns.unflatten(
                tuplize_dict(parse_params(request.POST))))
            if ckan_phase:
                # prevent clearing of groups etc
                context['allow_partial_update'] = True
                # sort the tags
                if 'tag_string' in data_dict:
                    data_dict['tags'] = self._tag_string_to_list(
                        data_dict['tag_string'])
                if data_dict.get('pkg_name'):
                    is_an_update = True
                    # This is actually an update not a save
                    data_dict['id'] = data_dict['pkg_name']
                    del data_dict['pkg_name']
                    # don't change the dataset state
                    data_dict['state'] = 'draft'
                    # this is actually an edit not a save
                    pkg_dict = get_action('package_update')(context, data_dict)

                    if request.params['save'] == 'go-metadata':
                        # redirect to add metadata
                        url = h.url_for(controller='package',
                                        action='new_metadata',
                                        id=pkg_dict['name'])
                    else:
                        # redirect to add dataset resources
                        url = h.url_for(controller='package',
                                        action='new_resource',
                                        id=pkg_dict['name'])
                    redirect(url)
                # Make sure we don't index this dataset
                if request.params['save'] not in ['go-resource', 'go-metadata']:
                    data_dict['state'] = 'draft'
                # allow the state to be changed
                context['allow_state_change'] = True

            data_dict['type'] = package_type
            context['message'] = data_dict.get('log_message', '')

            #  MULTILANG - retrieving dict for localized tag's strings
            extra_tag = None
            if data_dict.get('extra_tag'):
                extra_tag = data_dict.get('extra_tag')
                # After saving in memory the extra_tag dict this must be removed because not present in the schema
                del data_dict['extra_tag']

            pkg_dict = get_action('package_create')(context, data_dict)

            lang = helpers.getLanguage()

            #  MULTILANG - persisting tags
            self.localized_tags_persist(extra_tag, pkg_dict, lang)

            # MULTILANG - persisting the localized package dict
            log.info('::::: Persisting localised metadata locale :::::')
            for field in self.pkg_localized_fields:
                if pkg_dict.get(field):
                    PackageMultilang.persist({'id': pkg_dict.get('id'), 'field': field, 'text': pkg_dict.get(field)}, lang)

            if ckan_phase:
                # redirect to add dataset resources
                url = h.url_for(controller='package',
                                action='new_resource',
                                id=pkg_dict['name'])
                redirect(url)

            self._form_save_redirect(pkg_dict['name'], 'new', package_type=package_type)
        except NotAuthorized:
            abort(401, _('Unauthorized to read package %s') % '')
        except NotFound, e:
            abort(404, _('Dataset not found'))
        except dict_fns.DataError:
            abort(400, _(u'Integrity Error'))
        except SearchIndexError, e:
            try:
                exc_str = unicode(repr(e.args))
            except Exception:  # We don't like bare excepts
                exc_str = unicode(str(e))
            abort(500, _(u'Unable to add package to search index.') + exc_str)
        except ValidationError, e:
            errors = e.error_dict
            error_summary = e.error_summary
            if is_an_update:
                # we need to get the state of the dataset to show the stage we
                # are on.
                pkg_dict = get_action('package_show')(context, data_dict)
                data_dict['state'] = pkg_dict['state']
                return self.edit(data_dict['id'], data_dict,
                                 errors, error_summary)
            data_dict['state'] = 'none'
            return self.new(data_dict, errors, error_summary)

    def _save_edit(self, name_or_id, context, package_type=None):        
        from ckan.lib.search import SearchIndexError
        log.debug('Package save request name: %s POST: %r',
                  name_or_id, request.POST)
        try:
            data_dict = clean_dict(dict_fns.unflatten(
                tuplize_dict(parse_params(request.POST))))
            if '_ckan_phase' in data_dict:
                # we allow partial updates to not destroy existing resources
                context['allow_partial_update'] = True
                if 'tag_string' in data_dict:
                    data_dict['tags'] = self._tag_string_to_list(
                        data_dict['tag_string'])
                del data_dict['_ckan_phase']
                del data_dict['save']
            context['message'] = data_dict.get('log_message', '')
            data_dict['id'] = name_or_id

            #  MULTILANG - retrieving dict for localized tag's strings
            extra_tag = None
            if data_dict.get('extra_tag'):
                extra_tag = data_dict.get('extra_tag')
                # After saving in memory the extra_tag dict this must be removed because not present in the schema
                del data_dict['extra_tag']

            pkg = get_action('package_update')(context, data_dict)
            
            c.pkg = context['package']
            c.pkg_dict = pkg

            lang = helpers.getLanguage()

            #  MULTILANG - persisting tags
            self.localized_tags_persist(extra_tag, c.pkg_dict, lang)

            #  MULTILANG - persisting package dict
            log.info(':::::::::::: Saving the corresponding localized title and abstract :::::::::::::::')
            
            # q_results = model.Session.query(PackageMultilang).filter(PackageMultilang.package_id == c.pkg_dict.get('id'), PackageMultilang.lang == lang).all()
            q_results = PackageMultilang.get_for_package_id_and_lang(c.pkg_dict.get('id'), lang) 
            
            if q_results:
                pkg_processed_field = []
                for result in q_results:
                    pkg_processed_field.append(result.field)
                    log.debug('::::::::::::::: value before %r', result.text)
                    result.text = c.pkg_dict.get(result.field)
                    log.debug('::::::::::::::: value after %r', result.text)
                    result.save()

                ## Check for missing localized fields in DB
                for field in self.pkg_localized_fields:
                    if field not in pkg_processed_field:
                        if c.pkg_dict.get(field):
                            PackageMultilang.persist({'id': c.pkg_dict.get('id'), 'field': field, 'text': c.pkg_dict.get(field)}, lang)
            else:
                log.info(':::::::::::: Localised fields are missing in package_multilang table, persisting defaults using values in the table package :::::::::::::::')
                for field in self.pkg_localized_fields:
                    if c.pkg_dict.get(field):
                        PackageMultilang.persist({'id': c.pkg_dict.get('id'), 'field': field, 'text': c.pkg_dict.get(field)}, lang)

            self._form_save_redirect(pkg['name'], 'edit', package_type=package_type)
        except NotAuthorized:
            abort(401, _('Unauthorized to read package %s') % id)
        except NotFound, e:
            abort(404, _('Dataset not found'))
        except dict_fns.DataError:
            abort(400, _(u'Integrity Error'))
        except SearchIndexError, e:
            try:
                exc_str = unicode(repr(e.args))
            except Exception:  # We don't like bare excepts
                exc_str = unicode(str(e))
            abort(500, _(u'Unable to update search index.') + exc_str)
        except ValidationError, e:
            errors = e.error_dict
            error_summary = e.error_summary
            return self.edit(name_or_id, data_dict, errors, error_summary)
