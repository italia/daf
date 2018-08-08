package it.gov.daf.catalogmanager.kylo

import catalog_manager.yaml.MetaCatalog
import play.api.libs.functional.FunctionalBuilder
import play.api.libs.json._
import play.api.libs.json.Reads._
import play.api.libs.functional.syntax._

object KyloTrasformers {


  def generateInputSftpPath(mc: MetaCatalog) :String = {
    val user = mc.operational.group_own
    val domain = mc.operational.theme
    val subDomain = mc.operational.subtheme
    val dsName = mc.dcatapit.name
    val path =  s"/home/$user/$domain/$subDomain/$dsName"
    path
  }

  def  transformTemplates(value: String): Reads[JsObject] = {
    __.json.update(
      (__ \ "value").json.put(JsString(value))
    )
  }

 // val inferKylo = """{"name":null,"description":null,"charset":"UTF-8","properties":{},"fields":[{"sampleValues":["GTT","GTT_E","GTT_F","GTT_T","FS"],"name":"agency_id","description":"","nativeDataType":null,"derivedDataType":"string","primaryKey":false,"nullable":true,"modifiable":true,"dataTypeDescriptor":{"numeric":false,"date":false,"complex":false},"updatedTracker":false,"precisionScale":null,"createdTracker":false,"tags":null,"descriptionWithoutNewLines":"","dataTypeWithPrecisionAndScale":"string"},{"sampleValues":["Gruppo Torinese Trasporti","GTT servizio extraurbano","Gruppo Torinese Trasporti","GTT Servizi Turistici","Trenitalia"],"name":"agency_name","description":"","nativeDataType":null,"derivedDataType":"string","primaryKey":false,"nullable":true,"modifiable":true,"dataTypeDescriptor":{"numeric":false,"date":false,"complex":false},"updatedTracker":false,"precisionScale":null,"createdTracker":false,"tags":null,"descriptionWithoutNewLines":"","dataTypeWithPrecisionAndScale":"string"},{"sampleValues":["http://www.gtt.to.it","http://www.gtt.to.it","http://www.gtt.to.it","http://www.gtt.to.it","http://www.trenitalia.com"],"name":"agency_url","description":"","nativeDataType":null,"derivedDataType":"string","primaryKey":false,"nullable":true,"modifiable":true,"dataTypeDescriptor":{"numeric":false,"date":false,"complex":false},"updatedTracker":false,"precisionScale":null,"createdTracker":false,"tags":null,"descriptionWithoutNewLines":"","dataTypeWithPrecisionAndScale":"string"},{"sampleValues":["Europe/Rome","Europe/Rome","Europe/Rome","Europe/Rome","Europe/Rome"],"name":"agency_timezone","description":"","nativeDataType":null,"derivedDataType":"string","primaryKey":false,"nullable":true,"modifiable":true,"dataTypeDescriptor":{"numeric":false,"date":false,"complex":false},"updatedTracker":false,"precisionScale":null,"createdTracker":false,"tags":null,"descriptionWithoutNewLines":"","dataTypeWithPrecisionAndScale":"string"},{"sampleValues":["it","it","it","it","it"],"name":"agency_lang","description":"","nativeDataType":null,"derivedDataType":"string","primaryKey":false,"nullable":true,"modifiable":true,"dataTypeDescriptor":{"numeric":false,"date":false,"complex":false},"updatedTracker":false,"precisionScale":null,"createdTracker":false,"tags":null,"descriptionWithoutNewLines":"","dataTypeWithPrecisionAndScale":"string"},{"sampleValues":["800-019152","800-019152","800-019152","800-019152","199-892021"],"name":"agency_phone","description":"","nativeDataType":null,"derivedDataType":"string","primaryKey":false,"nullable":true,"modifiable":true,"dataTypeDescriptor":{"numeric":false,"date":false,"complex":false},"updatedTracker":false,"precisionScale":null,"createdTracker":false,"tags":null,"descriptionWithoutNewLines":"","dataTypeWithPrecisionAndScale":"string"},{"sampleValues":["21","43","0","121","2"],"name":"num","description":"","nativeDataType":null,"derivedDataType":"int","primaryKey":false,"nullable":true,"modifiable":true,"dataTypeDescriptor":{"numeric":true,"date":false,"complex":false},"updatedTracker":false,"precisionScale":null,"createdTracker":false,"tags":null,"descriptionWithoutNewLines":"","dataTypeWithPrecisionAndScale":"int"}],"schemaName":null,"databaseName":null,"hiveFormat":"ROW FORMAT SERDE 'org.apache.hadoop.hive.serde2.OpenCSVSerde'\n WITH SERDEPROPERTIES ( 'separatorChar' = ',' ,'escapeChar' = '\\\\' ,'quoteChar' = '\\\"') STORED AS TEXTFILE","structured":false,"id":"51ebbdc8-83fd-4e70-b49d-f36dcc995ea9"}"""

 // val inferJson = Json.parse(inferKylo)


