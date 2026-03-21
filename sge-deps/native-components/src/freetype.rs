// FreeType bindings — wraps freetype-rs crate for font rasterization
//
// Provides C ABI functions for:
//   - Desktop JVM via Panama FFM (java.lang.foreign)
//   - Scala Native via @extern
//
// All public functions are prefixed with sge_ft_ to avoid symbol collisions.
// Opaque handles are returned as *mut c_void (usize on Scala side).

use std::ffi::c_void;
use std::ptr;
use std::slice;
use std::sync::Mutex;

use freetype::freetype as ft;

// ---------------------------------------------------------------------------
// Manual FFI declarations for glyph/stroker types not exported by freetype-sys
// ---------------------------------------------------------------------------

#[allow(non_camel_case_types)]
type FT_Glyph = *mut FT_GlyphRec;
#[allow(non_camel_case_types)]
type FT_BitmapGlyph = *mut FT_BitmapGlyphRec;
#[allow(non_camel_case_types)]
type FT_Stroker = *mut c_void;
#[allow(non_camel_case_types)]
type FT_Stroker_LineCap = i32;
#[allow(non_camel_case_types)]
type FT_Stroker_LineJoin = i32;

#[repr(C)]
#[allow(non_camel_case_types)]
struct FT_GlyphRec {
    _library: ft::FT_Library,
    _clazz: *mut c_void,
    _format: i32, // FT_Glyph_Format
    _advance: ft::FT_Vector,
}

#[repr(C)]
#[allow(non_camel_case_types)]
struct FT_BitmapGlyphRec {
    _root: FT_GlyphRec,
    left: ft::FT_Int,
    top: ft::FT_Int,
    bitmap: ft::FT_Bitmap,
}

extern "C" {
    fn FT_Get_Glyph(slot: ft::FT_GlyphSlot, aglyph: *mut FT_Glyph) -> ft::FT_Error;
    fn FT_Done_Glyph(glyph: FT_Glyph);
    fn FT_Glyph_StrokeBorder(
        pglyph: *mut FT_Glyph,
        stroker: FT_Stroker,
        inside: ft::FT_Bool,
        destroy: ft::FT_Bool,
    ) -> ft::FT_Error;
    fn FT_Glyph_To_Bitmap(
        the_glyph: *mut FT_Glyph,
        render_mode: i32,
        origin: *const ft::FT_Vector,
        destroy: ft::FT_Bool,
    ) -> ft::FT_Error;
    fn FT_Stroker_New(
        library: ft::FT_Library,
        astroker: *mut FT_Stroker,
    ) -> ft::FT_Error;
    fn FT_Stroker_Set(
        stroker: FT_Stroker,
        radius: ft::FT_Fixed,
        line_cap: FT_Stroker_LineCap,
        line_join: FT_Stroker_LineJoin,
        miter_limit: ft::FT_Fixed,
    );
    fn FT_Stroker_Done(stroker: FT_Stroker);
}

// ---------------------------------------------------------------------------
// Thread-safe error code storage
// ---------------------------------------------------------------------------

static LAST_ERROR: Mutex<i32> = Mutex::new(0);

fn set_error(code: i32) {
    if let Ok(mut e) = LAST_ERROR.lock() {
        *e = code;
    }
}

fn ft_ok(code: ft::FT_Error) -> bool {
    let ok = code == 0;
    set_error(code as i32);
    ok
}

// ---------------------------------------------------------------------------
// Library lifecycle
// ---------------------------------------------------------------------------

/// Initialize a new FreeType library instance.
/// Returns a handle (pointer) to the FT_Library, or null on failure.
#[no_mangle]
pub unsafe extern "C" fn sge_ft_init_freetype() -> *mut c_void {
    let mut library: ft::FT_Library = ptr::null_mut();
    if ft_ok(ft::FT_Init_FreeType(&mut library)) {
        library as *mut c_void
    } else {
        ptr::null_mut()
    }
}

/// Destroy a FreeType library instance.
#[no_mangle]
pub unsafe extern "C" fn sge_ft_done_freetype(library: *mut c_void) {
    if !library.is_null() {
        ft::FT_Done_FreeType(library as ft::FT_Library);
    }
}

/// Returns the last FreeType error code.
#[no_mangle]
pub extern "C" fn sge_ft_get_last_error_code() -> i32 {
    LAST_ERROR.lock().map(|e| *e).unwrap_or(-1)
}

