/*
 * Copyright (c) 2007, Slick 2D
 *
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are permitted
 * provided that the following conditions are met:
 *
 * Redistributions of source code must retain the above copyright notice, this list of conditions
 * and the following disclaimer. Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the documentation and/or other
 * materials provided with the distribution. Neither the name of the Slick 2D nor the names of its
 * contributors may be used to endorse or promote products derived from this software without
 * specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
 * FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY
 * WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: backends/gdx-backend-lwjgl3/.../audio/OggInputStream.java
 * Original authors: kevin (Slick 2D)
 *
 * Migration notes:
 *   Renames: OggInputStream stays the same
 *   Convention: JVM-only (depends on JOrbis); placed in scalajvm/sge/audio/
 *   Convention: boundary/break instead of return
 *   Idiom: LWJGL BufferUtils → java.nio.ByteBuffer.allocateDirect
 *   Idiom: Gdx.app.error → Sge().app.error
 *   Idiom: GdxRuntimeException → SgeError
 *   Idiom: Nullable[OggInputStream] for previousStream parameter
 *   Audited: 2026-03-08
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package audio

import java.io.{ IOException, InputStream }
import java.nio.{ ByteBuffer, ByteOrder }
import com.jcraft.jogg.{ Packet, Page, StreamState, SyncState }
import com.jcraft.jorbis.{ Block, Comment, DspState, Info }
import sge.utils.{ Nullable, SgeError, StreamUtils }
import scala.compiletime.uninitialized
import scala.util.boundary
import scala.util.boundary.break

/** An input stream to read Ogg Vorbis.
  *
  * @param input
  *   the input stream from which to read the OGG file
  * @param previousStream
  *   optional stream instance to reuse buffers from (it's not a good idea to use the old stream instance afterwards)
  * @author
  *   kevin
  */
class OggInputStream(input: InputStream, previousStream: Nullable[OggInputStream])(using Sge) extends InputStream {

  import OggInputStream.*

  def this(input: InputStream)(using Sge) = {
    this(input, Nullable.empty)
  }

  /** The conversion buffer size */
  private var convsize: Int = BufferSize * 4

  /** The buffer used to read OGG file */
  private var convbuffer: Array[Byte] = uninitialized

  /** The audio information from the OGG header */
  private val oggInfo: Info = new Info() // struct that stores all the static vorbis bitstream settings

  /** True if we're at the end of the available data */
  private var endOfStream: Boolean = false

  /** The Vorbis SyncState used to decode the OGG */
  private val syncState: SyncState = new SyncState() // sync and verify incoming physical bitstream

  /** The Vorbis Stream State used to decode the OGG */
  private val streamState: StreamState = new StreamState() // take physical pages, weld into a logical stream of packets

  /** The current OGG page */
  private val page: Page = new Page() // one Ogg bitstream page. Vorbis packets are inside

  /** The current packet page */
  private val packet: Packet = new Packet() // one raw packet of data for decode

  /** The comment read from the OGG file */
  private val comment: Comment = new Comment() // struct that stores all the bitstream user comments

  /** The Vorbis DSP state used to decode the OGG */
  private val dspState: DspState = new DspState() // central working state for the packet->PCM decoder

  /** The OGG block we're currently working with to convert PCM */
  private val vorbisBlock: Block = new Block(dspState) // local working space for packet->PCM decode

  /** Temporary scratch buffer */
  private var buffer: Array[Byte] = uninitialized

  /** The number of bytes read */
  private var bytes: Int = 0

  /** True if we should be reading big endian */
  private val bigEndian: Boolean = ByteOrder.nativeOrder() == ByteOrder.BIG_ENDIAN

  /** True if we've reached the end of the current bit stream */
  private var endOfBitStream: Boolean = true

  /** True if we've initialised the OGG info block */
  private var inited: Boolean = false

  /** The index into the byte array we currently read from */
  private var readIndex: Int = 0

  /** The byte array store used to hold the data read from the ogg */
  private var pcmBuffer: ByteBuffer = uninitialized

  /** The total number of bytes */
  private var total: Int = 0

  // Initialise buffers
  previousStream.fold {
    convbuffer = new Array[Byte](convsize)
    pcmBuffer = ByteBuffer.allocateDirect(4096 * 500)
  } { prev =>
    convbuffer = prev.convbuffer
    pcmBuffer = prev.pcmBuffer
  }

  try
    total = input.available()
  catch {
    case ex: IOException => throw SgeError.InvalidInput("Failed to get available bytes", Some(ex))
  }

  init()

  /** Get the number of bytes on the stream */
  def length: Int = total

  def channels: Int = oggInfo.channels

  def sampleRate: Int = oggInfo.rate

  /** Initialise the streams and thread involved in the streaming of OGG data */
  private def init(): Unit = {
    initVorbis()
    readPCM()
  }

  override def available(): Int = if (endOfStream) 0 else 1

  /** Initialise the vorbis decoding */
  private def initVorbis(): Unit =
    syncState.init()

