package com.gu.zuora

package object soap {

  trait ZuoraException extends Throwable

  case class ZuoraQueryException(s: String) extends ZuoraException {
    override def getMessage: String = s
  }
}
