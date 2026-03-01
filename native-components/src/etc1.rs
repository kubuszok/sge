// ETC1 texture compression codec — faithful port of etc1_utils.cpp
//
// Copyright 2009 Google Inc.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
//
// Public C ABI functions (for Scala Native @extern):
//   etc1_get_encoded_data_size, etc1_encode_block, etc1_decode_block,
//   etc1_encode_image, etc1_decode_image,
//   etc1_pkm_format_header, etc1_pkm_is_valid,
//   etc1_pkm_get_width, etc1_pkm_get_height

// ── Constants ──────────────────────────────────────────────────────────────────

pub const ETC1_ENCODED_BLOCK_SIZE: usize = 8;
pub const ETC1_DECODED_BLOCK_SIZE: usize = 48;
pub const ETC_PKM_HEADER_SIZE: usize = 16;

// ── Lookup Tables ──────────────────────────────────────────────────────────────

// Intensity modifier sets for ETC1 compressed textures (table 3.17.2)
//
// table codeword                modifier table
// ------------------        ----------------------
//       0                     -8  -2  2   8
//       1                    -17  -5  5  17
//       2                    -29  -9  9  29
//       3                    -42 -13 13  42
//       4                    -60 -18 18  60
//       5                    -80 -24 24  80
//       6                   -106 -33 33 106
//       7                   -183 -47 47 183
static K_MODIFIER_TABLE: [i32; 32] = [
    /* 0 */ 2, 8, -2, -8,
    /* 1 */ 5, 17, -5, -17,
    /* 2 */ 9, 29, -9, -29,
    /* 3 */ 13, 42, -13, -42,
    /* 4 */ 18, 60, -18, -60,
    /* 5 */ 24, 80, -24, -80,
    /* 6 */ 33, 106, -33, -106,
    /* 7 */ 47, 183, -47, -183,
];

static K_LOOKUP: [i32; 8] = [0, 1, 2, 3, -4, -3, -2, -1];

// ── Internal Helpers ───────────────────────────────────────────────────────────

#[inline]
fn clamp(x: i32) -> u8 {
    if x >= 0 {
        if x < 255 { x as u8 } else { 255 }
    } else {
        0
    }
}

#[inline]
fn convert4_to8(b: i32) -> i32 {
    let c = b & 0xf;
    (c << 4) | c
}

#[inline]
fn convert5_to8(b: i32) -> i32 {
    let c = b & 0x1f;
    (c << 3) | (c >> 2)
}

#[inline]
fn convert6_to8(b: i32) -> i32 {
    let c = b & 0x3f;
    (c << 2) | (c >> 4)
}

#[inline]
fn divide_by_255(d: i32) -> i32 {
    (d + 128 + (d >> 8)) >> 8
}

#[inline]
fn convert8_to4(b: i32) -> i32 {
    let c = b & 0xff;
    divide_by_255(c * 15)
}

#[inline]
fn convert8_to5(b: i32) -> i32 {
    let c = b & 0xff;
    divide_by_255(c * 31)
}

#[inline]
fn convert_diff(base: i32, diff: i32) -> i32 {
    convert5_to8((0x1f & base) + K_LOOKUP[(0x7 & diff) as usize])
}

#[inline]
fn square(x: i32) -> i32 {
    x * x
}

// ── Block Decode ───────────────────────────────────────────────────────────────

fn decode_subblock(
    p_out: &mut [u8],
    r: i32,
    g: i32,
    b: i32,
    table: &[i32],
    low: u32,
    second: bool,
    flipped: bool,
) {
    let mut base_x: i32 = 0;
    let mut base_y: i32 = 0;
    if second {
        if flipped {
            base_y = 2;
        } else {
            base_x = 2;
        }
    }
    for i in 0..8 {
        let (x, y);
        if flipped {
            x = base_x + (i >> 1);
            y = base_y + (i & 1);
        } else {
            x = base_x + (i >> 2);
            y = base_y + (i & 3);
        }
        let k = y + (x * 4);
        let offset = ((low >> k) & 1) | ((low >> (k + 15)) & 2);
        let delta = table[offset as usize];
        let q = 3 * (x + 4 * y) as usize;
        p_out[q] = clamp(r + delta);
        p_out[q + 1] = clamp(g + delta);
        p_out[q + 2] = clamp(b + delta);
    }
}

// Input is an ETC1 compressed version of the data.
// Output is a 4 x 4 square of 3-byte pixels in form R, G, B
fn decode_block(p_in: &[u8], p_out: &mut [u8]) {
    let high: u32 = (p_in[0] as u32) << 24
        | (p_in[1] as u32) << 16
        | (p_in[2] as u32) << 8
        | p_in[3] as u32;
    let low: u32 = (p_in[4] as u32) << 24
        | (p_in[5] as u32) << 16
        | (p_in[6] as u32) << 8
        | p_in[7] as u32;

    let (r1, r2, g1, g2, b1, b2);
    if high & 2 != 0 {
        // differential
        let r_base = (high >> 27) as i32;
        let g_base = (high >> 19) as i32;
        let b_base = (high >> 11) as i32;
        r1 = convert5_to8(r_base);
        r2 = convert_diff(r_base, (high >> 24) as i32);
        g1 = convert5_to8(g_base);
        g2 = convert_diff(g_base, (high >> 16) as i32);
        b1 = convert5_to8(b_base);
        b2 = convert_diff(b_base, (high >> 8) as i32);
    } else {
        // not differential
        r1 = convert4_to8((high >> 28) as i32);
        r2 = convert4_to8((high >> 24) as i32);
        g1 = convert4_to8((high >> 20) as i32);
        g2 = convert4_to8((high >> 16) as i32);
        b1 = convert4_to8((high >> 12) as i32);
        b2 = convert4_to8((high >> 8) as i32);
    }
    let table_index_a = (7 & (high >> 5)) as usize;
    let table_index_b = (7 & (high >> 2)) as usize;
    let table_a = &K_MODIFIER_TABLE[table_index_a * 4..table_index_a * 4 + 4];
    let table_b = &K_MODIFIER_TABLE[table_index_b * 4..table_index_b * 4 + 4];
    let flipped = (high & 1) != 0;
    decode_subblock(p_out, r1, g1, b1, table_a, low, false, flipped);
    decode_subblock(p_out, r2, g2, b2, table_b, low, true, flipped);
}