// ---------------------------------------------------------------------------
// Face lifecycle
// ---------------------------------------------------------------------------

/// Load a font face from memory.
/// `data` must remain valid for the lifetime of the face.
/// Returns a handle to FT_Face, or null on failure.
#[no_mangle]
pub unsafe extern "C" fn sge_ft_new_memory_face(
    library: *mut c_void,
    data: *const u8,
    data_size: i32,
    face_index: i32,
) -> *mut c_void {
    let mut face: ft::FT_Face = ptr::null_mut();
    if ft_ok(ft::FT_New_Memory_Face(
        library as ft::FT_Library,
        data,
        data_size as ft::FT_Long,
        face_index as ft::FT_Long,
        &mut face,
    )) {
        face as *mut c_void
    } else {
        ptr::null_mut()
    }
}

/// Destroy a font face.
#[no_mangle]
pub unsafe extern "C" fn sge_ft_done_face(face: *mut c_void) {
    if !face.is_null() {
        ft::FT_Done_Face(face as ft::FT_Face);
    }
}

// ---------------------------------------------------------------------------
// Face configuration
// ---------------------------------------------------------------------------

#[no_mangle]
pub unsafe extern "C" fn sge_ft_select_size(face: *mut c_void, strike_index: i32) -> i32 {
    let ok = ft_ok(ft::FT_Select_Size(
        face as ft::FT_Face,
        strike_index as ft::FT_Int,
    ));
    ok as i32
}

#[no_mangle]
pub unsafe extern "C" fn sge_ft_set_char_size(
    face: *mut c_void,
    char_width: i32,
    char_height: i32,
    horz_res: i32,
    vert_res: i32,
) -> i32 {
    let ok = ft_ok(ft::FT_Set_Char_Size(
        face as ft::FT_Face,
        char_width as ft::FT_F26Dot6,
        char_height as ft::FT_F26Dot6,
        horz_res as ft::FT_UInt,
        vert_res as ft::FT_UInt,
    ));
    ok as i32
}

#[no_mangle]
pub unsafe extern "C" fn sge_ft_set_pixel_sizes(
    face: *mut c_void,
    pixel_width: i32,
    pixel_height: i32,
) -> i32 {
    let ok = ft_ok(ft::FT_Set_Pixel_Sizes(
        face as ft::FT_Face,
        pixel_width as ft::FT_UInt,
        pixel_height as ft::FT_UInt,
    ));
    ok as i32
}

// ---------------------------------------------------------------------------
// Face metrics
// ---------------------------------------------------------------------------

#[no_mangle]
pub unsafe extern "C" fn sge_ft_get_face_flags(face: *mut c_void) -> i32 {
    let f = face as ft::FT_Face;
    (*f).face_flags as i32
}

#[no_mangle]
pub unsafe extern "C" fn sge_ft_get_style_flags(face: *mut c_void) -> i32 {
    let f = face as ft::FT_Face;
    (*f).style_flags as i32
}

#[no_mangle]
pub unsafe extern "C" fn sge_ft_get_num_glyphs(face: *mut c_void) -> i32 {
    let f = face as ft::FT_Face;
    (*f).num_glyphs as i32
}

#[no_mangle]
pub unsafe extern "C" fn sge_ft_get_ascender(face: *mut c_void) -> i32 {
    let f = face as ft::FT_Face;
    (*f).ascender as i32
}

#[no_mangle]
pub unsafe extern "C" fn sge_ft_get_descender(face: *mut c_void) -> i32 {
    let f = face as ft::FT_Face;
    (*f).descender as i32
}

#[no_mangle]
pub unsafe extern "C" fn sge_ft_get_height(face: *mut c_void) -> i32 {
    let f = face as ft::FT_Face;
    (*f).height as i32
}

#[no_mangle]
pub unsafe extern "C" fn sge_ft_get_max_advance_width(face: *mut c_void) -> i32 {
    let f = face as ft::FT_Face;
    (*f).max_advance_width as i32
}

#[no_mangle]
pub unsafe extern "C" fn sge_ft_get_max_advance_height(face: *mut c_void) -> i32 {
    let f = face as ft::FT_Face;
    (*f).max_advance_height as i32
}

#[no_mangle]
pub unsafe extern "C" fn sge_ft_get_underline_position(face: *mut c_void) -> i32 {
    let f = face as ft::FT_Face;
    (*f).underline_position as i32
}

