//import sbt.File
//
//object CommonBuild {
//  def getRecursiveListOfFiles(dir: File): Array[File] = {
//    val these = dir.listFiles
//    these ++ these.filter(_.isDirectory).flatMap(getRecursiveListOfFiles)
//  }
//}