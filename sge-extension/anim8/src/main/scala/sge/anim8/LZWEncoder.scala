/*
 * Ported from anim8-gdx - https://github.com/tommyettinger/anim8-gdx
 * Original authors: Tommy Ettinger
 * Licensed under the Apache License, Version 2.0
 *
 * LZW encoding specific to the GIF format.
 * Adapted from Jef Poskanzer's Java port by way of J. M. G. Elliott.
 * Created by K Weiner in December 2000
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 318
 * Covenant-baseline-methods: BITS,ClearCode,EOF,EOFCode,HSIZE,LZWEncoder,MAXCODE,a_count,accum,c,char_out,cl_block,cl_hash,clear_flg,codetab,compress,curPixel,cur_accum,cur_bits,disp,encode,ent,fcode,flush_char,free_ent,g_init_bits,hshift,hsize,hsize_reg,htab,i,initCodeSize,masks,maxbits,maxcode,maxmaxcode,n_bits,nextPixel,output,remaining
 * Covenant-source-reference: com/github/tommyettinger/anim8/LZWEncoder.java
 * Covenant-verified: 2026-04-19
 */
package sge
package anim8

import java.io.IOException
import java.io.OutputStream
import scala.util.boundary

/** LZW encoding specific to the GIF format. Adapted from Jef Poskanzer's Java port by way of J. M. G. Elliott.
  *
  * Created by K Weiner in December 2000
  */
