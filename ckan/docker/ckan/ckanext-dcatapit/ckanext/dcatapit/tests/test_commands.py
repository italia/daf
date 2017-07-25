
import os
import nose
import ckanext.dcatapit.interfaces as interfaces

from ckanext.dcatapit.commands.dcatapit import DCATAPITCommands

eq_ = nose.tools.eq_
ok_ = nose.tools.ok_


class BaseOptions(object):

    def __init__(self, options):
        self.url = options.get("url", None)
        self.name = options.get("name", None)
        self.filename = options.get("filename", None)

class BaseCommandTest(object):

    def _get_file_contents(self, file_name):
        path = os.path.join(os.path.dirname(__file__),
                            '..', '..', '..', 'vocabularies',
                            file_name)
        return path


class TestDCATAPITCommand(BaseCommandTest):

    def test_vocabulary_command(self):
        dcatapit_commands = DCATAPITCommands('eu_themes')

        vocab_file_path = self._get_file_contents('data-theme-skos.rdf')

        options = BaseOptions({
            'url': vocab_file_path,
            'name': 'eu_themes'
        })

        setattr(dcatapit_commands, 'options', options)

        dcatapit_commands.initdb()
        dcatapit_commands.load()

        tag_localized = interfaces.get_localized_tag_name('ECON')
        ok_(tag_localized)