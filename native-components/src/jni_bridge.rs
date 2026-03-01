// JNI bridge — exports for JVM via System.loadLibrary("sge_native_ops")
//
// Maps Java native method signatures to Rust etc1/buffer_ops functions.
// Only compiled with --features jvm.
//
// Java bridge classes:
//   sge.platform.ETC1Bridge      — ETC1 texture compression codec
//   sge.platform.BufferOpsBridge — Memory copy, vertex transforms, vertex find

use jni::JNIEnv;
use jni::objects::{JByteArray, JByteBuffer, JClass, JFloatArray};
use jni::sys::*;

// ─── Helper: i8 <-> u8 slice reinterpretation ─────────────────────────
//
// JNI byte arrays use i8 (jbyte) while Rust functions use u8.
// These helpers perform zero-cost reinterpretation of slices.

#[inline]
fn i8_slice_as_u8(s: &[i8]) -> &[u8] {
    // SAFETY: i8 and u8 have identical size, alignment, and valid bit patterns.
    unsafe { std::slice::from_raw_parts(s.as_ptr() as *const u8, s.len()) }
}

#[inline]
fn i8_slice_as_u8_mut(s: &mut [i8]) -> &mut [u8] {
    // SAFETY: i8 and u8 have identical size, alignment, and valid bit patterns.
    unsafe { std::slice::from_raw_parts_mut(s.as_mut_ptr() as *mut u8, s.len()) }
}

// ═══════════════════════════════════════════════════════════════════════
// ETC1 Bridge
// ═══════════════════════════════════════════════════════════════════════

/// Java: `static native int getCompressedDataSize(int width, int height)`
#[no_mangle]
pub extern "system" fn Java_sge_platform_ETC1Bridge_getCompressedDataSize<'local>(
    _env: JNIEnv<'local>,
    _class: JClass<'local>,
    width: jint,
    height: jint,
) -> jint {
    crate::etc1::get_encoded_data_size(width as u32, height as u32) as jint
}

/// Java: `static native void formatHeader(byte[] header, int offset, int width, int height)`
#[no_mangle]
pub extern "system" fn Java_sge_platform_ETC1Bridge_formatHeader<'local>(
    mut env: JNIEnv<'local>,
    _class: JClass<'local>,
    header: JByteArray<'local>,
    offset: jint,
    width: jint,
    height: jint,
) {
    let len = env.get_array_length(&header).unwrap_or(0) as usize;
    let mut buf = vec![0i8; len];
    let _ = env.get_byte_array_region(&header, 0, &mut buf);

    let u8_buf = i8_slice_as_u8_mut(&mut buf);
    crate::etc1::pkm_format_header(
        &mut u8_buf[offset as usize..],
        width as u32,
        height as u32,
    );

    let _ = env.set_byte_array_region(&header, 0, &buf);
}

/// Java: `static native int getWidthPKM(byte[] header, int offset)`
#[no_mangle]
pub extern "system" fn Java_sge_platform_ETC1Bridge_getWidthPKM<'local>(
    mut env: JNIEnv<'local>,
    _class: JClass<'local>,
    header: JByteArray<'local>,
    offset: jint,
) -> jint {
    let len = env.get_array_length(&header).unwrap_or(0) as usize;
    let mut buf = vec![0i8; len];
    let _ = env.get_byte_array_region(&header, 0, &mut buf);

    crate::etc1::pkm_get_width(&i8_slice_as_u8(&buf)[offset as usize..]) as jint
}

/// Java: `static native int getHeightPKM(byte[] header, int offset)`
#[no_mangle]
pub extern "system" fn Java_sge_platform_ETC1Bridge_getHeightPKM<'local>(
    mut env: JNIEnv<'local>,
    _class: JClass<'local>,
    header: JByteArray<'local>,
    offset: jint,
) -> jint {
    let len = env.get_array_length(&header).unwrap_or(0) as usize;
    let mut buf = vec![0i8; len];
    let _ = env.get_byte_array_region(&header, 0, &mut buf);

    crate::etc1::pkm_get_height(&i8_slice_as_u8(&buf)[offset as usize..]) as jint
}

/// Java: `static native boolean isValidPKM(byte[] header, int offset)`
#[no_mangle]
pub extern "system" fn Java_sge_platform_ETC1Bridge_isValidPKM<'local>(
    mut env: JNIEnv<'local>,
    _class: JClass<'local>,
    header: JByteArray<'local>,
    offset: jint,
) -> jboolean {
    let len = env.get_array_length(&header).unwrap_or(0) as usize;
    let mut buf = vec![0i8; len];
    let _ = env.get_byte_array_region(&header, 0, &mut buf);

    if crate::etc1::pkm_is_valid(&i8_slice_as_u8(&buf)[offset as usize..]) {
        JNI_TRUE
    } else {
        JNI_FALSE
    }
}

