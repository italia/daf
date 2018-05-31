import de.heikoseeberger.sbtheader.HeaderPlugin.autoImport.HeaderLicense
import de.heikoseeberger.sbtheader.License
import sbt.URL

object Licenses {

  val apache2: (String, URL) = { "Apache-2.0" -> new URL("https://www.apache.org/licenses/LICENSE-2.0.txt") }

  val header: Option[License] = Some { HeaderLicense.ALv2("2017", "TEAM PER LA TRASFORMAZIONE DIGITALE") }

}
