package io.gatling.core.assertion

import org.scalatest.{ FlatSpec, Matchers }

import io.gatling.core.assertion.AssertionTags._

class AssertionSerializationSpec extends FlatSpec with Matchers {

  private def tabSeparated(elems: String*) = elems.mkString("\t")

  "The serialization mechanism" should "be able to serialize Paths" in {
    Global.serialized.toString shouldBe tabSeparated(GlobalTag)
    Details(List("Group", "Subgroup", "Request")).serialized.toString() shouldBe tabSeparated(DetailsTag, "Group", "Subgroup", "Request")
  }

  it should "be able to serialize Metrics" in {
    AllRequests.serialized.toString shouldBe tabSeparated(AllRequestsTag)
    FailedRequests.serialized.toString shouldBe tabSeparated(FailedRequestsTag)
    SuccessfulRequests.serialized.toString shouldBe tabSeparated(SuccessfulRequestsTag)
    ResponseTime.serialized.toString shouldBe tabSeparated(ResponseTimeTag)
  }

  it should "be able to serialize Selections" in {
    Count.serialized.toString shouldBe tabSeparated(CountTag)
    Percent.serialized.toString shouldBe tabSeparated(PercentTag)
    Min.serialized.toString shouldBe tabSeparated(MinTag)
    Max.serialized.toString shouldBe tabSeparated(MaxTag)
    Mean.serialized.toString shouldBe tabSeparated(MeanTag)
    StandardDeviation.serialized.toString shouldBe tabSeparated(StandardDeviationTag)
    Percentiles1.serialized.toString shouldBe tabSeparated(Percentiles1Tag)
    Percentiles2.serialized.toString shouldBe tabSeparated(Percentiles2Tag)
  }

  it should "be able to serialize Conditions" in {
    LessThan(3).serialized.toString shouldBe tabSeparated(LessThanTag, "3.0")
    GreaterThan(4).serialized.toString shouldBe tabSeparated(GreaterThanTag, "4.0")
    Is(2.5).serialized.toString shouldBe tabSeparated(IsTag, "2.5")
    Between(2.5, 6.3).serialized.toString shouldBe tabSeparated(BetweenTag, "2.5", "6.3")
    In(List(2, 3, 4, 5.5, 7.2)).serialized.toString shouldBe tabSeparated(InTag, "2.0", "3.0", "4.0", "5.5", "7.2")
  }

  it should "be able to serialize Targets" in {
    CountTarget(AllRequests, Percent).serialized.toString shouldBe tabSeparated(AllRequestsTag, PercentTag)
    TimeTarget(ResponseTime, StandardDeviation).serialized.toString shouldBe tabSeparated(ResponseTimeTag, StandardDeviationTag)
    MeanRequestsPerSecondTarget.serialized.toString shouldBe tabSeparated(MeanRequestsPerSecondTag)
  }

  it should "be able to serialize to serialize assertions" in {
    Assertion(Global, MeanRequestsPerSecondTarget, LessThan(3)).serialized.toString shouldBe
      tabSeparated(AssertionTag, PathTag, GlobalTag, TargetTag, MeanRequestsPerSecondTag, ConditionTag, LessThanTag, "3.0")

    Assertion(Global, CountTarget(AllRequests, Count), Is(3)).serialized.toString shouldBe
      tabSeparated(AssertionTag, PathTag, GlobalTag, TargetTag, AllRequestsTag, CountTag, ConditionTag, IsTag, "3.0")

    Assertion(Details(List("Group", "Request")), TimeTarget(ResponseTime, Max), In(List(1, 2, 3))).serialized.toString shouldBe
      tabSeparated(AssertionTag, PathTag, DetailsTag, "Group", "Request", TargetTag,
        ResponseTimeTag, MaxTag, ConditionTag, InTag, "1.0", "2.0", "3.0")
  }

}