/// Java: `static native void decodeImage(byte[] compressedData, int compressedOffset,
///     byte[] decodedData, int decodedOffset, int width, int height, int pixelSize)`
#[no_mangle]
pub extern "system" fn Java_sge_platform_ETC1Bridge_decodeImage<'local>(
    mut env: JNIEnv<'local>,
    _class: JClass<'local>,
    compressed_data: JByteArray<'local>,
    compressed_offset: jint,
    decoded_data: JByteArray<'local>,
    decoded_offset: jint,
    width: jint,
    height: jint,
    pixel_size: jint,
) {
    let comp_len = env.get_array_length(&compressed_data).unwrap_or(0) as usize;
    let mut comp_buf = vec![0i8; comp_len];
    let _ = env.get_byte_array_region(&compressed_data, 0, &mut comp_buf);

    let dec_len = env.get_array_length(&decoded_data).unwrap_or(0) as usize;
    let mut dec_buf = vec![0i8; dec_len];
    let _ = env.get_byte_array_region(&decoded_data, 0, &mut dec_buf);

    let w = width as u32;
    let h = height as u32;
    let ps = pixel_size as u32;
    let stride = w * ps;

    let comp_u8 = i8_slice_as_u8(&comp_buf);
    let dec_u8 = i8_slice_as_u8_mut(&mut dec_buf);

    crate::etc1::decode_image(
        &comp_u8[compressed_offset as usize..],
        &mut dec_u8[decoded_offset as usize..],
        w,
        h,
        ps,
        stride,
    );

    let _ = env.set_byte_array_region(&decoded_data, 0, &dec_buf);
}

/// Java: `static native byte[] encodeImage(byte[] imageData, int offset,
///     int width, int height, int pixelSize)`
#[no_mangle]
pub extern "system" fn Java_sge_platform_ETC1Bridge_encodeImage<'local>(
    mut env: JNIEnv<'local>,
    _class: JClass<'local>,
    image_data: JByteArray<'local>,
    offset: jint,
    width: jint,
    height: jint,
    pixel_size: jint,
) -> jbyteArray {
    let len = env.get_array_length(&image_data).unwrap_or(0) as usize;
    let mut input = vec![0i8; len];
    let _ = env.get_byte_array_region(&image_data, 0, &mut input);

    let w = width as u32;
    let h = height as u32;
    let ps = pixel_size as u32;
    let stride = w * ps;
    let out_size = crate::etc1::get_encoded_data_size(w, h) as usize;
    let mut output = vec![0u8; out_size];

    let input_u8 = i8_slice_as_u8(&input);
    crate::etc1::encode_image(
        &input_u8[offset as usize..],
        w,
        h,
        ps,
        stride,
        &mut output,
    );

    let result = env.new_byte_array(output.len() as jsize).unwrap();
    let output_i8 =
        unsafe { std::slice::from_raw_parts(output.as_ptr() as *const i8, output.len()) };
    let _ = env.set_byte_array_region(&result, 0, output_i8);
    result.into_raw()
}

/// Java: `static native byte[] encodeImagePKM(byte[] imageData, int offset,
///     int width, int height, int pixelSize)`
#[no_mangle]
pub extern "system" fn Java_sge_platform_ETC1Bridge_encodeImagePKM<'local>(
    mut env: JNIEnv<'local>,
    _class: JClass<'local>,
    image_data: JByteArray<'local>,
    offset: jint,
    width: jint,
    height: jint,
    pixel_size: jint,
) -> jbyteArray {
    let len = env.get_array_length(&image_data).unwrap_or(0) as usize;
    let mut input = vec![0i8; len];
    let _ = env.get_byte_array_region(&image_data, 0, &mut input);

    let w = width as u32;
    let h = height as u32;
    let ps = pixel_size as u32;
    let stride = w * ps;
    let compressed_size = crate::etc1::get_encoded_data_size(w, h) as usize;
    let header_size = crate::etc1::ETC_PKM_HEADER_SIZE;
    let total_size = header_size + compressed_size;
    let mut output = vec![0u8; total_size];

    // Write PKM header
    crate::etc1::pkm_format_header(&mut output[..header_size], w, h);

    // Encode image data after header
    let input_u8 = i8_slice_as_u8(&input);
    crate::etc1::encode_image(
        &input_u8[offset as usize..],
        w,
        h,
        ps,
        stride,
        &mut output[header_size..],
    );

    let result = env.new_byte_array(total_size as jsize).unwrap();
    let output_i8 =
        unsafe { std::slice::from_raw_parts(output.as_ptr() as *const i8, total_size) };
    let _ = env.set_byte_array_region(&result, 0, output_i8);
    result.into_raw()
}

