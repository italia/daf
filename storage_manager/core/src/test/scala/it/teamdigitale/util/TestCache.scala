package it.teamdigitale.util

import play.api.cache.CacheApi

import scala.collection.mutable.{ Map => MutableMap }
import scala.concurrent.duration.Duration
import scala.reflect.ClassTag

class TestCache extends CacheApi {

  private val cache = MutableMap.empty[String, Any]

  def set(key: String, value: Any, expiration: Duration) = cache += { key -> value }

  def remove(key: String) = cache -= key

  def getOrElse[A](key: String, expiration: Duration)(orElse: => A)(implicit A: ClassTag[A]) = cache.get(key) match {
    case Some(value: A) => value
    case None           => orElse
    case Some(value)    => throw new IllegalArgumentException(s"Value found for key [$key] had type [${value.getClass.getSimpleName}] instead of the expected [${A.runtimeClass.getSimpleName}]")
  }

  def get[A](key: String)(implicit A: ClassTag[A]) = cache.get(key) match {
    case Some(value: A) => Some(value)
    case None           => None
    case Some(value)    => throw new IllegalArgumentException(s"Value found for key [$key] had type [${value.getClass.getSimpleName}] instead of the expected [${A.runtimeClass.getSimpleName}]")
  }

}
