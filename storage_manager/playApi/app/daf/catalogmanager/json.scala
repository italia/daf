/*
 * Copyright 2017 TEAM PER LA TRASFORMAZIONE DIGITALE
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package daf.catalogmanager

import play.api.libs.json._

// Generic Key/Value pair object
case class KeyValue(key: String, value: String)
// Generic Key/Value pair object
case class VocKeyValueSubtheme(key: String, value: String, keyTheme: String, valueTheme: Option[String])
case class MetaCatalog(dataschema: DatasetCatalog, operational: Operational, dcatapit: Dataset)
case class DatasetCatalog(avro: Avro, flatSchema: List[FlatSchema], kyloSchema: Option[String])
case class Avro(namespace: String, `type`: String, name: String, aliases: Option[List[String]], fields: Option[List[Field]])
case class Field(name: String, `type`: String)
case class FlatSchema(name: String, `type`: String, metadata: Option[Metadata])
case class Metadata(desc: Option[String], field_type: Option[String], cat: Option[String], tag: Option[List[String]], required: Option[Int], semantics: Option[Semantic], constr: Option[List[Constr]])
case class Lang(eng: Option[String], ita: Option[String])
case class Semantic(id: String, context: Option[String])
case class Constr(`type`: Option[String], param: Option[String])
case class Operational(theme: String, subtheme: String, logical_uri: String, physical_uri: Option[String], is_std: Boolean, group_own: String, group_access: Option[List[GroupAccess]], std_schema: Option[StdSchema], read_type: String, georef: Option[List[GeoRef]], input_src: InputSrc, ingestion_pipeline: Option[List[String]], storage_info: Option[StorageInfo], dataset_type: String)
// Type associated with group_access
case class GroupAccess(name: String, role: String)
case class InputSrc(sftp: Option[List[SourceSftp]], srv_pull: Option[List[SourceSrvPull]], srv_push: Option[List[SourceSrvPush]], daf_dataset: Option[List[SourceDafDataset]])
// Info for the ingestion source of type SFTP
case class SourceSftp(name: String, url: Option[String], username: Option[String], password: Option[String], param: Option[String])
// Info for the ingestion source of type pulling a service, that is we make a call to the specified url
case class SourceSrvPull(name: String, url: String, username: Option[String], password: Option[String], access_token: Option[String], param: Option[String])
// Info for the ingestion source of type pushing a service, that is we expose a service that is continuously listening
case class SourceSrvPush(name: String, url: String, username: Option[String], password: Option[String], access_token: Option[String], param: Option[String])
// It contains info to build the dataset based on already existing dataset in DAF.
case class SourceDafDataset(dataset_uri: Option[List[String]], sql: Option[String], param: Option[String])
case class StorageInfo(hdfs: Option[StorageHdfs], kudu: Option[StorageKudu], hbase: Option[StorageHbase], textdb: Option[StorageTextdb], mongo: Option[StorageMongo])
// If compiled, will tell the ingestion manager to store the data into HDFS.
case class StorageHdfs(name: String, path: Option[String], param: Option[String])
// If compiled, will tell the ingestion manager to store the data into Kudu.
case class StorageKudu(name: String, table_name: Option[String], param: Option[String])
// If compiled, will tell the ingestion manager to store the data into Hbase.
case class StorageHbase(name: String, metric: Option[String], tags: Option[List[String]], param: Option[String])
// If compiled, will tell the ingestion manager to store the data into Textdb.
case class StorageTextdb(name: String, path: Option[String], param: Option[String])
// If compiled, will tell the ingestion manager to store the data into Kudu.
case class StorageMongo(name: String, path: Option[String], param: Option[String])
case class GeoRef(lat: Double, lon: Double)
case class StdSchema(std_uri: String, fields_conv: List[ConversionField])
case class ConversionSchema(fields_conv: List[ConversionField], fields_custom: Option[List[CustomField]])
case class ConversionField(field_std: String, formula: String)
case class CustomField(name: String)
case class Error(code: Option[Int], message: String, fields: Option[String])
case class Success(message: String, fields: Option[String])
case class Dataset(alternate_identifier: Option[String], author: Option[String], frequency: Option[String], groups: Option[List[Group]], holder_identifier: Option[String], holder_name: Option[String], identifier: Option[String], license_id: Option[String], license_title: Option[String], modified: Option[String], name: String, notes: String, organization: Option[Organization], owner_org: Option[String], publisher_identifier: Option[String], publisher_name: Option[String], relationships_as_object: Option[List[Relationship]], relationships_as_subject: Option[List[Relationship]], resources: Option[List[Resource]], tags: Option[List[Tag]], theme: Option[String], title: Option[String])
case class Group(display_name: Option[String], description: Option[String], image_display_url: Option[String], title: Option[String], id: Option[String], name: Option[String])
case class Organization(approval_status: Option[String], created: Option[String], description: Option[String], email: Option[String], id: Option[String], image_url: Option[String], is_organization: Option[Boolean], name: String, revision_id: Option[String], state: Option[String], title: Option[String], `type`: Option[String], users: Option[List[UserOrg]])
case class Relationship(subject: Option[String], `object`: Option[String], `type`: Option[String], comment: Option[String])
case class Resource(cache_last_updated: Option[String], cache_url: Option[String], created: Option[String], datastore_active: Option[Boolean], description: Option[String], distribution_format: Option[String], format: Option[String], hash: Option[String], id: Option[String], last_modified: Option[String], mimetype: Option[String], mimetype_inner: Option[String], name: Option[String], package_id: Option[String], position: Option[Int], resource_type: Option[String], revision_id: Option[String], size: Option[Int], state: Option[String], url: Option[String])
case class Tag(display_name: Option[String], id: Option[String], name: Option[String], state: Option[String], vocabulary_id: Option[String])
case class Extra(key: Option[String], value: Option[String])
case class StdUris(label: Option[String], value: Option[String])
case class Token(token: Option[String])
case class User(name: Option[String], email: Option[String], password: Option[String], fullname: Option[String], about: Option[String])
case class AutocompRes(match_field: Option[String], match_displayed: Option[String], name: Option[String], title: Option[String])
case class UserOrg(name: Option[String], capacity: Option[String])
case class Credentials(username: Option[String], password: Option[String])

object json {
  implicit lazy val KeyValueReads: Reads[KeyValue] = Reads[KeyValue] {
    json => JsSuccess(KeyValue((json \ "key").as[String], (json \ "value").as[String]))
  }
  implicit lazy val KeyValueWrites: Writes[KeyValue] = Writes[KeyValue] {
    o => JsObject(Seq("key" -> Json.toJson(o.key), "value" -> Json.toJson(o.value)).filter(_._2 != JsNull))
  }
  implicit lazy val VocKeyValueSubthemeReads: Reads[VocKeyValueSubtheme] = Reads[VocKeyValueSubtheme] {
    json => JsSuccess(VocKeyValueSubtheme((json \ "key").as[String], (json \ "value").as[String], (json \ "keyTheme").as[String], (json \ "valueTheme").asOpt[String]))
  }
  implicit lazy val VocKeyValueSubthemeWrites: Writes[VocKeyValueSubtheme] = Writes[VocKeyValueSubtheme] {
    o => JsObject(Seq("key" -> Json.toJson(o.key), "value" -> Json.toJson(o.value), "keyTheme" -> Json.toJson(o.keyTheme), "valueTheme" -> Json.toJson(o.valueTheme)).filter(_._2 != JsNull))
  }
  implicit lazy val MetaCatalogReads: Reads[MetaCatalog] = Reads[MetaCatalog] {
    json => JsSuccess(MetaCatalog((json \ "dataschema").as[DatasetCatalog], (json \ "operational").as[Operational], (json \ "dcatapit").as[Dataset]))
  }
  implicit lazy val MetaCatalogWrites: Writes[MetaCatalog] = Writes[MetaCatalog] {
    o => JsObject(Seq("dataschema" -> Json.toJson(o.dataschema), "operational" -> Json.toJson(o.operational), "dcatapit" -> Json.toJson(o.dcatapit)).filter(_._2 != JsNull))
  }
  implicit lazy val DatasetCatalogReads: Reads[DatasetCatalog] = Reads[DatasetCatalog] {
    json => JsSuccess(DatasetCatalog((json \ "avro").as[Avro], (json \ "flatSchema").as[List[FlatSchema]],(json \ "kyloSchema").as[Option[String]]))
  }
  implicit lazy val DatasetCatalogWrites: Writes[DatasetCatalog] = Writes[DatasetCatalog] {
    o => JsObject(Seq("avro" -> Json.toJson(o.avro), "flatSchema" -> Json.toJson(o.flatSchema), "kyloSchema" -> Json.toJson(o.kyloSchema)).filter(_._2 != JsNull))
  }
  implicit lazy val AvroReads: Reads[Avro] = Reads[Avro] {
    json => JsSuccess(Avro((json \ "namespace").as[String], (json \ "type").as[String], (json \ "name").as[String], (json \ "aliases").asOpt[List[String]], (json \ "fields").asOpt[List[Field]]))
  }
  implicit lazy val AvroWrites: Writes[Avro] = Writes[Avro] {
    o => JsObject(Seq("namespace" -> Json.toJson(o.namespace), "type" -> Json.toJson(o.`type`), "name" -> Json.toJson(o.name), "aliases" -> Json.toJson(o.aliases), "fields" -> Json.toJson(o.fields)).filter(_._2 != JsNull))
  }
  implicit lazy val FieldReads: Reads[Field] = Reads[Field] {
    json => JsSuccess(Field((json \ "name").as[String], (json \ "type").as[String]))
  }
  implicit lazy val FieldWrites: Writes[Field] = Writes[Field] {
    o => JsObject(Seq("name" -> Json.toJson(o.name), "type" -> Json.toJson(o.`type`)).filter(_._2 != JsNull))
  }
  implicit lazy val FlatSchemaReads: Reads[FlatSchema] = Reads[FlatSchema] {
    json => JsSuccess(FlatSchema((json \ "name").as[String], (json \ "type").as[String], (json \ "metadata").asOpt[Metadata]))
  }
  implicit lazy val FlatSchemaWrites: Writes[FlatSchema] = Writes[FlatSchema] {
    o => JsObject(Seq("name" -> Json.toJson(o.name), "type" -> Json.toJson(o.`type`), "metadata" -> Json.toJson(o.metadata)).filter(_._2 != JsNull))
  }
  implicit lazy val MetadataReads: Reads[Metadata] = Reads[Metadata] {
    json => JsSuccess(Metadata((json \ "desc").asOpt[String], (json \ "field_type").asOpt[String], (json \ "cat").asOpt[String], (json \ "tag").asOpt[List[String]], (json \ "required").asOpt[Int], (json \ "semantics").asOpt[Semantic], (json \ "constr").asOpt[List[Constr]]))
  }
  implicit lazy val MetadataWrites: Writes[Metadata] = Writes[Metadata] {
    o => JsObject(Seq("desc" -> Json.toJson(o.desc), "field_type" -> Json.toJson(o.field_type), "cat" -> Json.toJson(o.cat), "tag" -> Json.toJson(o.tag), "required" -> Json.toJson(o.required), "semantics" -> Json.toJson(o.semantics), "constr" -> Json.toJson(o.constr)).filter(_._2 != JsNull))
  }
  implicit lazy val LangReads: Reads[Lang] = Reads[Lang] {
    json => JsSuccess(Lang((json \ "eng").asOpt[String], (json \ "ita").asOpt[String]))
  }
  implicit lazy val LangWrites: Writes[Lang] = Writes[Lang] {
    o => JsObject(Seq("eng" -> Json.toJson(o.eng), "ita" -> Json.toJson(o.ita)).filter(_._2 != JsNull))
  }
  implicit lazy val SemanticReads: Reads[Semantic] = Reads[Semantic] {
    json => JsSuccess(Semantic((json \ "id").as[String], (json \ "context").asOpt[String]))
  }
  implicit lazy val SemanticWrites: Writes[Semantic] = Writes[Semantic] {
    o => JsObject(Seq("id" -> Json.toJson(o.id), "context" -> Json.toJson(o.context)).filter(_._2 != JsNull))
  }
  implicit lazy val ConstrReads: Reads[Constr] = Reads[Constr] {
    json => JsSuccess(Constr((json \ "type").asOpt[String], (json \ "param").asOpt[String]))
  }
  implicit lazy val ConstrWrites: Writes[Constr] = Writes[Constr] {
    o => JsObject(Seq("type" -> Json.toJson(o.`type`), "param" -> Json.toJson(o.param)).filter(_._2 != JsNull))
  }
  implicit lazy val OperationalReads: Reads[Operational] = Reads[Operational] {
    json => JsSuccess(Operational((json \ "theme").as[String], (json \ "subtheme").as[String], (json \ "logical_uri").as[String], (json \ "physical_uri").asOpt[String], (json \ "is_std").as[Boolean], (json \ "group_own").as[String], (json \ "group_access").asOpt[List[GroupAccess]], (json \ "std_schema").asOpt[StdSchema], (json \ "read_type").as[String], (json \ "georef").asOpt[List[GeoRef]], (json \ "input_src").as[InputSrc], (json \ "ingestion_pipeline").asOpt[List[String]], (json \ "storage_info").asOpt[StorageInfo], (json \ "dataset_type").as[String]))
  }
  implicit lazy val OperationalWrites: Writes[Operational] = Writes[Operational] {
    o => JsObject(Seq("theme" -> Json.toJson(o.theme), "subtheme" -> Json.toJson(o.subtheme), "logical_uri" -> Json.toJson(o.logical_uri), "physical_uri" -> Json.toJson(o.physical_uri), "is_std" -> Json.toJson(o.is_std), "group_own" -> Json.toJson(o.group_own), "group_access" -> Json.toJson(o.group_access), "std_schema" -> Json.toJson(o.std_schema), "read_type" -> Json.toJson(o.read_type), "georef" -> Json.toJson(o.georef), "input_src" -> Json.toJson(o.input_src), "ingestion_pipeline" -> Json.toJson(o.ingestion_pipeline), "storage_info" -> Json.toJson(o.storage_info), "dataset_type" -> Json.toJson(o.dataset_type)).filter(_._2 != JsNull))
  }
  implicit lazy val GroupAccessReads: Reads[GroupAccess] = Reads[GroupAccess] {
    json => JsSuccess(GroupAccess((json \ "name").as[String], (json \ "role").as[String]))
  }
  implicit lazy val GroupAccessWrites: Writes[GroupAccess] = Writes[GroupAccess] {
    o => JsObject(Seq("name" -> Json.toJson(o.name), "role" -> Json.toJson(o.role)).filter(_._2 != JsNull))
  }
  implicit lazy val InputSrcReads: Reads[InputSrc] = Reads[InputSrc] {
    json => JsSuccess(InputSrc((json \ "sftp").asOpt[List[SourceSftp]], (json \ "srv_pull").asOpt[List[SourceSrvPull]], (json \ "srv_push").asOpt[List[SourceSrvPush]], (json \ "daf_dataset").asOpt[List[SourceDafDataset]]))
  }
  implicit lazy val InputSrcWrites: Writes[InputSrc] = Writes[InputSrc] {
    o => JsObject(Seq("sftp" -> Json.toJson(o.sftp), "srv_pull" -> Json.toJson(o.srv_pull), "srv_push" -> Json.toJson(o.srv_push), "daf_dataset" -> Json.toJson(o.daf_dataset)).filter(_._2 != JsNull))
  }
  implicit lazy val SourceSftpReads: Reads[SourceSftp] = Reads[SourceSftp] {
    json => JsSuccess(SourceSftp((json \ "name").as[String], (json \ "url").asOpt[String], (json \ "username").asOpt[String], (json \ "password").asOpt[String], (json \ "param").asOpt[String]))
  }
  implicit lazy val SourceSftpWrites: Writes[SourceSftp] = Writes[SourceSftp] {
    o => JsObject(Seq("name" -> Json.toJson(o.name), "url" -> Json.toJson(o.url), "username" -> Json.toJson(o.username), "password" -> Json.toJson(o.password), "param" -> Json.toJson(o.param)).filter(_._2 != JsNull))
  }
  implicit lazy val SourceSrvPullReads: Reads[SourceSrvPull] = Reads[SourceSrvPull] {
    json => JsSuccess(SourceSrvPull((json \ "name").as[String], (json \ "url").as[String], (json \ "username").asOpt[String], (json \ "password").asOpt[String], (json \ "access_token").asOpt[String], (json \ "param").asOpt[String]))
  }
  implicit lazy val SourceSrvPullWrites: Writes[SourceSrvPull] = Writes[SourceSrvPull] {
    o => JsObject(Seq("name" -> Json.toJson(o.name), "url" -> Json.toJson(o.url), "username" -> Json.toJson(o.username), "password" -> Json.toJson(o.password), "access_token" -> Json.toJson(o.access_token), "param" -> Json.toJson(o.param)).filter(_._2 != JsNull))
  }
  implicit lazy val SourceSrvPushReads: Reads[SourceSrvPush] = Reads[SourceSrvPush] {
    json => JsSuccess(SourceSrvPush((json \ "name").as[String], (json \ "url").as[String], (json \ "username").asOpt[String], (json \ "password").asOpt[String], (json \ "access_token").asOpt[String], (json \ "param").asOpt[String]))
  }
  implicit lazy val SourceSrvPushWrites: Writes[SourceSrvPush] = Writes[SourceSrvPush] {
    o => JsObject(Seq("name" -> Json.toJson(o.name), "url" -> Json.toJson(o.url), "username" -> Json.toJson(o.username), "password" -> Json.toJson(o.password), "access_token" -> Json.toJson(o.access_token), "param" -> Json.toJson(o.param)).filter(_._2 != JsNull))
  }
  implicit lazy val SourceDafDatasetReads: Reads[SourceDafDataset] = Reads[SourceDafDataset] {
    json => JsSuccess(SourceDafDataset((json \ "dataset_uri").asOpt[List[String]], (json \ "sql").asOpt[String], (json \ "param").asOpt[String]))
  }
  implicit lazy val SourceDafDatasetWrites: Writes[SourceDafDataset] = Writes[SourceDafDataset] {
    o => JsObject(Seq("dataset_uri" -> Json.toJson(o.dataset_uri), "sql" -> Json.toJson(o.sql), "param" -> Json.toJson(o.param)).filter(_._2 != JsNull))
  }
  implicit lazy val StorageInfoReads: Reads[StorageInfo] = Reads[StorageInfo] {
    json => JsSuccess(StorageInfo((json \ "hdfs").asOpt[StorageHdfs], (json \ "kudu").asOpt[StorageKudu], (json \ "hbase").asOpt[StorageHbase], (json \ "textdb").asOpt[StorageTextdb], (json \ "mongo").asOpt[StorageMongo]))
  }
  implicit lazy val StorageInfoWrites: Writes[StorageInfo] = Writes[StorageInfo] {
    o => JsObject(Seq("hdfs" -> Json.toJson(o.hdfs), "kudu" -> Json.toJson(o.kudu), "hbase" -> Json.toJson(o.hbase), "textdb" -> Json.toJson(o.textdb), "mongo" -> Json.toJson(o.mongo)).filter(_._2 != JsNull))
  }
  implicit lazy val StorageHdfsReads: Reads[StorageHdfs] = Reads[StorageHdfs] {
    json => JsSuccess(StorageHdfs((json \ "name").as[String], (json \ "path").asOpt[String], (json \ "param").asOpt[String]))
  }
  implicit lazy val StorageHdfsWrites: Writes[StorageHdfs] = Writes[StorageHdfs] {
    o => JsObject(Seq("name" -> Json.toJson(o.name), "path" -> Json.toJson(o.path), "param" -> Json.toJson(o.param)).filter(_._2 != JsNull))
  }
  implicit lazy val StorageKuduReads: Reads[StorageKudu] = Reads[StorageKudu] {
    json => JsSuccess(StorageKudu((json \ "name").as[String], (json \ "table_name").asOpt[String], (json \ "param").asOpt[String]))
  }
  implicit lazy val StorageKuduWrites: Writes[StorageKudu] = Writes[StorageKudu] {
    o => JsObject(Seq("name" -> Json.toJson(o.name), "table_name" -> Json.toJson(o.table_name), "param" -> Json.toJson(o.param)).filter(_._2 != JsNull))
  }
  implicit lazy val StorageHbaseReads: Reads[StorageHbase] = Reads[StorageHbase] {
    json => JsSuccess(StorageHbase((json \ "name").as[String], (json \ "metric").asOpt[String], (json \ "tags").asOpt[List[String]], (json \ "param").asOpt[String]))
  }
  implicit lazy val StorageHbaseWrites: Writes[StorageHbase] = Writes[StorageHbase] {
    o => JsObject(Seq("name" -> Json.toJson(o.name), "metric" -> Json.toJson(o.metric), "tags" -> Json.toJson(o.tags), "param" -> Json.toJson(o.param)).filter(_._2 != JsNull))
  }
  implicit lazy val StorageTextdbReads: Reads[StorageTextdb] = Reads[StorageTextdb] {
    json => JsSuccess(StorageTextdb((json \ "name").as[String], (json \ "path").asOpt[String], (json \ "param").asOpt[String]))
  }
  implicit lazy val StorageTextdbWrites: Writes[StorageTextdb] = Writes[StorageTextdb] {
    o => JsObject(Seq("name" -> Json.toJson(o.name), "path" -> Json.toJson(o.path), "param" -> Json.toJson(o.param)).filter(_._2 != JsNull))
  }
  implicit lazy val StorageMongoReads: Reads[StorageMongo] = Reads[StorageMongo] {
    json => JsSuccess(StorageMongo((json \ "name").as[String], (json \ "path").asOpt[String], (json \ "param").asOpt[String]))
  }
  implicit lazy val StorageMongoWrites: Writes[StorageMongo] = Writes[StorageMongo] {
    o => JsObject(Seq("name" -> Json.toJson(o.name), "path" -> Json.toJson(o.path), "param" -> Json.toJson(o.param)).filter(_._2 != JsNull))
  }
  implicit lazy val GeoRefReads: Reads[GeoRef] = Reads[GeoRef] {
    json => JsSuccess(GeoRef((json \ "lat").as[Double], (json \ "lon").as[Double]))
  }
  implicit lazy val GeoRefWrites: Writes[GeoRef] = Writes[GeoRef] {
    o => JsObject(Seq("lat" -> Json.toJson(o.lat), "lon" -> Json.toJson(o.lon)).filter(_._2 != JsNull))
  }
  implicit lazy val StdSchemaReads: Reads[StdSchema] = Reads[StdSchema] {
    json => JsSuccess(StdSchema((json \ "std_uri").as[String], (json \ "fields_conv").as[List[ConversionField]]))
  }
  implicit lazy val StdSchemaWrites: Writes[StdSchema] = Writes[StdSchema] {
    o => JsObject(Seq("std_uri" -> Json.toJson(o.std_uri), "fields_conv" -> Json.toJson(o.fields_conv)).filter(_._2 != JsNull))
  }
  implicit lazy val ConversionSchemaReads: Reads[ConversionSchema] = Reads[ConversionSchema] {
    json => JsSuccess(ConversionSchema((json \ "fields_conv").as[List[ConversionField]], (json \ "fields_custom").asOpt[List[CustomField]]))
  }
  implicit lazy val ConversionSchemaWrites: Writes[ConversionSchema] = Writes[ConversionSchema] {
    o => JsObject(Seq("fields_conv" -> Json.toJson(o.fields_conv), "fields_custom" -> Json.toJson(o.fields_custom)).filter(_._2 != JsNull))
  }
  implicit lazy val ConversionFieldReads: Reads[ConversionField] = Reads[ConversionField] {
    json => JsSuccess(ConversionField((json \ "field_std").as[String], (json \ "formula").as[String]))
  }
  implicit lazy val ConversionFieldWrites: Writes[ConversionField] = Writes[ConversionField] {
    o => JsObject(Seq("field_std" -> Json.toJson(o.field_std), "formula" -> Json.toJson(o.formula)).filter(_._2 != JsNull))
  }
  implicit lazy val CustomFieldReads: Reads[CustomField] = Reads[CustomField] {
    json => JsSuccess(CustomField((json \ "name").as[String]))
  }
  implicit lazy val CustomFieldWrites: Writes[CustomField] = Writes[CustomField] {
    o => JsObject(Seq("name" -> Json.toJson(o.name)).filter(_._2 != JsNull))
  }
  implicit lazy val ErrorReads: Reads[Error] = Reads[Error] {
    json => JsSuccess(Error((json \ "code").asOpt[Int], (json \ "message").as[String], (json \ "fields").asOpt[String]))
  }
  implicit lazy val ErrorWrites: Writes[Error] = Writes[Error] {
    o => JsObject(Seq("code" -> Json.toJson(o.code), "message" -> Json.toJson(o.message), "fields" -> Json.toJson(o.fields)).filter(_._2 != JsNull))
  }
  implicit lazy val SuccessReads: Reads[Success] = Reads[Success] {
    json => JsSuccess(Success((json \ "message").as[String], (json \ "fields").asOpt[String]))
  }
  implicit lazy val SuccessWrites: Writes[Success] = Writes[Success] {
    o => JsObject(Seq("message" -> Json.toJson(o.message), "fields" -> Json.toJson(o.fields)).filter(_._2 != JsNull))
  }
  implicit lazy val DatasetReads: Reads[Dataset] = Reads[Dataset] {
    json => JsSuccess(Dataset((json \ "alternate_identifier").asOpt[String], (json \ "author").asOpt[String], (json \ "frequency").asOpt[String], (json \ "groups").asOpt[List[Group]], (json \ "holder_identifier").asOpt[String], (json \ "holder_name").asOpt[String], (json \ "identifier").asOpt[String], (json \ "license_id").asOpt[String], (json \ "license_title").asOpt[String], (json \ "modified").asOpt[String], (json \ "name").as[String], (json \ "notes").as[String], (json \ "organization").asOpt[Organization], (json \ "owner_org").asOpt[String], (json \ "publisher_identifier").asOpt[String], (json \ "publisher_name").asOpt[String], (json \ "relationships_as_object").asOpt[List[Relationship]], (json \ "relationships_as_subject").asOpt[List[Relationship]], (json \ "resources").asOpt[List[Resource]], (json \ "tags").asOpt[List[Tag]], (json \ "theme").asOpt[String], (json \ "title").asOpt[String]))
  }
  implicit lazy val DatasetWrites: Writes[Dataset] = Writes[Dataset] {
    o => JsObject(Seq("alternate_identifier" -> Json.toJson(o.alternate_identifier), "author" -> Json.toJson(o.author), "frequency" -> Json.toJson(o.frequency), "groups" -> Json.toJson(o.groups), "holder_identifier" -> Json.toJson(o.holder_identifier), "holder_name" -> Json.toJson(o.holder_name), "identifier" -> Json.toJson(o.identifier), "license_id" -> Json.toJson(o.license_id), "license_title" -> Json.toJson(o.license_title), "modified" -> Json.toJson(o.modified), "name" -> Json.toJson(o.name), "notes" -> Json.toJson(o.notes), "organization" -> Json.toJson(o.organization), "owner_org" -> Json.toJson(o.owner_org), "publisher_identifier" -> Json.toJson(o.publisher_identifier), "publisher_name" -> Json.toJson(o.publisher_name), "relationships_as_object" -> Json.toJson(o.relationships_as_object), "relationships_as_subject" -> Json.toJson(o.relationships_as_subject), "resources" -> Json.toJson(o.resources), "tags" -> Json.toJson(o.tags), "theme" -> Json.toJson(o.theme), "title" -> Json.toJson(o.title)).filter(_._2 != JsNull))
  }
  implicit lazy val GroupReads: Reads[Group] = Reads[Group] {
    json => JsSuccess(Group((json \ "display_name").asOpt[String], (json \ "description").asOpt[String], (json \ "image_display_url").asOpt[String], (json \ "title").asOpt[String], (json \ "id").asOpt[String], (json \ "name").asOpt[String]))
  }
  implicit lazy val GroupWrites: Writes[Group] = Writes[Group] {
    o => JsObject(Seq("display_name" -> Json.toJson(o.display_name), "description" -> Json.toJson(o.description), "image_display_url" -> Json.toJson(o.image_display_url), "title" -> Json.toJson(o.title), "id" -> Json.toJson(o.id), "name" -> Json.toJson(o.name)).filter(_._2 != JsNull))
  }
  implicit lazy val OrganizationReads: Reads[Organization] = Reads[Organization] {
    json => JsSuccess(Organization((json \ "approval_status").asOpt[String], (json \ "created").asOpt[String], (json \ "description").asOpt[String], (json \ "email").asOpt[String], (json \ "id").asOpt[String], (json \ "image_url").asOpt[String], (json \ "is_organization").asOpt[Boolean], (json \ "name").as[String], (json \ "revision_id").asOpt[String], (json \ "state").asOpt[String], (json \ "title").asOpt[String], (json \ "type").asOpt[String], (json \ "users").asOpt[List[UserOrg]]))
  }
  implicit lazy val OrganizationWrites: Writes[Organization] = Writes[Organization] {
    o => JsObject(Seq("approval_status" -> Json.toJson(o.approval_status), "created" -> Json.toJson(o.created), "description" -> Json.toJson(o.description), "email" -> Json.toJson(o.email), "id" -> Json.toJson(o.id), "image_url" -> Json.toJson(o.image_url), "is_organization" -> Json.toJson(o.is_organization), "name" -> Json.toJson(o.name), "revision_id" -> Json.toJson(o.revision_id), "state" -> Json.toJson(o.state), "title" -> Json.toJson(o.title), "type" -> Json.toJson(o.`type`), "users" -> Json.toJson(o.users)).filter(_._2 != JsNull))
  }
  implicit lazy val RelationshipReads: Reads[Relationship] = Reads[Relationship] {
    json => JsSuccess(Relationship((json \ "subject").asOpt[String], (json \ "object").asOpt[String], (json \ "type").asOpt[String], (json \ "comment").asOpt[String]))
  }
  implicit lazy val RelationshipWrites: Writes[Relationship] = Writes[Relationship] {
    o => JsObject(Seq("subject" -> Json.toJson(o.subject), "object" -> Json.toJson(o.`object`), "type" -> Json.toJson(o.`type`), "comment" -> Json.toJson(o.comment)).filter(_._2 != JsNull))
  }
  implicit lazy val ResourceReads: Reads[Resource] = Reads[Resource] {
    json => JsSuccess(Resource((json \ "cache_last_updated").asOpt[String], (json \ "cache_url").asOpt[String], (json \ "created").asOpt[String], (json \ "datastore_active").asOpt[Boolean], (json \ "description").asOpt[String], (json \ "distribution_format").asOpt[String], (json \ "format").asOpt[String], (json \ "hash").asOpt[String], (json \ "id").asOpt[String], (json \ "last_modified").asOpt[String], (json \ "mimetype").asOpt[String], (json \ "mimetype_inner").asOpt[String], (json \ "name").asOpt[String], (json \ "package_id").asOpt[String], (json \ "position").asOpt[Int], (json \ "resource_type").asOpt[String], (json \ "revision_id").asOpt[String], (json \ "size").asOpt[Int], (json \ "state").asOpt[String], (json \ "url").asOpt[String]))
  }
  implicit lazy val ResourceWrites: Writes[Resource] = Writes[Resource] {
    o => JsObject(Seq("cache_last_updated" -> Json.toJson(o.cache_last_updated), "cache_url" -> Json.toJson(o.cache_url), "created" -> Json.toJson(o.created), "datastore_active" -> Json.toJson(o.datastore_active), "description" -> Json.toJson(o.description), "distribution_format" -> Json.toJson(o.distribution_format), "format" -> Json.toJson(o.format), "hash" -> Json.toJson(o.hash), "id" -> Json.toJson(o.id), "last_modified" -> Json.toJson(o.last_modified), "mimetype" -> Json.toJson(o.mimetype), "mimetype_inner" -> Json.toJson(o.mimetype_inner), "name" -> Json.toJson(o.name), "package_id" -> Json.toJson(o.package_id), "position" -> Json.toJson(o.position), "resource_type" -> Json.toJson(o.resource_type), "revision_id" -> Json.toJson(o.revision_id), "size" -> Json.toJson(o.size), "state" -> Json.toJson(o.state), "url" -> Json.toJson(o.url)).filter(_._2 != JsNull))
  }
  implicit lazy val TagReads: Reads[Tag] = Reads[Tag] {
    json => JsSuccess(Tag((json \ "display_name").asOpt[String], (json \ "id").asOpt[String], (json \ "name").asOpt[String], (json \ "state").asOpt[String], (json \ "vocabulary_id").asOpt[String]))
  }
  implicit lazy val TagWrites: Writes[Tag] = Writes[Tag] {
    o => JsObject(Seq("display_name" -> Json.toJson(o.display_name), "id" -> Json.toJson(o.id), "name" -> Json.toJson(o.name), "state" -> Json.toJson(o.state), "vocabulary_id" -> Json.toJson(o.vocabulary_id)).filter(_._2 != JsNull))
  }
  implicit lazy val ExtraReads: Reads[Extra] = Reads[Extra] {
    json => JsSuccess(Extra((json \ "key").asOpt[String], (json \ "value").asOpt[String]))
  }
  implicit lazy val ExtraWrites: Writes[Extra] = Writes[Extra] {
    o => JsObject(Seq("key" -> Json.toJson(o.key), "value" -> Json.toJson(o.value)).filter(_._2 != JsNull))
  }
  implicit lazy val StdUrisReads: Reads[StdUris] = Reads[StdUris] {
    json => JsSuccess(StdUris((json \ "label").asOpt[String], (json \ "value").asOpt[String]))
  }
  implicit lazy val StdUrisWrites: Writes[StdUris] = Writes[StdUris] {
    o => JsObject(Seq("label" -> Json.toJson(o.label), "value" -> Json.toJson(o.value)).filter(_._2 != JsNull))
  }
  implicit lazy val TokenReads: Reads[Token] = Reads[Token] {
    json => JsSuccess(Token((json \ "token").asOpt[String]))
  }
  implicit lazy val TokenWrites: Writes[Token] = Writes[Token] {
    o => JsObject(Seq("token" -> Json.toJson(o.token)).filter(_._2 != JsNull))
  }
  implicit lazy val UserReads: Reads[User] = Reads[User] {
    json => JsSuccess(User((json \ "name").asOpt[String], (json \ "email").asOpt[String], (json \ "password").asOpt[String], (json \ "fullname").asOpt[String], (json \ "about").asOpt[String]))
  }
  implicit lazy val UserWrites: Writes[User] = Writes[User] {
    o => JsObject(Seq("name" -> Json.toJson(o.name), "email" -> Json.toJson(o.email), "password" -> Json.toJson(o.password), "fullname" -> Json.toJson(o.fullname), "about" -> Json.toJson(o.about)).filter(_._2 != JsNull))
  }
  implicit lazy val AutocompResReads: Reads[AutocompRes] = Reads[AutocompRes] {
    json => JsSuccess(AutocompRes((json \ "match_field").asOpt[String], (json \ "match_displayed").asOpt[String], (json \ "name").asOpt[String], (json \ "title").asOpt[String]))
  }
  implicit lazy val AutocompResWrites: Writes[AutocompRes] = Writes[AutocompRes] {
    o => JsObject(Seq("match_field" -> Json.toJson(o.match_field), "match_displayed" -> Json.toJson(o.match_displayed), "name" -> Json.toJson(o.name), "title" -> Json.toJson(o.title)).filter(_._2 != JsNull))
  }
  implicit lazy val UserOrgReads: Reads[UserOrg] = Reads[UserOrg] {
    json => JsSuccess(UserOrg((json \ "name").asOpt[String], (json \ "capacity").asOpt[String]))
  }
  implicit lazy val UserOrgWrites: Writes[UserOrg] = Writes[UserOrg] {
    o => JsObject(Seq("name" -> Json.toJson(o.name), "capacity" -> Json.toJson(o.capacity)).filter(_._2 != JsNull))
  }
  implicit lazy val CredentialsReads: Reads[Credentials] = Reads[Credentials] {
    json => JsSuccess(Credentials((json \ "username").asOpt[String], (json \ "password").asOpt[String]))
  }
  implicit lazy val CredentialsWrites: Writes[Credentials] = Writes[Credentials] {
    o => JsObject(Seq("username" -> Json.toJson(o.username), "password" -> Json.toJson(o.password)).filter(_._2 != JsNull))
  }
}