// ═══════════════════════════════════════════════════════════════════════
// BufferOps Bridge
// ═══════════════════════════════════════════════════════════════════════

/// Java: `static native void copyBytes(byte[] src, int srcOffset, byte[] dst,
///     int dstOffset, int numBytes)`
#[no_mangle]
pub extern "system" fn Java_sge_platform_BufferOpsBridge_copyBytes<'local>(
    mut env: JNIEnv<'local>,
    _class: JClass<'local>,
    src: JByteArray<'local>,
    src_offset: jint,
    dst: JByteArray<'local>,
    dst_offset: jint,
    num_bytes: jint,
) {
    let src_len = env.get_array_length(&src).unwrap_or(0) as usize;
    let mut src_buf = vec![0i8; src_len];
    let _ = env.get_byte_array_region(&src, 0, &mut src_buf);

    let dst_len = env.get_array_length(&dst).unwrap_or(0) as usize;
    let mut dst_buf = vec![0i8; dst_len];
    let _ = env.get_byte_array_region(&dst, 0, &mut dst_buf);

    crate::buffer_ops::copy_bytes(
        i8_slice_as_u8(&src_buf),
        src_offset as usize,
        i8_slice_as_u8_mut(&mut dst_buf),
        dst_offset as usize,
        num_bytes as usize,
    );

    let _ = env.set_byte_array_region(&dst, 0, &dst_buf);
}

/// Java: `static native void copyFloats(float[] src, int srcOffset, float[] dst,
///     int dstOffset, int numFloats)`
#[no_mangle]
pub extern "system" fn Java_sge_platform_BufferOpsBridge_copyFloats<'local>(
    mut env: JNIEnv<'local>,
    _class: JClass<'local>,
    src: JFloatArray<'local>,
    src_offset: jint,
    dst: JFloatArray<'local>,
    dst_offset: jint,
    num_floats: jint,
) {
    let src_len = env.get_array_length(&src).unwrap_or(0) as usize;
    let mut src_buf = vec![0.0f32; src_len];
    let _ = env.get_float_array_region(&src, 0, &mut src_buf);

    let dst_len = env.get_array_length(&dst).unwrap_or(0) as usize;
    let mut dst_buf = vec![0.0f32; dst_len];
    let _ = env.get_float_array_region(&dst, 0, &mut dst_buf);

    // Direct slice copy — no Rust function needed for float-to-float copy
    let so = src_offset as usize;
    let do_ = dst_offset as usize;
    let n = num_floats as usize;
    dst_buf[do_..do_ + n].copy_from_slice(&src_buf[so..so + n]);

    let _ = env.set_float_array_region(&dst, 0, &dst_buf);
}

/// Java: `static native void transformV4M4(float[] data, int stride, int count,
///     float[] matrix, int offset)`
#[no_mangle]
pub extern "system" fn Java_sge_platform_BufferOpsBridge_transformV4M4<'local>(
    mut env: JNIEnv<'local>,
    _class: JClass<'local>,
    data: JFloatArray<'local>,
    stride: jint,
    count: jint,
    matrix: JFloatArray<'local>,
    offset: jint,
) {
    let data_len = env.get_array_length(&data).unwrap_or(0) as usize;
    let mut data_buf = vec![0.0f32; data_len];
    let _ = env.get_float_array_region(&data, 0, &mut data_buf);

    let mut mat_buf = [0.0f32; 16];
    let _ = env.get_float_array_region(&matrix, 0, &mut mat_buf);

    crate::buffer_ops::transform_v4m4(
        &mut data_buf,
        stride as usize,
        count as usize,
        &mat_buf,
        offset as usize,
    );

    let _ = env.set_float_array_region(&data, 0, &data_buf);
}

