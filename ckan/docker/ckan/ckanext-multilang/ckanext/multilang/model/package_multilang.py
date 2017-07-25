
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

__all__ = ['PackageMultilang', 'package_multilang_table', 'GroupMultilang', 'ResourceMultilang', 'group_multilang_table', 'TagMultilang', 'tag_multilang_table', 'setup']

package_multilang_table = Table('package_multilang', meta.metadata,
    Column('id', types.Integer, primary_key=True),
    Column('package_id', types.UnicodeText, ForeignKey("package.id", ondelete="CASCADE"), nullable=False),
    Column('field', types.UnicodeText, nullable=False, index=True),
    Column('field_type', types.UnicodeText, nullable=False, index=True),
    Column('lang', types.UnicodeText, nullable=False, index=True),
    Column('text', types.UnicodeText, nullable=False, index=True))

group_multilang_table = Table('group_multilang', meta.metadata,
    Column('id', types.Integer, primary_key=True),
    Column('group_id', types.UnicodeText, ForeignKey("group.id", ondelete="CASCADE"), nullable=False),
    Column('name', types.UnicodeText, nullable=False, index=True),
    Column('field', types.UnicodeText, nullable=False, index=True),
    Column('lang', types.UnicodeText, nullable=False, index=True),
    Column('text', types.UnicodeText, nullable=False, index=True))

resource_multilang_table = Table('resource_multilang', meta.metadata,
    Column('id', types.Integer, primary_key=True),
    Column('resource_id', types.UnicodeText, ForeignKey("resource.id", ondelete="CASCADE"), nullable=False),
    Column('field', types.UnicodeText, nullable=False, index=True),
    Column('lang', types.UnicodeText, nullable=False, index=True),
    Column('text', types.UnicodeText, nullable=False, index=True))

tag_multilang_table = Table('tag_multilang', meta.metadata,
    Column('id', types.Integer, primary_key=True),
    Column('tag_id', types.UnicodeText, ForeignKey("tag.id", ondelete="CASCADE"), nullable=False),
    Column('tag_name', types.UnicodeText, nullable=False, index=True),
    Column('lang', types.UnicodeText, nullable=False, index=True),
    Column('text', types.UnicodeText, nullable=False, index=True))

def setup():
    log.debug('Multilingual tables defined in memory')

    #Setting up package multilang table
    if not package_multilang_table.exists():
        try:
            package_multilang_table.create()
        except Exception,e:
            # Make sure the table does not remain incorrectly created
            if package_multilang_table.exists():
                Session.execute('DROP TABLE package_multilang')
                Session.commit()

            raise e

        log.info('Package Multilingual table created')
    else:
        log.info('Package Multilingual table already exist')
    
    #Setting up group multilang table
    if not group_multilang_table.exists():
        try:
            group_multilang_table.create()
        except Exception,e:
            # Make sure the table does not remain incorrectly created
            if group_multilang_table.exists():
                Session.execute('DROP TABLE group_multilang')
                Session.commit()

            raise e

        log.info('Group Multilingual table created')
    else:
        log.info('Group Multilingual table already exist')

    #Setting up resource multilang table
    if not resource_multilang_table.exists():
        try:
            resource_multilang_table.create()
        except Exception,e:
            # Make sure the table does not remain incorrectly created
            if resource_multilang_table.exists():
                Session.execute('DROP TABLE resource_multilang')
                Session.commit()

            raise e

        log.info('Resource Multilingual table created')
    else:
        log.info('Resource Multilingual table already exist')

    #Setting up tag multilang table
    if not tag_multilang_table.exists():
        try:
            tag_multilang_table.create()
        except Exception,e:
            # Make sure the table does not remain incorrectly created
            if tag_multilang_table.exists():
                Session.execute('DROP TABLE tag_multilang')
                Session.commit()

            raise e

        log.info('Tag Multilingual table created')
    else:
        log.info('Tag Multilingual table already exist')

class PackageMultilang(DomainObject):
    def __init__(self, package_id=None, field=None, field_type=None, lang=None, text=None):
        self.package_id = package_id
        self.field = field
        self.lang = lang
        self.field_type = field_type
        self.text = text

    @classmethod
    def get(self, pkg_id, field, pkg_lang, field_type):
        obj = meta.Session.query(self).autoflush(False)
        record = obj.filter_by(package_id=pkg_id, field=field, lang=pkg_lang, field_type=field_type).first()
        return record

    @classmethod
    def get_for_package(self, package_id):
        obj = meta.Session.query(self).autoflush(False)
        records = obj.filter(self.package_id == package_id).all()  
        return records

    @classmethod
    def get_for_package_id_and_lang(self, pkg_id, pkg_lang):
        obj = meta.Session.query(self).autoflush(False)
        records = obj.filter_by(package_id=pkg_id, lang=pkg_lang)
        return records

    @classmethod
    def persist(self, package, lang, field_type='package'):
        session = meta.Session
        try:
            session.add_all([
                PackageMultilang(package_id=package.get('id'), field=package.get('field'), field_type=field_type, lang=lang, text=package.get('text')),
            ])

            session.commit()
        except Exception, e:
            # on rollback, the same closure of state
            # as that of commit proceeds.
            session.rollback()

            log.error('Exception occurred while persisting DB objects: %s', e)
            raise