  def buildProfiling(inferJson :JsValue) : JsArray = {
    val fields = (inferJson \ "fields").as[IndexedSeq[JsObject]]
    val profilingFields = fields.map(x => {
      val fieldName = (x \ "name").as[String]
      val feedFieldName = (x \ "name").as[String]
      Json.obj("fieldName" -> fieldName,
        "feedFieldName" -> feedFieldName,
        "index" -> false,
        "partitionColumn" -> false,
        "profile" -> false,
        "validation" -> JsArray(),
        "standardization" -> JsArray()
      )
    })
    JsArray(profilingFields)
  }

  def buildUserProperties(userProperties :Seq[JsValue], metaCatalog: MetaCatalog, fileType :String) :JsArray = {
    val userProps = userProperties.map(_.as[JsObject])
    val result = userProps.map(x => {
      val systemName = (x \ "systemName").as[String]
      systemName match {
        case "daf_type" => { if (metaCatalog.operational.is_std)
                                x + ("value" -> JsString("standard"))
                              else
                                x + ("value" -> JsString("ordinary"))
                            }
        case "daf_domain" => x + ("value" -> JsString(metaCatalog.operational.theme))
        case "daf_subdomain" => x + ("value" -> JsString(metaCatalog.operational.subtheme))
        case "daf_format" => x + ("value" -> JsString(fileType))
        case "daf_opendata" => { if (metaCatalog.dcatapit.privatex.getOrElse(true))
                                   x + ("value" -> JsBoolean(false))
                                 else
                                   x + ("value" -> JsBoolean(true))
        }
        case "daf_owner" => x + ("value" -> JsString(metaCatalog.dcatapit.author.getOrElse("")))
      }
    })
    JsArray(result)
  }

  //id: "efc036fe-ef47-42a6-bb00-7067efb358a5",
  //name: "DAF Category",
  //systemName: "daf_category"

  //"id": "6ef0ef5b-5c8f-42fc-9f0d-37f67430f1f5",
  //"name": "default_org",
  //"systemName": "default_org"

  def feedTrasform(metaCatalog: MetaCatalog, template :JsValue, templates : List[JsObject], inferJson :JsValue, category :JsValue, fileType :String, skipHeader :Boolean): Reads[JsObject] = __.json.update(
    (__ \ 'feedName).json.put(JsString(metaCatalog.dcatapit.holder_identifier.get + "_o_" + metaCatalog.dcatapit.name)) and
        (__ \ 'description).json.put(JsString(metaCatalog.dcatapit.name)) and
         (__ \ 'systemFeedName).json.put(JsString(metaCatalog.dcatapit.holder_identifier.get + "_o_" + metaCatalog.dcatapit.name)) and
         (__ \ 'templateId).json.put(JsString((template \ "id").get.as[String])) and
         (__ \ 'templateName).json.put(JsString((template \ "templateName").get.as[String])) and
         (__ \ 'inputProcessorType).json.put(JsString(((template \ "inputProcessors")(0) \ "type").get.as[String])) and
         (__ \ 'inputProcessorName).json.put(JsString(((template \ "inputProcessors")(0) \ "name").get.as[String])) and
         (__ \ 'properties).json.put(JsArray(templates)) and
         ((__ \ 'table) \ 'tableSchema).json.put(Json.obj("name" -> (metaCatalog.dcatapit.holder_identifier.get + "_o_" + metaCatalog.dcatapit.name)
         , "fields" -> (inferJson \ "fields").as[JsArray]  )) and
         (((__ \ 'table) \ 'sourceTableSchema) \ 'fields).json.put((inferJson \ "fields").as[JsArray]) and
         (((__ \ 'table) \ 'feedTableSchema) \ 'fields).json.put((inferJson \ "fields").as[JsArray]) and
         ((__ \ 'table) \ 'feedFormat).json.put(JsString((inferJson \ "hiveFormat").as[String])) and
         ((__ \ 'table) \ 'targetMergeStrategy).json.put(JsString(metaCatalog.operational.dataset_proc.get.merge_strategy)) and
         ((__ \ 'table) \ 'fieldPolicies).json.put(buildProfiling(inferJson)) and
         ((__ \ 'schedule) \ 'schedulingStrategy).json.put(JsString("CRON_DRIVEN")) and
         ((__ \ 'schedule) \ 'schedulingPeriod).json.put(JsString(metaCatalog.operational.dataset_proc.get.cron)) and
         (__ \ 'category).json.put(Json.obj("id" -> (category \ "id").as[String],
                                      "name" ->  (category \ "name").as[String],
                                      "systemName" -> (category \ "systemName").as[String])) and
         (__ \ 'dataOwner).json.put(JsString((category \ "systemName").as[String])) and
      ((__ \ 'options) \ 'skipHeader).json.put(JsBoolean(skipHeader))
         reduce
  ) andThen (__ \ 'userProperties).json.update(
    of[JsArray].map{ case JsArray(arr) => buildUserProperties(arr, metaCatalog, fileType) }
  )

}
