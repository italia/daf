import logging
import ckan.logic as logic
import ckan.logic.action.get as get
import sqlalchemy
import ckan.plugins.toolkit as toolkit

from paste.deploy.converters import asbool
from pylons.i18n import get_lang
from ckanext.multilang.model import GroupMultilang

import ckan.lib as lib

import ckanext.multilang.helpers as helpers

_check_access = logic.check_access
_unpick_search = get._unpick_search
_or_ = sqlalchemy.or_

_validate = lib.navl.dictization_functions.validate
ValidationError = logic.ValidationError

log = logging.getLogger(__file__)

@toolkit.side_effect_free
def group_list(context, data_dict):
    '''Return a list of the names of the site's groups.

    :param order_by: the field to sort the list by, must be ``'name'`` or
      ``'packages'`` (optional, default: ``'name'``) Deprecated use sort.
    :type order_by: string
    :param sort: sorting of the search results.  Optional.  Default:
        "name asc" string of field name and sort-order. The allowed fields are
        'name', 'package_count' and 'title'
    :type sort: string
    :param groups: a list of names of the groups to return, if given only
        groups whose names are in this list will be returned (optional)
    :type groups: list of strings
    :param all_fields: return group dictionaries instead of just names. Only
        core fields are returned - get some more using the include_* options.
        Returning a list of packages is too expensive, so the `packages`
        property for each group is deprecated, but there is a count of the
        packages in the `package_count` property.
        (optional, default: ``False``)
    :type all_fields: boolean
    :param include_extras: if all_fields, include the group extra fields
        (optional, default: ``False``)
    :type include_extras: boolean
    :param include_tags: if all_fields, include the group tags
        (optional, default: ``False``)
    :type include_tags: boolean
    :param include_groups: if all_fields, include the groups the groups are in
        (optional, default: ``False``).
    :type include_groups: boolean

    :rtype: list of strings

    '''
    _check_access('group_list', context, data_dict)
    return _group_or_org_list(context, data_dict)

@toolkit.side_effect_free
def organization_list(context, data_dict):
    '''Return a list of the names of the site's organizations.

    :param order_by: the field to sort the list by, must be ``'name'`` or
      ``'packages'`` (optional, default: ``'name'``) Deprecated use sort.
    :type order_by: string
    :param sort: sorting of the search results.  Optional.  Default:
        "name asc" string of field name and sort-order. The allowed fields are
        'name', 'package_count' and 'title'
    :type sort: string
    :param organizations: a list of names of the groups to return,
        if given only groups whose names are in this list will be
        returned (optional)
    :type organizations: list of strings
    :param all_fields: return group dictionaries instead of just names. Only
        core fields are returned - get some more using the include_* options.
        Returning a list of packages is too expensive, so the `packages`
        property for each group is deprecated, but there is a count of the
        packages in the `package_count` property.
        (optional, default: ``False``)
    :type all_fields: boolean
    :param include_extras: if all_fields, include the group extra fields
        (optional, default: ``False``)
    :type include_extras: boolean
    :param include_tags: if all_fields, include the group tags
        (optional, default: ``False``)
    :type include_tags: boolean
    :param include_groups: if all_fields, include the groups the groups are in
        (optional, default: ``False``)
    :type all_fields: boolean

    :rtype: list of strings

    '''
    _check_access('organization_list', context, data_dict)
    data_dict['groups'] = data_dict.pop('organizations', [])
    data_dict['type'] = 'organization'
    return _group_or_org_list(context, data_dict, is_org=True)

