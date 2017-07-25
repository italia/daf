
import nose
import ckanext.dcatapit.harvesters.utils as utils

eq_ = nose.tools.eq_
ok_ = nose.tools.ok_

csw_harvester_config = {
    "dataset_themes":"OP_DATPRO",
    "dataset_places":"ITA_BZO",
    "dataset_languages":"{ITA,DEU}",
    "frequency":"UNKNOWN",
    "agents":{
        "publisher":{
            "code":"p_bz",
            "role":"publisher",
            "code_regex":{
                "regex":"\\(([^)]+)\\:([^)]+)\\)",
                "groups":[2]
            },
            "name_regex":{
                "regex":"([^(]*)(\\(IPa[^)]*\\))(.+)",
                "groups":[1, 3]
            }
        },
        "owner":{
            "code":"p_bz",
            "role":"owner",
            "code_regex":{
                "regex":"\\(([^)]+)\\:([^)]+)\\)",
                "groups":[2]
            },
            "name_regex":{
                "regex":"([^(]*)(\\(IPa[^)]*\\))(.+)",
                "groups":[1, 3]
            }
        },
        "author":{
            "code":"p_bz",
            "role":"author",
            "code_regex":{
                "regex":"\\(([^)]+)\\:([^)]+)\\)",
                "groups":[2]
            },
            "name_regex":{
                "regex":"([^(]*)(\\(IPa[^)]*\\))(.+)",
                "groups":[1, 3]
            }
        }
    },
    "controlled_vocabularies":{
        "dcatapit_skos_theme_id":"theme.data-theme-skos",
        "dcatapit_skos_places_id":"theme.places-skos"
    }
}

responsiblePartys = [
    {
        'organisation-name': 'Provincia Autonoma di Bolzano (IPa: p_bz) - Ripartizione 28 - Natura, paesaggio e sviluppo del territorio',
        'role': 'publisher'
    }, {
        'organisation-name': 'Comune di Bolzano (IPa: c_a952) - Ufficio Sistema Informativo Territoriale',
        'role': 'author'
    }, {
        'organisation-name': 'Comune di Bolzano (IPa: c_a952) - Ufficio Sistema Informativo Territoriale',
        'role': 'owner'
    }
]

def test_get_responsible_party():
    name, code = utils.get_responsible_party(responsiblePartys, csw_harvester_config.get('agents').get('publisher'))

    eq_(name, 'Provincia Autonoma di Bolzano  - Ripartizione 28 - Natura, paesaggio e sviluppo del territorio')
    eq_(code, 'p_bz')

    name, code = utils.get_responsible_party(responsiblePartys, csw_harvester_config.get('agents').get('owner'))

    eq_(name, 'Comune di Bolzano  - Ufficio Sistema Informativo Territoriale')
    eq_(code, 'c_a952')

    name, code = utils.get_responsible_party(responsiblePartys, csw_harvester_config.get('agents').get('author'))

    eq_(name, 'Comune di Bolzano  - Ufficio Sistema Informativo Territoriale')
    eq_(code, 'c_a952')

