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

package daf.dataset

import daf.dataset.query.AggregationsValidator
import org.apache.spark.sql.DataFrame

import scala.util.{Failure, Success, Try}

//FIXME port it in the poc
trait DatasetOperations {

  def defaultLimit: Int
  /**
   *
   * @param df
   * @param conditions
   * @return a valid condition
   */
  //FIXME validate the conditions
  def where(df: Try[DataFrame], conditions: List[String]): Try[DataFrame] = {
    df.map { d =>
      conditions.foldLeft(d)((d, c) => d.where(c))
    }
  }

  private def validateColumns(df: Try[DataFrame], columns: Set[String]): Try[DataFrame] = {
    if (columns.isEmpty) df
    else if (columns.contains("*")) df
    else df.flatMap { d =>
      if (columns.diff(d.columns.toSet).isEmpty) Success(d)
      else Failure(new IllegalArgumentException(s"Columns $columns not found in [${d.columns.mkString(",")}]"))
    }
  }

  /**
   *
   * @param df
   * @param column
   * @return a dataset with only the selected column
   */
  def select(df: Try[DataFrame], column: String): Try[DataFrame] = {
    validateColumns(df, Set(column))
      .map(_.select(column))
  }

  def select(df: Try[DataFrame], columns: List[String]): Try[DataFrame] = {
    if (columns.isEmpty) df
    else validateColumns(df, columns.toSet)
      .map { df =>
        val head :: tail = columns
        df.select(head, tail: _*)
      }
  }

  type Column = String
  type Func = String
  type GroupExpr = (Column, Func)

  /**
   *
   * @param df a dataframe
   * @param column the column used for the aggregation
   * @param groupByOps a list of valid aggregations expression in the form (column,func) where func is in in Set("count", "max", "mean", "min", "sum")
   * @return
   */
  def groupBy(df: Try[DataFrame], column: String, groupByOps: GroupExpr*): Try[DataFrame] = {

    val validatedAggrs = groupByOps.map(kv => AggregationsValidator.validate(kv._2))

    val failureMsg = validatedAggrs.filter(_.isFailure)
      .map {
        case Failure(ex) => ex.getMessage
        case Success(_) => ""
      }.mkString("[", ",", "]")

    //if there are not validations errors
    if (validatedAggrs.forall(_.isSuccess)) {

      val columns: Seq[String] = groupByOps.map(_._1) :+ column
      validateColumns(df, columns.toSet)
        .map(_.groupBy(column).agg(groupByOps.toMap))

    } else Failure(new IllegalArgumentException(failureMsg))
  }

  /**
   *
   * @param df
   * @param limit
   * @return
   */
  def limit(df: Try[DataFrame], limit: Int = defaultLimit): Try[DataFrame] =
    df.map(_.limit(limit))
}

object DatasetOperations extends DatasetOperations {
  override val defaultLimit: Int = 100
}

