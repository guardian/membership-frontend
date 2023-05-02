package com.gu.memsub

case class SupplierCode(get: String)

object SupplierCodeBuilder {

  /**
    * As a SupplierCode may get stored in the session etc, this method generates a safe-to-store SupplierCode from an
    * unsafe String, by stripping out any characters that are not alpha-numeric and trimming to max 255 characters.
    * If the resulting String is non-empty then Some(SupplierCode) will be returned with all characters capitalised,
    * else it will return None.
    * @param code any String
    * @return Option[SupplierCode]
    */
  def buildSupplierCode(code: String): Option[SupplierCode] = {
    val nonNull = if (code != null) code else ""
    val sanitised = nonNull.filter(_.isLetterOrDigit).toUpperCase
    val trimmed = sanitised.take(255)
    if (trimmed.isEmpty) None else Some(SupplierCode(trimmed))
  }
}