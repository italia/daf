
import logging

import urllib
import ckan.model as model
import ckan.logic as logic

from ckan.controllers.api import ApiController
from ckan.common import c, request

log = logging.getLogger(__file__)

# shortcuts
get_action = logic.get_action

class DCATAPITApiController(ApiController):

    def vocabulary_autocomplete(self):
        q = request.str_params.get('incomplete', '')
        q = unicode(urllib.unquote(q), 'utf-8')

        vocab = request.params.get('vocabulary_id', None)

        vocab = str(vocab)

        log.debug('Looking for Vocab %r', vocab)

        limit = request.params.get('limit', 10)

        tag_names = []
        if q:
            context = {'model': model, 'session': model.Session, 'user': c.user, 'auth_user_obj': c.userobj}
            data_dict = {'q': q, 'limit': limit, 'vocabulary_id': vocab}
            tag_names = get_action('tag_autocomplete')(context, data_dict)

        resultSet = {
            'ResultSet': {
                'Result': [{'Name': tag} for tag in tag_names]
            }
        }

        return super(DCATAPITApiController, self)._finish_ok(resultSet)