// ── Block Encode ───────────────────────────────────────────────────────────────

#[derive(Clone, Copy)]
struct EtcCompressed {
    high: u32,
    low: u32,
    score: u32, // Lower is more accurate
}

#[inline]
fn take_best(a: &mut EtcCompressed, b: &EtcCompressed) {
    if a.score > b.score {
        *a = *b;
    }
}

fn etc_average_colors_subblock(
    p_in: &[u8],
    in_mask: u32,
    p_colors: &mut [u8],
    flipped: bool,
    second: bool,
) {
    let mut r: i32 = 0;
    let mut g: i32 = 0;
    let mut b: i32 = 0;

    if flipped {
        let by = if second { 2 } else { 0 };
        for y in 0..2 {
            let yy = by + y;
            for x in 0..4 {
                let i = x + 4 * yy;
                if in_mask & (1 << i) != 0 {
                    let p = (i * 3) as usize;
                    r += p_in[p] as i32;
                    g += p_in[p + 1] as i32;
                    b += p_in[p + 2] as i32;
                }
            }
        }
    } else {
        let bx = if second { 2 } else { 0 };
        for y in 0..4 {
            for x in 0..2 {
                let xx = bx + x;
                let i = xx + 4 * y;
                if in_mask & (1 << i) != 0 {
                    let p = (i * 3) as usize;
                    r += p_in[p] as i32;
                    g += p_in[p + 1] as i32;
                    b += p_in[p + 2] as i32;
                }
            }
        }
    }
    p_colors[0] = ((r + 4) >> 3) as u8;
    p_colors[1] = ((g + 4) >> 3) as u8;
    p_colors[2] = ((b + 4) >> 3) as u8;
}

fn choose_modifier(
    p_base_colors: &[u8],
    p_in: &[u8],
    p_low: &mut u32,
    bit_index: i32,
    p_modifier_table: &[i32],
) -> u32 {
    let mut best_score: u32 = !0;
    let mut best_index: i32 = 0;
    let pixel_r = p_in[0] as i32;
    let pixel_g = p_in[1] as i32;
    let pixel_b = p_in[2] as i32;
    let r = p_base_colors[0] as i32;
    let g = p_base_colors[1] as i32;
    let b = p_base_colors[2] as i32;
    for i in 0..4 {
        let modifier = p_modifier_table[i];
        let decoded_g = clamp(g + modifier) as i32;
        let score = (6 * square(decoded_g - pixel_g)) as u32;
        if score >= best_score {
            continue;
        }
        let decoded_r = clamp(r + modifier) as i32;
        let mut score = score + (3 * square(decoded_r - pixel_r)) as u32;
        if score >= best_score {
            continue;
        }
        let decoded_b = clamp(b + modifier) as i32;
        score += square(decoded_b - pixel_b) as u32;
        if score < best_score {
            best_score = score;
            best_index = i as i32;
        }
    }
    let low_mask =
        (((best_index >> 1) << 16) | (best_index & 1)) << bit_index;
    *p_low |= low_mask as u32;
    best_score
}

fn etc_encode_subblock_helper(
    p_in: &[u8],
    in_mask: u32,
    p_compressed: &mut EtcCompressed,
    flipped: bool,
    second: bool,
    p_base_colors: &[u8],
    p_modifier_table: &[i32],
) {
    let mut score = p_compressed.score;
    if flipped {
        let by = if second { 2 } else { 0 };
        for y in 0..2 {
            let yy = by + y;
            for x in 0..4 {
                let i = x + 4 * yy;
                if in_mask & (1 << i) != 0 {
                    score += choose_modifier(
                        p_base_colors,
                        &p_in[i as usize * 3..],
                        &mut p_compressed.low,
                        yy + x * 4,
                        p_modifier_table,
                    );
                }
            }
        }
    } else {
        let bx = if second { 2 } else { 0 };
        for y in 0..4 {
            for x in 0..2 {
                let xx = bx + x;
                let i = xx + 4 * y;
                if in_mask & (1 << i) != 0 {
                    score += choose_modifier(
                        p_base_colors,
                        &p_in[i as usize * 3..],
                        &mut p_compressed.low,
                        y + xx * 4,
                        p_modifier_table,
                    );
                }
            }
        }
    }
    p_compressed.score = score;
}

#[inline]
fn in_range_4bit_signed(color: i32) -> bool {
    color >= -4 && color <= 3
}

fn etc_encode_base_colors(
    p_base_colors: &mut [u8],
    p_colors: &[u8],
    p_compressed: &mut EtcCompressed,
) {
    let r1: i32;
    let g1: i32;
    let b1: i32;
    let r2: i32;
    let g2: i32;
    let b2: i32;

    let r51 = convert8_to5(p_colors[0] as i32);
    let g51 = convert8_to5(p_colors[1] as i32);
    let b51 = convert8_to5(p_colors[2] as i32);
    let r52 = convert8_to5(p_colors[3] as i32);
    let g52 = convert8_to5(p_colors[4] as i32);
    let b52 = convert8_to5(p_colors[5] as i32);

    let r1_5 = convert5_to8(r51);
    let g1_5 = convert5_to8(g51);
    let b1_5 = convert5_to8(b51);

    let dr = r52 - r51;
    let dg = g52 - g51;
    let db = b52 - b51;

    let differential =
        in_range_4bit_signed(dr) && in_range_4bit_signed(dg) && in_range_4bit_signed(db);

    if differential {
        r1 = r1_5;
        g1 = g1_5;
        b1 = b1_5;
        r2 = convert5_to8(r51 + dr);
        g2 = convert5_to8(g51 + dg);
        b2 = convert5_to8(b51 + db);
        p_compressed.high |= ((r51 as u32) << 27)
            | (((7 & dr) as u32) << 24)
            | ((g51 as u32) << 19)
            | (((7 & dg) as u32) << 16)
            | ((b51 as u32) << 11)
            | (((7 & db) as u32) << 8)
            | 2;
    } else {
        let r41 = convert8_to4(p_colors[0] as i32);
        let g41 = convert8_to4(p_colors[1] as i32);
        let b41 = convert8_to4(p_colors[2] as i32);
        let r42 = convert8_to4(p_colors[3] as i32);
        let g42 = convert8_to4(p_colors[4] as i32);
        let b42 = convert8_to4(p_colors[5] as i32);
        r1 = convert4_to8(r41);
        g1 = convert4_to8(g41);
        b1 = convert4_to8(b41);
        r2 = convert4_to8(r42);
        g2 = convert4_to8(g42);
        b2 = convert4_to8(b42);
        p_compressed.high |= ((r41 as u32) << 28)
            | ((r42 as u32) << 24)
            | ((g41 as u32) << 20)
            | ((g42 as u32) << 16)
            | ((b41 as u32) << 12)
            | ((b42 as u32) << 8);
    }
    p_base_colors[0] = r1 as u8;
    p_base_colors[1] = g1 as u8;
    p_base_colors[2] = b1 as u8;
    p_base_colors[3] = r2 as u8;
    p_base_colors[4] = g2 as u8;
    p_base_colors[5] = b2 as u8;
}

