import ckanext.dcatapit.interfaces as interfaces

from ckan.common import _, ungettext
from ckan.plugins import PluginImplementations


def get_custom_config_schema(show=True):
	if show:
		return [
		    {
			    'name': 'ckanext.dcatapit_configpublisher_name',
			    'validator': ['not_empty'],
			    'element': 'input',
			    'type': 'text',
			    'label': _('Dataset Editor'),
			    'placeholder': _('dataset editor'),
			    'description': _('The responsible organization of the catalog'),
			    'is_required': True
		    },
		    {
			    'name': 'ckanext.dcatapit_configpublisher_code_identifier',
			    'validator': ['not_empty'],
			    'element': 'input',
			    'type': 'text',
			    'label': _('Catalog Organization Code'),
			    'placeholder': _('IPA/IVA'),
			    'description': _('The IVA/IPA code of the catalog organization'),
			    'is_required': True
		    },
		    {
			    'name': 'ckanext.dcatapit_config.catalog_issued',
			    'validator': ['ignore_missing'],
			    'element': 'input',
			    'type': 'date',
			    'label': _('Catalog Release Date'),
			    'format': '%d-%m-%Y',
			    'placeholder': _('catalog release date'),
			    'description': _('The creation date of the catalog'),
			    'is_required': False
		    }
		]
	else:
		return [
		    {
			    'name': 'ckanext.dcatapit_configpublisher_name',
			    'validator': ['not_empty']
		    },
		    {
			    'name': 'ckanext.dcatapit_configpublisher_code_identifier',
			    'validator': ['not_empty']
		    },
		    {
			    'name': 'ckanext.dcatapit_config.catalog_issued',
			    'validator': ['ignore_missing']
		    }
		]

def get_custom_organization_schema():
	return [
	    {
		    'name': 'email',
		    'validator': ['ignore_missing'],
		    'element': 'input',
		    'type': 'email',
		    'label': _('EMail'),
		    'placeholder': _('organization email'),
		    'is_required': True
	    },
	    {
		    'name': 'telephone',
		    'validator': ['ignore_missing'],
		    'element': 'input',
		    'type': 'text',
		    'label': _('Telephone'),
		    'placeholder': _('organization telephone'),
		    'is_required': False
	    },
	    {
		    'name': 'site',
		    'validator': ['ignore_missing'],
		    'element': 'input',
		    'type': 'url',
		    'label': _('Site URL'),
		    'placeholder': _('organization site url'),
		    'is_required': False
	    }
	]

