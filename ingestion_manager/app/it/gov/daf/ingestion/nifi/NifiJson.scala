package it.gov.daf.ingestion.nifi

import play.api.libs.json._

object NifiJson {



  def listFileProc(clientId: String,
                   name: String,
                   inputDir: String,
                   recurseSubdir: String = "true",
                   inputDirLocation: String = "Local",
                   user: String = "",
                   pass: String = "",
                   token: String = "",
                   //fileFilter: String = "[\\S]+(\\.csv)(?!.read)",
                   fileFilter: String = "[\\\\S]+(\\\\.csv)(?!.read)",
                   pathFilter: String = "null",
                   minFileAge: String = "20 sec",
                   maxFileAge: String = "null",
                   minFileSize: String = "0 B",
                   maxFileSize: String = "null",
                   ignoreHiddenFile: String = "true",
                   position_x: String = "-2895.8654596332703",
                   position_y: String = "-1434.8490999040378"
                  ): JsValue = {

    val json =
      Json.parse(s"""
                   |{
                   |  "revision":{
                   |     "clientId":"$clientId",
                   |      "version":0
                   |   },
                   |   "component":{
                   |     "type":"org.apache.nifi.processors.standard.ListFile",
                   |      "bundle":{
                   |        "group":"org.apache.nifi",
                   |         "artifact":"nifi-standard-nar",
                   |         "version":"1.3.0"
                   |      },
                   |      "name":"$name",
                   |      "config":{
                   |       "properties":{
                   |          "Input Directory":"$inputDir",
                   |           "Recurse Subdirectories":"$recurseSubdir",
                   |           "Input Directory Location":"$inputDirLocation",
                   |           "File Filter":"$fileFilter",
                   |           "Path Filter":$pathFilter,
                   |           "Minimum File Age":"$minFileAge",
                   |           "Maximum File Age":$maxFileAge,
                   |           "Minimum File Size":"$minFileSize",
                   |           "Maximum File Size":$maxFileSize,
                   |           "Ignore Hidden Files":"$ignoreHiddenFile"
                   |          }
                   |    },
                   |      "position":{
                   |        "x":$position_x,
                   |         "y":$position_y
                   |      }
                   |   }
                   |}
                 """.stripMargin)
    json
  }

  def listSftpProc(clientId: String,
                   name: String,
                   hostname: String,
                   port: String = "22",
                   user: String,
                   pass: String = "",
                   privateKeyPath: String = "",
                   privateKeyPassphrase: String = "",
                   remotePath: String = "",
                   distribuedCacheService: String = "",
                   recursiveSearch: String = "",
                   fileFilterRegex: String = "[\\\\S]+(\\\\.csv)(?!.read)",
                   pathFilterRegex: String = "",
                   ignoreDottedFile: String = "true",
                   strictHostKeyCheck: String = "false",
                   hostKeyFile: String = "",
                   connectionTimeout: String = "30 sec",
                   dataTimeout: String = "30 sec",
                   sendKeepAliveOntimeout: String = "true",
                   position_x: String = "-2895.8654596332703",
                   position_y: String = "-1434.8490999040378"
                  ): JsValue = {

    val json =
      Json.parse(s"""
                    |{
                    |  "revision":{
                    |     "clientId":"$clientId",
                    |      "version":0
                    |   },
                    |   "component":{
                    |     "type":"org.apache.nifi.processors.standard.ListSFTP",
                    |      "bundle":{
                    |        "group":"org.apache.nifi",
                    |         "artifact":"nifi-standard-nar",
                    |         "version":"1.3.0"
                    |      },
                    |      "name":"$name",
                    |      "config":{
                    |       "properties":{
                    |          "Hostname":"$hostname",
                    |           "Port":"$port",
                    |           "Username":"$user",
                    |           "Password":"$pass",
                    |           "Private Key Path":"$privateKeyPath",
                    |           "Private Key Passphrase":"$privateKeyPassphrase",
                    |           "Remote Path":"$remotePath",
                    |           "Distributed Chache Service":"$distribuedCacheService",
                    |           "Search Recursively":"$recursiveSearch",
                    |           "File Filter Regex":"$fileFilterRegex",
                    |           "Path Filter Regex":"$pathFilterRegex",
                    |           "Ignore Dotted Files": "$ignoreDottedFile",
                    |           "Strict Host Key checking": "$strictHostKeyCheck",
                    |           "Host Key File": "$hostKeyFile",
                    |           "Connection timeout": "$connectionTimeout",
                    |           "Data timeout": "$dataTimeout",
                    |           "Send Keep Alive On Timeout": "$sendKeepAliveOntimeout"
                    |          }
                    |    },
                    |      "position":{
                    |        "x":$position_x,
                    |         "y":$position_y
                    |      }
                    |   }
                    |}
                 """.stripMargin)
    json
  }

