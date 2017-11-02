package it.gov.daf.catalogmanager.repository.voc

import catalog_manager.yaml._

trait VocThemeRepository {
  import play.api.libs.functional.syntax._

  def listThemeAll() :Seq[List[String]]
  def listSubthemeAll(): Seq[List[String]]
  def listSubtheme(themeId: String): Seq[List[String]]
  def daf2dcatapitTheme(dafThemeId: String): Seq[String]
  def daf2dcatapitSubtheme(dafThemeId: String, dafSubthemeId: String): Seq[List[String]]
//  def createTheme(metaCatalog: MetaCatalog,callingUserid :MetadataCat) :Success
}

trait VocThemeRepositoryComponent {
  val vocThemeRepository: VocThemeRepository = null
}


object VocThemeRepository{
  def apply(config: String): VocThemeRepository = config match {
    case "dev" => new VocThemeRepositoryFile
    //case "prod" => new VocRepositoryMongo
  }
}