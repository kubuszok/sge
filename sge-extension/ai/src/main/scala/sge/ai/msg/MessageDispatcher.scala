/*
 * Ported from libGDX gdx-ai - https://github.com/libgdx/gdx-ai
 * Original source: com/badlogic/gdx/ai/msg/MessageDispatcher.java
 * Original authors: davebaol
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Renames: `com.badlogic.gdx.ai.msg` -> `sge.ai.msg`; `Array` -> `DynamicArray`;
 *     `IntMap` -> `mutable.HashMap[Int, _]`; `GdxAI.getTimepiece()` -> `(using Timepiece)`
 *   Convention: split packages; `null` -> `Nullable`; 20+ overloaded `dispatchMessage` methods
 *     consolidated into one method with default parameters; `isDebugEnabled`/`setDebugEnabled` -> `var debugEnabled`;
 *     logging removed (SGE AI module has no Logger abstraction)
 *   Idiom: `Pool` created via anonymous trait implementation; `ClassReflection.isInstance` -> `isInstanceOf`
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 388
 * Covenant-baseline-methods: MessageDispatcher,PendingMessageCallback,addListener,addListeners,addProvider,addProviders,clear,clearAllListeners,clearAllProviders,clearListeners,clearProviders,clearQueue,continue,currentTime,debugEnabled,defaultPool,discharge,dispatchMessage,handleMessage,i,initialCapacity,listeners,max,msgListeners,msgProviders,newObject,providers,queue,queueSize,removeListener,removeListeners,report,scanQueue,telegram,this,update
 * Covenant-source-reference: com/badlogic/gdx/ai/msg/MessageDispatcher.java
 *   Renames: `com.badlogic.gdx.ai.msg` -> `sge.ai.msg`; `Array` -> `DynamicArray`;
 *   Convention: split packages; `null` -> `Nullable`; 20+ overloaded `dispatchMessage` methods
 *   Idiom: `Pool` created via anonymous trait implementation; `ClassReflection.isInstance` -> `isInstanceOf`
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 388
 * Covenant-baseline-methods: MessageDispatcher,PendingMessageCallback,addListener,addListeners,addProvider,addProviders,clear,clearAllListeners,clearAllProviders,clearListeners,clearProviders,clearQueue,continue,currentTime,debugEnabled,defaultPool,discharge,dispatchMessage,handleMessage,i,initialCapacity,listeners,max,msgListeners,msgProviders,newObject,providers,queue,queueSize,removeListener,removeListeners,report,scanQueue,telegram,this,update
 * Covenant-verified: 2026-04-19
 * Covenant-verified: 2026-04-19
 *
 * upstream-commit: 6726e345248ddcad7cec0737f6ad83e4e028266d
 */
package sge
package ai
package msg

import sge.utils.Nullable
import sge.utils.DynamicArray
import sge.utils.Pool
import scala.collection.mutable.HashMap

/** A `MessageDispatcher` is in charge of the creation, dispatch, and management of telegrams.
  *
  * @author
  *   davebaol (original implementation)
  */
class MessageDispatcher(pool: Pool[Telegram]) extends Telegraph {

  def this() = this(MessageDispatcher.defaultPool)

  private val queue: PriorityQueue[Telegram] = PriorityQueue[Telegram]()

  private val msgListeners: HashMap[Int, DynamicArray[Telegraph]] = HashMap.empty

  private val msgProviders: HashMap[Int, DynamicArray[TelegramProvider]] = HashMap.empty

  /** Whether debug mode is on. When enabled, dispatch events are printed to stdout. */
  var debugEnabled: Boolean = false

  /** Registers a listener for the specified message code. Messages without an explicit receiver are broadcasted to all its registered listeners.
    * @param listener
    *   the listener to add
    * @param msg
    *   the message code
    */
  def addListener(listener: Telegraph, msg: Int)(using timepiece: Timepiece): Unit = {
    val listeners = msgListeners.getOrElseUpdate(msg, DynamicArray[Telegraph](false, 16))
    listeners.add(listener)

    // Dispatch messages from registered providers
    msgProviders.get(msg).foreach { providers =>
      var i = 0
      while (i < providers.size) {
        val provider = providers(i)
        val info     = provider.provideMessageInfo(msg, listener)
        if (info.isDefined) {
          val sender: Nullable[Telegraph] =
            if (provider.isInstanceOf[Telegraph]) Nullable(provider.asInstanceOf[Telegraph])
            else Nullable.empty
          dispatchMessage(
            msg = msg,
            sender = sender,
            receiver = Nullable(listener),
            extraInfo = info
          )
        }
        i += 1
      }
    }
  }

  /** Registers a listener for a selection of message types.
    * @param listener
    *   the listener to add
    * @param msgs
    *   the message codes
    */
  def addListeners(listener: Telegraph, msgs: Int*)(using Timepiece): Unit =
    msgs.foreach(addListener(listener, _))

