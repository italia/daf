
import nose
import ckanext.dcatapit.validators as validators

eq_ = nose.tools.eq_
ok_ = nose.tools.ok_

def test_is_blank():
    test_string = validators.is_blank("")
    eq_(test_string, True)

def test_couple_validator():
    test_couple = 'test1,test2'
    values = validators.couple_validator(test_couple, None)
    eq_(len(values), 11)

def test_no_number():
    test_number = "test"

    try:
        value = validators.no_number(test_number, None)
        ok_(value)
    except Exception:
        eq_(True, True)

def test_dcatapit_id_unique():
    '''
    result = helpers.call_action('package_create',
        name='test_dcatapit_package',
        identifier='4b6fe9ca-dc77-4cec-92a4-55c6624a5bd6',
        theme='{ECON,ENVI}',
        publisher_name='bolzano',
        publisher_identifier='234234234',
        modified='2016-11-29',
        holder_name='bolzano',
        holder_identifier='234234234',
        notes='dcatapit dataset di test',
        frequency='UPDATE_CONT',
        geographical_geonames_url='http://www.geonames.org/3181913')
    '''

    pass

