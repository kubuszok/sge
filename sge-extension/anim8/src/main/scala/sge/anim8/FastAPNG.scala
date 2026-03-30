/*
 * Ported from anim8-gdx - https://github.com/tommyettinger/anim8-gdx
 * Original authors: Matthias Mann, Nathan Sweet, Tommy Ettinger
 * Licensed under the Apache License, Version 2.0
 *
 * Full-color animated PNG encoder with compression.
 * This is purely here for compatibility; FastAPNG is identical to AnimatedPNG.
 *
 * Copyright (c) 2007 Matthias Mann - www.matthiasmann.de
 * Copyright (c) 2014 Nathan Sweet
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package anim8

/** Full-color animated PNG encoder with compression. This is purely here for compatibility; FastAPNG is identical to [[AnimatedPNG]].
  *
  * @see
  *   [[AnimatedPNG]] the recommended variant on this class; identical in code
  * @author
  *   Matthias Mann
  * @author
  *   Nathan Sweet
  * @author
  *   Tommy Ettinger
  * @deprecated
  *   Use [[AnimatedPNG]] instead.
  */
@deprecated("Use AnimatedPNG instead", "anim8")
class FastAPNG(initialBufferSize: Int) extends AnimatedPNG(initialBufferSize) {

  /** Creates an AnimatedPNG writer with an initial buffer size of 1024. The buffer can resize later if needed. */
  def this() = this(1024)
}
