package io.gatling.core.feeder

import akka.actor.ActorSystem

import scala.io.Source

import io.gatling.BaseSpec
import io.gatling.core.config.GatlingConfiguration

class SeparatedValuesFeederSpec extends BaseSpec with FeederSupport {

  implicit val configuration = GatlingConfiguration.loadForTest()

  "tsv" should "handle file without escape char" in {
    val data = tsv("sample1.tsv").build(mock[ActorSystem]).toArray
    data shouldBe Array(Map("foo" -> "hello", "bar" -> "world"))
  }

  it should "handle file with escape char" in {
    val data = tsv("sample2.tsv").build(mock[ActorSystem]).toArray
    data shouldBe Array(Map("foo" -> "hello", "bar" -> "world"))
  }

  "ssv" should "not handle file without escape char" in {
    val data = ssv("sample1.tsv").build(mock[ActorSystem]).toArray
    data should not be Array(Map("foo" -> "hello", "bar" -> "world"))
  }

  it should "handle file with escape char" in {
    val data = ssv("sample2.ssv").build(mock[ActorSystem]).toArray
    data shouldBe Array(Map("foo" -> "hello", "bar" -> "world"))
  }

  "csv" should "not handle file without escape char" in {
    val data = csv("sample1.tsv").build(mock[ActorSystem]).toArray
    data should not be Array(Map("foo" -> "hello", "bar" -> "world"))
  }

  it should "handle file with escape char" in {
    val data = csv("sample2.csv").build(mock[ActorSystem]).toArray
    data shouldBe Array(Map("foo" -> "hello", "bar" -> "world"))
  }

  "SeparatedValuesParser" should "have a proper raw split" in {
    val data = tsv("sample1.tsv", rawSplit = true).build(mock[ActorSystem]).toArray
    data shouldBe Array(Map("foo" -> "hello", "bar" -> "world"))
  }

  "SeparatedValuesParser" should "throw an exception when provided with bad resource" in {
    import io.gatling.core.feeder.SeparatedValuesParser._
    an[Exception] should be thrownBy
      stream(this.getClass.getClassLoader.getResourceAsStream("empty.csv"), CommaSeparator, '\'', rawSplit = false)
  }
}
