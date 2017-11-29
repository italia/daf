
import play.api.mvc.{Action,Controller}

import play.api.data.validation.Constraint

import play.api.i18n.MessagesApi

import play.api.inject.{ApplicationLifecycle,ConfigurationProvider}

import de.zalando.play.controllers._

import PlayBodyParsing._

import PlayValidations._

import scala.util._

import javax.inject._

import de.zalando.play.controllers.PlayBodyParsing._
import org.apache.tinkerpop.gremlin.structure.{T,Vertex}
import org.janusgraph.core.attribute.Geoshape
import org.janusgraph.core.schema.JanusGraphManagement
import org.janusgraph.core.{JanusGraphFactory,PropertyKey}
import org.janusgraph.diskstorage.configuration.backend.CommonsConfiguration
import scala.concurrent.Future
import org.janusgraph.core.Cardinality
import collection.convert.decorateAsScala._
import collection.convert.decorateAsScala._
import collection.convert.decorateAsScala._
import collection.convert.decorateAsScala._
import collection.convert.decorateAsScala._
import collection.convert.decorateAsScala._

/**
 * This controller is re-generated after each change in the specification.
 * Please only place your hand-written code between appropriate comments in the body of the controller.
 */

package entity_manager.yaml {
    // ----- Start of unmanaged code area for package Entity_managerYaml
              @SuppressWarnings(
    Array(
      "org.wartremover.warts.AsInstanceOf"
    )
  )
    // ----- End of unmanaged code area for package Entity_managerYaml
    class Entity_managerYaml @Inject() (
        // ----- Start of unmanaged code area for injections Entity_managerYaml

        // ----- End of unmanaged code area for injections Entity_managerYaml
        val messagesApi: MessagesApi,
        lifecycle: ApplicationLifecycle,
        config: ConfigurationProvider
    ) extends Entity_managerYamlBase {
        // ----- Start of unmanaged code area for constructor Entity_managerYaml
    val janusgraphConfiguration = new CommonsConfiguration

    config.get.getConfig("janusgraph").foreach(_.entrySet.foreach(
      entry =>
        janusgraphConfiguration.set(entry._1, entry._2.unwrapped().asInstanceOf[String])
    ))

    val graph = JanusGraphFactory.open(janusgraphConfiguration)

    val mgmt: JanusGraphManagement = graph.openManagement

    val entityName: PropertyKey = {
      if (!mgmt.containsPropertyKey("entityName"))
        mgmt.makePropertyKey("entityName").dataType(classOf[String]).cardinality(Cardinality.SINGLE).make
      else
        mgmt.getPropertyKey("entityName")
    }

    val place: PropertyKey = {
      if (!mgmt.containsPropertyKey("place"))
        mgmt.makePropertyKey("place").dataType(classOf[Geoshape]).make
      else
        mgmt.getPropertyKey("place")
    }

    val index = {
      val index = mgmt.getGraphIndex("vertices")
      if (index == null)
        mgmt.buildIndex("vertices", classOf[Vertex]).addKey(entityName).addKey(place).buildMixedIndex("search")
      else
        index
    }

    mgmt.commit()
    /*
    val tx: JanusGraphTransaction = graph.newTransaction

    val v1: Vertex = tx.addVertex(T.label, "myvertex", "place", Geoshape.point(38.1f, 23.7f))
    val v2: Vertex = tx.addVertex(T.label, "myvertex", "place", Geoshape.point(37.7f, 23.9f))
    val v3: Vertex = tx.addVertex(T.label, "myvertex", "place", Geoshape.point(39f, 22f))

    tx.commit()

    import collection.convert.decorateAsScala._

    val vertices = graph.traversal.V().has("place", Geo.geoWithin(Geoshape.circle(37.97, 23.72, 50))).asScala

    vertices.foreach(println(_))
    */
    lifecycle.addStopHook { () =>
      Future.successful({
        graph.close()
      })
    }

        // ----- End of unmanaged code area for constructor Entity_managerYaml
        val getEntities = getEntitiesAction { input: (EntitiesGetPageNumber, EntitiesGetPageNumber) =>
            val (pageSize, pageNumber) = input
            // ----- Start of unmanaged code area for action  Entity_managerYaml.getEntities
            NotImplementedYet
            // ----- End of unmanaged code area for action  Entity_managerYaml.getEntities
        }
        val createEntity = createEntityAction { (entity: Entity) =>  
            // ----- Start of unmanaged code area for action  Entity_managerYaml.createEntity
            val tx = graph.newTransaction
      val vertex = tx.addVertex(T.label, "entity", "entityName", entity.entityname)
      val id = vertex.id().asInstanceOf[Long]
      tx.commit()
      CreateEntity200(EntityId(id))
            // ----- End of unmanaged code area for action  Entity_managerYaml.createEntity
        }
        val getEntity = getEntityAction { (entityname: String) =>  
            // ----- Start of unmanaged code area for action  Entity_managerYaml.getEntity
            import collection.convert.decorateAsScala._
      val vertices = graph.traversal.V().has("entityName", entityname).asScala
      if (vertices.isEmpty)
        GetEntity404()
      else {
        val vertex = vertices.next()
        GetEntity200(Entity(vertex.property[String]("entityName").value()))
      }
            // ----- End of unmanaged code area for action  Entity_managerYaml.getEntity
        }
    
    }
}
