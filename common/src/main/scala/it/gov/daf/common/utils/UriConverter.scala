package it.gov.daf.common.utils

object UriConverter {

  sealed trait DatasetType
  case object Standard extends DatasetType
  case object Ordinary extends DatasetType
  case object OpenData extends DatasetType

  final case class LogicalUri(
    dsType: DatasetType,
    organization: String,
    theme: String,
    subTheme: String,
    dsName: String
  ) {

    private val logicalUri = dsType match {
      case Standard =>
        s"daf://dataset/standard/$theme/$subTheme/$dsName"
      case Ordinary =>
        s"daf://dataset/$organization/$theme/$subTheme$dsName"
      case OpenData =>
        s"daf://opendata/$dsName"
    }

    private val physicalUri = dsType match {
      case Standard =>
        s"/daf/standard/$theme/$subTheme/$dsName"
      case Ordinary =>
        s"/daf/$organization/$theme/$subTheme$dsName"
      case OpenData =>
        s"/daf/opendata/$dsName"
    }

    override def toString: String = logicalUri

    def toPhysicalUri(): String = physicalUri
  }


}
