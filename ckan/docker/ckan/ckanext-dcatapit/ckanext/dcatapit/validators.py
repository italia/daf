import logging

from ckan.common import _, ungettext

from ckan.plugins.toolkit import Invalid
from datetime import datetime

log = logging.getLogger(__file__)


def is_blank (string):
    return not (string and string.strip())

def couple_validator(value, context):
    if not is_blank(value):
        couples = value.split(',')

        for c in couples:
            if not c:
                raise Invalid(_('Invalid couple, one value is missing'))

    return value

def no_number(value, context):
    if value and value.isdigit():
        raise Invalid(_('This field cannot be a number'))

    return value

def dcatapit_id_unique(value, context):
    model = context['model']
    session = context['session']

    package = context.get('package', None)
    if package:
        package_id = package.id

        result = session.query(model.PackageExtra).filter(model.PackageExtra.package_id != package_id, model.PackageExtra.key == 'identifier', model.PackageExtra.value == value).first()

        if result:
            raise Invalid(_('Another package exists with the same identifier'))

    return value