private[anim8] class LZWEncoder(imgW: Int, imgH: Int, pixArray: Array[Byte], colorDepth: Int) {

  private val EOF = -1

  private val initCodeSize: Int = Math.max(2, colorDepth)

  private var remaining: Int = 0
  private var curPixel:  Int = 0

  // GIFCOMPR.C - GIF Image compression routines
  //
  // Lempel-Ziv compression based on 'compress'. GIF modifications by
  // David Rowley (mgardi@watdcsu.waterloo.edu)

  // General DEFINEs
  private val BITS  = 12
  private val HSIZE = 5003 // 80% occupancy

  // GIF Image compression - modified 'compress'
  //
  // Based on: compress.c - File compression ala IEEE Computer, June 1984.
  //
  // By Authors: Spencer W. Thomas (decvax!harpo!utah-cs!utah-gr!thomas)
  // Jim McKie (decvax!mcvax!jim)
  // Steve Davies (decvax!vax135!petsd!peora!srd)
  // Ken Turkowski (decvax!decwrl!turtlevax!ken)
  // James A. Woods (decvax!ihnp4!ames!jaw)
  // Joe Orost (decvax!vax135!petsd!joe)

  private var n_bits:     Int = 0 // number of bits/code
  private val maxbits:    Int = BITS // user settable max # bits/code
  private var maxcode:    Int = 0 // maximum code, given n_bits
  private val maxmaxcode: Int = 1 << BITS // should NEVER generate this code

  private val htab:    Array[Int] = new Array[Int](HSIZE)
  private val codetab: Array[Int] = new Array[Int](HSIZE)

  private val hsize:    Int = HSIZE // for dynamic table sizing
  private var free_ent: Int = 0 // first unused entry

  // block compression parameters -- after all codes are used up,
  // and compression rate changes, start over.
  private var clear_flg: Boolean = false

  // Algorithm: use open addressing double hashing (no chaining) on the
  // prefix code / next character combination. We do a variant of Knuth's
  // algorithm D (vol. 3, sec. 6.4) along with G. Knott's relatively-prime
  // secondary probe. Here, the modular division first probe is gives way
  // to a faster exclusive-or manipulation. Also do block compression with
  // an adaptive reset, whereby the code table is cleared when the compression
  // ratio decreases, but after the table fills. The variable-length output
  // codes are re-sized at this point, and a special CLEAR code is generated
  // for the decompressor. Late addition: construct the table according to
  // file size for noticeable speed improvement on small files. Please direct
  // questions about this implementation to ames!jaw.

  private var g_init_bits: Int = 0
  private var ClearCode:   Int = 0
  private var EOFCode:     Int = 0

  // output
  //
  // Output the given code.
  // Inputs:
  // code: A n_bits-bit integer. If == -1, then EOF. This assumes
  // that n_bits =< wordsize - 1.
  // Outputs:
  // Outputs code to the file.
  // Assumptions:
  // Chars are 8 bits long.
  // Algorithm:
  // Maintain a BITS character long buffer (so that 8 codes will
  // fit in it exactly). Use the VAX insv instruction to insert each
  // code in turn. When the buffer fills up empty it and start over.

  private var cur_accum: Int = 0
  private var cur_bits:  Int = 0

  private val masks: Array[Int] = Array(
    0x0000, 0x0001, 0x0003, 0x0007, 0x000f, 0x001f, 0x003f, 0x007f, 0x00ff, 0x01ff, 0x03ff, 0x07ff, 0x0fff, 0x1fff, 0x3fff, 0x7fff, 0xffff
  )

  // Number of characters so far in this 'packet'
  private var a_count: Int = 0

  // Define the storage for the packet accumulator
  private val accum: Array[Byte] = new Array[Byte](256)

  private def MAXCODE(n_bits: Int): Int = (1 << n_bits) - 1

  // Add a character to the end of the current packet, and if it is 254
  // characters, flush the packet to disk.
  @throws[IOException]
  private def char_out(c: Byte, outs: OutputStream): Unit = {
    accum(a_count) = c
    a_count += 1
    if (a_count >= 254) {
      flush_char(outs)
    }
  }

  // Clear out the hash table
  // table clear for block compress
  @throws[IOException]
  private def cl_block(outs: OutputStream): Unit = {
    cl_hash(hsize)
    free_ent = ClearCode + 2
    clear_flg = true
    output(ClearCode, outs)
  }

  // reset code table
  private def cl_hash(hsize: Int): Unit = {
    var i = 0
    while (i < hsize) {
      htab(i) = -1
      i += 1
    }
  }

  @throws[IOException]
  private def compress(init_bits: Int, outs: OutputStream): Unit = {
    var fcode:     Int = 0
    var i:         Int = 0
    var c:         Int = 0
    var ent:       Int = 0
    var disp:      Int = 0
    var hsize_reg: Int = 0
    var hshift:    Int = 0

    // Set up the globals: g_init_bits - initial number of bits
    g_init_bits = init_bits

    // Set up the necessary values
    clear_flg = false
    n_bits = g_init_bits
    maxcode = MAXCODE(n_bits)

    ClearCode = 1 << (init_bits - 1)
    EOFCode = ClearCode + 1
    free_ent = ClearCode + 2

    a_count = 0 // clear packet

    ent = nextPixel()

    hshift = 0
    fcode = hsize
    while (fcode < 65536) {
      hshift += 1
      fcode *= 2
    }
    hshift = 8 - hshift // set hash code range bound

    hsize_reg = hsize
    cl_hash(hsize_reg) // clear hash table

    output(ClearCode, outs)

    boundary {
      var done = false
      while (!done) {
        c = nextPixel()
        if (c == EOF) {
          done = true
        } else {
          fcode = (c << maxbits) + ent
          i = (c << hshift) ^ ent // xor hashing

          if (htab(i) == fcode) {
            ent = codetab(i)
          } else if (htab(i) >= 0) { // non-empty slot
            disp = hsize_reg - i // secondary hash (after G. Knott)
            if (i == 0) disp = 1
            var continueOuter = false
            boundary {
              var searching = true
              while (searching) {
                i -= disp
                if (i < 0) i += hsize_reg
                if (htab(i) == fcode) {
                  ent = codetab(i)
                  continueOuter = true
                  searching = false
                } else if (htab(i) < 0) {
                  searching = false
                }
              }
            }
            if (continueOuter) {
              // equivalent of continue outer_loop
            } else {
              output(ent, outs)
              ent = c
              if (free_ent < maxmaxcode) {
                codetab(i) = free_ent
                free_ent += 1
                htab(i) = fcode
              } else {
                cl_block(outs)
              }
            }
          } else {
            output(ent, outs)
            ent = c
            if (free_ent < maxmaxcode) {
              codetab(i) = free_ent
              free_ent += 1
              htab(i) = fcode
            } else {
              cl_block(outs)
            }
          }
        }
      }
    }
    // Put out the final code.
    output(ent, outs)
    output(EOFCode, outs)
  }

  @throws[IOException]
  def encode(os: OutputStream): Unit = {
    os.write(initCodeSize) // write "initial code size" byte
    remaining = imgW * imgH // reset navigation variables
    curPixel = 0
    compress(initCodeSize + 1, os) // compress and write the pixel data
    os.write(0) // write block terminator
  }

  // Flush the packet to disk, and reset the accumulator
  @throws[IOException]
  private def flush_char(outs: OutputStream): Unit =
    if (a_count > 0) {
      outs.write(a_count)
      outs.write(accum, 0, a_count)
      a_count = 0
    }

  // Return the next pixel from the image
  private def nextPixel(): Int =
    if (remaining == 0) {
      EOF
    } else {
      remaining -= 1
      val pix = pixArray(curPixel)
      curPixel += 1
      pix & 0xff
    }

  @throws[IOException]
  private def output(code: Int, outs: OutputStream): Unit = {
    cur_accum &= masks(cur_bits)

    if (cur_bits > 0) {
      cur_accum |= (code << cur_bits)
    } else {
      cur_accum = code
    }

    cur_bits += n_bits

    while (cur_bits >= 8) {
      char_out((cur_accum & 0xff).toByte, outs)
      cur_accum >>= 8
      cur_bits -= 8
    }

    // If the next entry is going to be too big for the code size,
    // then increase it, if possible.
    if (free_ent > maxcode || clear_flg) {
      if (clear_flg) {
        n_bits = g_init_bits
        maxcode = MAXCODE(n_bits)
        clear_flg = false
      } else {
        n_bits += 1
        if (n_bits == maxbits) {
          maxcode = maxmaxcode
        } else {
          maxcode = MAXCODE(n_bits)
        }
      }
    }

    if (code == EOFCode) {
      // At EOF, write the rest of the buffer.
      while (cur_bits > 0) {
        char_out((cur_accum & 0xff).toByte, outs)
        cur_accum >>= 8
        cur_bits -= 8
      }
      flush_char(outs)
    }
  }
}
