import logging

import ckan.plugins as plugins
import ckan.plugins.toolkit as toolkit

import ckanext.multilang.helpers as helpers
import ckanext.multilang.actions as actions


import ckan.lib.dictization.model_dictize as model_dictize
import ckan.model as model

from routes.mapper import SubMapper, Mapper as _Mapper

from pylons.i18n import get_lang
from ckanext.multilang.model import PackageMultilang, GroupMultilang, TagMultilang, ResourceMultilang

log = logging.getLogger(__name__)


class MultilangPlugin(plugins.SingletonPlugin):
    plugins.implements(plugins.IConfigurer)
    plugins.implements(plugins.ITemplateHelpers)
    plugins.implements(plugins.IRoutes, inherit=True)
    plugins.implements(plugins.IPackageController, inherit=True)
    plugins.implements(plugins.IGroupController, inherit=True)
    plugins.implements(plugins.IOrganizationController, inherit=True)
    plugins.implements(plugins.IResourceController, inherit=True)
    plugins.implements(plugins.IActions, inherit=True)

    # IConfigurer
    def update_config(self, config_):
        toolkit.add_template_directory(config_, 'templates')
        toolkit.add_public_directory(config_, 'public')
        toolkit.add_resource('fanstatic', 'multilang')

    # see the ITemplateHelpers plugin interface.
    def get_helpers(self):
        return {
            'get_localized_pkg': helpers.get_localized_pkg,
            'get_localized_group': helpers.get_localized_group,
            'get_localized_resource': helpers.get_localized_resource
        }

    def get_actions(self):
        return {
            'group_list': actions.group_list,
            'organization_list': actions.organization_list
        }

    def before_map(self, map):
        map.connect('/dataset/edit/{id}', controller='ckanext.multilang.controllers.package:MultilangPackageController', action='edit')
        map.connect('/dataset/new', controller='ckanext.multilang.controllers.package:MultilangPackageController', action='new')
        return map

    def before_index(self, pkg_dict):
        from ckanext.multilang.model import PackageMultilang
        multilang_localized = PackageMultilang.get_for_package(pkg_dict['id'])

        for package in multilang_localized:
            log.debug('...Creating index for Package localized field: ' + package.field + ' - ' + package.lang)
            pkg_dict['package_multilang_localized_' + package.field + '_' + package.lang] = package.text
            log.debug('Index successfully created for Package: %r', pkg_dict.get('package_multilang_localized_' + package.field + '_' + package.lang))

        return pkg_dict

    def before_view(self, odict):        
        otype = odict.get('type')
        lang = helpers.getLanguage()

        if lang:
            if otype == 'group' or otype == 'organization':
                #  MULTILANG - Localizing Group/Organizzation names and descriptions in search list
                # q_results = model.Session.query(GroupMultilang).filter(GroupMultilang.group_id == odict.get('id'), GroupMultilang.lang == lang).all()
                q_results = GroupMultilang.get_for_group_id_and_lang(odict.get('id'), lang) 

                if q_results:
                    for result in q_results:
                        odict[result.field] = result.text
                        if result.field == 'title':
                            odict['display_name'] = result.text


            elif otype == 'dataset':
                #  MULTILANG - Localizing Datasets names and descriptions in search list
                #  MULTILANG - Localizing Tags display names in Facet list
                tags = odict.get('tags')
                if tags:
                    for tag in tags:
                        localized_tag = TagMultilang.by_tag_id(tag.get('id'), lang)

                        if localized_tag:
                            tag['display_name'] = localized_tag.text

                #  MULTILANG - Localizing package sub dict for the dataset read page
                # q_results = model.Session.query(PackageMultilang).filter(PackageMultilang.package_id == odict['id'], PackageMultilang.lang == lang).all()
                q_results = PackageMultilang.get_for_package_id_and_lang(odict.get('id'), lang) 

                if q_results:
                    for result in q_results:
                    	if odict.get(result.field, None):
                    		odict[result.field] = result.text
                    	else:
                    		extras = odict.get('extras', None)
                    		if extras and len(extras) > 0:
                    			for extra in extras:
                    				extra_key = extra.get('key', None)
                    				if extra_key and extra_key == result.field:
                    					extra['value'] = result.text

                        #if result.field == 'notes':
                        #    odict['notes'] = result.text

                #  MULTILANG - Localizing organization sub dict for the dataset read page
                organization = odict.get('organization')
                if organization:
                    # q_results = model.Session.query(GroupMultilang).filter(GroupMultilang.group_id == organization.get('id'), GroupMultilang.lang == lang).all() 
                    q_results = GroupMultilang.get_for_group_id_and_lang(organization.get('id'), lang)
                    
                    if q_results:
                        for result in q_results:
                            organization[result.field] = result.text

                    odict['organization'] = organization

                #  MULTILANG - Localizing resources dict
                resources = odict.get('resources')
                if resources:
                    for resource in resources:
                        # q_results = model.Session.query(ResourceMultilang).filter(ResourceMultilang.resource_id == resource.get('id'), ResourceMultilang.lang == lang).all()
                        q_results = ResourceMultilang.get_for_resource_id_and_lang(resource.get('id'), lang)
                
                        if q_results:
                            for result in q_results:
                                resource[result.field] = result.text

        return odict

    def after_search(self, search_results, search_params):
        search_facets = search_results.get('search_facets')
        lang = helpers.getLanguage()

        if search_facets and lang:
            if 'tags' in search_facets:
                #  MULTILANG - Localizing Tags display names in Facet list
                tags = search_facets.get('tags')
                for tag in tags.get('items'):
                    localized_tag = TagMultilang.by_name(tag.get('name'), lang)

                    if localized_tag:
                        tag['display_name'] = localized_tag.text
            
            if 'organization' in search_facets:
                #  MULTILANG - Localizing Organizations display names in Facet list
                organizations = search_facets.get('organization')
                for org in organizations.get('items'):
                    # q_results = model.Session.query(GroupMultilang).filter(GroupMultilang.name == org.get('name'), GroupMultilang.lang == lang).all()
                    q_results = GroupMultilang.get_for_group_name_and_lang(org.get('name'), lang) 

                    if q_results:
                        for result in q_results:
                            if result.field == 'title':
                                org['display_name'] = result.text

            if 'groups' in search_facets:
                #  MULTILANG - Localizing Groups display names in Facet list
                groups = search_facets.get('groups')
                for group in groups.get('items'):
                    # q_results = model.Session.query(GroupMultilang).filter(GroupMultilang.name == group.get('name'), GroupMultilang.lang == lang).all()
                    q_results = GroupMultilang.get_for_group_name_and_lang(group.get('name'), lang)

                    if q_results:
                        for result in q_results:
                            if result.field == 'title':
                                group['display_name'] = result.text

        search_results['search_facets'] = search_facets
        return search_results

    ## ##############
    ## CREATE 
    ## ##############
    def create(self, model_obj):
        otype = model_obj.type
        lang = helpers.getLanguage()

        ## CREATE GROUP OR ORGANIZATION
        if otype == 'group' or otype == 'organization' and lang:
            log.info('::::: Persisting localised metadata locale :::::')
            lang = helpers.getLanguage()

            group = model_dictize.group_dictize(model_obj, {'model': model, 'session': model.Session})

            GroupMultilang.persist(group, lang)

    ## ##############
    ## EDIT
    ## ##############
    def edit(self, model_obj):     
        otype = model_obj.type
        lang = helpers.getLanguage()

        ## EDIT GROUP OR ORGANIZATION
        if otype == 'group' or otype == 'organization' and lang:
            group = model_dictize.group_dictize(model_obj, {'model': model, 'session': model.Session})

            # q_results = model.Session.query(GroupMultilang).filter(GroupMultilang.group_id == group.get('id')).all()
            q_results = GroupMultilang.get_for_group_id(group.get('id'))

            create_new = False
            if q_results:
                available_db_lang = []

                for result in q_results:
                    if result.lang not in available_db_lang:
                        available_db_lang.append(result.lang)

                    # check if the group identifier name has been changed
                    if result.name != group.get('name'):
                        result.name = group.get('name')
                        result.save()

                if lang not in available_db_lang:
                    create_new = True
                else:
                    for result in q_results:
                        if result.lang == lang:
                            result.text = group.get(result.field)
                            result.save()
            else:
                create_new = True

            if create_new == True:
                log.info(':::::::::::: Localized fields are missing in package_multilang table, persisting defaults using values in the table group :::::::::::::::')
                GroupMultilang.persist(group, lang)
        
    ## ##############
    ## DELETE
    ## ##############
    def delete(self, model_obj):
        return model_obj

    def before_show(self, resource_dict):
        lang = helpers.getLanguage()
        if lang:            
            #  MULTILANG - Localizing resources dict
            # q_results = model.Session.query(ResourceMultilang).filter(ResourceMultilang.resource_id == resource_dict.get('id'), ResourceMultilang.lang == lang).all()
            q_results = ResourceMultilang.get_for_resource_id_and_lang(resource_dict.get('id'), lang)

            if q_results:
                for result in q_results:
                    resource_dict[result.field] = result.text

        return resource_dict

    def after_update(self, context, resource):
        otype = resource.get('type')
        lang = helpers.getLanguage()

        if otype != 'dataset' and lang:
            # r = model.Session.query(model.Resource).filter(model.Resource.id == resource.get('id')).all()
            r = model.Resource.get(resource.get('id'))
            if r:
                r = model_dictize.resource_dictize(r, {'model': model, 'session': model.Session})

                # MULTILANG - persisting resource localized record in multilang table
                # q_results = model.Session.query(ResourceMultilang).filter(ResourceMultilang.resource_id == resource.get('id'), ResourceMultilang.lang == lang).all()
                q_results = ResourceMultilang.get_for_resource_id_and_lang(r.get('id'), lang)
                if q_results and q_results.count() > 0:
                    for result in q_results:
                        result.text = r.get(result.field)
                        result.save()
                else:
                    log.info('Localised fields are missing in resource_multilang table, persisting ...')
                    ResourceMultilang.persist(r, lang)

    def after_create(self, context, resource):
        otype = resource.get('type')
        lang = helpers.getLanguage()

        if otype != 'dataset' and lang:
            #  MULTILANG - Creating new resource for multilang table
            # r = model.Session.query(model.Resource).filter(model.Resource.id == resource.get('id')).all()
            r = model.Resource.get(resource.get('id'))
            if r:
                log.info('Localised fields are missing in resource_multilang table, persisting ...')
                ResourceMultilang.persist(resource, lang)


