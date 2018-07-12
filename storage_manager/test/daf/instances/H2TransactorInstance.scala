package daf.instances

import cats.effect.IO
import doobie.util.transactor.Transactor
import org.apache.commons.dbcp.BasicDataSource

trait H2TransactorInstance extends TransactorInstance {

  private def configureDataSource(dataSource: BasicDataSource = new BasicDataSource) = {
    dataSource.setUrl("jdbc:h2:mem:")
    dataSource
  }

  private val internal = Transactor.fromDataSource[IO] { configureDataSource() }

  final def transactor(userId: String) = internal

}
