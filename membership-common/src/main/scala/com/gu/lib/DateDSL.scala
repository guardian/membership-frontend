package com.gu.lib
import org.joda.time.LocalDate

/**
  * This is entirely optional!
  * Its only really useful in unit tests
  */
object DateDSL {
  implicit class IntOps(in: Int) {
    val yearMonth = new LocalDate(_: Int, _: Int, in)
    def Jan(yr: Int) = yearMonth(yr, 1)
    def Feb(yr: Int) = yearMonth(yr, 2)
    def Mar(yr: Int) = yearMonth(yr, 3)
    def Apr(yr: Int) = yearMonth(yr, 4)
    def May(yr: Int) = yearMonth(yr, 5)
    def Jun(yr: Int) = yearMonth(yr, 6)
    def Jul(yr: Int) = yearMonth(yr, 7)
    def Aug(yr: Int) = yearMonth(yr, 8)
    def Sep(yr: Int) = yearMonth(yr, 9)
    def Oct(yr: Int) = yearMonth(yr, 10)
    def Nov(yr: Int) = yearMonth(yr, 11)
    def Dec(yr: Int) = yearMonth(yr, 12)
  }
}