#[no_mangle]
pub unsafe extern "C" fn sge_ft_get_underline_thickness(face: *mut c_void) -> i32 {
    let f = face as ft::FT_Face;
    (*f).underline_thickness as i32
}

// ---------------------------------------------------------------------------
// Glyph loading
// ---------------------------------------------------------------------------

#[no_mangle]
pub unsafe extern "C" fn sge_ft_load_glyph(
    face: *mut c_void,
    glyph_index: i32,
    load_flags: i32,
) -> i32 {
    let ok = ft_ok(ft::FT_Load_Glyph(
        face as ft::FT_Face,
        glyph_index as ft::FT_UInt,
        load_flags as ft::FT_Int32,
    ));
    ok as i32
}

#[no_mangle]
pub unsafe extern "C" fn sge_ft_load_char(
    face: *mut c_void,
    char_code: i32,
    load_flags: i32,
) -> i32 {
    let ok = ft_ok(ft::FT_Load_Char(
        face as ft::FT_Face,
        char_code as ft::FT_ULong,
        load_flags as ft::FT_Int32,
    ));
    ok as i32
}

#[no_mangle]
pub unsafe extern "C" fn sge_ft_render_glyph(glyph_slot: *mut c_void, render_mode: i32) -> i32 {
    let ok = ft_ok(ft::FT_Render_Glyph(
        glyph_slot as ft::FT_GlyphSlot,
        std::mem::transmute::<i32, ft::FT_Render_Mode>(render_mode),
    ));
    ok as i32
}

// ---------------------------------------------------------------------------
// Kerning
// ---------------------------------------------------------------------------

#[no_mangle]
pub unsafe extern "C" fn sge_ft_has_kerning(face: *mut c_void) -> i32 {
    let f = face as ft::FT_Face;
    let has = ((*f).face_flags & (ft::FT_FACE_FLAG_KERNING as ft::FT_Long)) != 0;
    has as i32
}

#[no_mangle]
pub unsafe extern "C" fn sge_ft_get_kerning(
    face: *mut c_void,
    left_glyph: i32,
    right_glyph: i32,
    kern_mode: i32,
) -> i32 {
    let mut kerning = ft::FT_Vector { x: 0, y: 0 };
    ft::FT_Get_Kerning(
        face as ft::FT_Face,
        left_glyph as ft::FT_UInt,
        right_glyph as ft::FT_UInt,
        kern_mode as ft::FT_UInt,
        &mut kerning,
    );
    kerning.x as i32
}

#[no_mangle]
pub unsafe extern "C" fn sge_ft_get_char_index(face: *mut c_void, char_code: i32) -> i32 {
    ft::FT_Get_Char_Index(face as ft::FT_Face, char_code as ft::FT_ULong) as i32
}

// ---------------------------------------------------------------------------
// Glyph slot accessors
// ---------------------------------------------------------------------------

#[no_mangle]
pub unsafe extern "C" fn sge_ft_get_glyph_slot(face: *mut c_void) -> *mut c_void {
    let f = face as ft::FT_Face;
    (*f).glyph as *mut c_void
}

/// Fills `out` with [width, height, horiBearingX, horiBearingY, horiAdvance] (5 ints).
#[no_mangle]
pub unsafe extern "C" fn sge_ft_get_glyph_metrics(glyph_slot: *mut c_void, out: *mut i32) {
    let gs = glyph_slot as ft::FT_GlyphSlot;
    let m = &(*gs).metrics;
    let arr = slice::from_raw_parts_mut(out, 5);
    arr[0] = m.width as i32;
    arr[1] = m.height as i32;
    arr[2] = m.horiBearingX as i32;
    arr[3] = m.horiBearingY as i32;
    arr[4] = m.horiAdvance as i32;
}

#[no_mangle]
pub unsafe extern "C" fn sge_ft_get_glyph_linear_hori_advance(glyph_slot: *mut c_void) -> i32 {
    let gs = glyph_slot as ft::FT_GlyphSlot;
    (*gs).linearHoriAdvance as i32
}

#[no_mangle]
pub unsafe extern "C" fn sge_ft_get_glyph_advance_x(glyph_slot: *mut c_void) -> i32 {
    let gs = glyph_slot as ft::FT_GlyphSlot;
    (*gs).advance.x as i32
}

