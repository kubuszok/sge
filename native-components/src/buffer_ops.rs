// Buffer operations — memory copy, vertex transforms, vertex find/compare
//
// Port of the C/C++ inline code from LibGDX BufferUtils.java (lines 612-708).
//
// Public safe Rust API (for JNI bridge and Scala.js reference):
//   copy_bytes,
//   transform_v4m4, transform_v3m4, transform_v2m4, transform_v3m3, transform_v2m3,
//   find_vertex, find_vertex_epsilon
//
// Public C ABI functions (for Scala Native @extern):
//   sge_copy_bytes,
//   sge_transform_v4m4, sge_transform_v3m4, sge_transform_v2m4,
//   sge_transform_v3m3, sge_transform_v2m3,
//   sge_find_vertex, sge_find_vertex_epsilon

// ---------------------------------------------------------------------------
// Copy operations
// ---------------------------------------------------------------------------

/// Copies `num_bytes` bytes from `src[src_offset..]` into `dst[dst_offset..]`.
///
/// Equivalent to the C `memcpy(dst + dstOffset, src + srcOffset, numBytes)`.
///
/// # Panics
///
/// Panics if either the source or destination range is out of bounds.
pub fn copy_bytes(
    src: &[u8],
    src_offset: usize,
    dst: &mut [u8],
    dst_offset: usize,
    num_bytes: usize,
) {
    dst[dst_offset..dst_offset + num_bytes]
        .copy_from_slice(&src[src_offset..src_offset + num_bytes]);
}

// ---------------------------------------------------------------------------
// Transform operations
// ---------------------------------------------------------------------------
//
// Each transform applies a matrix multiplication to `count` vertices stored
// in `data`, starting at `offset` (in floats) and advancing by `stride`
// (in floats) per vertex.  The transform is IN-PLACE: components are read
// into locals before results are written back.
//
// Matrix layout is COLUMN-MAJOR, matching the original C++ templates.

/// Vec4 * Mat4x4 in-place transform.
///
/// For each vertex at `data[pos..pos+4]`:
///   x' = x*m[0] + y*m[4] + z*m[8]  + w*m[12]
///   y' = x*m[1] + y*m[5] + z*m[9]  + w*m[13]
///   z' = x*m[2] + y*m[6] + z*m[10] + w*m[14]
///   w' = x*m[3] + y*m[7] + z*m[11] + w*m[15]
pub fn transform_v4m4(
    data: &mut [f32],
    stride: usize,
    count: usize,
    matrix: &[f32],
    offset: usize,
) {
    let m = matrix;
    let mut pos = offset;
    for _ in 0..count {
        let x = data[pos];
        let y = data[pos + 1];
        let z = data[pos + 2];
        let w = data[pos + 3];
        data[pos]     = x * m[0]  + y * m[4]  + z * m[8]  + w * m[12];
        data[pos + 1] = x * m[1]  + y * m[5]  + z * m[9]  + w * m[13];
        data[pos + 2] = x * m[2]  + y * m[6]  + z * m[10] + w * m[14];
        data[pos + 3] = x * m[3]  + y * m[7]  + z * m[11] + w * m[15];
        pos += stride;
    }
}

/// Vec3 * Mat4x4 in-place transform (implicit w=1).
///
/// For each vertex at `data[pos..pos+3]`:
///   x' = x*m[0] + y*m[4] + z*m[8]  + m[12]
///   y' = x*m[1] + y*m[5] + z*m[9]  + m[13]
///   z' = x*m[2] + y*m[6] + z*m[10] + m[14]
pub fn transform_v3m4(
    data: &mut [f32],
    stride: usize,
    count: usize,
    matrix: &[f32],
    offset: usize,
) {
    let m = matrix;
    let mut pos = offset;
    for _ in 0..count {
        let x = data[pos];
        let y = data[pos + 1];
        let z = data[pos + 2];
        data[pos]     = x * m[0]  + y * m[4]  + z * m[8]  + m[12];
        data[pos + 1] = x * m[1]  + y * m[5]  + z * m[9]  + m[13];
        data[pos + 2] = x * m[2]  + y * m[6]  + z * m[10] + m[14];
        pos += stride;
    }
}

