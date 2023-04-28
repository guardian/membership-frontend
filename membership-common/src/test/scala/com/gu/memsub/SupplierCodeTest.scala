package com.gu.memsub

import org.specs2.mutable.Specification

class SupplierCodeTest extends Specification {
  "buildSupplierCode" should {
    "returnNoneWhenStringIsWhollyInappropriate" in {
      SupplierCodeBuilder.buildSupplierCode(null) mustEqual None
      SupplierCodeBuilder.buildSupplierCode("") mustEqual None
      SupplierCodeBuilder.buildSupplierCode(" ") mustEqual None
      SupplierCodeBuilder.buildSupplierCode("_") mustEqual None
      SupplierCodeBuilder.buildSupplierCode("<%>$!") mustEqual None
    }
    "stripOutNonAlphaNumericCharacters" in {
      SupplierCodeBuilder.buildSupplierCode("FOO ") mustEqual Some(SupplierCode("FOO"))
      SupplierCodeBuilder.buildSupplierCode("FOO BAR") mustEqual Some(SupplierCode("FOOBAR"))
      SupplierCodeBuilder.buildSupplierCode("FOO@BAR") mustEqual Some(SupplierCode("FOOBAR"))
      SupplierCodeBuilder.buildSupplierCode("1@A") mustEqual Some(SupplierCode("1A"))
    }
    "trimWhenLongerThan255Chars" in {
      SupplierCodeBuilder.buildSupplierCode("F" * 256) mustEqual Some(SupplierCode("F" * 255))
      SupplierCodeBuilder.buildSupplierCode("F" * 255) mustEqual Some(SupplierCode("F" * 255))
    }
    "autoCapitaliseAllCharacters" in {
      SupplierCodeBuilder.buildSupplierCode("FooBar123") mustEqual Some(SupplierCode("FOOBAR123"))
    }
  }
}
