package it.gov.daf.catalogmanager.repository.voc

import java.io.FileInputStream

import play.api.libs.json.{JsArray, JsValue}

object TestJson extends App {
  import play.api.libs.json.{JsError, JsResult, JsSuccess, Json}

  val stream = new FileInputStream("data/voc/cv_theme-subtheme.json")
  val json = try { (Json.parse(stream) \ "voc").asOpt[JsArray]} finally {stream.close()}
  val themeId = "agricoltura"
  val subthemeId = "policy"
  print {
    json match {
      case Some(s) => s.value.map(x => ((x \ "theme_daf_code").as[String],
        (x \ "theme_ita").as[String],
        (x \ "subthemes").as[List[JsValue]]))
        .filter(x => x._1.equals(themeId))
        .map{ x=>
          x._3.map{ y=>
            (x._2, (y \ "subthemes_ita").as[List[String]], (y \ "subtheme_daf_code").as[String])
          }
        }
          .flatten
          .filter(x=> x._3.equals(subthemeId) || subthemeId.equals("__-1NOFILTER__"))
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
