package io.gatling.core.assertion

import org.scalatest.{ FlatSpec, Matchers }

import io.gatling.core.assertion.AssertionTags._

class AssertionSerializationSpec extends FlatSpec with Matchers {

  private def tabSeparated(elems: String*) = elems.mkString("\t")

  "The serialization mechanism" should "be able to serialize Paths" in {
    Global.serialized.toString shouldBe tabSeparated(GlobalTag)
    ForAll.serialized.toString shouldBe tabSeparated(ForAllTag)
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
    Percentiles3.serialized.toString shouldBe tabSeparated(Percentiles3Tag)
    Percentiles4.serialized.toString shouldBe tabSeparated(Percentiles4Tag)
  }

  it should "be able to serialize Conditions" in {
    LessThan(3).serialized.toString shouldBe tabSeparated(LessThanTag, "3")
    GreaterThan(4).serialized.toString shouldBe tabSeparated(GreaterThanTag, "4")
    Is(2).serialized.toString shouldBe tabSeparated(IsTag, "2")
    Between(2, 6).serialized.toString shouldBe tabSeparated(BetweenTag, "2", "6")
    In(List(2, 3, 4, 5, 7)).serialized.toString shouldBe tabSeparated(InTag, "2", "3", "4", "5", "7")
  }

  it should "be able to serialize Targets" in {
    CountTarget(AllRequests, Percent).serialized.toString shouldBe tabSeparated(AllRequestsTag, PercentTag)
    TimeTarget(ResponseTime, StandardDeviation).serialized.toString shouldBe tabSeparated(ResponseTimeTag, StandardDeviationTag)
    MeanRequestsPerSecondTarget.serialized.toString shouldBe tabSeparated(MeanRequestsPerSecondTag)
  }

  it should "be able to serialize assertions" in {
    Assertion(Global, MeanRequestsPerSecondTarget, LessThan(3)).serialized.toString shouldBe
      tabSeparated(AssertionTag, PathTag, GlobalTag, TargetTag, MeanRequestsPerSecondTag, ConditionTag, LessThanTag, "3")

    Assertion(Global, CountTarget(AllRequests, Count), Is(3)).serialized.toString shouldBe
      tabSeparated(AssertionTag, PathTag, GlobalTag, TargetTag, AllRequestsTag, CountTag, ConditionTag, IsTag, "3")

    Assertion(ForAll, CountTarget(SuccessfulRequests, Percent), LessThan(5)).serialized.toString shouldBe
      tabSeparated(AssertionTag, PathTag, ForAllTag, TargetTag, SuccessfulRequestsTag, PercentTag, ConditionTag, LessThanTag, "5")

    Assertion(Details(List("Group", "Request")), TimeTarget(ResponseTime, Max), In(List(1, 2, 3))).serialized.toString shouldBe
      tabSeparated(AssertionTag, PathTag, DetailsTag, "Group", "Request", TargetTag,
        ResponseTimeTag, MaxTag, ConditionTag, InTag, "1", "2", "3")
  }

}
