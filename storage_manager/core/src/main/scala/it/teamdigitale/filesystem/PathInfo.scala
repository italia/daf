package it.teamdigitale.filesystem

import org.apache.hadoop.fs.{ FileStatus, FileSystem, Path, PathFilter }

/**
  * Interface for containers abstracting information on a path element on the `FileSystem`
  */
sealed trait PathInfo {

  def path: Path

  /**
    * The size of the data on disk in KB
    */
  def size: Double

  /**
    * The '''estimated''' size of the uncompressed data in KB
    */
  def estimatedSize: Double

  def isFile: Boolean

  final def isDir = !isFile

}

case class FileInfo(path: Path,
                    size: Double,
                    format: FileDataFormat,
                    compression: FileCompressionFormat) extends PathInfo {


  lazy val estimatedSize = size / format.compressionRatio / compression.compressionRatio

  val isFile = true

  val isCompressed = compression != NoCompressionFormat

}

case class DirectoryInfo(path: Path, files: Seq[FileInfo]) extends PathInfo {

  lazy val size = files.foldLeft(0d) { _ + _.size }

  lazy val estimatedSize = files.foldLeft(0d) { _ + _.estimatedSize }

  val isFile = false

  def numFiles = files.size

  def isEmpty = files.isEmpty

  def nonEmpty = !isEmpty

  lazy val fileFormats = files.foldLeft(Set.empty[FileDataFormat]) { _ + _.format }

  lazy val fileCompressionFormats = files.foldLeft(Set.empty[FileCompressionFormat]) { _ + _.compression }

  def hasMixedFormats = fileFormats.size > 1

  def hasMixedCompressions = fileCompressionFormats.size > 1

}

object PathInfo {

  private val basicFileExclusions = Set(
    "_log",
    "_SUCCESS"
  )

  private val basicPathFilter: PathFilter = new PathFilter {
    def accept(path: Path) = !basicFileExclusions.contains(path.getName)
  }

  private def collectFiles(statuses: Seq[FileStatus]) = statuses.collect {
    case status if status.isFile => file(status)
  }

  private def file(status: FileStatus) = FileInfo(
    path        = status.getPath,
    size        = status.getLen / 1000d,
    format      = FileDataFormats.fromName(status.getPath.getName),
    compression = FileCompressionFormats.fromName(status.getPath.getName)
  )

  private def directory(fileStatus: FileStatus)(implicit fileSystem: FileSystem) = DirectoryInfo(
    path  = fileStatus.getPath,
    files = collectFiles { fileSystem.listStatus(fileStatus.getPath, basicPathFilter).toSeq }
  )

  def fromHadoop(fileStatus: FileStatus)(implicit fileSystem: FileSystem): PathInfo = if (fileStatus.isDirectory) directory(fileStatus) else file(fileStatus)

  def fromHadoop(path: Path)(implicit fileSystem: FileSystem): PathInfo = fromHadoop { fileSystem.getFileStatus(path) }

}
