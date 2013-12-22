package io.gatling.jms
import javax.jms.Message

package object jms {

  /**
   * Type for jms checks
   */
  type JmsCheck = (Message) => Boolean

}


