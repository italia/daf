package it.teamdigitale.filesystem

trait FileFormat {

  def extensions: Set[String]

  def compressionRatio: Double

}

trait SingleExtension { this: FileFormat =>

  protected def extension: String

  final lazy val extensions = Set(extension)

}

trait NoExtensions { this: FileFormat =>

  final def extensions = Set.empty[String]

}

trait NoCompression { this: FileFormat =>

  final lazy val compressionRatio = 1.0

}