  /** Get a page and packet from that page
    *
    * @return
    *   true if there was a page available
    */
  private def getPageAndPacket(): Boolean = boundary {
    // grab some data at the head of the stream. We want the first page
    // (which is guaranteed to be small and only contain the Vorbis
    // stream initial header) We need the first page to get the stream
    // serialno.

    // submit a 4k block to libvorbis' Ogg layer
    val index = syncState.buffer(BufferSize)
    if (index == -1) break(false)

    buffer = syncState.data
    if (buffer == null) {
      endOfStream = true
      break(false)
    }

    try
      bytes = input.read(buffer, index, BufferSize)
    catch {
      case e: Exception => throw SgeError.InvalidInput("Failure reading Vorbis.", Some(e))
    }
    syncState.wrote(bytes)

    // Get the first page.
    if (syncState.pageout(page) != 1) {
      // have we simply run out of data? If so, we're done.
      if (bytes < BufferSize) break(false)

      // error case. Must not be Vorbis data
      throw SgeError.InvalidInput("Input does not appear to be an Ogg bitstream.")
    }

    // Get the serial number and set up the rest of decode.
    // serialno first; use it to set up a logical stream
    streamState.init(page.serialno())

    // extract the initial header from the first page and verify that the
    // Ogg bitstream is in fact Vorbis data

    // I handle the initial header first instead of just having the code
    // read all three Vorbis headers at once because reading the initial
    // header is an easy way to identify a Vorbis bitstream and it's
    // useful to see that functionality separated out.

    oggInfo.init()
    comment.init()
    if (streamState.pagein(page) < 0) {
      // error; stream version mismatch perhaps
      throw SgeError.InvalidInput("Error reading first page of Ogg bitstream.")
    }

    if (streamState.packetout(packet) != 1) {
      // no page? must not be vorbis
      throw SgeError.InvalidInput("Error reading initial header packet.")
    }

    if (oggInfo.synthesis_headerin(comment, packet) < 0) {
      // error case; not a vorbis header
      throw SgeError.InvalidInput("Ogg bitstream does not contain Vorbis audio data.")
    }

    // At this point, we're sure we're Vorbis. We've set up the logical
    // (Ogg) bitstream decoder. Get the comment and codebook headers and
    // set up the Vorbis decoder

    // The next two packets in order are the comment and codebook headers.
    // They're likely large and may span multiple pages. Thus we read
    // and submit data until we get our two packets, watching that no
    // pages are missing. If a page is missing, error out; losing a
    // header page is the only place where missing data is fatal. */

    var i = 0
    while (i < 2) {
      var innerDone = false
      while (i < 2 && !innerDone) {
        val result = syncState.pageout(page)
        if (result == 0) {
          innerDone = true // Need more data
        }
        // Don't complain about missing or corrupt data yet.
        // We'll catch it at the packet output phase

        if (!innerDone && result == 1) {
          streamState.pagein(page) // we can ignore any errors here
          // as they'll also become apparent at packetout
          var packetDone = false
          while (i < 2 && !packetDone) {
            val packetResult = streamState.packetout(packet)
            if (packetResult == 0) {
              packetDone = true
            } else if (packetResult == -1) {
              // Uh oh; data at some point was corrupted or missing!
              // We can't tolerate that in a header. Die.
              throw SgeError.InvalidInput("Corrupt secondary header.")
            } else {
              oggInfo.synthesis_headerin(comment, packet)
              i += 1
            }
          }
        }
      }
      // no harm in not checking before adding more
      val bufIndex = syncState.buffer(BufferSize)
      if (bufIndex == -1) break(false)
      buffer = syncState.data
      try
        bytes = input.read(buffer, bufIndex, BufferSize)
      catch {
        case e: Exception => throw SgeError.InvalidInput("Failed to read Vorbis.", Some(e))
      }
      if (bytes == 0 && i < 2) {
        throw SgeError.InvalidInput("End of file before finding all Vorbis headers.")
      }
      syncState.wrote(bytes)
    }

    convsize = BufferSize / oggInfo.channels

    // OK, got and parsed all three headers. Initialize the Vorbis packet->PCM decoder.
    dspState.synthesis_init(oggInfo) // central decode state
    vorbisBlock.init(dspState) // local state for most of the decode
    // so multiple block decodes can proceed in parallel.
    // We could init multiple vorbis_block structures for vd here

    true
  }

