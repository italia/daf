package it.gov.daf.catalogmanager.repository.voc

import java.io.FileInputStream
import play.api.libs.json._

class VocThemeRepositoryFile extends VocThemeRepository{

  def listThemeAll() :Seq[List[String]] = {
    val stream = new FileInputStream("data/voc/cv_theme-subtheme.json")
    val json = try { (Json.parse(stream) \ "voc").asOpt[JsArray]} finally {stream.close()}

    json match {
      case Some(s) => s.value.map(x=> List((x \ "theme_daf_code").as[String], (x \ "theme_daf_ita").as[String]))
      case None =>
        println("VocRepositoryFile - Error occurred with Json")
        Seq()
    }
  }


  def listSubthemeAll(): Seq[List[String]] = {
    val stream = new FileInputStream("data/voc/cv_theme-subtheme.json")
    val json = try { (Json.parse(stream) \ "voc").asOpt[JsArray]} finally {stream.close()}

    json match {
      case Some(s) => s.value.map(x => ((x \ "theme_daf_code").as[String],
        (x \ "subthemes").as[List[JsValue]])).map{ x=>

        x._2.map{ y=>
          List((y \ "subtheme_daf_code").as[String], (y \ "subtheme_daf_ita").as[String], x._1)
        }
      }.flatMap(x=>x)
      case None =>
        println("VocRepositoryFile - Error occurred with Json")
        Seq()
    }
  }
  def listSubtheme(themeId: String): Seq[List[String]] = {
    val stream = new FileInputStream("data/voc/cv_theme-subtheme.json")
    val json = try { (Json.parse(stream) \ "voc").asOpt[JsArray]} finally {stream.close()}
    json match {
      case Some(s) => s.value.map(x => ((x \ "theme_daf_code").as[String],
        (x \ "subthemes").as[List[JsValue]]))
        .filter(x => x._1.equals(themeId))
        .map{ x=>

          x._2.map{ y=>
            List((y \ "subtheme_daf_code").as[String], (y \ "subtheme_daf_ita").as[String])
          }
        }.flatten
      case None =>
        println("VocRepositoryFile - Error occurred with Json")
        Seq()
    }
  }
  def daf2dcatapitTheme(dafThemeId: String): Seq[String] = {
    val stream = new FileInputStream("data/voc/cv_theme-subtheme.json")
    val json = try { (Json.parse(stream) \ "voc").asOpt[JsArray]} finally {stream.close()}
    json match {
      case Some(s) => s.value.map(x => ((x \ "theme_daf_code").as[String],
        (x \ "theme_ita").as[String]))
        .filter(x => x._1.equals(dafThemeId))
        .map(x=> x._2)
      case None =>
        println("VocRepositoryFile - Error occurred with Json")
        Seq()
    }
  }
  def daf2dcatapitSubtheme(dafThemeId: String, dafSubthemeId: String = "__-1NOFILTER__"): Seq[List[String]] = {
    val stream = new FileInputStream("data/voc/cv_theme-subtheme.json")
    val json = try { (Json.parse(stream) \ "voc").asOpt[JsArray]} finally {stream.close()}


    json match {
      case Some(s) => s.value.map(x => ((x \ "theme_daf_code").as[String],
        (x \ "theme_ita").as[String],
        (x \ "subthemes").as[List[JsValue]]))
        .filter(x => x._1.equals(dafThemeId))
        .map{ x=>
          x._3.map{ y=>
            (x._2, (y \ "subthemes_ita").as[List[String]], (y \ "subtheme_daf_code").as[String])
          }
        }
        .flatten
        .filter(x=> x._3.equals(dafSubthemeId) || dafSubthemeId.equals("__-1NOFILTER__"))
        .map{ x=>
          x._2.map{ y=>
            List(x._1, y)
          }

        }.flatten
      case None =>
        println("VocRepositoryFile - Error occurred with Json")
        Seq()
    }
  }
}
