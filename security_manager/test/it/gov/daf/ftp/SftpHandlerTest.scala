package it.gov.daf.ftp

import org.scalatest.{FlatSpec, Matchers}

class SftpHandlerTest extends FlatSpec with Matchers {

  val host = "edge1.platform.daf.gov.it"
  val user = "put your user"
  val pwd = "put your password"

  "A SftpHandler" should "be able to print the working directory" in {
    val client = new SftpHandler(user, pwd, host)
    val res = client.workingDir()
    println(res)
    res shouldBe 'Success

    client.disconnect()
  }

  it should "create a directory from an absolute path" in {
    val path = s"/home/$user/test"
    val client = new SftpHandler(user, pwd, host)
    val res = client.mkdir(path)

    println(res)
    res shouldBe 'Success

    client.rmdir(path)
    client.disconnect()
  }

  it should "create folders recursively from a relative path" in {
    val path = s"/home/$user/test/subtest"
    val client = new SftpHandler(user, pwd, host)
    val res = client.mkdir(path)

    println(res)
    res shouldBe 'Success

    client.rmdir(path)
    client.disconnect()
  }

  it should "create a directory from a relative path" in {
    val path = "test"
    val client = new SftpHandler(user, pwd, host)
    val res = client.mkdir(path)

    println(res)
    res shouldBe 'Success

    client.rmdir(path)
    client.disconnect()
  }

}
