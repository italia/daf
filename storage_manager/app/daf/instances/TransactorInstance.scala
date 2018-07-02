/*
 * Copyright 2017 TEAM PER LA TRASFORMAZIONE DIGITALE
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package daf.instances

import java.util.Properties

import cats.effect.IO
import com.cloudera.impala.jdbc41.{ Driver => ImpalaDriver }
import config.ImpalaConfig
import doobie.free.KleisliInterpreter
import doobie.free.connection.{ close, setAutoCommit, unit }
import doobie.util.transactor.{ Strategy, Transactor }
import org.apache.hive.jdbc.HiveConnection

trait TransactorInstance {

  protected def impalaConfig: ImpalaConfig

  def transactor(userId: String): Transactor[IO]

}

trait HiveTransactorInstance extends TransactorInstance {

  private val connectionStrategy = Strategy(
    before = setAutoCommit(false),
    after  = unit,
    oops   = unit,
    always = close
  )

  private val sslPart = impalaConfig.sslConfig.map { config => s";ssl=true;sslTrustStore=${config.keystore};trustStorePassword=${config.password}" } getOrElse ""

  private def impersonationPart(userId: String) = s";impala.doas.user=$userId"

  private def createUrl(userId: String) = s"${impalaConfig.hiveUrl}$sslPart"

  private def createConnection(userId: String) = new HiveConnection(createUrl(userId), new Properties())

  final def transactor(userId: String) = Transactor[IO, HiveConnection](
    createConnection(userId),
    IO(_),
    KleisliInterpreter[IO].ConnectionInterpreter,
    connectionStrategy
  )

}

trait ImpalaTransactorInstance extends TransactorInstance {

  private val driver = new ImpalaDriver

  private def connectionProps(userId: String, props: Properties = new Properties()) = {
    props.put("DelegationUID", userId)

    impalaConfig.sslConfig.foreach { config =>
      props.put("SSL", "1")
      props.put("SSLKeyStore", config.keystore)
      props.put("SSLKeyStorePwd", config.password)
    }

    props
  }

  private def createConnection(userId: String) = driver.connect(impalaConfig.impalaUrl, connectionProps(userId))

  final def transactor(userId: String) = Transactor.fromConnection[IO] { createConnection(userId) }

}