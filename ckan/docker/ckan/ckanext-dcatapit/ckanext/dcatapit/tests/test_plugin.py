
import nose
import ckanext.dcatapit.plugin as plugin

eq_ = nose.tools.eq_
ok_ = nose.tools.ok_


# ####################################
# DCATAPITPackagePlugin test methods #
# ####################################
package_plugin = plugin.DCATAPITPackagePlugin()

def test_package_plugin():
    ok_(package_plugin)

def test_package_i18n_domain():
    eq_(package_plugin.i18n_domain(), 'ckanext-dcatapit')

def test_package_create_schema():
    schema = package_plugin.create_package_schema()
    ok_(schema)

def test_package_update_schema():
    schema = package_plugin.update_package_schema()
    ok_(schema)

def test_package_show_package_schema():
    schema = package_plugin.show_package_schema()
    ok_(schema)

def test_package_if_falback():
    eq_(package_plugin.is_fallback(), True)

def test_package_package_types():
    eq_(package_plugin.package_types(), [])

def test_package_get_validators():
    validators = package_plugin.get_validators()
    ok_(validators.get('couple_validator', None))
    ok_(validators.get('no_number', None))
    ok_(validators.get('dcatapit_id_unique', None))

def test_package_get_helpers():
    helpers = package_plugin.get_helpers()
    ok_(helpers.get('get_dcatapit_package_schema', None))
    ok_(helpers.get('get_vocabulary_items', None))
    ok_(helpers.get('get_dcatapit_resource_schema', None))
    ok_(helpers.get('list_to_string', None))
    ok_(helpers.get('couple_to_html', None))
    ok_(helpers.get('couple_to_string', None))
    ok_(helpers.get('format', None))
    ok_(helpers.get('validate_dateformat', None))
    ok_(helpers.get('get_localized_field_value', None))

# ####################################
# DCATAPITPackagePlugin test methods #
# ####################################
organization_plugin = plugin.DCATAPITOrganizationPlugin()

def test_org_plugin():
    ok_(organization_plugin)

def test_organization_get_helpers():
    helpers = organization_plugin.get_helpers()
    ok_(helpers.get('get_dcatapit_organization_schema', None))

def test_organization_group_controller():
    eq_(organization_plugin.group_controller(), 'organization')

def test_organization_group_types():
    eq_(organization_plugin.group_types()[0], 'organization')

def test_org_form_to_db_schema_api_create():
    schema = organization_plugin.form_to_db_schema_api_create()
    ok_(schema)

def test_org_form_to_db_schema_api_update():
    schema = organization_plugin.form_to_db_schema_api_update()
    ok_(schema)

def test_org_db_to_form_schema():
    schema = organization_plugin.db_to_form_schema()
    ok_(schema)

def test_org_default_show_group_schema():
    schema = organization_plugin.default_show_group_schema()
    ok_(schema)

# ####################################
# DCATAPITPackagePlugin test methods #
# ####################################
configuration_plugin = plugin.DCATAPITConfigurerPlugin()

def test_config_plugin():
    ok_(configuration_plugin)

def test_config_update_config_schema():
    schema = configuration_plugin.update_config_schema({})
    ok_(schema)

def test_config_get_helpers():
    helpers = configuration_plugin.get_helpers()
    ok_(helpers.get('get_dcatapit_configuration_schema', None))
