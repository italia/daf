package it.gov.daf.catalogmanager.repository.voc

import java.io.FileInputStream

import catalog_manager.yaml.KeyValue
import play.api.libs.json.{JsArray, JsValue}

object TestJson extends App {
  import play.api.libs.json.{JsError, JsResult, JsSuccess, Json}

  val stream = new FileInputStream("data/voc/cv_theme-subtheme_bk.json")
  val json = try { (Json.parse(stream) \ "voc").asOpt[JsArray]} finally {stream.close()}
  val dcatapitThemeId = "AGRI"
  val subthemeId = "policy"
  print {
    json match {
      case Some(s) => s.value.map(x => ((x \ "theme_code").as[String],
        (x \ "subthemes").as[List[JsValue]])).map{ x=>

        x._2.map{ y=> //subthemes

          (x._1, (y \ "subthemes_ita").as[List[List[String]]])
        }
      }.flatten
        .filter(x=>x._1.equals(dcatapitThemeId))
        .map{x=>
          println(x)
          x._2.map{y=>
            println(y)
            println(y.length)
            //println(y(0))
            //println(y(1))
            KeyValue(y(0), y(1))
          }
        }.flatMap(x=>x)
      case None =>
        println("VocRepositoryFile - Error occurred with Json")
        Seq()
    }
  }

}
