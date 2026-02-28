/*******************************************************************************
 * Copyright 2011 See AUTHORS file.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/

package com.badlogic.gdx.graphics.glutils;

import java.nio.ByteBuffer;

// Stripped to native method declarations only. All logic lives in the Scala wrapper (sge.graphics.glutils.ETC1).

/** Class for encoding and decoding ETC1 compressed images. Also provides methods to add a PKM header.
 * @author mzechner */
public class ETC1 {

	// @off
	/*JNI
	#include <etc1/etc1_utils.h>
	#include <stdlib.h>
	 */

	/** @param width the width in pixels
	 * @param height the height in pixels
	 * @return the number of bytes needed to store the compressed data */
	public static native int getCompressedDataSize (int width, int height); /*
		return etc1_get_encoded_data_size(width, height);
	*/

	/** Writes a PKM header to the {@link ByteBuffer}. Does not modify the position or limit of the ByteBuffer.
	 * @param header the direct native order {@link ByteBuffer}
	 * @param offset the offset to the header in bytes
	 * @param width the width in pixels
	 * @param height the height in pixels */
	public static native void formatHeader (ByteBuffer header, int offset, int width, int height); /*
		etc1_pkm_format_header((etc1_byte*)header + offset, width, height);
	*/

	/** @param header direct native order {@link ByteBuffer} holding the PKM header
	 * @param offset the offset in bytes to the PKM header from the ByteBuffer's start
	 * @return the width stored in the PKM header */
	public static native int getWidthPKM (ByteBuffer header, int offset); /*
		return etc1_pkm_get_width((etc1_byte*)header + offset);
	*/

	/** @param header direct native order {@link ByteBuffer} holding the PKM header
	 * @param offset the offset in bytes to the PKM header from the ByteBuffer's start
	 * @return the height stored in the PKM header */
	public static native int getHeightPKM (ByteBuffer header, int offset); /*
		return etc1_pkm_get_height((etc1_byte*)header + offset);
	*/

	/** @param header direct native order {@link ByteBuffer} holding the PKM header
	 * @param offset the offset in bytes to the PKM header from the ByteBuffer's start
	 * @return whether the PKM header is valid */
	public static native boolean isValidPKM (ByteBuffer header, int offset); /*
		return etc1_pkm_is_valid((etc1_byte*)header + offset) != 0?true:false;
	*/

	/** Decodes the compressed image data to RGB565 or RGB888 pixel data. Does not modify the position or limit of the
	 * {@link ByteBuffer} instances.
	 * @param compressedData the compressed image data in a direct native order {@link ByteBuffer}
	 * @param offset the offset in bytes to the image data from the start of the buffer
	 * @param decodedData the decoded data in a direct native order ByteBuffer, must hold width * height * pixelSize bytes.
	 * @param offsetDec the offset in bytes to the decoded image data.
	 * @param width the width in pixels
	 * @param height the height in pixels
	 * @param pixelSize the pixel size, either 2 (RBG565) or 3 (RGB888) */
	public static native void decodeImage (ByteBuffer compressedData, int offset, ByteBuffer decodedData, int offsetDec,
		int width, int height, int pixelSize); /*
		etc1_decode_image((etc1_byte*)compressedData + offset, (etc1_byte*)decodedData + offsetDec, width, height, pixelSize, width * pixelSize);
	*/

	/** Encodes the image data given as RGB565 or RGB888. Does not modify the position or limit of the {@link ByteBuffer}.
	 * @param imageData the image data in a direct native order {@link ByteBuffer}
	 * @param offset the offset in bytes to the image data from the start of the buffer
	 * @param width the width in pixels
	 * @param height the height in pixels
	 * @param pixelSize the pixel size, either 2 (RGB565) or 3 (RGB888)
	 * @return a new direct native order ByteBuffer containing the compressed image data */
	public static native ByteBuffer encodeImage (ByteBuffer imageData, int offset, int width, int height, int pixelSize); /*
		int compressedSize = etc1_get_encoded_data_size(width, height);
		etc1_byte* compressedData = (etc1_byte*)malloc(compressedSize);
		etc1_encode_image((etc1_byte*)imageData + offset, width, height, pixelSize, width * pixelSize, compressedData);
		return env->NewDirectByteBuffer(compressedData, compressedSize);
	*/

	/** Encodes the image data given as RGB565 or RGB888. Does not modify the position or limit of the {@link ByteBuffer}.
	 * @param imageData the image data in a direct native order {@link ByteBuffer}
	 * @param offset the offset in bytes to the image data from the start of the buffer
	 * @param width the width in pixels
	 * @param height the height in pixels
	 * @param pixelSize the pixel size, either 2 (RGB565) or 3 (RGB888)
	 * @return a new direct native order ByteBuffer containing the compressed image data */
	public static native ByteBuffer encodeImagePKM (ByteBuffer imageData, int offset, int width, int height, int pixelSize); /*
		int compressedSize = etc1_get_encoded_data_size(width, height);
		etc1_byte* compressed = (etc1_byte*)malloc(compressedSize + ETC_PKM_HEADER_SIZE);
		etc1_pkm_format_header(compressed, width, height);
		etc1_encode_image((etc1_byte*)imageData + offset, width, height, pixelSize, width * pixelSize, compressed + ETC_PKM_HEADER_SIZE);
		return env->NewDirectByteBuffer(compressed, compressedSize + ETC_PKM_HEADER_SIZE);
	*/
}
