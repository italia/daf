
package catalog_manager.yaml

import de.zalando.play.controllers.ResponseWriters

import scala.concurrent.Future
import play.api.mvc._
import de.zalando.play.controllers.SwaggerSecurityExtractors._
import catalog_manager.yaml
import play.api.libs.json.JsValue

import scala.math.BigInt



object SecurityExtractorsExecutionContext {
    // this ExecutionContext might be overridden if default configuration is not suitable for some reason
    implicit val ec = de.zalando.play.controllers.Contexts.tokenChecking
}

trait SecurityExtractors {
    def basicAuth_Extractor[User >: Any](): RequestHeader => Future[Option[User]] =
        header => basicAuth(header) { (username: String, password: String) =>
            userFromToken("abracadabra")
    }


    implicit val unauthorizedContentWriter = ResponseWriters.choose[String]("application/json")
    def unauthorizedContent(req: RequestHeader) = Results.Unauthorized("Unauthorized")

     def userFromToken[User >: Any](token: String): User = {
         Token(Some(token))
         // Gettest200(token)
         //controllers_base. Gettest200(Token(Some(token)))
    }
}

