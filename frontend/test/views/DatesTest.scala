package views

import org.specs2.mutable.Specification

class DatesTest extends Specification {
  "addSuffix" should {

    "append the correct suffix for 'th'" in {
      forall((4 to 20) ++ (24 to 30)) ((num:Int) => Dates.suffix(num) mustEqual("th"))
    }

    "append the correct suffix for 'st" in {
      forall(Seq(1, 21, 31)) ((num:Int) => Dates.suffix(num) mustEqual("st"))
    }

    "append the correct suffix for 'nd" in {
      forall(Seq(2, 22)) ((num:Int) => Dates.suffix(num) mustEqual("nd"))
    }

    "append the correct suffix for 'rd" in {
      forall(Seq(3, 23)) ((num:Int) => Dates.suffix(num) mustEqual("rd"))
    }
  }
}
