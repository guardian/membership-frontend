package com.gu.zuora.soap.readers
import com.gu.zuora.soap.models

trait Query[T <: models.Query] {
  val table: String
  val fields: Seq[String]

  def format(where: String) =
    s"SELECT ${fields.mkString(",")} FROM ${table} WHERE $where"

  def read(results: Seq[Map[String, String]]): Seq[T] = results.map(extract)

  def extract(result: Map[String, String]): T
}

object Query {
  def apply[T <: models.Query](tableName: String, fieldSeq: Seq[String])(extractFn: Map[String, String] => T) =
    new Query[T] {
      val table = tableName
      val fields = fieldSeq
      def extract(results: Map[String, String]) = extractFn(results)
    }
}
