
import logging
import re

import ckan.plugins.toolkit as toolkit
import ckanext.dcatapit.interfaces as interfaces

from pylons import config
from ckan.lib.cli import CkanCommand

from rdflib import Graph
from rdflib.namespace import SKOS, DC
from ckanext.dcat.profiles import namespaces

LANGUAGE_THEME_NAME = 'languages'
EUROPEAN_THEME_NAME = 'eu_themes'
LOCATIONS_THEME_NAME = 'places'
FREQUENCIES_THEME_NAME = 'frequencies'
FILETYPE_THEME_NAME = 'filetype'

log = logging.getLogger(__name__)


class DCATAPITCommands(CkanCommand):
    '''  A command for working with vocabularies
    Usage::
     # Loading a vocabulary
     paster --plugin=ckanext-dcatapit vocabulary load --url URL --name NAME --config=PATH_TO_INI_FILE
     paster --plugin=ckanext-dcatapit vocabulary load --filename FILE --name NAME --config=PATH_TO_INI_FILE
     Where:
       URL  is the url to a SKOS document
       FILE is the local path to a SKOS document
       NAME is the short-name of the vocabulary (only allowed languages, eu_themes, places, frequencies)
       Where the corresponding rdf are:
          languages   -> http://publications.europa.eu/mdr/resource/authority/language/skos/languages-skos.rdf
          eu_themes   -> http://publications.europa.eu/mdr/resource/authority/data-theme/skos/data-theme-skos.rdf
          places      -> http://publications.europa.eu/mdr/resource/authority/place/skos/places-skos.rdf
          frequencies -> http://publications.europa.eu/mdr/resource/authority/frequency/skos/frequencies-skos.rdf
          filetype -> http://publications.europa.eu/mdr/resource/authority/file-type/skos/filetypes-skos.rdf
       PATH_TO_INI_FILE is the path to the Ckan configuration file
    '''

    summary = __doc__.split('\n')[0]
    usage = __doc__

    places_theme_regex = '^ITA_.+'

    _locales_ckan_mapping = {
        'it': 'it',
        'de': 'de',
        'en': 'en_GB',
        'fr': 'fr',
        'es': 'es'
    }

    _ckan_language_theme_mapping = {
        'it': 'ITA',
        'de': 'DEU',
        'en_GB': 'ENG',
        'fr': 'FRA',
        'es': 'SPA'
    }

    _controlled_vocabularies_allowed = [EUROPEAN_THEME_NAME, LOCATIONS_THEME_NAME, LANGUAGE_THEME_NAME, FREQUENCIES_THEME_NAME, FILETYPE_THEME_NAME]

    def __init__(self, name):
        self.parser.add_option('--filename', dest='filename', default=None,
                               help='Path to a file')
        self.parser.add_option('--url', dest='url', default=None,
                               help='URL to a resource')
        self.parser.add_option('--name', dest='name', default=None,
                               help='Name of the vocabulary to work with')

        super(DCATAPITCommands, self).__init__(name)

    def command(self):
        '''
        Parse command line arguments and call appropriate method.
        '''
        cmd = self.args[0]
        self._load_config()

        if cmd == 'load':
            self.load()
        elif cmd == 'initdb':
            self.initdb()
        else:
            print self.usage
            log.error('ERROR: Command "%s" not recognized' % (cmd,))
            return

    def initdb(self):
        from ckanext.dcatapit.model import setup as db_setup
        db_setup()

    def load(self):
        ##
        # Checking command given options
        ##
        url = self.options.url
        filename = self.options.filename

        if not url and not filename:
            print "ERROR: No URL or FILENAME provided and one is required"
            print self.usage
            return

        vocab_name = self.options.name

        if not vocab_name:
            print "ERROR: No vocabulary name provided and is required"
            print self.usage
            return

        if vocab_name not in self._controlled_vocabularies_allowed:
            print "ERROR: Incorrect vocabulary name, only one of these values are allowed: {0}".format(self._controlled_vocabularies_allowed)
            print self.usage
            return

        if vocab_name == LANGUAGE_THEME_NAME:
            ckan_offered_languages = config.get('ckan.locales_offered', []).split(' ')
            for offered_language in ckan_offered_languages:
                if offered_language not in self._ckan_language_theme_mapping:
                    print "INFO: '{0}' CKAN locale is not mapped in this plugin and will be skipped during the import stage (vocabulary name '{1}')".format(offered_language, vocab_name)

        ##
        # Loading the RDF vocabulary
        ##
        print "Loading graph ..."

        g = Graph()
        for prefix, namespace in namespaces.iteritems():
            g.bind(prefix, namespace)

        try:
            if url:
                g.parse(location=url)
            else:
                g.parse(source=filename)
        except Exception,e:
            log.error("ERROR: Problem occurred while retrieving the document %r", e)
            print self.usage
            return

        concepts = []
        pref_labels = []

        for concept,_pred,_conc in g.triples((None, None, SKOS.Concept)):
            about = str(concept)
            identifier = str(g.value(subject=concept, predicate=DC.identifier))

            #
            # Skipping the ckan locales not mapped in this plugin (subject: language theme)
            #
            if vocab_name == 'languages' and identifier not in self._ckan_language_theme_mapping.values():
                continue

            #
            # Skipping the ckan places not in italy according to the regex (subject: places theme)
            #
            if vocab_name == 'places' and not re.match(self.places_theme_regex, identifier, flags=0):
                continue

            print 'Concept {0} ({1})'.format(about, identifier)
            concepts.append(identifier)

            for pref_label in g.objects(concept, SKOS.prefLabel):
                lang = pref_label.language
                label = pref_label.value

                print u'    Label {0}: {1}'.format(lang, label)
                pref_labels.append({
                    'name': identifier,
                    'lang': lang,
                    'localized_text': label
                })

        ##
        # Creating the Tag Vocabulary using the given name
        ##
        log.info('Creating tag vocabulary {0} ...'.format(vocab_name))

        user = toolkit.get_action('get_site_user')({'ignore_auth': True}, {})
        context = {'user': user['name'], 'ignore_auth': True}

        print "Using site user '{0}'".format(user['name'])

        try:
            data = {'id': vocab_name}
            toolkit.get_action('vocabulary_show')(context, data)

            log.info("Vocabulary {0} already exists, skipping...".format(vocab_name))
        except toolkit.ObjectNotFound:
            log.info("Creating vocabulary '{0}'".format(vocab_name))

            data = {'name': vocab_name}
            vocab = toolkit.get_action('vocabulary_create')(context, data)

            for tag in concepts:
                log.info("Adding tag {0} to vocabulary '{1}'".format(tag, vocab_name))

                data = {'name': tag, 'vocabulary_id': vocab['id']}
                toolkit.get_action('tag_create')(context, data)

        ##
        # Persisting Multilag Tags or updating existing
        ##
        log.info('Creating the corresponding multilang tags for vocab: {0} ...'.format(vocab_name))

        for pref_label in pref_labels:
            if pref_label['lang'] in self._locales_ckan_mapping:
                tag_name = pref_label['name']
                tag_lang = self._locales_ckan_mapping[pref_label['lang']]
                tag_localized_name = pref_label['localized_text']

                interfaces.persist_tag_multilang(tag_name, tag_lang, tag_localized_name, vocab_name)

        print 'Vocabulary successfully loaded ({0})'.format(vocab_name)

