import CommonBuild._

name := "storage_manager"

version := "1.0.0"

// Wart Remover Plugin Configuration
wartremoverErrors ++= Warts.allBut(Wart.Nothing, Wart.PublicInference, Wart.Any, Wart.Equals)

wartremoverExcluded ++= getRecursiveListOfFiles(baseDirectory.value / "target" / "scala-2.11" / "routes").toSeq