/// Vec2 * Mat4x4 in-place transform (implicit z=0, w=1).
///
/// For each vertex at `data[pos..pos+2]`:
///   x' = x*m[0] + y*m[4] + m[12]
///   y' = x*m[1] + y*m[5] + m[13]
pub fn transform_v2m4(
    data: &mut [f32],
    stride: usize,
    count: usize,
    matrix: &[f32],
    offset: usize,
) {
    let m = matrix;
    let mut pos = offset;
    for _ in 0..count {
        let x = data[pos];
        let y = data[pos + 1];
        data[pos]     = x * m[0] + y * m[4] + m[12];
        data[pos + 1] = x * m[1] + y * m[5] + m[13];
        pos += stride;
    }
}

/// Vec3 * Mat3x3 in-place transform.
///
/// For each vertex at `data[pos..pos+3]`:
///   x' = x*m[0] + y*m[3] + z*m[6]
///   y' = x*m[1] + y*m[4] + z*m[7]
///   z' = x*m[2] + y*m[5] + z*m[8]
pub fn transform_v3m3(
    data: &mut [f32],
    stride: usize,
    count: usize,
    matrix: &[f32],
    offset: usize,
) {
    let m = matrix;
    let mut pos = offset;
    for _ in 0..count {
        let x = data[pos];
        let y = data[pos + 1];
        let z = data[pos + 2];
        data[pos]     = x * m[0] + y * m[3] + z * m[6];
        data[pos + 1] = x * m[1] + y * m[4] + z * m[7];
        data[pos + 2] = x * m[2] + y * m[5] + z * m[8];
        pos += stride;
    }
}

/// Vec2 * Mat3x3 in-place transform (implicit z=1 for translation).
///
/// For each vertex at `data[pos..pos+2]`:
///   x' = x*m[0] + y*m[3] + m[6]
///   y' = x*m[1] + y*m[4] + m[7]
pub fn transform_v2m3(
    data: &mut [f32],
    stride: usize,
    count: usize,
    matrix: &[f32],
    offset: usize,
) {
    let m = matrix;
    let mut pos = offset;
    for _ in 0..count {
        let x = data[pos];
        let y = data[pos + 1];
        data[pos]     = x * m[0] + y * m[3] + m[6];
        data[pos + 1] = x * m[1] + y * m[4] + m[7];
        pos += stride;
    }
}

// ---------------------------------------------------------------------------
// Find / compare operations
// ---------------------------------------------------------------------------

/// Compares two float slices for exact equality using both bitwise and value
/// comparison.  Two floats are considered equal if their bit patterns match
/// OR their values are equal (handles +0/-0, but NaN==NaN only when bit-identical).
///
/// This mirrors the C++ `compare(lhs, rhs, size)`:
/// ```cpp
/// if ((*(unsigned int*)&lhs[i] != *(unsigned int*)&rhs[i]) && lhs[i] != rhs[i])
///     return false;
/// ```
#[inline]
fn compare_exact(lhs: &[f32], rhs: &[f32], size: usize) -> bool {
    for i in 0..size {
        if lhs[i].to_bits() != rhs[i].to_bits() && lhs[i] != rhs[i] {
            return false;
        }
    }
    true
}

/// Compares two float slices for approximate equality within an epsilon
/// tolerance.  Two floats are considered equal if their bit patterns match
/// OR their absolute difference is <= epsilon.
///
/// This mirrors the C++ `compare(lhs, rhs, size, epsilon)`:
/// ```cpp
/// if ((*(unsigned int*)&lhs[i] != *(unsigned int*)&rhs[i]) &&
///     ((lhs[i] > rhs[i] ? lhs[i] - rhs[i] : rhs[i] - lhs[i]) > epsilon))
///     return false;
/// ```
#[inline]
fn compare_epsilon(lhs: &[f32], rhs: &[f32], size: usize, epsilon: f32) -> bool {
    for i in 0..size {
        if lhs[i].to_bits() != rhs[i].to_bits() {
            let diff = if lhs[i] > rhs[i] {
                lhs[i] - rhs[i]
            } else {
                rhs[i] - lhs[i]
            };
            if diff > epsilon {
                return false;
            }
        }
    }
    true
}

/// Finds the first vertex in `vertices` that exactly matches `vertex`.
///
/// `vertex` has `vertex.len()` floats (the vertex size).
/// `stride` is the number of floats per vertex in the `vertices` array
/// (must be >= `vertex.len()`).
/// `count` is the number of vertices to search.
///
/// Returns the 0-based index of the matching vertex, or -1 if not found.
pub fn find_vertex(
    vertex: &[f32],
    stride: usize,
    vertices: &[f32],
    count: usize,
) -> i64 {
    let size = vertex.len();
    for i in 0..count {
        let base = i * stride;
        if compare_exact(&vertices[base..base + size], vertex, size) {
            return i as i64;
        }
    }
    -1
}