  /** Registers a provider for the specified message code.
    * @param provider
    *   the provider to add
    * @param msg
    *   the message code
    */
  def addProvider(provider: TelegramProvider, msg: Int): Unit = {
    val providers = msgProviders.getOrElseUpdate(msg, DynamicArray[TelegramProvider](false, 16))
    providers.add(provider)
  }

  /** Registers a provider for a selection of message types.
    * @param provider
    *   the provider to add
    * @param msgs
    *   the message codes
    */
  def addProviders(provider: TelegramProvider, msgs: Int*): Unit =
    msgs.foreach(addProvider(provider, _))

  /** Unregister the specified listener for the specified message code.
    * @param listener
    *   the listener to remove
    * @param msg
    *   the message code
    */
  def removeListener(listener: Telegraph, msg: Int): Unit =
    msgListeners.get(msg).foreach { listeners =>
      listeners.removeValue(listener)
    }

  /** Unregister the specified listener for the selection of message codes.
    * @param listener
    *   the listener to remove
    * @param msgs
    *   the message codes
    */
  def removeListeners(listener: Telegraph, msgs: Int*): Unit =
    msgs.foreach(removeListener(listener, _))

  /** Unregisters all the listeners for the specified message code. */
  def clearListeners(msg: Int): Unit =
    msgListeners.remove(msg)

  /** Unregisters all the listeners for the given message codes. */
  def clearListeners(msgs: Int*): Unit =
    msgs.foreach(msg => msgListeners.remove(msg))

  /** Removes all the registered listeners for all the message codes. */
  def clearAllListeners(): Unit =
    msgListeners.clear()

  /** Unregisters all the providers for the specified message code. */
  def clearProviders(msg: Int): Unit =
    msgProviders.remove(msg)

  /** Unregisters all the providers for the given message codes. */
  def clearProviders(msgs: Int*): Unit =
    msgs.foreach(msg => msgProviders.remove(msg))

  /** Removes all the registered providers for all the message codes. */
  def clearAllProviders(): Unit =
    msgProviders.clear()

  /** Removes all the telegrams from the queue and releases them to the internal pool. */
  def clearQueue(): Unit = {
    var i = 0
    while (i < queue.size) {
      queue.get(i).foreach(pool.free)
      i += 1
    }
    queue.clear()
  }

  /** Removes all the telegrams from the queue and the registered listeners for all the messages. */
  def clear(): Unit = {
    clearQueue()
    clearAllListeners()
    clearAllProviders()
  }

  /** Given a message, a receiver, a sender and any time delay, this method routes the message to the correct agents (if no delay) or stores in the message queue to be dispatched at the correct time.
    *
    * @param msg
    *   the message code
    * @param delay
    *   the delay in seconds (0 for immediate dispatch)
    * @param sender
    *   the sender of the telegram
    * @param receiver
    *   the receiver of the telegram; if empty the telegram is broadcasted to all registered listeners
    * @param extraInfo
    *   an optional object
    * @param needsReturnReceipt
    *   whether the return receipt is needed or not
    * @throws IllegalArgumentException
    *   if the sender is empty and the return receipt is needed
    */
  def dispatchMessage(
    msg:                Int,
    delay:              Float = 0f,
    sender:             Nullable[Telegraph] = Nullable.empty,
    receiver:           Nullable[Telegraph] = Nullable.empty,
    extraInfo:          Nullable[Any] = Nullable.empty,
    needsReturnReceipt: Boolean = false
  )(using timepiece: Timepiece): Unit = {
    if (sender.isEmpty && needsReturnReceipt)
      throw new IllegalArgumentException("Sender cannot be null when a return receipt is needed")

    // Get a telegram from the pool
    val telegram = pool.obtain()
    telegram.sender = sender
    telegram.receiver = receiver
    telegram.message = msg
    telegram.extraInfo = extraInfo
    telegram.returnReceiptStatus =
      if (needsReturnReceipt) Telegram.RETURN_RECEIPT_NEEDED else Telegram.RETURN_RECEIPT_UNNEEDED

    // If there is no delay, route telegram immediately
    if (delay <= 0.0f) {
      if (debugEnabled) {
        val currentTime = timepiece.time
        println(
          s"Instant telegram dispatched at time: $currentTime by $sender for $receiver. Message code is $msg"
        )
      }

      // Send the telegram to the recipient
      discharge(telegram)
    } else {
      val currentTime = timepiece.time

      // Set the timestamp for the delayed telegram
      telegram.timestamp = currentTime + delay

      // Put the telegram in the queue
      val added = queue.add(telegram)

      // Return it to the pool if it has been rejected
      if (!added) pool.free(telegram)

      if (debugEnabled) {
        if (added)
          println(
            s"Delayed telegram from $sender for $receiver recorded at time $currentTime. Message code is $msg"
          )
        else
          println(
            s"Delayed telegram from $sender for $receiver rejected by the queue. Message code is $msg"
          )
      }
    }
  }

