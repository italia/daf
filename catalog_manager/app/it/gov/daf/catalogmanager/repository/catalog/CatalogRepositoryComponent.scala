package it.gov.daf.catalogmanager.repository.catalog

import catalog_manager.yaml._
import play.api.libs.ws.WSClient
import scala.concurrent.Future


/**
  * Created by ale on 05/05/17.
  */

trait CatalogRepository {

    import play.api.libs.functional.syntax._

    def listCatalogs(page :Option[Int], limit :Option[Int]) :Seq[MetaCatalog]
    def catalog(catalogId :String): Option[MetaCatalog]
    def catalogByName(name :String, groups: List[String]): Option[MetaCatalog]
    def publicCatalogByName(name: String):Option[MetaCatalog]
    def createCatalog(metaCatalog: MetaCatalog,callingUserid :MetadataCat, ws :WSClient) :Success
    def createCatalogExtOpenData(metaCatalog: MetaCatalog,callingUserid :MetadataCat, ws :WSClient) :Success
    def standardUris() : List[String]
    def isDatasetOnCatalog(name :String): Option[Boolean]
    def deleteCatalogByName(nameCatalog: String, user: String, isAdmin: Boolean): Either[Error, Success]


    // DO NOT DELETE

    /* implicit val SemanticRead :Reads[Semantic] = (
       (JsPath \ "id").readNullable[String] and
         (JsPath \ "context").readNullable[Seq[String]]
       )(Semantic.apply _)

     implicit val MetadataRead :Reads[Metadata] = (
       (JsPath \ "semantics").readNullable[Semantic]  and
         (JsPath \ "desc").readNullable[String] and
         (JsPath \ "tag").readNullable[Seq[String]] and
         (JsPath \ "field_type").readNullable[String] and
         (JsPath \ "cat").readNullable[String]
       ) (Metadata.apply _)

     implicit val FieldRead: Reads[Field] = (
       (JsPath \ "name").read[String] and
         (JsPath \ "type").read[String] and
         (JsPath \ "doc").readNullable[String] and
         (JsPath \ "metadata").readNullable[Metadata]
       ) (Field.apply _)

     implicit val DatasetCatalogRead :Reads[DatasetCatalog] = (
       (JsPath \ "namespace").read[String] and
         (JsPath \ "name").read[String] and
         (JsPath \ "type").read[String] and
         (JsPath \ "fields").readNullable[Seq[Field]]
       )(DatasetCatalog.apply _)

     implicit val InputTypeIng_typeRead :Reads[InputTypeIng_type] =

     implicit val OperationalRead_typeRead :Reads[OperationalRead_type] = Reads(OperationalRead_type(_))

     implicit val InputTypeRead :Reads[InputType] = (
       (JsPath \ "url").read[String] and
         (JsPath \ "src_type").read[String] and
         (JsPath \ "ing_type").read[InputTypeIng_type] and
         (JsPath \ "frequency").readNullable[String] and
         (JsPath \ "table").readNullable[String]
       )(InputType.apply _)

     implicit val OperationalRead :Reads[Operational] = (
       (JsPath \ "group_own").read[String] and
         (JsPath \ "std_schema").readNullable[String] and
         (JsPath \ "read_type").read[OperationalRead_type] and
         (JsPath \ "input_src").read[Seq[InputType]]
       )(Operational.apply _) */

}

trait CatalogRepositoryComponent {
    val catalogRepository: CatalogRepository //= new MonitorRepository
}

object CatalogRepository {
    def apply(config: String): CatalogRepository = config match {
        case "dev" => new CatalogRepositoryFile
        case "prod" => new CatalogRepositoryMongo
    }
}


