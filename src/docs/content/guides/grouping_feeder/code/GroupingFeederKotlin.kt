import io.gatling.javaapi.core.CoreDsl.*
import io.gatling.javaapi.http.HttpDsl.*
import java.util.*
import java.util.stream.Collectors

class GroupingFeederKotlin {
//#grouping-feeder
val records: List<Map<String, Any>> = csv("file.csv").readRecords()

val recordsGroupedByUsername =
  records
    .stream()
    .collect(Collectors.groupingBy { record: Map<String, Any> -> record["username"] as String })

val groupedRecordsFeeder =
  recordsGroupedByUsername
    .values
    .stream()
    .map { Collections.singletonMap("userRecords", it as Any) }
    .iterator()

val chain =
  feed(groupedRecordsFeeder)
    .foreach("#{userRecords}", "record").on(
      exec(http("request")["#{record.url}"])
    )
//#grouping-feeder
}