#[no_mangle]
pub unsafe extern "C" fn sge_ft_get_glyph_advance_y(glyph_slot: *mut c_void) -> i32 {
    let gs = glyph_slot as ft::FT_GlyphSlot;
    (*gs).advance.y as i32
}

#[no_mangle]
pub unsafe extern "C" fn sge_ft_get_glyph_format(glyph_slot: *mut c_void) -> i32 {
    let gs = glyph_slot as ft::FT_GlyphSlot;
    (*gs).format as i32
}

// ---------------------------------------------------------------------------
// Bitmap accessors (from glyph slot)
// ---------------------------------------------------------------------------

#[no_mangle]
pub unsafe extern "C" fn sge_ft_get_glyph_bitmap_rows(glyph_slot: *mut c_void) -> i32 {
    let gs = glyph_slot as ft::FT_GlyphSlot;
    (*gs).bitmap.rows as i32
}

#[no_mangle]
pub unsafe extern "C" fn sge_ft_get_glyph_bitmap_width(glyph_slot: *mut c_void) -> i32 {
    let gs = glyph_slot as ft::FT_GlyphSlot;
    (*gs).bitmap.width as i32
}

#[no_mangle]
pub unsafe extern "C" fn sge_ft_get_glyph_bitmap_pitch(glyph_slot: *mut c_void) -> i32 {
    let gs = glyph_slot as ft::FT_GlyphSlot;
    (*gs).bitmap.pitch as i32
}

/// Copies bitmap buffer into `out_buffer`. Caller must ensure `buffer_size` is large enough.
#[no_mangle]
pub unsafe extern "C" fn sge_ft_get_glyph_bitmap_buffer(
    glyph_slot: *mut c_void,
    out_buffer: *mut u8,
    buffer_size: i32,
) {
    let gs = glyph_slot as ft::FT_GlyphSlot;
    let bm = &(*gs).bitmap;
    let src_size = (bm.rows * bm.width) as usize;
    let copy_size = src_size.min(buffer_size as usize);
    if copy_size > 0 && !bm.buffer.is_null() {
        ptr::copy_nonoverlapping(bm.buffer, out_buffer, copy_size);
    }
}

#[no_mangle]
pub unsafe extern "C" fn sge_ft_get_glyph_bitmap_num_gray(glyph_slot: *mut c_void) -> i32 {
    let gs = glyph_slot as ft::FT_GlyphSlot;
    (*gs).bitmap.num_grays as i32
}

#[no_mangle]
pub unsafe extern "C" fn sge_ft_get_glyph_bitmap_pixel_mode(glyph_slot: *mut c_void) -> i32 {
    let gs = glyph_slot as ft::FT_GlyphSlot;
    (*gs).bitmap.pixel_mode as i32
}

#[no_mangle]
pub unsafe extern "C" fn sge_ft_get_glyph_bitmap_left(glyph_slot: *mut c_void) -> i32 {
    let gs = glyph_slot as ft::FT_GlyphSlot;
    (*gs).bitmap_left as i32
}

#[no_mangle]
pub unsafe extern "C" fn sge_ft_get_glyph_bitmap_top(glyph_slot: *mut c_void) -> i32 {
    let gs = glyph_slot as ft::FT_GlyphSlot;
    (*gs).bitmap_top as i32
}

// ---------------------------------------------------------------------------
// Size metrics
// ---------------------------------------------------------------------------

/// Fills `out` with [xPpem, yPpem, xScale, yScale, ascender, descender, height, maxAdvance] (8 ints).
#[no_mangle]
pub unsafe extern "C" fn sge_ft_get_size_metrics(face: *mut c_void, out: *mut i32) {
    let f = face as ft::FT_Face;
    let size = (*f).size;
    if size.is_null() {
        return;
    }
    let m = &(*size).metrics;
    let arr = slice::from_raw_parts_mut(out, 8);
    arr[0] = m.x_ppem as i32;
    arr[1] = m.y_ppem as i32;
    arr[2] = m.x_scale as i32;
    arr[3] = m.y_scale as i32;
    arr[4] = m.ascender as i32;
    arr[5] = m.descender as i32;
    arr[6] = m.height as i32;
    arr[7] = m.max_advance as i32;
}

// ---------------------------------------------------------------------------
// Stroker
// ---------------------------------------------------------------------------

