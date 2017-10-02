package it.gov.daf.sso.common

import play.api.libs.json.JsValue

class Payload(_body:JsValue, _params:Seq[Any]) {

  val body=_body
  val parmas = _params

}