/// Finds the first vertex in `vertices` that matches `vertex` within an
/// epsilon tolerance.
///
/// `vertex` has `vertex.len()` floats (the vertex size).
/// `stride` is the number of floats per vertex in the `vertices` array
/// (must be >= `vertex.len()`).
/// `count` is the number of vertices to search.
/// `epsilon` is the maximum allowed absolute difference per component.
///
/// Returns the 0-based index of the matching vertex, or -1 if not found.
pub fn find_vertex_epsilon(
    vertex: &[f32],
    stride: usize,
    vertices: &[f32],
    count: usize,
    epsilon: f32,
) -> i64 {
    let size = vertex.len();
    for i in 0..count {
        let base = i * stride;
        if compare_epsilon(&vertices[base..base + size], vertex, size, epsilon) {
            return i as i64;
        }
    }
    -1
}

// ---------------------------------------------------------------------------
// C ABI exports (for Scala Native @extern)
// ---------------------------------------------------------------------------

/// # Safety
///
/// Caller must ensure `src` and `dst` point to valid memory of sufficient
/// size, and that `src_offset + num_bytes` and `dst_offset + num_bytes`
/// are within bounds.
#[no_mangle]
pub unsafe extern "C" fn sge_copy_bytes(
    src: *const u8,
    src_offset: i32,
    dst: *mut u8,
    dst_offset: i32,
    num_bytes: i32,
) {
    let num = num_bytes as usize;
    let s = unsafe { core::slice::from_raw_parts(src.add(src_offset as usize), num) };
    let d = unsafe { core::slice::from_raw_parts_mut(dst.add(dst_offset as usize), num) };
    d.copy_from_slice(s);
}

/// # Safety
///
/// Caller must ensure `data` points to a valid f32 buffer of sufficient size,
/// and `matrix` points to at least 16 f32 values.
#[no_mangle]
pub unsafe extern "C" fn sge_transform_v4m4(
    data: *mut f32,
    stride: i32,
    count: i32,
    matrix: *const f32,
    offset: i32,
) {
    let total = offset as usize + (count as usize).saturating_sub(1) * stride as usize + 4;
    let d = unsafe { core::slice::from_raw_parts_mut(data, total) };
    let m = unsafe { core::slice::from_raw_parts(matrix, 16) };
    transform_v4m4(d, stride as usize, count as usize, m, offset as usize);
}

/// # Safety
///
/// Caller must ensure `data` points to a valid f32 buffer of sufficient size,
/// and `matrix` points to at least 16 f32 values.
#[no_mangle]
pub unsafe extern "C" fn sge_transform_v3m4(
    data: *mut f32,
    stride: i32,
    count: i32,
    matrix: *const f32,
    offset: i32,
) {
    let total = offset as usize + (count as usize).saturating_sub(1) * stride as usize + 3;
    let d = unsafe { core::slice::from_raw_parts_mut(data, total) };
    let m = unsafe { core::slice::from_raw_parts(matrix, 16) };
    transform_v3m4(d, stride as usize, count as usize, m, offset as usize);
}

/// # Safety
///
/// Caller must ensure `data` points to a valid f32 buffer of sufficient size,
/// and `matrix` points to at least 16 f32 values.
#[no_mangle]
pub unsafe extern "C" fn sge_transform_v2m4(
    data: *mut f32,
    stride: i32,
    count: i32,
    matrix: *const f32,
    offset: i32,
) {
    let total = offset as usize + (count as usize).saturating_sub(1) * stride as usize + 2;
    let d = unsafe { core::slice::from_raw_parts_mut(data, total) };
    let m = unsafe { core::slice::from_raw_parts(matrix, 16) };
    transform_v2m4(d, stride as usize, count as usize, m, offset as usize);
}

/// # Safety
///
/// Caller must ensure `data` points to a valid f32 buffer of sufficient size,
/// and `matrix` points to at least 9 f32 values.
#[no_mangle]
pub unsafe extern "C" fn sge_transform_v3m3(
    data: *mut f32,
    stride: i32,
    count: i32,
    matrix: *const f32,
    offset: i32,
) {
    let total = offset as usize + (count as usize).saturating_sub(1) * stride as usize + 3;
    let d = unsafe { core::slice::from_raw_parts_mut(data, total) };
    let m = unsafe { core::slice::from_raw_parts(matrix, 9) };
    transform_v3m3(d, stride as usize, count as usize, m, offset as usize);
}