  /** Decode the OGG file as shown in the jogg/jorbis examples */
  private def readPCM(): Unit = {
    var wrote = false

    var chainDone = false
    while (!chainDone) { // we repeat if the bitstream is chained
      if (endOfBitStream) {
        if (!getPageAndPacket()) {
          chainDone = true
        } else {
          endOfBitStream = false
        }
      }

      if (!chainDone) {
        if (!inited) {
          inited = true
          chainDone = true // early exit (was return in original)
        }

        if (!chainDone) {
          val _pcm   = new Array[Array[Array[Float]]](1)
          val _index = new Array[Int](oggInfo.channels)
          // The rest is just a straight decode loop until end of stream
          var outerDone = false
          while (!endOfBitStream && !outerDone) {
            var innerDone = false
            while (!endOfBitStream && !innerDone) {
              val result = syncState.pageout(page)

              if (result == 0) {
                innerDone = true // need more data
              } else if (result == -1) { // missing or corrupt data at this page position
                scribe.error("Error reading OGG: Corrupt or missing data in bitstream.")
              } else {
                streamState.pagein(page) // can safely ignore errors at this point
                var packetsDone = false
                while (!packetsDone) {
                  val packetResult = streamState.packetout(packet)

                  if (packetResult == 0) {
                    packetsDone = true // need more data
                  } else if (packetResult != -1) {
                    // we have a packet. Decode it
                    if (vorbisBlock.synthesis(packet) == 0) { // test for success!
                      dspState.synthesis_blockin(vorbisBlock)
                    }

                    // **pcm is a multichannel float vector. In stereo, for
                    // example, pcm[0] is left, and pcm[1] is right. samples is
                    // the size of each channel. Convert the float values
                    // (-1.<=range<=1.) to whatever PCM format and write it out

                    var samples = dspState.synthesis_pcmout(_pcm, _index)
                    while (samples > 0) {
                      val pcm = _pcm(0)
                      // boolean clipflag = false;
                      val bout = if (samples < convsize) samples else convsize

                      // convert floats to 16 bit signed ints (host order) and interleave
                      var ch = 0
                      while (ch < oggInfo.channels) {
                        var ptr  = ch * 2
                        val mono = _index(ch)
                        var j    = 0
                        while (j < bout) {
                          var value = (pcm(ch)(mono + j) * 32767.0).toInt
                          // might as well guard against clipping
                          if (value > 32767) value = 32767
                          if (value < -32768) value = -32768
                          if (value < 0) value = value | 0x8000

                          if (bigEndian) {
                            convbuffer(ptr) = (value >>> 8).toByte
                            convbuffer(ptr + 1) = value.toByte
                          } else {
                            convbuffer(ptr) = value.toByte
                            convbuffer(ptr + 1) = (value >>> 8).toByte
                          }
                          ptr += 2 * oggInfo.channels
                          j += 1
                        }
                        ch += 1
                      }

                      val bytesToWrite = 2 * oggInfo.channels * bout
                      if (bytesToWrite > pcmBuffer.remaining()) {
                        throw SgeError.InvalidInput(
                          "Ogg block too big to be buffered: " + bytesToWrite + " :: " + pcmBuffer.remaining()
                        )
                      } else {
                        pcmBuffer.put(convbuffer, 0, bytesToWrite)
                      }

                      wrote = true
                      dspState.synthesis_read(bout) // tell libvorbis how many samples we actually consumed
                      samples = dspState.synthesis_pcmout(_pcm, _index)
                    }
                  }
                }
                if (page.eos() != 0) {
                  endOfBitStream = true
                }

                if (!endOfBitStream && wrote) {
                  outerDone = true
                  innerDone = true
                }
              }
            }

            if (!outerDone && !endOfBitStream) {
              bytes = 0
              val bufIndex = syncState.buffer(BufferSize)
              if (bufIndex >= 0) {
                buffer = syncState.data
                try
                  bytes = input.read(buffer, bufIndex, BufferSize)
                catch {
                  case e: Exception => throw SgeError.InvalidInput("Error during Vorbis decoding.", Some(e))
                }
              } else {
                bytes = 0
              }
              syncState.wrote(bytes)
              if (bytes == 0) {
                endOfBitStream = true
              }
            }
          }

          if (!outerDone) {
            // clean up this logical bitstream; before exit we see if we're
            // followed by another [chained]
            streamState.clear()

            // ogg_page and ogg_packet structs always point to storage in
            // libvorbis. They're never freed or manipulated directly

            vorbisBlock.clear()
            dspState.clear()
            oggInfo.clear() // must be called last
          } else {
            chainDone = true
          }
        }
      }
    }

    if (!inited) {
      // OK, clean up the framer
      syncState.clear()
      endOfStream = true
    }
  }

  override def read(): Int = {
    if (readIndex >= pcmBuffer.position()) {
      pcmBuffer.clear()
      readPCM()
      readIndex = 0
    }
    if (readIndex >= pcmBuffer.position()) {
      -1
    } else {
      var value = pcmBuffer.get(readIndex)
      if (value < 0) value = (256 + value).toByte
      readIndex += 1
      value.toInt & 0xff
    }
  }

  def atEnd: Boolean = endOfStream && readIndex >= pcmBuffer.position()

  override def read(b: Array[Byte], off: Int, len: Int): Int = boundary {
    var i = 0
    while (i < len) {
      val value = read()
      if (value >= 0) {
        b(off + i) = value.toByte
      } else {
        if (i == 0) break(-1) else break(i)
      }
      i += 1
    }
    len
  }

  override def read(b: Array[Byte]): Int = read(b, 0, b.length)

  override def close(): Unit =
    StreamUtils.closeQuietly(input)
}

object OggInputStream {
  private val BufferSize = 512
}