#[no_mangle]
pub unsafe extern "C" fn sge_ft_stroker_new(library: *mut c_void) -> *mut c_void {
    let mut stroker: FT_Stroker = ptr::null_mut();
    if ft_ok(FT_Stroker_New(
        library as ft::FT_Library,
        &mut stroker,
    )) {
        stroker
    } else {
        ptr::null_mut()
    }
}

#[no_mangle]
pub unsafe extern "C" fn sge_ft_stroker_set(
    stroker: *mut c_void,
    radius: i32,
    line_cap: i32,
    line_join: i32,
    miter_limit: i32,
) {
    FT_Stroker_Set(
        stroker as FT_Stroker,
        radius as ft::FT_Fixed,
        line_cap,
        line_join,
        miter_limit as ft::FT_Fixed,
    );
}

#[no_mangle]
pub unsafe extern "C" fn sge_ft_stroker_done(stroker: *mut c_void) {
    if !stroker.is_null() {
        FT_Stroker_Done(stroker as FT_Stroker);
    }
}

// ---------------------------------------------------------------------------
// Glyph operations (for stroking)
// ---------------------------------------------------------------------------

/// Gets the glyph from the current glyph slot. Returns a FT_Glyph handle.
#[no_mangle]
pub unsafe extern "C" fn sge_ft_get_glyph(glyph_slot: *mut c_void) -> *mut c_void {
    let mut glyph: FT_Glyph = ptr::null_mut();
    if ft_ok(FT_Get_Glyph(
        glyph_slot as ft::FT_GlyphSlot,
        &mut glyph,
    )) {
        glyph as *mut c_void
    } else {
        ptr::null_mut()
    }
}

/// Stroke-border a glyph. Returns a new glyph handle (the input is consumed).
#[no_mangle]
pub unsafe extern "C" fn sge_ft_stroke_border(
    glyph: *mut c_void,
    stroker: *mut c_void,
    inside: i32,
) -> *mut c_void {
    let mut g = glyph as FT_Glyph;
    if ft_ok(FT_Glyph_StrokeBorder(
        &mut g,
        stroker as FT_Stroker,
        if inside != 0 { 1 } else { 0 },
        1, // destroy original
    )) {
        g as *mut c_void
    } else {
        ptr::null_mut()
    }
}

/// Convert a glyph to bitmap. Returns a BitmapGlyph handle.
#[no_mangle]
pub unsafe extern "C" fn sge_ft_glyph_to_bitmap(
    glyph: *mut c_void,
    render_mode: i32,
) -> *mut c_void {
    let mut g = glyph as FT_Glyph;
    if ft_ok(FT_Glyph_To_Bitmap(
        &mut g,
        render_mode,
        ptr::null(),
        1, // destroy original
    )) {
        g as *mut c_void
    } else {
        ptr::null_mut()
    }
}

/// Fills `out` with [rows, width, pitch, numGray, pixelMode, left, top] (7 ints).
#[no_mangle]
pub unsafe extern "C" fn sge_ft_get_bitmap_glyph_bitmap(glyph: *mut c_void, out: *mut i32) {
    let bg = glyph as FT_BitmapGlyph;
    let bm = &(*bg).bitmap;
    let arr = slice::from_raw_parts_mut(out, 7);
    arr[0] = bm.rows as i32;
    arr[1] = bm.width as i32;
    arr[2] = bm.pitch as i32;
    arr[3] = bm.num_grays as i32;
    arr[4] = bm.pixel_mode as i32;
    arr[5] = (*bg).left as i32;
    arr[6] = (*bg).top as i32;
}

/// Copies bitmap glyph buffer into `out_buffer`.
#[no_mangle]
pub unsafe extern "C" fn sge_ft_get_bitmap_glyph_buffer(
    glyph: *mut c_void,
    out_buffer: *mut u8,
    buffer_size: i32,
) {
    let bg = glyph as FT_BitmapGlyph;
    let bm = &(*bg).bitmap;
    let src_size = (bm.rows * bm.width) as usize;
    let copy_size = src_size.min(buffer_size as usize);
    if copy_size > 0 && !bm.buffer.is_null() {
        ptr::copy_nonoverlapping(bm.buffer, out_buffer, copy_size);
    }
}

/// Destroy a glyph.
#[no_mangle]
pub unsafe extern "C" fn sge_ft_done_glyph(glyph: *mut c_void) {
    if !glyph.is_null() {
        FT_Done_Glyph(glyph as FT_Glyph);
    }
}