/// # Safety
///
/// Caller must ensure `data` points to a valid f32 buffer of sufficient size,
/// and `matrix` points to at least 9 f32 values.
#[no_mangle]
pub unsafe extern "C" fn sge_transform_v2m3(
    data: *mut f32,
    stride: i32,
    count: i32,
    matrix: *const f32,
    offset: i32,
) {
    let total = offset as usize + (count as usize).saturating_sub(1) * stride as usize + 2;
    let d = unsafe { core::slice::from_raw_parts_mut(data, total) };
    let m = unsafe { core::slice::from_raw_parts(matrix, 9) };
    transform_v2m3(d, stride as usize, count as usize, m, offset as usize);
}

/// # Safety
///
/// Caller must ensure `vertex` points to at least `size` f32 values, and
/// `vertices` points to at least `count * size` f32 values.
#[no_mangle]
pub unsafe extern "C" fn sge_find_vertex(
    vertex: *const f32,
    size: u32,
    vertices: *const f32,
    count: u32,
) -> i64 {
    let sz = size as usize;
    let cnt = count as usize;
    let v = unsafe { core::slice::from_raw_parts(vertex, sz) };
    let vs = unsafe { core::slice::from_raw_parts(vertices, cnt * sz) };
    find_vertex(v, sz, vs, cnt)
}

/// # Safety
///
/// Caller must ensure `vertex` points to at least `size` f32 values, and
/// `vertices` points to at least `count * size` f32 values.
#[no_mangle]
pub unsafe extern "C" fn sge_find_vertex_epsilon(
    vertex: *const f32,
    size: u32,
    vertices: *const f32,
    count: u32,
    epsilon: f32,
) -> i64 {
    let sz = size as usize;
    let cnt = count as usize;
    let v = unsafe { core::slice::from_raw_parts(vertex, sz) };
    let vs = unsafe { core::slice::from_raw_parts(vertices, cnt * sz) };
    find_vertex_epsilon(v, sz, vs, cnt, epsilon)
}

// ---------------------------------------------------------------------------
// Tests
// ---------------------------------------------------------------------------

#[cfg(test)]
mod tests {
    use super::*;

    // -- Copy tests --------------------------------------------------------

    #[test]
    fn copy_bytes_round_trip() {
        let src: Vec<u8> = (0..32).collect();
        let mut dst = vec![0u8; 32];
        copy_bytes(&src, 4, &mut dst, 8, 16);
        assert_eq!(&dst[8..24], &src[4..20]);
        // Untouched regions remain zero
        assert_eq!(&dst[0..8], &[0u8; 8]);
        assert_eq!(&dst[24..32], &[0u8; 8]);
    }

    #[test]
    fn copy_bytes_full_slice() {
        let src = vec![0xAA_u8; 64];
        let mut dst = vec![0u8; 64];
        copy_bytes(&src, 0, &mut dst, 0, 64);
        assert_eq!(src, dst);
    }

    // -- Transform identity tests ------------------------------------------

    /// Column-major 4x4 identity matrix.
    const IDENTITY_4X4: [f32; 16] = [
        1.0, 0.0, 0.0, 0.0, // col 0
        0.0, 1.0, 0.0, 0.0, // col 1
        0.0, 0.0, 1.0, 0.0, // col 2
        0.0, 0.0, 0.0, 1.0, // col 3
    ];

    /// Column-major 3x3 identity matrix.
    const IDENTITY_3X3: [f32; 9] = [
        1.0, 0.0, 0.0, // col 0
        0.0, 1.0, 0.0, // col 1
        0.0, 0.0, 1.0, // col 2
    ];

    #[test]
    fn transform_v4m4_identity() {
        let mut data = vec![1.0, 2.0, 3.0, 4.0];
        transform_v4m4(&mut data, 4, 1, &IDENTITY_4X4, 0);
        assert_eq!(data, vec![1.0, 2.0, 3.0, 4.0]);
    }

    #[test]
    fn transform_v3m4_identity() {
        let mut data = vec![5.0, 6.0, 7.0];
        transform_v3m4(&mut data, 3, 1, &IDENTITY_4X4, 0);
        assert_eq!(data, vec![5.0, 6.0, 7.0]);
    }

