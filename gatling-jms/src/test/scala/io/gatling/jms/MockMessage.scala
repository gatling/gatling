package io.gatling.jms

import org.specs2.mock.Mockito
import javax.jms.{ Message, TextMessage }

trait MockMessage extends Mockito {

  def textMessage(text: String) = {
    val msg = mock[TextMessage]
    msg.getText returns text
  }

  def message = mock[Message]
}
