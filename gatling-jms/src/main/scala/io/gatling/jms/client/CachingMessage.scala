/*
 * Copyright 2011-2024 GatlingCorp (https://gatling.io)
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

package io.gatling.jms.client

import java.util
import javax.jms.{ BytesMessage, Destination, Message, TextMessage }

private[jms] object CachingMessage {

  def apply(message: Message): Message = message match {
    case tm: TextMessage  => new Text(tm)
    case bm: BytesMessage => new Bytes(bm)
  }
  final class Text(message: TextMessage) extends TextMessage {
    lazy val text: String = message.getText

    override def setText(string: String): Unit = throw new UnsupportedOperationException("read-only")

    override def getText: String = text

    override def getJMSMessageID: String = message.getJMSMessageID

    override def setJMSMessageID(id: String): Unit = throw new UnsupportedOperationException("read-only")

    override def getJMSTimestamp: Long = message.getJMSTimestamp

    override def setJMSTimestamp(timestamp: Long): Unit = throw new UnsupportedOperationException("read-only")

    override def getJMSCorrelationIDAsBytes: Array[Byte] = message.getJMSCorrelationIDAsBytes

    override def setJMSCorrelationIDAsBytes(correlationID: Array[Byte]): Unit = throw new UnsupportedOperationException("read-only")

    override def setJMSCorrelationID(correlationID: String): Unit = throw new UnsupportedOperationException("read-only")

    override def getJMSCorrelationID: String = message.getJMSCorrelationID

    override def getJMSReplyTo: Destination = message.getJMSReplyTo

    override def setJMSReplyTo(replyTo: Destination): Unit = throw new UnsupportedOperationException("read-only")

    override def getJMSDestination: Destination = message.getJMSDestination

    override def setJMSDestination(destination: Destination): Unit = throw new UnsupportedOperationException("read-only")

    override def getJMSDeliveryMode: Int = message.getJMSDeliveryMode

    override def setJMSDeliveryMode(deliveryMode: Int): Unit = throw new UnsupportedOperationException("read-only")

    override def getJMSRedelivered: Boolean = message.getJMSRedelivered

    override def setJMSRedelivered(redelivered: Boolean): Unit = throw new UnsupportedOperationException("read-only")

    override def getJMSType: String = message.getJMSType

    override def setJMSType(`type`: String): Unit = throw new UnsupportedOperationException("read-only")

    override def getJMSExpiration: Long = message.getJMSExpiration

    override def setJMSExpiration(expiration: Long): Unit = throw new UnsupportedOperationException("read-only")

    override def getJMSDeliveryTime: Long = message.getJMSDeliveryTime

    override def setJMSDeliveryTime(deliveryTime: Long): Unit = throw new UnsupportedOperationException("read-only")

    override def getJMSPriority: Int = message.getJMSPriority

    override def setJMSPriority(priority: Int): Unit = throw new UnsupportedOperationException("read-only")

    override def clearProperties(): Unit = throw new UnsupportedOperationException("read-only")

    override def propertyExists(name: String): Boolean = message.propertyExists(name)

    override def getBooleanProperty(name: String): Boolean = message.getBooleanProperty(name)

    override def getByteProperty(name: String): Byte = message.getByteProperty(name)

    override def getShortProperty(name: String): Short = message.getShortProperty(name)

    override def getIntProperty(name: String): Int = message.getIntProperty(name)

    override def getLongProperty(name: String): Long = message.getLongProperty(name)

    override def getFloatProperty(name: String): Float = message.getFloatProperty(name)

    override def getDoubleProperty(name: String): Double = message.getDoubleProperty(name)

    override def getStringProperty(name: String): String = message.getStringProperty(name)

    override def getObjectProperty(name: String): AnyRef = message.getObjectProperty(name)

    override def getPropertyNames: util.Enumeration[_] = message.getPropertyNames

    override def setBooleanProperty(name: String, value: Boolean): Unit = throw new UnsupportedOperationException("read-only")

    override def setByteProperty(name: String, value: Byte): Unit = throw new UnsupportedOperationException("read-only")

    override def setShortProperty(name: String, value: Short): Unit = throw new UnsupportedOperationException("read-only")

    override def setIntProperty(name: String, value: Int): Unit = throw new UnsupportedOperationException("read-only")

    override def setLongProperty(name: String, value: Long): Unit = throw new UnsupportedOperationException("read-onl")

    override def setFloatProperty(name: String, value: Float): Unit = throw new UnsupportedOperationException("read-only")

    override def setDoubleProperty(name: String, value: Double): Unit = throw new UnsupportedOperationException("read-only")

    override def setStringProperty(name: String, value: String): Unit = throw new UnsupportedOperationException("read-only")

    override def setObjectProperty(name: String, value: Any): Unit = throw new UnsupportedOperationException("read-only")

    override def acknowledge(): Unit = message.acknowledge()

    override def clearBody(): Unit = throw new UnsupportedOperationException("read-only")

    override def getBody[T](c: Class[T]): T = message.getBody(c)

    override def isBodyAssignableTo(c: Class[_]): Boolean = message.isBodyAssignableTo(c)
  }

  final class Bytes(message: BytesMessage) extends BytesMessage {
    private var streamFullyConsumed = false
    private def tryFullyConsumingStream(): Unit = {
      if (streamPartiallyConsumed) {
        throw new UnsupportedOperationException("BytesMessage has already been partially consumed with readXXX calls")
      }
      streamFullyConsumed = true
    }

    private var streamPartiallyConsumed = false
    private def tryPartiallyConsumingStream(): Unit = {
      if (streamFullyConsumed) {
        throw new UnsupportedOperationException("BytesMessage has already been fully consumed with reading the full bytes")
      }
      streamPartiallyConsumed = true
    }

    lazy val bytes: Array[Byte] = {
      tryFullyConsumingStream()
      val buffer = Array.ofDim[Byte](message.getBodyLength.toInt)
      message.readBytes(buffer)
      buffer
    }

    override def getBodyLength: Long = message.getBodyLength

    override def readBoolean(): Boolean = {
      tryPartiallyConsumingStream()
      message.readBoolean()
    }

    override def readByte(): Byte = {
      tryPartiallyConsumingStream()
      message.readByte()
    }

    override def readUnsignedByte(): Int = {
      tryPartiallyConsumingStream()
      message.readUnsignedByte()
    }

    override def readShort(): Short = {
      tryPartiallyConsumingStream()
      message.readShort()
    }

    override def readUnsignedShort(): Int = {
      tryPartiallyConsumingStream()
      message.readUnsignedShort()
    }

    override def readChar(): Char = {
      tryPartiallyConsumingStream()
      message.readChar()
    }

    override def readInt(): Int = {
      tryPartiallyConsumingStream()
      message.readInt()
    }

    override def readLong(): Long = {
      tryPartiallyConsumingStream()
      message.readLong()
    }

    override def readFloat(): Float = {
      tryPartiallyConsumingStream()
      message.readFloat()
    }

    override def readDouble(): Double = {
      tryPartiallyConsumingStream()
      message.readDouble()
    }

    override def readUTF(): String = {
      tryPartiallyConsumingStream()
      message.readUTF()
    }

    override def readBytes(value: Array[Byte]): Int = {
      tryPartiallyConsumingStream()
      message.readBytes(value)
    }

    override def readBytes(value: Array[Byte], length: Int): Int = {
      tryPartiallyConsumingStream()
      message.readBytes(value, length)
    }

    override def writeBoolean(value: Boolean): Unit = throw new UnsupportedOperationException("read-only")

    override def writeByte(value: Byte): Unit = throw new UnsupportedOperationException("read-only")

    override def writeShort(value: Short): Unit = throw new UnsupportedOperationException("read-only")

    override def writeChar(value: Char): Unit = throw new UnsupportedOperationException("read-only")

    override def writeInt(value: Int): Unit = throw new UnsupportedOperationException("read-only")

    override def writeLong(value: Long): Unit = throw new UnsupportedOperationException("read-only")

    override def writeFloat(value: Float): Unit = throw new UnsupportedOperationException("read-only")

    override def writeDouble(value: Double): Unit = throw new UnsupportedOperationException("read-only")

    override def writeUTF(value: String): Unit = throw new UnsupportedOperationException("read-only")

    override def writeBytes(value: Array[Byte]): Unit = throw new UnsupportedOperationException("read-only")

    override def writeBytes(value: Array[Byte], offset: Int, length: Int): Unit = throw new UnsupportedOperationException("read-only")

    override def writeObject(value: Any): Unit = throw new UnsupportedOperationException("read-only")

    override def reset(): Unit = throw new UnsupportedOperationException("read-only")

    override def getJMSMessageID: String = message.getJMSMessageID

    override def setJMSMessageID(id: String): Unit = throw new UnsupportedOperationException("read-only")

    override def getJMSTimestamp: Long = message.getJMSTimestamp

    override def setJMSTimestamp(timestamp: Long): Unit = throw new UnsupportedOperationException("read-only")

    override def getJMSCorrelationIDAsBytes: Array[Byte] = message.getJMSCorrelationIDAsBytes

    override def setJMSCorrelationIDAsBytes(correlationID: Array[Byte]): Unit = throw new UnsupportedOperationException("read-only")

    override def setJMSCorrelationID(correlationID: String): Unit = throw new UnsupportedOperationException("read-only")

    override def getJMSCorrelationID: String = message.getJMSCorrelationID

    override def getJMSReplyTo: Destination = message.getJMSReplyTo

    override def setJMSReplyTo(replyTo: Destination): Unit = throw new UnsupportedOperationException("read-only")

    override def getJMSDestination: Destination = message.getJMSDestination

    override def setJMSDestination(destination: Destination): Unit = throw new UnsupportedOperationException("read-only")

    override def getJMSDeliveryMode: Int = message.getJMSDeliveryMode

    override def setJMSDeliveryMode(deliveryMode: Int): Unit = throw new UnsupportedOperationException("read-only")

    override def getJMSRedelivered: Boolean = message.getJMSRedelivered

    override def setJMSRedelivered(redelivered: Boolean): Unit = throw new UnsupportedOperationException("read-only")

    override def getJMSType: String = message.getJMSType

    override def setJMSType(`type`: String): Unit = throw new UnsupportedOperationException("read-only")

    override def getJMSExpiration: Long = message.getJMSExpiration

    override def setJMSExpiration(expiration: Long): Unit = throw new UnsupportedOperationException("read-only")

    override def getJMSDeliveryTime: Long = message.getJMSDeliveryTime

    override def setJMSDeliveryTime(deliveryTime: Long): Unit = throw new UnsupportedOperationException("read-only")

    override def getJMSPriority: Int = message.getJMSPriority

    override def setJMSPriority(priority: Int): Unit = throw new UnsupportedOperationException("read-only")

    override def clearProperties(): Unit = throw new UnsupportedOperationException("read-only")

    override def propertyExists(name: String): Boolean = message.propertyExists(name)

    override def getBooleanProperty(name: String): Boolean = message.getBooleanProperty(name)

    override def getByteProperty(name: String): Byte = message.getByteProperty(name)

    override def getShortProperty(name: String): Short = message.getShortProperty(name)

    override def getIntProperty(name: String): Int = message.getIntProperty(name)

    override def getLongProperty(name: String): Long = message.getLongProperty(name)

    override def getFloatProperty(name: String): Float = message.getFloatProperty(name)

    override def getDoubleProperty(name: String): Double = message.getDoubleProperty(name)

    override def getStringProperty(name: String): String = message.getStringProperty(name)

    override def getObjectProperty(name: String): AnyRef = message.getObjectProperty(name)

    override def getPropertyNames: util.Enumeration[_] = message.getPropertyNames

    override def setBooleanProperty(name: String, value: Boolean): Unit = throw new UnsupportedOperationException("read-only")

    override def setByteProperty(name: String, value: Byte): Unit = throw new UnsupportedOperationException("read-only")

    override def setShortProperty(name: String, value: Short): Unit = throw new UnsupportedOperationException("read-only")

    override def setIntProperty(name: String, value: Int): Unit = throw new UnsupportedOperationException("read-only")

    override def setLongProperty(name: String, value: Long): Unit = throw new UnsupportedOperationException("read-only")

    override def setFloatProperty(name: String, value: Float): Unit = throw new UnsupportedOperationException("read-only")

    override def setDoubleProperty(name: String, value: Double): Unit = throw new UnsupportedOperationException("read-only")

    override def setStringProperty(name: String, value: String): Unit = throw new UnsupportedOperationException("read-only")

    override def setObjectProperty(name: String, value: Any): Unit = throw new UnsupportedOperationException("read-only")

    override def acknowledge(): Unit = message.acknowledge()

    override def clearBody(): Unit = throw new UnsupportedOperationException("read-only")

    override def getBody[T](c: Class[T]): T = message.getBody(c)

    override def isBodyAssignableTo(c: Class[_]): Boolean = message.isBodyAssignableTo(c)
  }
}