    #[test]
    fn transform_v2m4_identity() {
        let mut data = vec![8.0, 9.0];
        transform_v2m4(&mut data, 2, 1, &IDENTITY_4X4, 0);
        assert_eq!(data, vec![8.0, 9.0]);
    }

    #[test]
    fn transform_v3m3_identity() {
        let mut data = vec![10.0, 11.0, 12.0];
        transform_v3m3(&mut data, 3, 1, &IDENTITY_3X3, 0);
        assert_eq!(data, vec![10.0, 11.0, 12.0]);
    }

    #[test]
    fn transform_v2m3_identity() {
        let mut data = vec![13.0, 14.0];
        transform_v2m3(&mut data, 2, 1, &IDENTITY_3X3, 0);
        assert_eq!(data, vec![13.0, 14.0]);
    }

    // -- Transform with known values ---------------------------------------

    #[test]
    fn transform_v4m4_known_values() {
        // Column-major scale matrix: scale x by 2, y by 3, z by 4, w by 5
        let matrix: [f32; 16] = [
            2.0, 0.0, 0.0, 0.0, // col 0
            0.0, 3.0, 0.0, 0.0, // col 1
            0.0, 0.0, 4.0, 0.0, // col 2
            0.0, 0.0, 0.0, 5.0, // col 3
        ];
        let mut data = vec![1.0, 1.0, 1.0, 1.0];
        transform_v4m4(&mut data, 4, 1, &matrix, 0);
        assert_eq!(data, vec![2.0, 3.0, 4.0, 5.0]);
    }

    #[test]
    fn transform_v3m4_translation() {
        // Column-major translation matrix: translate by (10, 20, 30)
        let matrix: [f32; 16] = [
            1.0, 0.0, 0.0, 0.0, // col 0
            0.0, 1.0, 0.0, 0.0, // col 1
            0.0, 0.0, 1.0, 0.0, // col 2
            10.0, 20.0, 30.0, 1.0, // col 3 (translation)
        ];
        let mut data = vec![1.0, 2.0, 3.0];
        transform_v3m4(&mut data, 3, 1, &matrix, 0);
        assert_eq!(data, vec![11.0, 22.0, 33.0]);
    }

    #[test]
    fn transform_v2m4_translation() {
        // Column-major translation: translate by (5, 7)
        let matrix: [f32; 16] = [
            1.0, 0.0, 0.0, 0.0, // col 0
            0.0, 1.0, 0.0, 0.0, // col 1
            0.0, 0.0, 1.0, 0.0, // col 2
            5.0, 7.0, 0.0, 1.0, // col 3
        ];
        let mut data = vec![3.0, 4.0];
        transform_v2m4(&mut data, 2, 1, &matrix, 0);
        assert_eq!(data, vec![8.0, 11.0]);
    }

    #[test]
    fn transform_v3m3_scale() {
        // Column-major scale: (2, 3, 4)
        let matrix: [f32; 9] = [
            2.0, 0.0, 0.0, // col 0
            0.0, 3.0, 0.0, // col 1
            0.0, 0.0, 4.0, // col 2
        ];
        let mut data = vec![1.0, 2.0, 3.0];
        transform_v3m3(&mut data, 3, 1, &matrix, 0);
        assert_eq!(data, vec![2.0, 6.0, 12.0]);
    }

    #[test]
    fn transform_v2m3_known_values() {
        // Column-major 3x3 with translation:
        // [ 2  0  5 ]
        // [ 0  3  7 ]
        // [ 0  0  1 ]
        // In column-major storage: col0=[2,0,0], col1=[0,3,0], col2=[5,7,1]
        let matrix: [f32; 9] = [
            2.0, 0.0, 0.0, // col 0
            0.0, 3.0, 0.0, // col 1
            5.0, 7.0, 1.0, // col 2
        ];
        let mut data = vec![1.0, 1.0];
        transform_v2m3(&mut data, 2, 1, &matrix, 0);
        // x' = 1*2 + 1*0 + 5 = 7
        // y' = 1*0 + 1*3 + 7 = 10
        assert_eq!(data, vec![7.0, 10.0]);
    }