/// Java: `static native void transformV3M4(float[] data, int stride, int count,
///     float[] matrix, int offset)`
#[no_mangle]
pub extern "system" fn Java_sge_platform_BufferOpsBridge_transformV3M4<'local>(
    mut env: JNIEnv<'local>,
    _class: JClass<'local>,
    data: JFloatArray<'local>,
    stride: jint,
    count: jint,
    matrix: JFloatArray<'local>,
    offset: jint,
) {
    let data_len = env.get_array_length(&data).unwrap_or(0) as usize;
    let mut data_buf = vec![0.0f32; data_len];
    let _ = env.get_float_array_region(&data, 0, &mut data_buf);

    let mut mat_buf = [0.0f32; 16];
    let _ = env.get_float_array_region(&matrix, 0, &mut mat_buf);

    crate::buffer_ops::transform_v3m4(
        &mut data_buf,
        stride as usize,
        count as usize,
        &mat_buf,
        offset as usize,
    );

    let _ = env.set_float_array_region(&data, 0, &data_buf);
}

/// Java: `static native void transformV2M4(float[] data, int stride, int count,
///     float[] matrix, int offset)`
#[no_mangle]
pub extern "system" fn Java_sge_platform_BufferOpsBridge_transformV2M4<'local>(
    mut env: JNIEnv<'local>,
    _class: JClass<'local>,
    data: JFloatArray<'local>,
    stride: jint,
    count: jint,
    matrix: JFloatArray<'local>,
    offset: jint,
) {
    let data_len = env.get_array_length(&data).unwrap_or(0) as usize;
    let mut data_buf = vec![0.0f32; data_len];
    let _ = env.get_float_array_region(&data, 0, &mut data_buf);

    let mut mat_buf = [0.0f32; 16];
    let _ = env.get_float_array_region(&matrix, 0, &mut mat_buf);

    crate::buffer_ops::transform_v2m4(
        &mut data_buf,
        stride as usize,
        count as usize,
        &mat_buf,
        offset as usize,
    );

    let _ = env.set_float_array_region(&data, 0, &data_buf);
}

/// Java: `static native void transformV3M3(float[] data, int stride, int count,
///     float[] matrix, int offset)`
#[no_mangle]
pub extern "system" fn Java_sge_platform_BufferOpsBridge_transformV3M3<'local>(
    mut env: JNIEnv<'local>,
    _class: JClass<'local>,
    data: JFloatArray<'local>,
    stride: jint,
    count: jint,
    matrix: JFloatArray<'local>,
    offset: jint,
) {
    let data_len = env.get_array_length(&data).unwrap_or(0) as usize;
    let mut data_buf = vec![0.0f32; data_len];
    let _ = env.get_float_array_region(&data, 0, &mut data_buf);

    let mut mat_buf = [0.0f32; 9];
    let _ = env.get_float_array_region(&matrix, 0, &mut mat_buf);

    crate::buffer_ops::transform_v3m3(
        &mut data_buf,
        stride as usize,
        count as usize,
        &mat_buf,
        offset as usize,
    );

    let _ = env.set_float_array_region(&data, 0, &data_buf);
}

/// Java: `static native void transformV2M3(float[] data, int stride, int count,
///     float[] matrix, int offset)`
#[no_mangle]
pub extern "system" fn Java_sge_platform_BufferOpsBridge_transformV2M3<'local>(
    mut env: JNIEnv<'local>,
    _class: JClass<'local>,
    data: JFloatArray<'local>,
    stride: jint,
    count: jint,
    matrix: JFloatArray<'local>,
    offset: jint,
) {
    let data_len = env.get_array_length(&data).unwrap_or(0) as usize;
    let mut data_buf = vec![0.0f32; data_len];
    let _ = env.get_float_array_region(&data, 0, &mut data_buf);

    let mut mat_buf = [0.0f32; 9];
    let _ = env.get_float_array_region(&matrix, 0, &mut mat_buf);

    crate::buffer_ops::transform_v2m3(
        &mut data_buf,
        stride as usize,
        count as usize,
        &mat_buf,
        offset as usize,
    );

    let _ = env.set_float_array_region(&data, 0, &data_buf);
}

