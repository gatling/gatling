package com.excilys.ebi.gatling.core.result.message

import org.specs2.mutable.Specification
import org.junit.runner.RunWith
import org.specs2.runner.JUnitRunner
import com.excilys.ebi.gatling.core.result.message.RequestStatus._

@RunWith(classOf[JUnitRunner])
class RequestRecordSpec extends Specification {

  "constructor" should {
    "have sensible defaults for optional parameters" in {
      val record: RequestRecord = RequestRecord("scenarioName", 1, "requestName", 0L, 0L, 0L, 0L, OK, "requestMessage")

      record.extraRequestInfo should beEmpty
    }

  }
}
