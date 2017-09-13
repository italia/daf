package it.almawave.linkeddata.kb.utils

import scala.util.Try
import java.net.Socket

object ServerChecker {

  def isListening(host: String, port: Int): Boolean = {

    val result = Try {
      val s = new Socket(host, port)
      true
    }

    result.isSuccess

  }

}