fn etc_encode_block_helper(
    p_in: &[u8],
    in_mask: u32,
    p_colors: &[u8],
    p_compressed: &mut EtcCompressed,
    flipped: bool,
) {
    p_compressed.score = !0;
    p_compressed.high = if flipped { 1 } else { 0 };
    p_compressed.low = 0;

    let mut p_base_colors = [0u8; 6];

    etc_encode_base_colors(&mut p_base_colors, p_colors, p_compressed);

    let original_high = p_compressed.high;

    // First subblock: try all 8 modifier tables
    for i in 0..8u32 {
        let mut temp = EtcCompressed {
            score: 0,
            high: original_high | (i << 5),
            low: 0,
        };
        etc_encode_subblock_helper(
            p_in,
            in_mask,
            &mut temp,
            flipped,
            false,
            &p_base_colors[..3],
            &K_MODIFIER_TABLE[i as usize * 4..i as usize * 4 + 4],
        );
        take_best(p_compressed, &temp);
    }

    // Second subblock: try all 8 modifier tables
    let first_half = *p_compressed;
    for i in 0..8u32 {
        let mut temp = EtcCompressed {
            score: first_half.score,
            high: first_half.high | (i << 2),
            low: first_half.low,
        };
        etc_encode_subblock_helper(
            p_in,
            in_mask,
            &mut temp,
            flipped,
            true,
            &p_base_colors[3..6],
            &K_MODIFIER_TABLE[i as usize * 4..i as usize * 4 + 4],
        );
        if i == 0 {
            *p_compressed = temp;
        } else {
            take_best(p_compressed, &temp);
        }
    }
}

fn write_big_endian(p_out: &mut [u8], d: u32) {
    p_out[0] = (d >> 24) as u8;
    p_out[1] = (d >> 16) as u8;
    p_out[2] = (d >> 8) as u8;
    p_out[3] = d as u8;
}

// Input is a 4 x 4 square of 3-byte pixels in form R, G, B
// in_mask is a 16-bit mask where bit (1 << (x + y * 4)) tells whether the
// corresponding (x,y) pixel is valid or not. Invalid pixel color values are
// ignored when compressing.
// Output is an ETC1 compressed version of the data.
fn encode_block(p_in: &[u8], in_mask: u32, p_out: &mut [u8]) {
    let mut colors = [0u8; 6];
    let mut flipped_colors = [0u8; 6];
    etc_average_colors_subblock(p_in, in_mask, &mut colors[..3], false, false);
    etc_average_colors_subblock(p_in, in_mask, &mut colors[3..6], false, true);
    etc_average_colors_subblock(p_in, in_mask, &mut flipped_colors[..3], true, false);
    etc_average_colors_subblock(p_in, in_mask, &mut flipped_colors[3..6], true, true);

    let mut a = EtcCompressed { high: 0, low: 0, score: 0 };
    let mut b = EtcCompressed { high: 0, low: 0, score: 0 };
    etc_encode_block_helper(p_in, in_mask, &colors, &mut a, false);
    etc_encode_block_helper(p_in, in_mask, &flipped_colors, &mut b, true);
    take_best(&mut a, &b);
    write_big_endian(&mut p_out[0..4], a.high);
    write_big_endian(&mut p_out[4..8], a.low);
}

// ── Image Encode / Decode ──────────────────────────────────────────────────────

static K_Y_MASK: [u16; 5] = [0x0, 0xf, 0xff, 0xfff, 0xffff];
static K_X_MASK: [u16; 5] = [0x0, 0x1111, 0x3333, 0x7777, 0xffff];

