package it.gov.daf.catalogmanager.tempo


import scala.util._
import java.security.SecureRandom
import java.security.MessageDigest

/*
 * Generates a Bearer Token with a length of
 * 32 characters (MD5) or 64 characters (SHA-256) according to the
 * specification RFC6750 (http://tools.ietf.org/html/rfc6750)
 *
 * Uniqueness obtained by hashing system time combined with a
 * application supplied 'tokenprefix' such as a sessionid or username
 *
 * public methods:
 *  generateMD5Token(tokenprefix: String): String
 *  generateSHAToken(tokenprefix: String): String
 *
 * Example usage:
 *
 * val tokenGenerator = new BearerTokenGenerator
 * val username = "mary.smith"
 * val token = tokenGenerator.generateMD5Token(username)
 * println(token)
 *
 * Author:	Jeff Steinmetz, @jeffsteinmetz
 *
 */
class BearerTokenGenerator {

  val TOKEN_LENGTH = 45	// TOKEN_LENGTH is not the return size from a hash,
  // but the total characters used as random token prior to hash
  // 45 was selected because System.nanoTime().toString returns
  // 19 characters.  45 + 19 = 64.  Therefore we are guaranteed
  // at least 64 characters (bytes) to use in hash, to avoid MD5 collision < 64
  val TOKEN_CHARS = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz_.-"
  val secureRandom = new SecureRandom()

  private def toHex(bytes: Array[Byte]): String = bytes.map( "%02x".format(_) ).mkString("")

  private def sha(s: String): String = {
    toHex(MessageDigest.getInstance("SHA-256").digest(s.getBytes("UTF-8")))
  }
  private def md5(s: String): String = {
    toHex(MessageDigest.getInstance("MD5").digest(s.getBytes("UTF-8")))
  }

  // use tail recursion, functional style to build string.
  private def generateToken(tokenLength: Int) : String = {
    val charLen = TOKEN_CHARS.length()
    def generateTokenAccumulator(accumulator: String, number: Int) : String = {
      if (number == 0) return accumulator
      else
        generateTokenAccumulator(accumulator + TOKEN_CHARS(secureRandom.nextInt(charLen)).toString, number - 1)
    }
    generateTokenAccumulator("", tokenLength)
  }

  /*
   *  Hash the Token to return a 32 or 64 character HEX String
   *
   *  Parameters:
   *  tokenprifix: string to concatenate with random generated token prior to HASH to improve uniqueness, such as username
     *
     *  Returns:
     *  MD5 hash of (username + current time + random token generator) as token, 128 bits, 32 characters
     * or
     *  SHA-256 hash of (username + current time + random token generator) as token, 256 bits, 64 characters
     */
  def generateMD5Token(tokenprefix: String): String =  {
    md5(tokenprefix + System.nanoTime() + generateToken(TOKEN_LENGTH))
  }
  def generateSHAToken(tokenprefix: String): String =  {
    sha(tokenprefix + System.nanoTime() + generateToken(TOKEN_LENGTH))
  }
}