def _group_or_org_list(context, data_dict, is_org=False):
    model = context['model']
    api = context.get('api_version')
    groups = data_dict.get('groups')
    group_type = data_dict.get('type', 'group')
    ref_group_by = 'id' if api == 2 else 'name'

    pagination_dict = {}
    limit = data_dict.get('limit')
    if limit:
        pagination_dict['limit'] = data_dict['limit']
    offset = data_dict.get('offset')
    if offset:
        pagination_dict['offset'] = data_dict['offset']
    if pagination_dict:
        pagination_dict, errors = _validate(
            data_dict, logic.schema.default_pagination_schema(), context)
        if errors:
            raise ValidationError(errors)

    sort = data_dict.get('sort') or 'name'
    q = data_dict.get('q')

    # order_by deprecated in ckan 1.8
    # if it is supplied and sort isn't use order_by and raise a warning
    order_by = data_dict.get('order_by', '')
    if order_by:
        log.warn('`order_by` deprecated please use `sort`')
        if not data_dict.get('sort'):
            sort = order_by

    # if the sort is packages and no sort direction is supplied we want to do a
    # reverse sort to maintain compatibility.
    if sort.strip() in ('packages', 'package_count'):
        sort = 'package_count desc'

    sort_info = _unpick_search(sort,
                               allowed_fields=['name', 'packages',
                                               'package_count', 'title'],
                               total=1)

    all_fields = data_dict.get('all_fields', None)
    include_extras = all_fields and \
                     asbool(data_dict.get('include_extras', False))

    query = model.Session.query(model.Group)
    
    if include_extras:
        # this does an eager load of the extras, avoiding an sql query every
        # time group_list_dictize accesses a group's extra.
        query = query.options(sqlalchemy.orm.joinedload(model.Group._extras))

    query = query.filter(model.Group.state == 'active')
    if groups:
        query = query.filter(model.Group.name.in_(groups))
    if q:
        q = u'%{0}%'.format(q)
        
        ## MULTILANG FRAGMENT
        lang = helpers.getLanguage()
        groups_multilang_id_list = []

        q_results = model.Session.query(GroupMultilang).filter(
            GroupMultilang.lang.ilike(lang),
            GroupMultilang.text.ilike(q)
        ).all()

        if q_results:
            for result in q_results:
                log.info(":::::::::::::: Group ID Found:::: %r", result.group_id)
                groups_multilang_id_list.append(result.group_id)
                
            groups_multilang_id_list = set(groups_multilang_id_list)

        if len(groups_multilang_id_list) > 0:
            #query = query.filter(model.Group.id.in_(groups_multilang_id_list))
            query = query.filter(_or_(
                model.Group.name.ilike(q),
                model.Group.title.ilike(q),
                model.Group.description.ilike(q),
                model.Group.id.in_(groups_multilang_id_list)
            ))
        else:
            query = query.filter(_or_(
                model.Group.name.ilike(q),
                model.Group.title.ilike(q),
                model.Group.description.ilike(q)
            ))
        ## END OF MULTILANG FRAGMENT

    query = query.filter(model.Group.is_organization == is_org)
    if not is_org:
        query = query.filter(model.Group.type == group_type)
    if sort_info:
        sort_field = sort_info[0][0]
        sort_direction = sort_info[0][1]
        if sort_field == 'package_count':
            query = query.group_by(model.Group.id, model.Group.name)
            sort_model_field = sqlalchemy.func.count(model.Group.id)
        elif sort_field == 'name':
            sort_model_field = model.Group.name
        elif sort_field == 'title':
            sort_model_field = model.Group.title

        if sort_direction == 'asc':
            query = query.order_by(sqlalchemy.asc(sort_model_field))
        else:
            query = query.order_by(sqlalchemy.desc(sort_model_field))

    if limit:
        query = query.limit(limit)
    if offset:
        query = query.offset(offset)

    groups = query.all()

    if all_fields:
        action = 'organization_show' if is_org else 'group_show'
        group_list = []
        for group in groups:
            data_dict['id'] = group.id
            for key in ('include_extras', 'include_tags', 'include_users',
                        'include_groups', 'include_followers'):
                if key not in data_dict:
                    data_dict[key] = False

            group_list.append(logic.get_action(action)(context, data_dict))
    else:
        group_list = [getattr(group, ref_group_by) for group in groups]

    return group_list