package it.gov.daf.catalogmanager.utilities

import java.security._
import javax.crypto._
import javax.crypto.spec._

import base64.Encode.{ urlSafe => toBase64UrlSafe }
import base64.Decode.{ urlSafe => fromBase64UrlSafe }

object SecurePasswordHashing {
  private val RandomSource = new SecureRandom()
  private val HashPartSeparator = ":"
  private val DefaultNrOfPasswordHashIterations = 2000 // 2014 recommended value: 1000-2000
  private val SizeOfPasswordSaltInBytes = 16
  private val SizeOfPasswordHashInBytes = 32

  // This Scala implementation of password hashing was inspired by:
  // https://crackstation.net/hashing-security.htm#javasourcecode
  def hashPassword(password: String): String = hashPassword(password, generateRandomBytes(SizeOfPasswordSaltInBytes))
  def hashPassword(password: String, salt: Array[Byte]): String = hashPassword(password, salt, DefaultNrOfPasswordHashIterations)
  def hashPassword(password: String, salt: Array[Byte], nrOfIterations: Int): String = {
    val hash = pbkdf2(password, salt, nrOfIterations)
    val salt64 = new String(toBase64UrlSafe(salt))
    val hash64 = new String(toBase64UrlSafe(hash))

    s"${nrOfIterations}${HashPartSeparator}${hash64}${HashPartSeparator}${salt64}"
  }

  def validatePassword(password: String, hashedPassword: String): Boolean = {
    /** Compares two byte arrays in length-constant time to prevent timing attacks. */
    def slowEquals(a: Array[Byte], b: Array[Byte]): Boolean = {
      var diff = a.length ^ b.length;
      for (i <- 0 until math.min(a.length, b.length)) diff |= a(i) ^ b(i)
      return diff == 0
    }

    val hashParts = hashedPassword.split(HashPartSeparator)

    if (hashParts.length != 3) return false
    if (!hashParts(0).forall(_.isDigit)) return false

    val nrOfIterations = hashParts(0).toInt // this will throw a NumberFormatException for non-Int numbers...
    val hash = fromBase64UrlSafe(hashParts(1))
    val salt = fromBase64UrlSafe(hashParts(2))

    if (hash.isLeft || salt.isLeft) return false
    if (hash.right.get.length == 0 || salt.right.get.length == 0) return false

    val calculatedHash = pbkdf2(password, salt.right.get, nrOfIterations)

    slowEquals(calculatedHash, hash.right.get)
  }

  private def pbkdf2(password: String, salt: Array[Byte], nrOfIterations: Int): Array[Byte] = {
    val keySpec = new PBEKeySpec(password.toCharArray(), salt, nrOfIterations, SizeOfPasswordHashInBytes * 8)
    val keyFactory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1")

    keyFactory.generateSecret(keySpec).getEncoded()
  }

  private def generateRandomBytes(length: Int): Array[Byte] = {
    val keyData = new Array[Byte](length)
    RandomSource.nextBytes(keyData)
    keyData
  }
}