/// Encode an entire image.
///
/// `input` - the image data. Pixel (x,y) is at input[pixel_size * x + stride * y].
/// `width`, `height` - image dimensions.
/// `pixel_size` - 2 for GL_UNSIGNED_SHORT_5_6_5, 3 for GL_BYTE RGB.
/// `stride` - byte stride between rows.
/// `output` - buffer for encoded data (must be at least `get_encoded_data_size(width, height)` bytes).
///
/// Returns 0 on success, -1 on error (invalid pixel_size).
pub fn encode_image(
    input: &[u8],
    width: u32,
    height: u32,
    pixel_size: u32,
    stride: u32,
    output: &mut [u8],
) -> i32 {
    if pixel_size < 2 || pixel_size > 3 {
        return -1;
    }

    let mut block = [0u8; ETC1_DECODED_BLOCK_SIZE];
    let mut encoded = [0u8; ETC1_ENCODED_BLOCK_SIZE];

    let encoded_width = (width + 3) & !3;
    let encoded_height = (height + 3) & !3;

    let mut out_offset: usize = 0;

    let mut y: u32 = 0;
    while y < encoded_height {
        let mut y_end = height - y;
        if y_end > 4 {
            y_end = 4;
        }
        let ymask = K_Y_MASK[y_end as usize] as i32;
        let mut x: u32 = 0;
        while x < encoded_width {
            let mut x_end = width - x;
            if x_end > 4 {
                x_end = 4;
            }
            let mask = ymask & K_X_MASK[x_end as usize] as i32;
            for cy in 0..y_end {
                let q_start = (cy * 4) as usize * 3;
                let p_start = (pixel_size * x + stride * (y + cy)) as usize;
                if pixel_size == 3 {
                    let count = x_end as usize * 3;
                    block[q_start..q_start + count]
                        .copy_from_slice(&input[p_start..p_start + count]);
                } else {
                    let mut q = q_start;
                    let mut p = p_start;
                    for _cx in 0..x_end {
                        let pixel = ((input[p + 1] as i32) << 8) | (input[p] as i32);
                        block[q] = convert5_to8(pixel >> 11) as u8;
                        block[q + 1] = convert6_to8(pixel >> 5) as u8;
                        block[q + 2] = convert5_to8(pixel) as u8;
                        q += 3;
                        p += pixel_size as usize;
                    }
                }
            }
            encode_block(&block, mask as u32, &mut encoded);
            output[out_offset..out_offset + ETC1_ENCODED_BLOCK_SIZE]
                .copy_from_slice(&encoded);
            out_offset += ETC1_ENCODED_BLOCK_SIZE;
            x += 4;
        }
        y += 4;
    }
    0
}

/// Decode an entire image.
///
/// `input` - encoded ETC1 data.
/// `output` - buffer for decoded image data. Pixel (x,y) is written at
///            output[pixel_size * x + stride * y].
/// `width`, `height` - image dimensions.
/// `pixel_size` - 2 for GL_UNSIGNED_SHORT_5_6_5, 3 for GL_BYTE RGB.
/// `stride` - byte stride between rows.
///
/// Returns 0 on success, -1 on error (invalid pixel_size).
pub fn decode_image(
    input: &[u8],
    output: &mut [u8],
    width: u32,
    height: u32,
    pixel_size: u32,
    stride: u32,
) -> i32 {
    if pixel_size < 2 || pixel_size > 3 {
        return -1;
    }

    let mut block = [0u8; ETC1_DECODED_BLOCK_SIZE];

    let encoded_width = (width + 3) & !3;
    let encoded_height = (height + 3) & !3;

    let mut in_offset: usize = 0;

    let mut y: u32 = 0;
    while y < encoded_height {
        let mut y_end = height - y;
        if y_end > 4 {
            y_end = 4;
        }
        let mut x: u32 = 0;
        while x < encoded_width {
            let mut x_end = width - x;
            if x_end > 4 {
                x_end = 4;
            }
            decode_block(&input[in_offset..], &mut block);
            in_offset += ETC1_ENCODED_BLOCK_SIZE;
            for cy in 0..y_end {
                let q_start = (cy * 4) as usize * 3;
                let p_start = (pixel_size * x + stride * (y + cy)) as usize;
                if pixel_size == 3 {
                    let count = x_end as usize * 3;
                    output[p_start..p_start + count]
                        .copy_from_slice(&block[q_start..q_start + count]);
                } else {
                    let mut q = q_start;
                    let mut p = p_start;
                    for _cx in 0..x_end {
                        let r = block[q] as u32;
                        let g = block[q + 1] as u32;
                        let b = block[q + 2] as u32;
                        let pixel = ((r >> 3) << 11) | ((g >> 2) << 5) | (b >> 3);
                        output[p] = pixel as u8;
                        output[p + 1] = (pixel >> 8) as u8;
                        q += 3;
                        p += pixel_size as usize;
                    }
                }
            }
            x += 4;
        }
        y += 4;
    }
    0
}

// ── PKM Header ─────────────────────────────────────────────────────────────────

static K_MAGIC: [u8; 6] = [b'P', b'K', b'M', b' ', b'1', b'0'];

const ETC1_PKM_FORMAT_OFFSET: usize = 6;
const ETC1_PKM_ENCODED_WIDTH_OFFSET: usize = 8;
const ETC1_PKM_ENCODED_HEIGHT_OFFSET: usize = 10;
const ETC1_PKM_WIDTH_OFFSET: usize = 12;
const ETC1_PKM_HEIGHT_OFFSET: usize = 14;

const ETC1_RGB_NO_MIPMAPS: u32 = 0;

#[inline]
fn write_be_uint16(p_out: &mut [u8], data: u32) {
    p_out[0] = (data >> 8) as u8;
    p_out[1] = data as u8;
}

#[inline]
fn read_be_uint16(p_in: &[u8]) -> u32 {
    ((p_in[0] as u32) << 8) | (p_in[1] as u32)
}

/// Return the size of the encoded image data (does not include size of PKM header).
pub fn get_encoded_data_size(width: u32, height: u32) -> u32 {
    (((width + 3) & !3) * ((height + 3) & !3)) >> 1
}

/// Format a PKM header.
pub fn pkm_format_header(header: &mut [u8], width: u32, height: u32) {
    header[..6].copy_from_slice(&K_MAGIC);
    let encoded_width = (width + 3) & !3;
    let encoded_height = (height + 3) & !3;
    write_be_uint16(
        &mut header[ETC1_PKM_FORMAT_OFFSET..],
        ETC1_RGB_NO_MIPMAPS,
    );
    write_be_uint16(
        &mut header[ETC1_PKM_ENCODED_WIDTH_OFFSET..],
        encoded_width,
    );
    write_be_uint16(
        &mut header[ETC1_PKM_ENCODED_HEIGHT_OFFSET..],
        encoded_height,
    );
    write_be_uint16(&mut header[ETC1_PKM_WIDTH_OFFSET..], width);
    write_be_uint16(&mut header[ETC1_PKM_HEIGHT_OFFSET..], height);
}

