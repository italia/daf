import json

import nose

from pylons import config

from dateutil.parser import parse as parse_date
from rdflib import URIRef, BNode, Literal
from rdflib.namespace import RDF

from geomet import wkt

try:
    from ckan.tests import helpers, factories
except ImportError:
    from ckan.new_tests import helpers, factories

from ckanext.dcat import utils
from ckanext.dcat.processors import RDFSerializer
from ckanext.dcat.profiles import (DCAT, DCT, ADMS, XSD, VCARD, FOAF, SCHEMA,
                                   SKOS, LOCN, GSP, OWL, SPDX, GEOJSON_IMT)
from ckanext.dcatapit.dcat.profiles import (DCATAPIT)

eq_ = nose.tools.eq_
assert_true = nose.tools.assert_true


class BaseSerializeTest(object):

    def _triples(self, graph, subject, predicate, _object, data_type=None):
        if not (isinstance(_object, URIRef) or isinstance(_object, BNode) or _object is None):
            if data_type:
                _object = Literal(_object, datatype=data_type)
            else:
                _object = Literal(_object)
        triples = [t for t in graph.triples((subject, predicate, _object))]
        return triples

    def _triple(self, graph, subject, predicate, _object, data_type=None):
        triples = self._triples(graph, subject, predicate, _object, data_type)
        return triples[0] if triples else None


class TestDCATAPITProfileSerializeDataset(BaseSerializeTest):

    def test_graph_from_dataset(self):

        dataset = {
            'id': '4b6fe9ca-dc77-4cec-92a4-55c6624a5bd6',
            'name': 'test-dataset',
            'title': 'Dataset di test DCAT_AP-IT',
            'notes': 'dcatapit dataset di test',
            'metadata_created': '2015-06-26T15:21:09.034694',
            'metadata_modified': '2015-06-26T15:21:09.075774',
            'tags': [{'name': 'Tag 1'}, {'name': 'Tag 2'}],
            'issued':'2016-11-29',
            'modified':'2016-11-29',
            'identifier':'ISBN',
            'temporal_start':'2016-11-01',
            'temporal_end':'2016-11-30',
            'frequency':'UPDATE_CONT',
            'publisher_name':'bolzano',
            'publisher_identifier':'234234234',
            'creator_name':'test',
            'creator_identifier':'412946129',
            'holder_name':'bolzano',
            'holder_identifier':'234234234',
            'alternate_identifier':'ISBN,TEST',
            'theme':'{ECON,ENVI}',
            'geographical_geonames_url':'http://www.geonames.org/3181913',
            'language':'{DEU,ENG,ITA}',
            'is_version_of':'http://dcat.geo-solutions.it/dataset/energia-da-fonti-rinnovabili2',
            'conforms_to':'{CONF1,CONF2,CONF3}'
        }

        s = RDFSerializer()
        g = s.g

        dataset_ref = s.graph_from_dataset(dataset)

        eq_(unicode(dataset_ref), utils.dataset_uri(dataset))

        # Basic fields
        assert self._triple(g, dataset_ref, RDF.type, DCATAPIT.Dataset)
        assert self._triple(g, dataset_ref, DCT.title, dataset['title'])
        assert self._triple(g, dataset_ref, DCT.description, dataset['notes'])

        assert self._triple(g, dataset_ref, DCT.identifier, dataset['identifier'])

        # Tags
        eq_(len([t for t in g.triples((dataset_ref, DCAT.keyword, None))]), 2)
        for tag in dataset['tags']:
            assert self._triple(g, dataset_ref, DCAT.keyword, tag['name'])


