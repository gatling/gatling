/**
 * Copyright 2011-2015 eBusiness Information, Groupe Excilys (www.ebusinessinformation.fr)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gatling.core.assertion

import boopickle._

trait AssertionCodec extends PathCodec with TargetCodec with ConditionCodec

trait PathCodec {

  implicit val defaultPathPickler: Pickler[Path] = new Pickler[Path] with PicklerHelper {

    def pickle(obj: Path)(implicit state: PickleState): Unit =
      state.identityRefFor(obj) match {
        case Some(idx) =>
          state.enc.writeInt(-idx)
        case None =>
          obj match {

            case Global => state.enc.writeInt(0)
            case ForAll => state.enc.writeInt(1)
            case Details(parts) =>
              state.enc.writeInt(2)
              state.pickle(parts)
          }

          state.addIdentityRef(obj)
      }
  }

  implicit val defaultPathUnpickler: Unpickler[Path] = new Unpickler[Path] with UnpicklerHelper {

    override def unpickle(implicit state: UnpickleState): Path =
      state.dec.readIntCode match {
        case Right(idx) if idx < 0 =>
          state.identityFor[Path](-idx)

        case Right(0) => Global
        case Right(1) => ForAll
        case Right(2) => Details(read[List[String]])
        case Right(i) => throw new IllegalArgumentException(s"Invalid coding for Path type: $i")
      }
  }
}

trait TargetCodec extends CountMetricCodec with TimeMetricCodec with CountSelectionCodec with TimeSelectionCodec {

  implicit val defaultTargetPickler: Pickler[Target] = new Pickler[Target] with PicklerHelper {

    def pickle(obj: Target)(implicit state: PickleState): Unit =
      state.identityRefFor(obj) match {
        case Some(idx) =>
          state.enc.writeInt(-idx)
        case None =>
          obj match {
            case CountTarget(metric: CountMetric, selection: CountSelection) =>
              state.enc.writeInt(0)
              state.pickle(metric)
              state.pickle(selection)

            case TimeTarget(metric: TimeMetric, selection: TimeSelection) =>
              state.enc.writeInt(1)
              state.pickle(metric)
              state.pickle(selection)

            case MeanRequestsPerSecondTarget =>
              state.enc.writeInt(2)
          }

          state.addIdentityRef(obj)
      }
  }

  implicit val defaultTargetUnpickler = new Unpickler[Target] with UnpicklerHelper {

    override def unpickle(implicit state: UnpickleState): Target =
      state.dec.readIntCode match {
        case Right(idx) if idx < 0 =>
          state.identityFor[Target](-idx)

        case Right(0) => CountTarget(read[CountMetric], read[CountSelection])
        case Right(1) => TimeTarget(read[TimeMetric], read[TimeSelection])
        case Right(2) => MeanRequestsPerSecondTarget
        case Right(i) => throw new IllegalArgumentException(s"Invalid coding for Target type: $i")
      }
  }

}

trait CountMetricCodec {

  implicit val defaultCountMetricPickler: Pickler[CountMetric] = new Pickler[CountMetric] with PicklerHelper {

    def pickle(obj: CountMetric)(implicit state: PickleState): Unit =
      state.identityRefFor(obj) match {
        case Some(idx) =>
          state.enc.writeInt(-idx)
        case None =>
          obj match {
            case AllRequests        => state.enc.writeInt(0)
            case FailedRequests     => state.enc.writeInt(1)
            case SuccessfulRequests => state.enc.writeInt(2)
          }

          state.addIdentityRef(obj)
      }
  }

  implicit val defaultCountMetricUnpickler: Unpickler[CountMetric] = new Unpickler[CountMetric] with UnpicklerHelper {

    override def unpickle(implicit state: UnpickleState): CountMetric =
      state.dec.readIntCode match {
        case Right(idx) if idx < 0 =>
          state.identityFor[CountMetric](-idx)

        case Right(0) => AllRequests
        case Right(1) => FailedRequests
        case Right(2) => SuccessfulRequests
        case Right(i) => throw new IllegalArgumentException(s"Invalid coding for CountMetric type: $i")
      }
  }

}

trait TimeMetricCodec {

  implicit val defaultTimeMetricPickler: Pickler[TimeMetric] = new Pickler[TimeMetric] with PicklerHelper {

    def pickle(obj: TimeMetric)(implicit state: PickleState): Unit =
      state.identityRefFor(obj) match {
        case Some(idx) =>
          state.enc.writeInt(-idx)
        case None =>
          obj match {
            case ResponseTime => state.enc.writeInt(0)
          }

          state.addIdentityRef(obj)
      }
  }

  implicit val defaultTimeMetricUnpickler: Unpickler[TimeMetric] = new Unpickler[TimeMetric] with UnpicklerHelper {

    override def unpickle(implicit state: UnpickleState): TimeMetric =
      state.dec.readIntCode match {
        case Right(idx) if idx < 0 =>
          state.identityFor[TimeMetric](-idx)

        case Right(0) => ResponseTime
        case Right(i) => throw new IllegalArgumentException(s"Invalid coding for TimeMetric type: $i")
      }
  }
}

trait CountSelectionCodec {

  implicit val defaultCountSelectionPickler: Pickler[CountSelection] = new Pickler[CountSelection] with PicklerHelper {

    def pickle(obj: CountSelection)(implicit state: PickleState): Unit =
      state.identityRefFor(obj) match {
        case Some(idx) =>
          state.enc.writeInt(-idx)
        case None =>
          obj match {
            case Count      => state.enc.writeInt(0)
            case Percent    => state.enc.writeInt(1)
            case PerMillion => state.enc.writeInt(2)
          }

          state.addIdentityRef(obj)
      }
  }

  implicit val defaultCountSelectionUnpickler: Unpickler[CountSelection] = new Unpickler[CountSelection] with UnpicklerHelper {

    override def unpickle(implicit state: UnpickleState): CountSelection =
      state.dec.readIntCode match {
        case Right(idx) if idx < 0 =>
          state.identityFor[CountSelection](-idx)

        case Right(0) => Count
        case Right(1) => Percent
        case Right(2) => PerMillion
        case Right(i) => throw new IllegalArgumentException(s"Invalid coding for CountSelection type: $i")
      }
  }
}

trait TimeSelectionCodec {

  implicit val defaultTimeSelectionPickler: Pickler[TimeSelection] = new Pickler[TimeSelection] with PicklerHelper {

    def pickle(obj: TimeSelection)(implicit state: PickleState): Unit =
      state.identityRefFor(obj) match {
        case Some(idx) =>
          state.enc.writeInt(-idx)
        case None =>
          obj match {
            case Min               => state.enc.writeInt(0)
            case Max               => state.enc.writeInt(1)
            case Mean              => state.enc.writeInt(2)
            case StandardDeviation => state.enc.writeInt(3)
            case Percentiles1      => state.enc.writeInt(4)
            case Percentiles2      => state.enc.writeInt(5)
            case Percentiles3      => state.enc.writeInt(6)
            case Percentiles4      => state.enc.writeInt(7)
          }

          state.addIdentityRef(obj)
      }
  }

  implicit val defaultTimeSelectionUnpickler: Unpickler[TimeSelection] = new Unpickler[TimeSelection] with UnpicklerHelper {

    override def unpickle(implicit state: UnpickleState): TimeSelection =
      state.dec.readIntCode match {
        case Right(idx) if idx < 0 =>
          state.identityFor[TimeSelection](-idx)

        case Right(0) => Min
        case Right(1) => Max
        case Right(2) => Mean
        case Right(3) => StandardDeviation
        case Right(4) => Percentiles1
        case Right(5) => Percentiles2
        case Right(6) => Percentiles3
        case Right(7) => Percentiles4
        case Right(i) => throw new IllegalArgumentException(s"Invalid coding for TimeSelection type: $i")
      }
  }
}

trait ConditionCodec {

  implicit val defaultConditionPickler: Pickler[Condition] = new Pickler[Condition] with PicklerHelper {

    def pickle(obj: Condition)(implicit state: PickleState): Unit =
      state.identityRefFor(obj) match {
        case Some(idx) =>
          state.enc.writeInt(-idx)
        case None =>
          obj match {
            case LessThan(value)                 => state.enc.writeInt(0).writeInt(value)
            case GreaterThan(value)              => state.enc.writeInt(1).writeInt(value)
            case Is(value)                       => state.enc.writeInt(2).writeInt(value)
            case Between(lowerBound, upperBound) => state.enc.writeInt(3).writeInt(lowerBound).writeInt(upperBound)
            case In(elements) =>
              state.enc.writeInt(4)
              state.pickle(elements)
          }

          state.addIdentityRef(obj)
      }
  }

  implicit val defaultConditionUnpickler: Unpickler[Condition] = new Unpickler[Condition] with UnpicklerHelper {

    override def unpickle(implicit state: UnpickleState): Condition =
      state.dec.readIntCode match {
        case Right(idx) if idx < 0 =>
          state.identityFor[Condition](-idx)

        case Right(0) => LessThan(read[Int])
        case Right(1) => GreaterThan(read[Int])
        case Right(2) => Is(read[Int])
        case Right(3) => Between(read[Int], read[Int])
        case Right(4) => In(read[List[Int]])
        case Right(i) => throw new IllegalArgumentException(s"Invalid coding for Condition type: $i")
      }
  }
}