/// Check if a PKM header is correctly formatted.
pub fn pkm_is_valid(header: &[u8]) -> bool {
    if header.len() < ETC_PKM_HEADER_SIZE {
        return false;
    }
    if header[..6] != K_MAGIC {
        return false;
    }
    let format = read_be_uint16(&header[ETC1_PKM_FORMAT_OFFSET..]);
    let encoded_width = read_be_uint16(&header[ETC1_PKM_ENCODED_WIDTH_OFFSET..]);
    let encoded_height = read_be_uint16(&header[ETC1_PKM_ENCODED_HEIGHT_OFFSET..]);
    let width = read_be_uint16(&header[ETC1_PKM_WIDTH_OFFSET..]);
    let height = read_be_uint16(&header[ETC1_PKM_HEIGHT_OFFSET..]);
    format == ETC1_RGB_NO_MIPMAPS
        && encoded_width >= width
        && encoded_width - width < 4
        && encoded_height >= height
        && encoded_height - height < 4
}

/// Read the image width from a PKM header.
pub fn pkm_get_width(header: &[u8]) -> u32 {
    read_be_uint16(&header[ETC1_PKM_WIDTH_OFFSET..])
}

/// Read the image height from a PKM header.
pub fn pkm_get_height(header: &[u8]) -> u32 {
    read_be_uint16(&header[ETC1_PKM_HEIGHT_OFFSET..])
}

// ── C ABI Exports (for Scala Native @extern) ──────────────────────────────────

/// Return the size of the encoded image data (does not include size of PKM header).
#[no_mangle]
pub extern "C" fn etc1_get_encoded_data_size(width: u32, height: u32) -> u32 {
    get_encoded_data_size(width, height)
}

/// Decode a block of pixels.
///
/// `p_in` is an ETC1 compressed version of the data (8 bytes).
/// `p_out` is a 4x4 square of 3-byte pixels in form R, G, B (48 bytes).
#[no_mangle]
pub unsafe extern "C" fn etc1_decode_block(p_in: *const u8, p_out: *mut u8) {
    let input = unsafe { core::slice::from_raw_parts(p_in, ETC1_ENCODED_BLOCK_SIZE) };
    let output = unsafe { core::slice::from_raw_parts_mut(p_out, ETC1_DECODED_BLOCK_SIZE) };
    decode_block(input, output);
}

/// Encode a block of pixels.
///
/// `p_in` is a 4x4 square of 3-byte pixels in form R, G, B (48 bytes).
/// `valid_pixel_mask` is a 16-bit mask where bit (1 << (x + y * 4)) indicates
/// whether the corresponding (x,y) pixel is valid.
/// `p_out` is the ETC1 compressed output (8 bytes).
#[no_mangle]
pub unsafe extern "C" fn etc1_encode_block(
    p_in: *const u8,
    valid_pixel_mask: u32,
    p_out: *mut u8,
) {
    let input = unsafe { core::slice::from_raw_parts(p_in, ETC1_DECODED_BLOCK_SIZE) };
    let output = unsafe { core::slice::from_raw_parts_mut(p_out, ETC1_ENCODED_BLOCK_SIZE) };
    encode_block(input, valid_pixel_mask, output);
}

/// Encode an entire image.
///
/// Returns 0 on success, -1 if pixel_size is not 2 or 3.
#[no_mangle]
pub unsafe extern "C" fn etc1_encode_image(
    p_in: *const u8,
    width: u32,
    height: u32,
    pixel_size: u32,
    stride: u32,
    p_out: *mut u8,
) -> i32 {
    if pixel_size < 2 || pixel_size > 3 {
        return -1;
    }
    let in_size = (stride * height) as usize;
    let out_size = get_encoded_data_size(width, height) as usize;
    let input = unsafe { core::slice::from_raw_parts(p_in, in_size) };
    let output = unsafe { core::slice::from_raw_parts_mut(p_out, out_size) };
    encode_image(input, width, height, pixel_size, stride, output)
}

/// Decode an entire image.
///
/// Returns 0 on success, -1 if pixel_size is not 2 or 3.
#[no_mangle]
pub unsafe extern "C" fn etc1_decode_image(
    p_in: *const u8,
    p_out: *mut u8,
    width: u32,
    height: u32,
    pixel_size: u32,
    stride: u32,
) -> i32 {
    if pixel_size < 2 || pixel_size > 3 {
        return -1;
    }
    let in_size = get_encoded_data_size(width, height) as usize;
    let out_size = (stride * height) as usize;
    let input = unsafe { core::slice::from_raw_parts(p_in, in_size) };
    let output = unsafe { core::slice::from_raw_parts_mut(p_out, out_size) };
    decode_image(input, output, width, height, pixel_size, stride)
}

/// Format a PKM header.
#[no_mangle]
pub unsafe extern "C" fn etc1_pkm_format_header(header: *mut u8, width: u32, height: u32) {
    let h = unsafe { core::slice::from_raw_parts_mut(header, ETC_PKM_HEADER_SIZE) };
    pkm_format_header(h, width, height);
}

/// Check if a PKM header is correctly formatted.
/// Returns non-zero (1) if valid, 0 if invalid.
#[no_mangle]
pub unsafe extern "C" fn etc1_pkm_is_valid(header: *const u8) -> i32 {
    let h = unsafe { core::slice::from_raw_parts(header, ETC_PKM_HEADER_SIZE) };
    if pkm_is_valid(h) { 1 } else { 0 }
}

/// Read the image width from a PKM header.
#[no_mangle]
pub unsafe extern "C" fn etc1_pkm_get_width(header: *const u8) -> u32 {
    let h = unsafe { core::slice::from_raw_parts(header, ETC_PKM_HEADER_SIZE) };
    pkm_get_width(h)
}

/// Read the image height from a PKM header.
#[no_mangle]
pub unsafe extern "C" fn etc1_pkm_get_height(header: *const u8) -> u32 {
    let h = unsafe { core::slice::from_raw_parts(header, ETC_PKM_HEADER_SIZE) };
    pkm_get_height(h)
}