  def updateAttrProc(clientId: String,
                     name: String,
                     inputSrc: String,
                     storage: String,
                     dataschema: String,
                     dataset_type: String,
                     transfPipeline: String,
                     format: String,
                     inputType: String = "Local",
                     sep: String = ",",
                     execNode: String = "ALL",
                     penaltyDuration: String = "30 sec",
                     yieldDuration: String = "1 sec",
                     bulletinLevel: String = "WARN",
                     schedulingStrategy: String = "TIMER_DRIVEN",
                     runDurationMillis: String = "0",
                     position_x: String = "-2895.8654596332703",
                     position_y: String = "-1434.8490999040378"
                    ): JsValue = {


    val json = Json.parse(
      s"""
        |{
        |   "revision":{
        |      "clientId":"$clientId",
        |      "version":0
        |   },
        |   "component":{
        |     "type":"org.apache.nifi.processors.attributes.UpdateAttribute",
        |      "bundle":{
        |         "group":"org.apache.nifi",
        |         "artifact":"nifi-update-attribute-nar",
        |         "version":"1.3.0"
        |      },
        |      "name":"$name",
        |      "config":{
        |         "concurrentlySchedulableTaskCount":"1",
        |         "schedulingPeriod":"0 sec",
        |         "executionNode":"$execNode",
        |         "penaltyDuration":"$penaltyDuration",
        |         "yieldDuration":"$yieldDuration",
        |         "bulletinLevel":"$bulletinLevel",
        |         "schedulingStrategy":"$schedulingStrategy",
        |         "comments":"",
        |         "runDurationMillis":$runDurationMillis,
        |         "autoTerminatedRelationships":[
        |         ],
        |         "properties":{
        |            "inputSrc": "$inputSrc",
        |            "inputType": "$inputType",
        |            "savings":"$storage",
        |            "trasformations":"$transfPipeline",
        |            "fmt":"$format",
        |            "metadati":"$dataschema",
        |            "dataset_type":"$dataset_type"
        |         }
        |    },
        |      "position":{
        |         "x":$position_x,
        |         "y":$position_y
        |      }
        |   }
        |}
      """.stripMargin)
    json
  }

  def listAttrConn(clientId: String,
                   name: String,
                   sourceId: String,
                   sourceGroupId: String,
                   sourceType: String,
                   destId: String,
                   destGroupId: String,
                   destType: String,
                   selectedRel: String = "success",
                   flowFileExpiration: String = "0 sec",
                   backPressureDataSizeThreshold: String = "1 GB",
                   backPressureObjectThreshold: String = "1000"
                    ): JsValue = {


    val json = Json.parse(
      s"""
         |{
         |   "revision":{
         |      "clientId":"$clientId",
         |      "version":0
         |   },
         |   "component":{
         |      "name":"$name",
         |      "source":{
         |         "id":"$sourceId",
         |         "groupId":"$sourceGroupId",
         |         "type":"$sourceType"
         |      },
         |      "destination":{
         |         "id":"$destId",
         |         "groupId":"$destGroupId",
         |         "type":"$destType"
         |      },
         |      "selectedRelationships":[
         |         "$selectedRel"
         |      ],
         |      "flowFileExpiration":"$flowFileExpiration",
         |      "backPressureDataSizeThreshold":"$backPressureDataSizeThreshold",
         |      "backPressureObjectThreshold":"$backPressureObjectThreshold",
         |      "bends":[
         |      ],
         |      "prioritizers":[
         |      ]
         |   }
         |}
      """.stripMargin)
    json
  }


  def funnelConn(clientId: String,
                   name: String,
                   sourceId: String,
                   sourceGroupId: String,
                   sourceType: String,
                   destId: String,
                   destGroupId: String,
                   destType: String,
                   selectedRel: String = "success",
                   flowFileExpiration: String = "0 sec",
                   backPressureDataSizeThreshold: String = "1 GB",
                   backPressureObjectThreshold: String = "1000"
                  ): JsValue = {


    val json = Json.parse(
      s"""
         |{
         |   "revision":{
         |      "clientId":"$clientId",
         |      "version":0
         |   },
         |   "component":{
         |      "name":"$name",
         |      "source":{
         |         "id":"$sourceId",
         |         "groupId":"$sourceGroupId",
         |         "type":"$sourceType"
         |      },
         |      "destination":{
         |         "id":"$destId",
         |         "groupId":"$destGroupId",
         |         "type":"$destType"
         |      },
         |      "selectedRelationships":[
         |         "$selectedRel"
         |      ],
         |      "flowFileExpiration":"$flowFileExpiration",
         |      "backPressureDataSizeThreshold":"$backPressureDataSizeThreshold",
         |      "backPressureObjectThreshold":"$backPressureObjectThreshold",
         |      "bends":[
         |      ],
         |      "prioritizers":[
         |      ]
         |   }
         |}
      """.stripMargin)
    json
  }

  def playProc(clientId: String,
               componentId: String,
               componentState: String,
               version: String = "5"
                ): JsValue = {


    val json = Json.parse(
      s"""
         |{
         |  "revision": {
         |    "clientId": "$clientId",
         |    "version": $version
         |  },
         |  "component": {
         |    "id": "$componentId",
         |    "state": "$componentState"
         |  }
         |}
      """.stripMargin)
    json
  }


}
