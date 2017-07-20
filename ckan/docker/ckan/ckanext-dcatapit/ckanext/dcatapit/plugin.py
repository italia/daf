import logging

from ckan import logic
from ckan import lib

import ckan.plugins as plugins
import ckan.plugins.toolkit as toolkit

import ckan.plugins as plugins

import ckanext.dcatapit.validators as validators
import ckanext.dcatapit.schema as dcatapit_schema
import ckanext.dcatapit.helpers as helpers
import ckanext.dcatapit.interfaces as interfaces

from routes.mapper import SubMapper, Mapper as _Mapper

try:
    from ckan.lib.plugins import DefaultTranslation
except ImportError:
    class DefaultTranslation():
        pass

log = logging.getLogger(__file__)


class DCATAPITPackagePlugin(plugins.SingletonPlugin, toolkit.DefaultDatasetForm, DefaultTranslation):

    # IDatasetForm
    plugins.implements(plugins.IDatasetForm)
    
    # IConfigurer
    plugins.implements(plugins.IConfigurer)
    
    # IValidators
    plugins.implements(plugins.IValidators)
    
    # ITemplateHelpers
    plugins.implements(plugins.ITemplateHelpers)
    
    # IRoutes
    plugins.implements(plugins.IRoutes, inherit=True)
    
    # IPackageController
    plugins.implements(plugins.IPackageController, inherit=True)
    
    # ITranslation
    if toolkit.check_ckan_version(min_version='2.5.0'):
        plugins.implements(plugins.ITranslation, inherit=True)

    # ------------- ITranslation ---------------#

    def i18n_domain(self):
        '''Change the gettext domain handled by this plugin
        This implementation assumes the gettext domain is
        ckanext-{extension name}, hence your pot, po and mo files should be
        named ckanext-{extension name}.mo'''
        return 'ckanext-{name}'.format(name='dcatapit')

    # ------------- IRoutes ---------------#
    
    def before_map(self, map):
        GET = dict(method=['GET'])

        # /api/util ver 1, 2 or none
        with SubMapper(map, controller='ckanext.dcatapit.controllers.api:DCATAPITApiController', path_prefix='/api{ver:/1|/2|}',
                       ver='/1') as m:
            m.connect('/util/vocabulary/autocomplete', action='vocabulary_autocomplete',
                      conditions=GET)
        return map
    
    # ------------- IConfigurer ---------------#

    def update_config(self, config_):
        toolkit.add_template_directory(config_, 'templates')
        toolkit.add_public_directory(config_, 'public')
        toolkit.add_resource('fanstatic', 'ckanext-dcatapit')

    # ------------- IDatasetForm ---------------#

    def update_schema_field(self, schema, field):
        validators = []
        for validator in field['validator']:
            validators.append(toolkit.get_validator(validator))

        converters = [toolkit.get_converter('convert_to_extras')]

        schema.update({
            field['name']: validators + converters
        })

    def _modify_package_schema(self, schema):

        ##
        # Getting custom package schema
        ##

        for field in dcatapit_schema.get_custom_package_schema():
            if 'ignore' in field and field['ignore'] == True:
                continue

            if 'couples' in field:
                for couple in field['couples']:
                    self.update_schema_field(schema, couple)
            else:
                self.update_schema_field(schema, field)

        schema.update({
            'notes': [
                toolkit.get_validator('not_empty')
            ]
        })

        ##
        # Getting custom resource schema
        ##

        for field in dcatapit_schema.get_custom_resource_schema():
            if 'ignore' in field and field['ignore'] == True:
                continue

            validators = []
            for validator in field['validator']:
                validators.append(toolkit.get_validator(validator))

            schema['resources'].update({
                field['name']: validators
            })

        log.debug("Schema updated for DCAT_AP-TI:  %r", schema)

        return schema

    def create_package_schema(self):
        schema = super(DCATAPITPackagePlugin, self).create_package_schema()
        schema = self._modify_package_schema(schema)
        return schema

    def update_package_schema(self):
        schema = super(DCATAPITPackagePlugin, self).update_package_schema()
        schema = self._modify_package_schema(schema)
        return schema

    def update_show_schema_field(self, schema, field):
        validators = []
        for validator in field['validator']:
            validators.append(toolkit.get_validator(validator))

        converters = [toolkit.get_converter('convert_from_extras')]

        schema.update({
            field['name']: converters + validators
        })

    def show_package_schema(self):
        schema = super(DCATAPITPackagePlugin, self).show_package_schema()
        
        ##
        # Getting custom package schema
        ##

        for field in dcatapit_schema.get_custom_package_schema():
            if 'ignore' in field and field['ignore'] == True:
                continue

            if 'couples' in field:
                for couple in field['couples']:
                    self.update_show_schema_field(schema, couple)
            else:
                self.update_show_schema_field(schema, field)

        schema.update({
            'notes': [
                toolkit.get_validator('not_empty')
            ]
        })

        ##
        # Getting custom resource schema
        ##

        for field in dcatapit_schema.get_custom_resource_schema():
            if 'ignore' in field and field['ignore'] == True:
                continue

            validators = []
            for validator in field['validator']:
                validators.append(toolkit.get_validator(validator))

            schema['resources'].update({
                field['name']: validators
            })

        log.debug("Schema updated for DCAT_AP-TI:  %r", schema)

        return schema

    def is_fallback(self):
        # Return True to register this plugin as the default handler for
        # package types not handled by any other IDatasetForm plugin.
        return True
        
    def package_types(self):
        # This plugin doesn't handle any special package types, it just
        # registers itself as the default (above).
        return []

    # ------------- IValidators ---------------#

    def get_validators(self):
        return {
            'couple_validator': validators.couple_validator,
            'no_number': validators.no_number,
            'dcatapit_id_unique': validators.dcatapit_id_unique
        }

    # ------------- ITemplateHelpers ---------------#

    def get_helpers(self):
        return {
            'get_dcatapit_package_schema': helpers.get_dcatapit_package_schema,
            'get_vocabulary_items': helpers.get_vocabulary_items,
            'get_dcatapit_resource_schema': helpers.get_dcatapit_resource_schema,
            'list_to_string': helpers.list_to_string,
            'couple_to_html': helpers.couple_to_html,
            'couple_to_string': helpers.couple_to_string,
            'format': helpers.format,
            'validate_dateformat': helpers.validate_dateformat,
            'get_localized_field_value': helpers.get_localized_field_value,
            'get_package_resource_dcatapit_format_list': helpers.get_package_resource_dcatapit_format_list
        }

    # ------------- IPackageController ---------------#

    def after_create(self, context, pkg_dict):
        # During the harvest the get_lang() is not defined
        lang = interfaces.get_language()
        otype = pkg_dict.get('type')

        if lang and otype == 'dataset':    
            for extra in pkg_dict.get('extras'):
                for field in dcatapit_schema.get_custom_package_schema():

                    couples = field.get('couples', [])
                    if couples and len(couples) > 0:
                        for couple in couples:
                            if extra.get('key') == couple.get('name', None) and couple.get('localized', False) == True:
                                log.debug(':::::::::::::::Localizing custom schema field: %r', couple['name'])
                                # Create the localized field record
                                self.create_loc_field(extra, lang, pkg_dict.get('id'))
                    else:
                        if extra.get('key') == field.get('name', None) and field.get('localized', False) == True:
                            log.debug(':::::::::::::::Localizing custom schema field: %r', field['name'])
                            # Create the localized field record
                            self.create_loc_field(extra, lang, pkg_dict.get('id'))

    def after_update(self, context, pkg_dict):
        # During the harvest the get_lang() is not defined
        lang = interfaces.get_language()
        otype = pkg_dict.get('type')

        if lang and otype == 'dataset':             
            for extra in pkg_dict.get('extras'):
                for field in dcatapit_schema.get_custom_package_schema():
                    couples = field.get('couples', [])
                    if couples and len(couples) > 0:
                        for couple in couples:
                            self.update_loc_field(extra, pkg_dict.get('id'), couple, lang)
                    else:
                        self.update_loc_field(extra, pkg_dict.get('id'), field, lang)

    def after_search(self, search_results, search_params):
        ## ##################################################################### ##
        # This method move the dcatapit fields into the extras array (needed for  #
        # the Ckan base harevsting).                                              #
        # Basically dynamically rever what did by the 'convert_from_extras' to    #
        # allow harvesting the plugin's custom fields.                            #
        ## ##################################################################### ##
        search_dicts = search_results.get('results', [])

        dcatapit_schema_fields = dcatapit_schema.get_custom_package_schema()

        for _dict in search_dicts:
            _dict_extras = _dict.get('extras', [])

            for field in dcatapit_schema_fields:
                field_couple = field.get('couples', [])
                if len(field_couple) > 0:
                    for couple in field_couple:
                        self.manage_extras_for_search(couple, _dict, _dict_extras)
                else:
                    self.manage_extras_for_search(field, _dict, _dict_extras)

        return search_results

    def manage_extras_for_search(self, field, _dict, _dict_extras):
        field_name = field.get('name', None)

        if field_name:
            field_value = _dict.get(field_name, None)

            if field_value:
                _dict_extras.append({'key': field_name, 'value': field_value})
                del _dict[field_name]

    def update_loc_field(self, extra, pkg_id, field, lang):
        interfaces.update_extra_package_multilang(extra, pkg_id, field, lang)

    def create_loc_field(self, extra, lang, pkg_id): 
        interfaces.save_extra_package_multilang({'id': pkg_id, 'text': extra.get('value'), 'field': extra.get('key')}, lang, 'extra')