meta.mapper(PackageMultilang, package_multilang_table)

class GroupMultilang(DomainObject):
    def __init__(self, group_id=None, name=None, field=None, lang=None, text=None):
        self.group_id = group_id
        self.name = name
        self.field = field
        self.lang = lang
        self.text = text

    @classmethod
    def get_for_group_id(self, group_id):
        obj = meta.Session.query(self).autoflush(False)
        records = obj.filter(self.group_id == group_id).all()
        return records

    @classmethod
    def get_for_group_id_and_lang(self, group_id, group_lang):
        obj = meta.Session.query(self).autoflush(False)
        records = obj.filter_by(group_id=group_id, lang=group_lang)
        return records

    @classmethod
    def get_for_group_name(self, group_name):
        obj = meta.Session.query(self).autoflush(False)
        records = obj.filter_by(name=group_name)    
        return records

    @classmethod
    def get_for_group_name_and_lang(self, group_name, group_lang):
        obj = meta.Session.query(self).autoflush(False)
        records = obj.filter_by(name=group_name, lang=group_lang)    
        return records

    @classmethod
    def persist(self, group, lang):
        session = meta.Session
        try:
            session.add_all([
                self(group_id=group.get('id'), name=group.get('name'), field='title', lang=lang, text=group.get('title')),
                self(group_id=group.get('id'), name=group.get('name'), field='description', lang=lang, text=group.get('description')),
            ])

            session.commit()
        except Exception, e:
            # on rollback, the same closure of state
            # as that of commit proceeds. 
            session.rollback()

            log.error('Exception occurred while persisting DB objects: %s', e)
            raise

meta.mapper(GroupMultilang, group_multilang_table)

class ResourceMultilang(DomainObject):
    def __init__(self, resource_id=None, field=None, lang=None, text=None):
        self.resource_id = resource_id
        self.field = field
        self.lang = lang
        self.text = text
    
    @classmethod
    def get_for_pk(self, resource_id, field, lang):
        obj = meta.Session.query(self).autoflush(False)
        records = obj.filter_by(resource_id=resource_id, field=field, lang=lang).all()
        if len(records) > 1:
            log.error('Too many ResourceMultilang records: %s', records)
            return records[0]
        elif len(records) == 1:
            return records[0]
        else:
            return None

    @classmethod
    def get_for_resource_id(self, resource_id):
        obj = meta.Session.query(self).autoflush(False)
        records = obj.filter_by(resource_id=resource_id)
        return records

    @classmethod
    def get_for_resource_id_and_lang(self, res_id, res_lang):
        obj = meta.Session.query(self).autoflush(False)
        records = obj.filter_by(resource_id=res_id, lang=res_lang)
        return records

    @classmethod
    def persist(self, resource, lang):
        session = meta.Session
        try:
            session.add_all([
                self(resource_id=resource.get('id'), field='name', lang=lang, text=resource.get('name')),
                self(resource_id=resource.get('id'), field='description', lang=lang, text=resource.get('description')),
            ])

            session.commit()
        except Exception, e:
            # on rollback, the same closure of state
            # as that of commit proceeds. 
            session.rollback()

            log.error('Exception occurred while persisting DB objects: %s', e)
            raise

    @classmethod
    def persist_resources(self, resources_list):
        session = meta.Session
        try:
            session.add_all(resources_list)
            session.commit()
        except Exception, e:
            # on rollback, the same closure of state
            # as that of commit proceeds.
            session.rollback()

            log.error('Exception occurred while persisting DB objects: %s', e)
            raise

meta.mapper(ResourceMultilang, resource_multilang_table)

class TagMultilang(DomainObject):
    def __init__(self, tag_id=None, tag_name=None, lang=None, text=None):
        self.tag_id = tag_id
        self.tag_name = tag_name
        self.lang = lang
        self.text = text

    @classmethod
    def by_name(self, tag_name, tag_lang, autoflush=True):
        query = meta.Session.query(TagMultilang).filter(TagMultilang.tag_name==tag_name, TagMultilang.lang==tag_lang)
        query = query.autoflush(autoflush)
        tag = query.first()
        return tag

    @classmethod
    def all_by_name(self, tag_name, autoflush=True):
        query = meta.Session.query(TagMultilang).filter(TagMultilang.tag_name==tag_name)
        query = query.autoflush(autoflush)
        tags = query.all()

        ret = {}
        for record in tags:
            ret[record.lang] = record.text

        return ret

    @classmethod
    def by_tag_id(self, tag_id, tag_lang, autoflush=True):
        query = meta.Session.query(TagMultilang).filter(TagMultilang.tag_id==tag_id, TagMultilang.lang==tag_lang)
        query = query.autoflush(autoflush)
        tag = query.first()
        return tag

    @classmethod
    def persist(self, tag, lang):
        session = meta.Session
        try:
            session.add_all([
                TagMultilang(tag_id=tag.get('id'), tag_name=tag.get('name'), lang=lang, text=tag.get('text')),
            ])

            session.commit()
        except Exception, e:
            # on rollback, the same closure of state
            # as that of commit proceeds. 
            session.rollback()

            log.error('Exception occurred while persisting DB objects: %s', e)
            raise

meta.mapper(TagMultilang, tag_multilang_table)