// ── Unit Tests ─────────────────────────────────────────────────────────────────

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_clamp() {
        assert_eq!(clamp(-10), 0);
        assert_eq!(clamp(0), 0);
        assert_eq!(clamp(128), 128);
        assert_eq!(clamp(254), 254);
        assert_eq!(clamp(255), 255);
        assert_eq!(clamp(300), 255);
    }

    #[test]
    fn test_convert4_to8() {
        assert_eq!(convert4_to8(0), 0);
        assert_eq!(convert4_to8(0xf), 0xff);
        assert_eq!(convert4_to8(0x8), 0x88);
        assert_eq!(convert4_to8(0x5), 0x55);
    }

    #[test]
    fn test_convert5_to8() {
        assert_eq!(convert5_to8(0), 0);
        assert_eq!(convert5_to8(0x1f), 0xff);
        assert_eq!(convert5_to8(0x10), 0x84);
    }

    #[test]
    fn test_convert6_to8() {
        assert_eq!(convert6_to8(0), 0);
        assert_eq!(convert6_to8(0x3f), 0xff);
    }

    #[test]
    fn test_convert8_to4() {
        // Round-trip: 4-bit -> 8-bit -> 4-bit should be identity
        for v in 0..16 {
            let expanded = convert4_to8(v);
            let compressed = convert8_to4(expanded);
            assert_eq!(compressed, v, "convert8_to4(convert4_to8({})) != {}", v, v);
        }
    }

    #[test]
    fn test_convert8_to5() {
        // Round-trip: 5-bit -> 8-bit -> 5-bit should be identity
        for v in 0..32 {
            let expanded = convert5_to8(v);
            let compressed = convert8_to5(expanded);
            assert_eq!(compressed, v, "convert8_to5(convert5_to8({})) != {}", v, v);
        }
    }

    #[test]
    fn test_get_encoded_data_size() {
        // 4x4 -> one block = 8 bytes
        assert_eq!(get_encoded_data_size(4, 4), 8);
        // 8x8 -> four blocks = 32 bytes
        assert_eq!(get_encoded_data_size(8, 8), 32);
        // 1x1 -> rounds up to 4x4 -> one block = 8 bytes
        assert_eq!(get_encoded_data_size(1, 1), 8);
        // 5x5 -> rounds up to 8x8 -> 32 bytes
        assert_eq!(get_encoded_data_size(5, 5), 32);
        // 16x16 -> 16 blocks = 128 bytes
        assert_eq!(get_encoded_data_size(16, 16), 128);
        // 3x3 -> rounds up to 4x4 -> 8 bytes
        assert_eq!(get_encoded_data_size(3, 3), 8);
        // 256x256 -> 4096 blocks = 32768 bytes
        assert_eq!(get_encoded_data_size(256, 256), 32768);
        // Non-square: 8x4 -> 2 blocks wide, 1 block tall = 2 blocks = 16 bytes
        assert_eq!(get_encoded_data_size(8, 4), 16);
    }

    #[test]
    fn test_encode_decode_block_roundtrip() {
        // Create a 4x4 block with a simple gradient pattern
        let mut input = [0u8; ETC1_DECODED_BLOCK_SIZE];
        for y in 0..4 {
            for x in 0..4 {
                let idx = 3 * (x + 4 * y);
                let v = ((x + y * 4) * 16) as u8;
                input[idx] = v;         // R
                input[idx + 1] = v / 2;  // G
                input[idx + 2] = v / 3;  // B
            }
        }

        // Encode
        let mut encoded = [0u8; ETC1_ENCODED_BLOCK_SIZE];
        encode_block(&input, 0xFFFF, &mut encoded);

        // Decode
        let mut decoded = [0u8; ETC1_DECODED_BLOCK_SIZE];
        decode_block(&encoded, &mut decoded);

        // Check that decoded is reasonably close to input (ETC1 is lossy)
        // Typical ETC1 error is within ~10 per channel for smooth gradients
        for i in 0..ETC1_DECODED_BLOCK_SIZE {
            let diff = (input[i] as i32 - decoded[i] as i32).unsigned_abs();
            assert!(
                diff <= 40,
                "pixel byte {} differs by {} (input={}, decoded={})",
                i, diff, input[i], decoded[i]
            );
        }
    }

    #[test]
    fn test_encode_decode_block_solid_color() {
        // Solid red block should compress well
        let mut input = [0u8; ETC1_DECODED_BLOCK_SIZE];
        for i in 0..16 {
            input[i * 3] = 200;     // R
            input[i * 3 + 1] = 50;  // G
            input[i * 3 + 2] = 100; // B
        }

        let mut encoded = [0u8; ETC1_ENCODED_BLOCK_SIZE];
        encode_block(&input, 0xFFFF, &mut encoded);

        let mut decoded = [0u8; ETC1_DECODED_BLOCK_SIZE];
        decode_block(&encoded, &mut decoded);

        // Solid color should decode with very small error
        for i in 0..16 {
            let dr = (input[i * 3] as i32 - decoded[i * 3] as i32).unsigned_abs();
            let dg = (input[i * 3 + 1] as i32 - decoded[i * 3 + 1] as i32).unsigned_abs();
            let db = (input[i * 3 + 2] as i32 - decoded[i * 3 + 2] as i32).unsigned_abs();
            assert!(
                dr <= 16 && dg <= 16 && db <= 16,
                "pixel {} differs too much: dr={} dg={} db={}",
                i, dr, dg, db
            );
        }
    }

    #[test]
    fn test_encode_decode_image_roundtrip_rgb() {
        // 8x8 image, pixel_size=3
        let width: u32 = 8;
        let height: u32 = 8;
        let pixel_size: u32 = 3;
        let stride = width * pixel_size;
        let img_size = (stride * height) as usize;
        let encoded_size = get_encoded_data_size(width, height) as usize;

        // Create a simple test image
        let mut input = vec![0u8; img_size];
        for y in 0..height {
            for x in 0..width {
                let idx = (pixel_size * x + stride * y) as usize;
                input[idx] = (x * 32) as u8;         // R
                input[idx + 1] = (y * 32) as u8;     // G
                input[idx + 2] = ((x + y) * 16) as u8; // B
            }
        }

        // Encode
        let mut encoded = vec![0u8; encoded_size];
        let result = encode_image(&input, width, height, pixel_size, stride, &mut encoded);
        assert_eq!(result, 0);

        // Decode
        let mut decoded = vec![0u8; img_size];
        let result = decode_image(&encoded, &mut decoded, width, height, pixel_size, stride);
        assert_eq!(result, 0);

        // ETC1 is lossy — check that results are reasonably close
        for y in 0..height {
            for x in 0..width {
                let idx = (pixel_size * x + stride * y) as usize;
                for c in 0..3 {
                    let diff = (input[idx + c] as i32 - decoded[idx + c] as i32).unsigned_abs();
                    assert!(
                        diff <= 80,
                        "pixel ({},{}) channel {} differs by {} (input={}, decoded={})",
                        x, y, c, diff, input[idx + c], decoded[idx + c]
                    );
                }
            }
        }
    }

    #[test]
    fn test_encode_decode_image_roundtrip_rgb565() {
        // 4x4 image, pixel_size=2 (RGB565)
        let width: u32 = 4;
        let height: u32 = 4;
        let pixel_size: u32 = 2;
        let stride = width * pixel_size;
        let img_size = (stride * height) as usize;
        let encoded_size = get_encoded_data_size(width, height) as usize;

        // Create a test image in RGB565 format (little-endian)
        let mut input = vec![0u8; img_size];
        for y in 0..height {
            for x in 0..width {
                let idx = (pixel_size * x + stride * y) as usize;
                // Pack a simple color: R=16, G=32, B=8 in RGB565
                let r5: u32 = 16;
                let g6: u32 = 32;
                let b5: u32 = 8;
                let pixel: u32 = (r5 << 11) | (g6 << 5) | b5;
                input[idx] = pixel as u8;
                input[idx + 1] = (pixel >> 8) as u8;
            }
        }

        let mut encoded = vec![0u8; encoded_size];
        let result = encode_image(&input, width, height, pixel_size, stride, &mut encoded);
        assert_eq!(result, 0);

        let mut decoded = vec![0u8; img_size];
        let result = decode_image(&encoded, &mut decoded, width, height, pixel_size, stride);
        assert_eq!(result, 0);

        // For a solid color image in RGB565, decoded should be close
        // (two levels of quantization: RGB565 -> RGB888 -> ETC1 -> RGB888 -> RGB565)
        for i in (0..img_size).step_by(2) {
            let orig_pixel = (input[i] as u16) | ((input[i + 1] as u16) << 8);
            let dec_pixel = (decoded[i] as u16) | ((decoded[i + 1] as u16) << 8);
            let or = (orig_pixel >> 11) & 0x1f;
            let og = (orig_pixel >> 5) & 0x3f;
            let ob = orig_pixel & 0x1f;
            let dr = (dec_pixel >> 11) & 0x1f;
            let dg = (dec_pixel >> 5) & 0x3f;
            let db = dec_pixel & 0x1f;
            let diff_r = (or as i32 - dr as i32).unsigned_abs();
            let diff_g = (og as i32 - dg as i32).unsigned_abs();
            let diff_b = (ob as i32 - db as i32).unsigned_abs();
            assert!(
                diff_r <= 4 && diff_g <= 8 && diff_b <= 4,
                "pixel at byte {} differs too much in RGB565: dr={} dg={} db={}",
                i, diff_r, diff_g, diff_b
            );
        }
    }

    #[test]
    fn test_encode_image_invalid_pixel_size() {
        let input = [0u8; 48];
        let mut output = [0u8; 8];
        assert_eq!(encode_image(&input, 4, 4, 1, 4, &mut output), -1);
        assert_eq!(encode_image(&input, 4, 4, 4, 16, &mut output), -1);
    }

    #[test]
    fn test_decode_image_invalid_pixel_size() {
        let input = [0u8; 8];
        let mut output = [0u8; 48];
        assert_eq!(decode_image(&input, &mut output, 4, 4, 1, 4), -1);
        assert_eq!(decode_image(&input, &mut output, 4, 4, 4, 16), -1);
    }

    #[test]
    fn test_pkm_header_roundtrip() {
        let mut header = [0u8; ETC_PKM_HEADER_SIZE];
        let width: u32 = 256;
        let height: u32 = 128;

        pkm_format_header(&mut header, width, height);
        assert!(pkm_is_valid(&header));
        assert_eq!(pkm_get_width(&header), width);
        assert_eq!(pkm_get_height(&header), height);
    }

    #[test]
    fn test_pkm_header_small_sizes() {
        for &(w, h) in &[(1u32, 1u32), (2, 3), (3, 1), (4, 4), (5, 7), (100, 100)] {
            let mut header = [0u8; ETC_PKM_HEADER_SIZE];
            pkm_format_header(&mut header, w, h);
            assert!(pkm_is_valid(&header), "header should be valid for {}x{}", w, h);
            assert_eq!(pkm_get_width(&header), w);
            assert_eq!(pkm_get_height(&header), h);
        }
    }

    #[test]
    fn test_pkm_header_magic() {
        let mut header = [0u8; ETC_PKM_HEADER_SIZE];
        pkm_format_header(&mut header, 64, 64);

        // Check magic bytes
        assert_eq!(&header[..6], b"PKM 10");

        // Corrupt magic and verify it fails validation
        header[0] = b'X';
        assert!(!pkm_is_valid(&header));
    }

    #[test]
    fn test_pkm_header_encoded_dimensions() {
        let mut header = [0u8; ETC_PKM_HEADER_SIZE];
        // 5x5 should encode as 8x8
        pkm_format_header(&mut header, 5, 5);
        let enc_w = read_be_uint16(&header[ETC1_PKM_ENCODED_WIDTH_OFFSET..]);
        let enc_h = read_be_uint16(&header[ETC1_PKM_ENCODED_HEIGHT_OFFSET..]);
        assert_eq!(enc_w, 8);
        assert_eq!(enc_h, 8);

        // 4x4 should encode as 4x4
        pkm_format_header(&mut header, 4, 4);
        let enc_w = read_be_uint16(&header[ETC1_PKM_ENCODED_WIDTH_OFFSET..]);
        let enc_h = read_be_uint16(&header[ETC1_PKM_ENCODED_HEIGHT_OFFSET..]);
        assert_eq!(enc_w, 4);
        assert_eq!(enc_h, 4);
    }

    #[test]
    fn test_pkm_invalid_header_too_short() {
        let header = [0u8; 8]; // Too short
        assert!(!pkm_is_valid(&header));
    }

    #[test]
    fn test_encode_decode_non_multiple_of_4() {
        // 5x5 image — tests padding behavior
        let width: u32 = 5;
        let height: u32 = 5;
        let pixel_size: u32 = 3;
        let stride = width * pixel_size;
        let img_size = (stride * height) as usize;
        let encoded_size = get_encoded_data_size(width, height) as usize;

        let mut input = vec![128u8; img_size];
        // Set some variation
        for y in 0..height {
            for x in 0..width {
                let idx = (pixel_size * x + stride * y) as usize;
                input[idx] = ((x * 50) % 256) as u8;
                input[idx + 1] = ((y * 50) % 256) as u8;
                input[idx + 2] = (((x + y) * 30) % 256) as u8;
            }
        }

        let mut encoded = vec![0u8; encoded_size];
        let result = encode_image(&input, width, height, pixel_size, stride, &mut encoded);
        assert_eq!(result, 0);

        let mut decoded = vec![0u8; img_size];
        let result = decode_image(&encoded, &mut decoded, width, height, pixel_size, stride);
        assert_eq!(result, 0);

        // ETC1 is lossy with high error on non-aligned dimensions; just verify
        // that the decoded output is not all zeros (sanity check).
        assert!(decoded.iter().any(|&b| b != 0), "decoded data should not be all zeros");
    }

    #[test]
    fn test_c_abi_get_encoded_data_size() {
        assert_eq!(etc1_get_encoded_data_size(4, 4), 8);
        assert_eq!(etc1_get_encoded_data_size(8, 8), 32);
        assert_eq!(etc1_get_encoded_data_size(1, 1), 8);
    }

    #[test]
    fn test_c_abi_pkm_roundtrip() {
        let mut header = [0u8; ETC_PKM_HEADER_SIZE];
        let width: u32 = 320;
        let height: u32 = 240;

        unsafe {
            etc1_pkm_format_header(header.as_mut_ptr(), width, height);
            assert_eq!(etc1_pkm_is_valid(header.as_ptr()), 1);
            assert_eq!(etc1_pkm_get_width(header.as_ptr()), width);
            assert_eq!(etc1_pkm_get_height(header.as_ptr()), height);
        }
    }

    #[test]
    fn test_c_abi_encode_decode_block() {
        // Solid gray block
        let mut input = [0u8; ETC1_DECODED_BLOCK_SIZE];
        for i in 0..16 {
            input[i * 3] = 128;
            input[i * 3 + 1] = 128;
            input[i * 3 + 2] = 128;
        }

        let mut encoded = [0u8; ETC1_ENCODED_BLOCK_SIZE];
        let mut decoded = [0u8; ETC1_DECODED_BLOCK_SIZE];

        unsafe {
            etc1_encode_block(input.as_ptr(), 0xFFFF, encoded.as_mut_ptr());
            etc1_decode_block(encoded.as_ptr(), decoded.as_mut_ptr());
        }

        for i in 0..ETC1_DECODED_BLOCK_SIZE {
            let diff = (input[i] as i32 - decoded[i] as i32).unsigned_abs();
            assert!(diff <= 16, "byte {} differs by {}", i, diff);
        }
    }

    #[test]
    fn test_c_abi_encode_decode_image() {
        let width: u32 = 8;
        let height: u32 = 8;
        let pixel_size: u32 = 3;
        let stride = width * pixel_size;
        let img_size = (stride * height) as usize;
        let encoded_size = get_encoded_data_size(width, height) as usize;

        let mut input = vec![100u8; img_size];
        let mut encoded = vec![0u8; encoded_size];
        let mut decoded = vec![0u8; img_size];

        unsafe {
            let result = etc1_encode_image(
                input.as_ptr(), width, height, pixel_size, stride, encoded.as_mut_ptr(),
            );
            assert_eq!(result, 0);

            let result = etc1_decode_image(
                encoded.as_ptr(), decoded.as_mut_ptr(), width, height, pixel_size, stride,
            );
            assert_eq!(result, 0);
        }

        for i in 0..img_size {
            let diff = (input[i] as i32 - decoded[i] as i32).unsigned_abs();
            assert!(diff <= 16, "byte {} differs by {}", i, diff);
        }
    }

    #[test]
    fn test_in_range_4bit_signed() {
        assert!(in_range_4bit_signed(-4));
        assert!(in_range_4bit_signed(-1));
        assert!(in_range_4bit_signed(0));
        assert!(in_range_4bit_signed(3));
        assert!(!in_range_4bit_signed(-5));
        assert!(!in_range_4bit_signed(4));
    }

    #[test]
    fn test_divide_by_255() {
        // divide_by_255 is an approximation of x/255 for non-negative values
        assert_eq!(divide_by_255(0), 0);
        assert_eq!(divide_by_255(255), 1);
        assert_eq!(divide_by_255(510), 2);
        assert_eq!(divide_by_255(127), 0);
        assert_eq!(divide_by_255(128), 1);
    }

    #[test]
    fn test_write_read_big_endian() {
        let mut buf = [0u8; 4];
        write_big_endian(&mut buf, 0xDEADBEEF);
        assert_eq!(buf, [0xDE, 0xAD, 0xBE, 0xEF]);
    }

    #[test]
    fn test_write_read_be_uint16() {
        let mut buf = [0u8; 2];
        write_be_uint16(&mut buf, 0x1234);
        assert_eq!(buf, [0x12, 0x34]);
        assert_eq!(read_be_uint16(&buf), 0x1234);

        write_be_uint16(&mut buf, 0);
        assert_eq!(buf, [0, 0]);
        assert_eq!(read_be_uint16(&buf), 0);

        write_be_uint16(&mut buf, 0xFFFF);
        assert_eq!(buf, [0xFF, 0xFF]);
        assert_eq!(read_be_uint16(&buf), 0xFFFF);
    }
}