class DCATAPITOrganizationPlugin(plugins.SingletonPlugin, toolkit.DefaultGroupForm):

    # IConfigurer
    plugins.implements(plugins.IConfigurer)

    # ITemplateHelpers
    plugins.implements(plugins.ITemplateHelpers)

    # IGroupForm
    plugins.implements(plugins.IGroupForm, inherit=True)
    
    # ------------- IConfigurer ---------------#

    def update_config(self, config_):
        toolkit.add_template_directory(config_, 'templates')
        toolkit.add_public_directory(config_, 'public')
        toolkit.add_resource('fanstatic', 'ckanext-dcatapit')

    # ------------- ITemplateHelpers ---------------#

    def get_helpers(self):
        return {
            'get_dcatapit_organization_schema': helpers.get_dcatapit_organization_schema
        }

    # ------------- IGroupForm ---------------#

    def group_controller(self):
        return 'organization'
        
    def group_form(self):
        return 'organization/new_organization_form.html'

    def setup_template_variables(self, context, data_dict):
        pass

    def new_template(self):
        return 'organization/new.html'

    def about_template(self):
        return 'organization/about.html'

    def index_template(self):
        return 'organization/index.html'

    def admins_template(self):
        return 'organization/admins.html'

    def bulk_process_template(self):
        return 'organization/bulk_process.html'

    def read_template(self):
        return 'organization/read.html'

    # don't override history_template - use group template for history

    def edit_template(self):
        return 'organization/edit.html'

    def activity_template(self):
        return 'organization/activity_stream.html'

    def is_fallback(self):
        # Return True to register this plugin as the default handler for
        # group types not handled by any other IGroupForm plugin.
        return False

    def group_types(self):
        # This plugin doesn't handle any special group types, it just
        # registers itself as the default (above).
        return ['organization']

    def form_to_db_schema_options(self, options):
        ''' This allows us to select different schemas for different
        purpose eg via the web interface or via the api or creation vs
        updating. It is optional and if not available form_to_db_schema
        should be used.
        If a context is provided, and it contains a schema, it will be
        returned.
        '''
        schema = options.get('context', {}).get('schema', None)
        if schema:
            return schema

        if options.get('api'):
            if options.get('type') == 'create':
                return self.form_to_db_schema_api_create()
            else:
                return self.form_to_db_schema_api_update()
        else:
            return self.form_to_db_schema()

    def form_to_db_schema_api_create(self):
        schema = super(DCATAPITOrganizationPlugin, self).form_to_db_schema_api_create()
        schema = self._modify_group_schema(schema)
        return schema

    def form_to_db_schema_api_update(self):
        schema = super(DCATAPITOrganizationPlugin, self).form_to_db_schema_api_update()
        schema = self._modify_group_schema(schema)
        return schema

    def form_to_db_schema(self):
        schema = super(DCATAPITOrganizationPlugin, self).form_to_db_schema()
        schema = self._modify_group_schema(schema)
        return schema

    def _modify_group_schema(self, schema):
        for field in dcatapit_schema.get_custom_organization_schema():

            validators = []
            for validator in field['validator']:
                validators.append(toolkit.get_validator(validator))

            schema.update({
                field['name']: validators + [
                    toolkit.get_converter('convert_to_extras')
                ]
            })

        return schema

    def db_to_form_schema(self):
        '''This is an interface to manipulate data from the database
        into a format suitable for the form (optional)'''
        schema = self.default_show_group_schema()

        for field in dcatapit_schema.get_custom_organization_schema():

            validators = []
            for validator in field['validator']:
                validators.append(toolkit.get_validator(validator))

            schema.update({
                field['name']: [
                    toolkit.get_converter('convert_from_extras')
                ] + validators
            })

        return schema

    def default_show_group_schema(self):
        schema = logic.schema.default_group_schema()

        # make default show schema behave like when run with no validation
        schema['num_followers'] = []
        schema['created'] = []
        schema['display_name'] = []
        #schema['extras'] = {'__extras': [ckan.lib.navl.validators.keep_extras]}
        schema['package_count'] = []
        schema['packages'] = {'__extras': [lib.navl.validators.keep_extras]}
        schema['revision_id'] = []
        schema['state'] = []
        schema['users'] = {'__extras': [lib.navl.validators.keep_extras]}

        return schema

class DCATAPITConfigurerPlugin(plugins.SingletonPlugin):

    # IConfigurer
    plugins.implements(plugins.IConfigurer)

    # ITemplateHelpers
    plugins.implements(plugins.ITemplateHelpers)
    
    # ------------- IConfigurer ---------------#

    def update_config(self, config):
        # Add extension templates directory
        toolkit.add_template_directory(config, 'templates')

    def update_config_schema(self, schema):        
        for field in dcatapit_schema.get_custom_config_schema(False):

            validators = []
            for validator in field['validator']:
                validators.append(toolkit.get_validator(validator))

            schema.update({
                field['name']: validators
            })

        return schema

    # ------------- ITemplateHelpers ---------------#

    def get_helpers(self):
        return {
            'get_dcatapit_configuration_schema': helpers.get_dcatapit_configuration_schema
        }