/// Java: `static native long find(float[] vertex, int vertexOffset, int stride,
///     float[] vertices, int verticesOffset, int numVertices)`
#[no_mangle]
pub extern "system" fn Java_sge_platform_BufferOpsBridge_find<'local>(
    mut env: JNIEnv<'local>,
    _class: JClass<'local>,
    vertex: JFloatArray<'local>,
    vertex_offset: jint,
    stride: jint,
    vertices: JFloatArray<'local>,
    vertices_offset: jint,
    num_vertices: jint,
) -> jlong {
    let v_len = env.get_array_length(&vertex).unwrap_or(0) as usize;
    let mut v_buf = vec![0.0f32; v_len];
    let _ = env.get_float_array_region(&vertex, 0, &mut v_buf);

    let vs_len = env.get_array_length(&vertices).unwrap_or(0) as usize;
    let mut vs_buf = vec![0.0f32; vs_len];
    let _ = env.get_float_array_region(&vertices, 0, &mut vs_buf);

    crate::buffer_ops::find_vertex(
        &v_buf[vertex_offset as usize..],
        stride as usize,
        &vs_buf[vertices_offset as usize..],
        num_vertices as usize,
    ) as jlong
}

/// Java: `static native long findEpsilon(float[] vertex, int vertexOffset, int stride,
///     float[] vertices, int verticesOffset, int numVertices, float epsilon)`
#[no_mangle]
pub extern "system" fn Java_sge_platform_BufferOpsBridge_findEpsilon<'local>(
    mut env: JNIEnv<'local>,
    _class: JClass<'local>,
    vertex: JFloatArray<'local>,
    vertex_offset: jint,
    stride: jint,
    vertices: JFloatArray<'local>,
    vertices_offset: jint,
    num_vertices: jint,
    epsilon: jfloat,
) -> jlong {
    let v_len = env.get_array_length(&vertex).unwrap_or(0) as usize;
    let mut v_buf = vec![0.0f32; v_len];
    let _ = env.get_float_array_region(&vertex, 0, &mut v_buf);

    let vs_len = env.get_array_length(&vertices).unwrap_or(0) as usize;
    let mut vs_buf = vec![0.0f32; vs_len];
    let _ = env.get_float_array_region(&vertices, 0, &mut vs_buf);

    crate::buffer_ops::find_vertex_epsilon(
        &v_buf[vertex_offset as usize..],
        stride as usize,
        &vs_buf[vertices_offset as usize..],
        num_vertices as usize,
        epsilon,
    ) as jlong
}

// ═══════════════════════════════════════════════════════════════════════
// Memory Management (JVM only — uses libc malloc/free)
// ═══════════════════════════════════════════════════════════════════════

/// Java: `static native void freeMemory(java.nio.ByteBuffer buffer)`
#[no_mangle]
pub extern "system" fn Java_sge_platform_BufferOpsBridge_freeMemory<'local>(
    env: JNIEnv<'local>,
    _class: JClass<'local>,
    buffer: JByteBuffer<'local>,
) {
    if let Ok(ptr) = env.get_direct_buffer_address(&buffer) {
        unsafe { libc::free(ptr as *mut libc::c_void) };
    }
}

/// Java: `static native java.nio.ByteBuffer newDisposableByteBuffer(int numBytes)`
#[no_mangle]
pub extern "system" fn Java_sge_platform_BufferOpsBridge_newDisposableByteBuffer<'local>(
    mut env: JNIEnv<'local>,
    _class: JClass<'local>,
    num_bytes: jint,
) -> jobject {
    unsafe {
        let ptr = libc::malloc(num_bytes as libc::size_t);
        if ptr.is_null() {
            return std::ptr::null_mut();
        }
        libc::memset(ptr, 0, num_bytes as libc::size_t);
        match env.new_direct_byte_buffer(ptr as *mut u8, num_bytes as usize) {
            Ok(buf) => buf.into_raw(),
            Err(_) => {
                libc::free(ptr);
                std::ptr::null_mut()
            }
        }
    }
}

/// Java: `static native long getBufferAddress(java.nio.Buffer buffer)`
#[no_mangle]
pub extern "system" fn Java_sge_platform_BufferOpsBridge_getBufferAddress<'local>(
    env: JNIEnv<'local>,
    _class: JClass<'local>,
    buffer: JByteBuffer<'local>,
) -> jlong {
    match env.get_direct_buffer_address(&buffer) {
        Ok(ptr) => ptr as jlong,
        Err(_) => 0,
    }
}

/// Java: `static native void clear(java.nio.ByteBuffer buffer, int numBytes)`
#[no_mangle]
pub extern "system" fn Java_sge_platform_BufferOpsBridge_clear<'local>(
    env: JNIEnv<'local>,
    _class: JClass<'local>,
    buffer: JByteBuffer<'local>,
    num_bytes: jint,
) {
    if let Ok(ptr) = env.get_direct_buffer_address(&buffer) {
        unsafe { std::ptr::write_bytes(ptr, 0, num_bytes as usize) };
    }
}
