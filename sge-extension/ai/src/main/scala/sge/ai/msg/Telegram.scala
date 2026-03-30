/*
 * Ported from libGDX gdx-ai - https://github.com/libgdx/gdx-ai
 * Original source: com/badlogic/gdx/ai/msg/Telegram.java
 * Original authors: davebaol
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Renames: `com.badlogic.gdx.ai.msg` -> `sge.ai.msg`
 *   Convention: split packages; `null` -> `Nullable`; `getTimestamp`/`setTimestamp` -> `var timestamp`
 *   Idiom: implements `Pool.Poolable` and `Comparable[Telegram]`
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package ai
package msg

import sge.utils.Nullable
import sge.utils.Pool

/** A Telegram is the container of a message. The [[MessageDispatcher]] manages telegram life-cycle.
  *
  * @author
  *   davebaol (original implementation)
  */
class Telegram extends Comparable[Telegram] with Pool.Poolable {

  /** The agent that sent this telegram. */
  var sender: Nullable[Telegraph] = Nullable.empty

  /** The agent that is to receive this telegram. */
  var receiver: Nullable[Telegraph] = Nullable.empty

  /** The message type. */
  var message: Int = 0

  /** Any additional information that may accompany the message. */
  var extraInfo: Nullable[Any] = Nullable.empty

  /** The return receipt status of this telegram. Its value should be [[Telegram.RETURN_RECEIPT_UNNEEDED]], [[Telegram.RETURN_RECEIPT_NEEDED]] or [[Telegram.RETURN_RECEIPT_SENT]].
    */
  var returnReceiptStatus: Int = Telegram.RETURN_RECEIPT_UNNEEDED

  /** Messages can be dispatched immediately or delayed for a specified amount of time. If a delay is necessary, this field is stamped with the time the message should be dispatched.
    */
  var timestamp: Float = 0f

  override def reset(): Unit = {
    sender = Nullable.empty
    receiver = Nullable.empty
    message = 0
    returnReceiptStatus = Telegram.RETURN_RECEIPT_UNNEEDED
    extraInfo = Nullable.empty
    timestamp = 0f
  }

  override def compareTo(other: Telegram): Int =
    if (this.equals(other)) 0
    else if (this.timestamp - other.timestamp < 0) -1
    else 1

  override def hashCode(): Int = {
    val prime  = 31
    var result = 1
    result = prime * result + message
    result = prime * result + receiver.fold(0)(_.hashCode())
    result = prime * result + sender.fold(0)(_.hashCode())
    result = prime * result + java.lang.Float.floatToIntBits(timestamp)
    result
  }

  override def equals(obj: Any): Boolean =
    if (obj.asInstanceOf[AnyRef] eq this) true
    else
      obj match {
        case other: Telegram =>
          message == other.message &&
          java.lang.Float.floatToIntBits(timestamp) == java.lang.Float.floatToIntBits(other.timestamp) &&
          sender.fold(other.sender.isEmpty)(s => other.sender.exists(_ == s)) &&
          receiver.fold(other.receiver.isEmpty)(r => other.receiver.exists(_ == r))
        case _ => false
      }
}

object Telegram {

  /** Indicates that the sender doesn't need any return receipt. */
  val RETURN_RECEIPT_UNNEEDED: Int = 0

  /** Indicates that the sender needs the return receipt. */
  val RETURN_RECEIPT_NEEDED: Int = 1

  /** Indicates that the return receipt has been sent back to the original sender of the telegram. */
  val RETURN_RECEIPT_SENT: Int = 2
}
