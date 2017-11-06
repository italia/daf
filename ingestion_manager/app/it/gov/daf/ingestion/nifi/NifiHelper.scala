package it.gov.daf.ingestion.nifi

import play.api.libs.json._

/**
  * An helper to create Json object to interact the the NIFI rest api
  */
object NifiHelper {

  /**
   *
   * @param clientId
   * @param name
   * @param inputDir
   * @param recurseSubdir
   * @param inputDirLocation
   * @param user
   * @param pass
   * @param token
   * @param fileFilter
   * @param pathFilter
   * @param minFileAge
   * @param maxFileAge
   * @param minFileSize
   * @param maxFileSize
   * @param ignoreHiddenFile
   * @param position_x
   * @param position_y
   * @return a json file with the description of a nifi processor to list files in a folder
   */
  def listFileProcessor(
    name: String,
    inputDir: String,
    recurseSubdir: String = "true",
    inputDirLocation: String = "Local",
    user: String = "",
    pass: String = "",
    token: String = "",
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
    Json.parse(s"""
                   |{
                   |  "revision":{
                   |     "clientId":"$name",
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
  }

  /**
   *
   * @param clientId
   * @param name
   * @param hostname
   * @param port
   * @param user
   * @param pass
   * @param privateKeyPath
   * @param privateKeyPassphrase
   * @param remotePath
   * @param distribuedCacheService
   * @param recursiveSearch
   * @param fileFilterRegex
   * @param pathFilterRegex
   * @param ignoreDottedFile
   * @param strictHostKeyCheck
   * @param hostKeyFile
   * @param connectionTimeout
   * @param dataTimeout
   * @param sendKeepAliveOnTimeout
   * @param position_x
   * @param position_y
   * @return a json file with the description of a nifi processor to list files in a sftp folder
   */
  def listSftpProcessor(
    name: String,
    hostname: String,
    port: String = "22",
    user: String,
    pass: String,
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
    sendKeepAliveOnTimeout: String = "true",
    position_x: String = "-2895.8654596332703",
    position_y: String = "-1434.8490999040378"
  ): JsValue = {

    Json.parse(s"""
                    |{
                    |  "revision":{
                    |     "clientId":"$name",
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
                    |           "Send Keep Alive On Timeout": "$sendKeepAliveOnTimeout"
                    |          }
                    |    },
                    |      "position":{
                    |        "x":$position_x,
                    |         "y":$position_y
                    |      }
                    |   }
                    |}
                 """.stripMargin)
  }

  /**
   *
   * @param clientId
   * @param name
   * @param inputSrc
   * @param storage
   * @param dataschema
   * @param dataset_type
   * @param transfPipeline
   * @param format
   * @param inputType
   * @param sep
   * @param execNode
   * @param penaltyDuration
   * @param yieldDuration
   * @param bulletinLevel
   * @param schedulingStrategy
   * @param runDurationMillis
   * @param position_x
   * @param position_y
   * @return a nifi processor of type org.apache.nifi.processors.attributes.UpdateAttribute
   */
  def updateAttrProcessor(
    clientId: String,
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
      """.stripMargin
    )
    json
  }

  /**
   * @param clientId
   * @param name
   * @param sourceId
   * @param sourceGroupId
   * @param sourceType
   * @param destId
   * @param destGroupId
   * @param destType
   * @param selectedRel
   * @param flowFileExpiration
   * @param backPressureDataSizeThreshold
   * @param backPressureObjectThreshold
   * @return a json that represents a connection between a sourceId an destId Nifi Processor
   */
  def defineConnection(
    clientId: String,
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
    Json.parse(
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
      """.stripMargin
    )
  }
  //FIXME why 5?
  def playProcessor(
    clientId: String,
    componentId: String,
    componentState: String,
    version: String = "5"
  ): JsValue = {
    Json.parse(
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
      """.stripMargin
    )
  }
}