def get_custom_package_schema():
	package_schema = [
	    {
		    'name': 'identifier',
		    'validator': ['not_empty', 'dcatapit_id_unique'],
		    'element': 'input',
		    'type': 'text',
		    'label': _('Dataset Identifier'),
		    'placeholder': _('dataset identifier'),
		    'is_required': True
	    },
	    {
		    'name': 'alternate_identifier',
		    'validator': ['ignore_missing', 'no_number'],
		    'element': 'input',
		    'type': 'text',
		    'label': _('Other Identifier'),
		    'placeholder': _('other identifier'),
		    'is_required': False
	    },
	    {
		    'name': 'theme',
		    'validator': ['not_empty'],
		    'element': 'theme',
		    'type': 'vocabulary',
		    'vocabulary_name': 'eu_themes',
		    'label': _('Dataset Themes'),
		    'placeholder': _('eg. education, agriculture, energy'),
		    'data_module_source': '/api/2/util/vocabulary/autocomplete?vocabulary_id=eu_themes&incomplete=?',
		    'is_required': True
	    },
	    {
		    'name': 'sub_theme',
		    'ignore': True,
		    'validator': ['ignore_missing'],
		    'element': 'theme',
		    'type': 'vocabulary',
		    'vocabulary_name': 'eurovoc',
		    'label': _('Sub Theme'),
		    'placeholder': _('sub theme of the dataset'),
			'data_module_source': '/api/2/util/vocabulary/autocomplete?vocabulary_id=eurovoc&incomplete=?',
		    'is_required': False
	    },
	    {
		    'name': 'publisher',
		    'element': 'couple',
		    'label': _('Dataset Editor'),
		    'is_required': True,
		    'couples': [
		    	{
		    		'name': 'publisher_name',
		    		'validator': ['not_empty'],
		    		'label': _('Name'),
		    		'type': 'text',
		    		'placeholder': _('publisher name'),
                	'localized': True
		    	},
			    {
		    		'name': 'publisher_identifier',
		    		'validator': ['not_empty'],
		    		'label': _('IPA/IVA'),
		    		'type': 'text',
		    		'placeholder': _('publisher identifier')
		    	}
		    ]
	    },
	    {
		    'name': 'issued',
		    'validator': ['ignore_missing'],
		    'element': 'input',
		    'type': 'date',
		    'label': _('Release Date'),
		    'format': '%d-%m-%Y',
		    'placeholder': _('release date'),
		    'is_required': False
	    },
	    {
		    'name': 'modified',
		    'validator': ['not_empty'],
		    'element': 'input',
		    'type': 'date',
		    'label': _('Modification Date'),
		    'format': '%d-%m-%Y',
		    'placeholder': _('modification date'),
		    'is_required': True
	    },
	    {
		    'name': 'geographical_name',
		    'validator': ['ignore_missing'],
		    'element': 'theme',
		    'type': 'vocabulary',
		    'vocabulary_name': 'places',
		    'label': _('Geographical Name'),
		    'placeholder': _('geographical name'),
		    'data_module_source': '/api/2/util/vocabulary/autocomplete?vocabulary_id=places&incomplete=?',
		    'is_required': False,
		    'default': _('Organizational Unit Responsible Competence Area')
	    },
	    {
		    'name': 'geographical_geonames_url',
		    'validator': ['ignore_missing'],
		    'element': 'input',
		    'type': 'url',
		    'label': _('GeoNames URL'),
		    'placeholder': _('http://www.geonames.org/3175395'),
		    'is_required': False,
	    },
	    {
		    'name': 'language',
		    'validator': ['ignore_missing'],
		    'element': 'theme',
		    'type': 'vocabulary',
		    'vocabulary_name': 'languages',
		    'label': _('Dataset Languages'),
		    'placeholder': _('eg. italian, german, english'),
		    'data_module_source': '/api/2/util/vocabulary/autocomplete?vocabulary_id=languages&incomplete=?',
		    'is_required': False
	    },
	    {
		    'name': 'temporal_coverage',
		    'element': 'couple',
		    'label': _('Temporal Coverage'),
		    'is_required': False,
		    'couples': [
		    	{
		    		'name': 'temporal_start',
		    		'label': _('Start Date'),
		    		'validator': ['ignore_missing'],
		    		'type': 'date',
		    		'format': '%d-%m-%Y',
		    		'placeholder': _('temporal coverage')
		    	},
			    {
		    		'name': 'temporal_end',
		    		'label': _('End Date'),
		    		'validator': ['ignore_missing'],
		    		'type': 'date',
		    		'format': '%d-%m-%Y',
		    		'placeholder': _('temporal coverage')
		    	}
		    ]
	    },
	    {
		    'name': 'frequency',
		    'validator': ['not_empty'],
		    'element': 'select',
		    'type': 'vocabulary',
		    'vocabulary_name': 'frequencies',
		    'label': _('Frequency'),
		    'placeholder': _('accrual periodicity'),
		    'data_module_source': '/api/2/util/vocabulary/autocomplete?vocabulary_id=frequencies&incomplete=?',
		    'is_required': True
	    },
	    {
		    'name': 'is_version_of',
		    'validator': ['ignore_missing'],
		    'element': 'input',
		    'type': 'url',
		    'label': _('Version Of'),
		    'placeholder': _('is version of a related dataset URI'),
		    'is_required': False
	    },
	    {
		    'name': 'conforms_to',
		    'validator': ['ignore_missing', 'no_number'],
		    'element': 'input',
		    'type': 'text',
		    'label': _('Conforms To'),
		    'placeholder': _('conforms to'),
		    'is_required': False
	    },
	    {
		    'name': 'rights_holder',
		    'element': 'couple',
		    'label': _('Rights Holder'),
		    'is_required': True,
		    'couples': [
		    	{
		    		'name': 'holder_name',
		    		'label': _('Name'),
		    		'validator': ['not_empty'],
		    		'type': 'text',
		    		'placeholder': _('rights holder of the dataset'),
		    		'localized': True

		    	},
			    {
		    		'name': 'holder_identifier',
		    		'label': _('IPA/IVA'),
		    		'validator': ['not_empty'],
		    		'type': 'text',
		    		'placeholder': _('rights holder of the dataset')
		    	}
		    ]
	    },
	    {
		    'name': 'creator',
		    'element': 'couple',
		    'label': _('Creator'),
		    'is_required': False,
		    'couples': [
		    	{
		    		'name': 'creator_name',
		    		'label': _('Name'),
		    		'validator': ['ignore_missing'],
		    		'type': 'text',
		    		'placeholder': _('creator of the dataset'),
		    		'localized': True
		    	},
			    {
		    		'name': 'creator_identifier',
		    		'label': _('IPA/IVA'),
		    		'validator': ['ignore_missing'],
		    		'type': 'text',
		    		'placeholder': _('creator of the dataset')
		    	}
		    ]
	    }
	]

	for plugin in PluginImplementations(interfaces.ICustomSchema):
		extra_schema = plugin.get_custom_schema()

		for extra in extra_schema:
			extra['external'] = True

		package_schema = package_schema + extra_schema

	return package_schema

def get_custom_resource_schema():
	return [
 		{
		    'name': 'distribution_format',
		    'validator': ['ignore_missing'],
		    'element': 'select',
		    'type': 'vocabulary',
		    'vocabulary_name': 'filetype',
		    'label': _('Distribution Format'),
		    'placeholder': _('distribution format'),
		    'data_module_source': '/api/2/util/vocabulary/autocomplete?vocabulary_id=filetype&incomplete=?',
		    'is_required': False
	    }
	]