  /** Dispatches any delayed telegrams with a timestamp that has expired. Dispatched telegrams are removed from the queue.
    *
    * This method must be called regularly from inside the main game loop to facilitate the correct and timely dispatch of any delayed messages. Notice that the message dispatcher internally calls
    * `timepiece.time` to get the current AI time and properly dispatch delayed messages. This means that:
    *   - if you forget to update the timepiece the delayed messages won't be dispatched.
    *   - ideally the timepiece should be updated before the message dispatcher.
    */
  def update()(using timepiece: Timepiece): Unit = {
    val currentTime = timepiece.time

    // Peek at the queue to see if any telegrams need dispatching.
    // Remove all telegrams from the front of the queue that have gone
    // past their time stamp.
    var continue = true
    while (continue) {
      val peeked = queue.peek()
      if (peeked.isEmpty) {
        continue = false
      } else {
        val telegram = peeked.get
        // Exit loop if the telegram is in the future
        if (telegram.timestamp > currentTime) {
          continue = false
        } else {
          if (debugEnabled) {
            println(
              s"Queued telegram ready for dispatch: Sent to ${telegram.receiver}. Message code is ${telegram.message}"
            )
          }

          // Send the telegram to the recipient
          discharge(telegram)

          // Remove it from the queue
          queue.poll()
        }
      }
    }
  }

  /** Scans the queue and passes pending messages to the given callback in any particular order.
    *
    * Typically this method is used to save (serialize) pending messages and restore (deserialize and schedule) them back on game loading.
    *
    * @param callback
    *   The callback used to report pending messages individually.
    */
  def scanQueue(callback: PendingMessageCallback)(using timepiece: Timepiece): Unit = {
    val currentTime = timepiece.time
    val queueSize   = queue.size
    var i           = 0
    while (i < queueSize) {
      queue.get(i).foreach { telegram =>
        callback.report(
          telegram.timestamp - currentTime,
          telegram.sender,
          telegram.receiver,
          telegram.message,
          telegram.extraInfo,
          telegram.returnReceiptStatus
        )
      }
      i += 1
    }
  }

  /** This method is used by `dispatchMessage` for immediate telegrams and `update` for delayed telegrams. It first calls the message handling method of the receiving agents with the specified
    * telegram then returns the telegram to the pool.
    */
  private def discharge(telegram: Telegram): Unit = {
    if (telegram.receiver.isDefined) {
      // Dispatch the telegram to the receiver specified by the telegram itself
      if (!telegram.receiver.get.handleMessage(telegram)) {
        // Telegram could not be handled
        if (debugEnabled) println(s"Message ${telegram.message} not handled")
      }
    } else {
      // Dispatch the telegram to all the registered receivers
      var handledCount = 0
      msgListeners.get(telegram.message).foreach { listeners =>
        var i = 0
        while (i < listeners.size) {
          if (listeners(i).handleMessage(telegram)) {
            handledCount += 1
          }
          i += 1
        }
      }
      // Telegram could not be handled
      if (debugEnabled && handledCount == 0) println(s"Message ${telegram.message} not handled")
    }

    if (telegram.returnReceiptStatus == Telegram.RETURN_RECEIPT_NEEDED) {
      // Use this telegram to send the return receipt
      telegram.receiver = telegram.sender
      telegram.sender = Nullable(this)
      telegram.returnReceiptStatus = Telegram.RETURN_RECEIPT_SENT
      discharge(telegram)
    } else {
      // Release the telegram to the pool
      pool.free(telegram)
    }
  }

  /** Handles the telegram just received. This method always returns `false` since usually the message dispatcher never receives telegrams. Actually, the message dispatcher implements [[Telegraph]]
    * just because it can send return receipts.
    */
  override def handleMessage(msg: Telegram): Boolean = false
}

object MessageDispatcher {

  /** The global Telegram pool shared by all dispatchers created without an explicit pool. */
  private val defaultPool: Pool[Telegram] = new Pool[Telegram] {
    override protected val max:             Int      = Int.MaxValue
    override protected val initialCapacity: Int      = 16
    override protected def newObject():     Telegram = Telegram()
  }
}

/** A `PendingMessageCallback` is used by the [[MessageDispatcher.scanQueue]] method to report pending messages individually.
  *
  * @author
  *   davebaol (original implementation)
  */
trait PendingMessageCallback {

  /** Reports a pending message.
    * @param delay
    *   The remaining delay in seconds
    * @param sender
    *   The message sender
    * @param receiver
    *   The message receiver
    * @param message
    *   The message code
    * @param extraInfo
    *   Any additional information that may accompany the message
    * @param returnReceiptStatus
    *   The return receipt status of the message
    */
  def report(
    delay:               Float,
    sender:              Nullable[Telegraph],
    receiver:            Nullable[Telegraph],
    message:             Int,
    extraInfo:           Nullable[Any],
    returnReceiptStatus: Int
  ): Unit
}
