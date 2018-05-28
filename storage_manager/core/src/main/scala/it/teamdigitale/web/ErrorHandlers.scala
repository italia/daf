package it.teamdigitale.web

import java.io.FileNotFoundException
import java.lang.reflect.UndeclaredThrowableException

import it.gov.daf.common.web.ErrorHandler
import org.apache.spark.sql.AnalysisException
import play.api.mvc.Results

object ErrorHandlers {

  val spark: ErrorHandler = {
    case _: FileNotFoundException => Results.NotFound
    case _: AnalysisException     => Results.NotFound
    case error: UndeclaredThrowableException if error.getUndeclaredThrowable.isInstanceOf[AnalysisException] => Results.NotFound
  }

}
