
import sys
import logging

from sqlalchemy import types, Column, Table, ForeignKey
from sqlalchemy import orm

from ckan.lib.base import config
from ckan import model
from ckan.model import Session
from ckan.model import meta
from ckan.model.domain_object import DomainObject

from ckan import model

log = logging.getLogger(__name__)

__all__ = ['DCATAPITTagVocabulary', 'dcatapit_vocabulary_table', 'setup']

dcatapit_vocabulary_table = Table('dcatapit_vocabulary', meta.metadata,
    Column('id', types.Integer, primary_key=True),
    Column('tag_id', types.UnicodeText, ForeignKey("tag.id", ondelete="CASCADE"), nullable=False),
    Column('tag_name', types.UnicodeText, nullable=False, index=True),
    Column('lang', types.UnicodeText, nullable=False, index=True),
    Column('text', types.UnicodeText, nullable=False, index=True))

def setup():
    log.debug('DCAT_AP-IT tables defined in memory')

    #Setting up tag multilang table
    if not dcatapit_vocabulary_table.exists():
        try:
            dcatapit_vocabulary_table.create()
        except Exception,e:
            # Make sure the table does not remain incorrectly created
            if dcatapit_vocabulary_table.exists():
                Session.execute('DROP TABLE dcatapit_vocabulary')
                Session.commit()

            raise e

        log.info('DCATAPIT Tag Vocabulary table created')
    else:
        log.info('DCATAPIT Tag Vocabulary table already exist')

class DCATAPITTagVocabulary(DomainObject):
    def __init__(self, tag_id=None, tag_name=None, lang=None, text=None):
        self.tag_id = tag_id
        self.tag_name = tag_name
        self.lang = lang
        self.text = text

    @classmethod
    def by_name(self, tag_name, tag_lang, autoflush=True):
        query = meta.Session.query(DCATAPITTagVocabulary).filter(DCATAPITTagVocabulary.tag_name==tag_name, DCATAPITTagVocabulary.lang==tag_lang)
        query = query.autoflush(autoflush)
        tag = query.first()
        return tag

    @classmethod
    def all_by_name(self, tag_name, autoflush=True):
        query = meta.Session.query(DCATAPITTagVocabulary).filter(DCATAPITTagVocabulary.tag_name==tag_name)
        query = query.autoflush(autoflush)
        tags = query.all()

        ret = {}
        for record in tags:
            ret[record.lang] = record.text

        return ret

    @classmethod
    def by_tag_id(self, tag_id, tag_lang, autoflush=True):
        query = meta.Session.query(DCATAPITTagVocabulary).filter(DCATAPITTagVocabulary.tag_id==tag_id, DCATAPITTagVocabulary.lang==tag_lang)
        query = query.autoflush(autoflush)
        tag = query.first()
        return tag

    @classmethod
    def persist(self, tag, lang):
        session = meta.Session
        try:
            session.add_all([
                DCATAPITTagVocabulary(tag_id=tag.get('id'), tag_name=tag.get('name'), lang=lang, text=tag.get('text')),
            ])

            session.commit()
        except Exception, e:
            # on rollback, the same closure of state
            # as that of commit proceeds. 
            session.rollback()

            log.error('Exception occurred while persisting DB objects: %s', e)
            raise

meta.mapper(DCATAPITTagVocabulary, dcatapit_vocabulary_table)