    #[test]
    fn transform_v3m4_full_matrix() {
        // Test with a non-trivial column-major matrix:
        // Row-major view:
        //   [ 1  5  9  13 ]
        //   [ 2  6 10  14 ]
        //   [ 3  7 11  15 ]
        //   [ 4  8 12  16 ]
        // Column-major storage:
        let matrix: [f32; 16] = [
            1.0, 2.0, 3.0, 4.0,   // col 0
            5.0, 6.0, 7.0, 8.0,   // col 1
            9.0, 10.0, 11.0, 12.0, // col 2
            13.0, 14.0, 15.0, 16.0, // col 3
        ];
        let mut data = vec![1.0, 0.0, 0.0];
        transform_v3m4(&mut data, 3, 1, &matrix, 0);
        // x' = 1*1 + 0*5 + 0*9 + 13 = 14
        // y' = 1*2 + 0*6 + 0*10 + 14 = 16
        // z' = 1*3 + 0*7 + 0*11 + 15 = 18
        assert_eq!(data, vec![14.0, 16.0, 18.0]);
    }

    // -- Multi-vertex transform with stride --------------------------------

    #[test]
    fn transform_v3m4_multi_vertex_with_stride() {
        // Stride of 5 floats: [x, y, z, u, v] per vertex
        // Translation by (10, 20, 30)
        let matrix: [f32; 16] = [
            1.0, 0.0, 0.0, 0.0,
            0.0, 1.0, 0.0, 0.0,
            0.0, 0.0, 1.0, 0.0,
            10.0, 20.0, 30.0, 1.0,
        ];
        let mut data = vec![
            1.0, 2.0, 3.0, 0.5, 0.5, // vertex 0: pos + uv
            4.0, 5.0, 6.0, 0.7, 0.8, // vertex 1: pos + uv
            7.0, 8.0, 9.0, 0.1, 0.2, // vertex 2: pos + uv
        ];
        transform_v3m4(&mut data, 5, 3, &matrix, 0);
        // Positions should be translated, UVs untouched
        assert_eq!(data[0], 11.0);
        assert_eq!(data[1], 22.0);
        assert_eq!(data[2], 33.0);
        assert_eq!(data[3], 0.5); // u untouched
        assert_eq!(data[4], 0.5); // v untouched
        assert_eq!(data[5], 14.0);
        assert_eq!(data[6], 25.0);
        assert_eq!(data[7], 36.0);
        assert_eq!(data[8], 0.7);
        assert_eq!(data[9], 0.8);
        assert_eq!(data[10], 17.0);
        assert_eq!(data[11], 28.0);
        assert_eq!(data[12], 39.0);
        assert_eq!(data[13], 0.1);
        assert_eq!(data[14], 0.2);
    }

    #[test]
    fn transform_v2m3_multi_vertex_with_stride() {
        // Stride of 4 floats: [x, y, r, g] per vertex
        // Scale x by 2, y by 3, translate by (1, 2)
        let matrix: [f32; 9] = [
            2.0, 0.0, 0.0, // col 0
            0.0, 3.0, 0.0, // col 1
            1.0, 2.0, 1.0, // col 2
        ];
        let mut data = vec![
            1.0, 1.0, 0.5, 0.5, // vertex 0
            2.0, 2.0, 0.7, 0.8, // vertex 1
        ];
        transform_v2m3(&mut data, 4, 2, &matrix, 0);
        // vertex 0: x'=1*2+1*0+1=3, y'=1*0+1*3+2=5
        assert_eq!(data[0], 3.0);
        assert_eq!(data[1], 5.0);
        assert_eq!(data[2], 0.5); // untouched
        assert_eq!(data[3], 0.5); // untouched
        // vertex 1: x'=2*2+2*0+1=5, y'=2*0+2*3+2=8
        assert_eq!(data[4], 5.0);
        assert_eq!(data[5], 8.0);
        assert_eq!(data[6], 0.7);
        assert_eq!(data[7], 0.8);
    }

    #[test]
    fn transform_with_offset() {
        // Start at offset 2 (skip first 2 floats)
        let matrix: [f32; 16] = [
            1.0, 0.0, 0.0, 0.0,
            0.0, 1.0, 0.0, 0.0,
            0.0, 0.0, 1.0, 0.0,
            10.0, 20.0, 30.0, 1.0,
        ];
        let mut data = vec![99.0, 99.0, 1.0, 2.0, 3.0];
        transform_v3m4(&mut data, 3, 1, &matrix, 2);
        assert_eq!(data[0], 99.0); // untouched
        assert_eq!(data[1], 99.0); // untouched
        assert_eq!(data[2], 11.0);
        assert_eq!(data[3], 22.0);
        assert_eq!(data[4], 33.0);
    }

