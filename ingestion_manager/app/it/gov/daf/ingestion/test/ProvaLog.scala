package it.gov.daf.ingestion.test

import org.slf4j.{Logger, LoggerFactory}


object ProvaLog extends App {
  val logger: Logger = LoggerFactory.getLogger(this.getClass)

  logger.info("ciao mamma")
  logger.error("ciao mamma errore")
}