    // -- Find exact tests --------------------------------------------------

    #[test]
    fn find_vertex_found() {
        let vertices: Vec<f32> = vec![
            1.0, 2.0, 3.0, // vertex 0
            4.0, 5.0, 6.0, // vertex 1
            7.0, 8.0, 9.0, // vertex 2
        ];
        let needle = vec![4.0, 5.0, 6.0];
        assert_eq!(find_vertex(&needle, 3, &vertices, 3), 1);
    }

    #[test]
    fn find_vertex_not_found() {
        let vertices: Vec<f32> = vec![
            1.0, 2.0, 3.0,
            4.0, 5.0, 6.0,
        ];
        let needle = vec![7.0, 8.0, 9.0];
        assert_eq!(find_vertex(&needle, 3, &vertices, 2), -1);
    }

    #[test]
    fn find_vertex_first_match() {
        let vertices: Vec<f32> = vec![
            1.0, 2.0, // vertex 0
            1.0, 2.0, // vertex 1 (duplicate)
        ];
        let needle = vec![1.0, 2.0];
        assert_eq!(find_vertex(&needle, 2, &vertices, 2), 0);
    }

    #[test]
    fn find_vertex_positive_negative_zero() {
        // +0.0 and -0.0 have different bit patterns but are equal by value.
        // The C++ compare returns true if EITHER bits match OR values match.
        // So +0 should match -0.
        let vertices: Vec<f32> = vec![0.0_f32, 1.0];
        let needle = vec![-0.0_f32, 1.0];
        assert_eq!(find_vertex(&needle, 2, &vertices, 1), 0);
    }

    // -- Find epsilon tests ------------------------------------------------

    #[test]
    fn find_vertex_epsilon_within_tolerance() {
        let vertices: Vec<f32> = vec![
            1.0, 2.0, 3.0,
            4.0, 5.0, 6.0,
        ];
        // Slightly off from vertex 1
        let needle = vec![4.001, 4.999, 6.0005];
        assert_eq!(find_vertex_epsilon(&needle, 3, &vertices, 2, 0.01), 1);
    }

    #[test]
    fn find_vertex_epsilon_outside_tolerance() {
        let vertices: Vec<f32> = vec![
            1.0, 2.0, 3.0,
            4.0, 5.0, 6.0,
        ];
        // Too far from any vertex
        let needle = vec![4.1, 5.0, 6.0];
        assert_eq!(find_vertex_epsilon(&needle, 3, &vertices, 2, 0.01), -1);
    }

    #[test]
    fn find_vertex_epsilon_exact_match_fast_path() {
        // When bits match exactly, epsilon check is skipped (fast path)
        let vertices: Vec<f32> = vec![1.0, 2.0];
        let needle = vec![1.0, 2.0];
        assert_eq!(find_vertex_epsilon(&needle, 2, &vertices, 1, 0.0), 0);
    }

    #[test]
    fn find_vertex_epsilon_boundary() {
        // Test at exact epsilon boundary
        let vertices: Vec<f32> = vec![1.0, 2.0];
        // diff = 0.5, epsilon = 0.5 => diff > epsilon is false => match
        let needle = vec![1.5, 2.0];
        assert_eq!(find_vertex_epsilon(&needle, 2, &vertices, 1, 0.5), 0);
        // diff = 0.500001 > 0.5 => no match
        let needle2 = vec![1.500001, 2.0];
        assert_eq!(find_vertex_epsilon(&needle2, 2, &vertices, 1, 0.5), -1);
    }

    // -- Edge cases --------------------------------------------------------

    #[test]
    fn transform_zero_count() {
        let mut data = vec![1.0, 2.0, 3.0, 4.0];
        let original = data.clone();
        transform_v4m4(&mut data, 4, 0, &IDENTITY_4X4, 0);
        assert_eq!(data, original);
    }

    #[test]
    fn find_vertex_zero_count() {
        let vertices: Vec<f32> = vec![];
        let needle = vec![1.0];
        assert_eq!(find_vertex(&needle, 1, &vertices, 0), -1);
    }

    #[test]
    fn copy_bytes_zero_length() {
        let src = vec![1u8, 2, 3];
        let mut dst = vec![0u8; 3];
        copy_bytes(&src, 1, &mut dst, 1, 0);
        assert_eq!(dst, vec![0, 0, 0]);
    }
}
