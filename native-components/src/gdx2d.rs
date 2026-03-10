// Image decoding via the `image` crate (pure Rust — PNG, JPEG, BMP)
//
// C ABI functions for Scala Native @extern:
//   sge_image_decode  — decode encoded bytes into raw pixels
//   sge_image_free    — free a decoded image result
//   sge_image_failure — get last failure reason (null-terminated C string)
//
// The result struct uses format codes matching Gdx2DPixmap:
//   3 = RGB888  (3 bytes per pixel)
//   4 = RGBA8888 (4 bytes per pixel)

use std::cell::RefCell;

/// Result of decoding an image, returned as a heap-allocated C struct.
#[repr(C)]
pub struct SgeImageResult {
    pub width: i32,
    pub height: i32,
    /// Pixel format: 3 = RGB888, 4 = RGBA8888
    pub format: i32,
    /// Pointer to raw pixel data (owned — freed by sge_image_free)
    pub pixels: *mut u8,
    /// Size of the pixel data in bytes (width * height * format)
    pub pixel_size: i32,
}

thread_local! {
    static LAST_ERROR: RefCell<String> = RefCell::new(String::from("No error"));
}

fn set_error(msg: String) {
    LAST_ERROR.with(|e| *e.borrow_mut() = msg);
}

/// Decode encoded image bytes (PNG/JPEG/BMP) into raw pixel data.
///
/// Returns a pointer to an `SgeImageResult`, or null on failure.
/// On failure, call `sge_image_failure` for the reason string.
/// On success, caller must free with `sge_image_free`.
pub fn decode_image(data: &[u8]) -> Option<Box<SgeImageResult>> {
    match image::load_from_memory(data) {
        Ok(img) => {
            // Detect if the image has an alpha channel
            let has_alpha = img.color().has_alpha();
            if has_alpha {
                let rgba = img.into_rgba8();
                let width = rgba.width() as i32;
                let height = rgba.height() as i32;
                let mut raw = rgba.into_raw(); // Vec<u8>
                let pixel_size = (width * height * 4) as i32;
                let pixels = raw.as_mut_ptr();
                std::mem::forget(raw); // Ownership transferred to C caller
                Some(Box::new(SgeImageResult {
                    width,
                    height,
                    format: 4, // RGBA8888
                    pixels,
                    pixel_size,
                }))
            } else {
                let rgb = img.into_rgb8();
                let width = rgb.width() as i32;
                let height = rgb.height() as i32;
                let mut raw = rgb.into_raw();
                let pixel_size = (width * height * 3) as i32;
                let pixels = raw.as_mut_ptr();
                std::mem::forget(raw);
                Some(Box::new(SgeImageResult {
                    width,
                    height,
                    format: 3, // RGB888
                    pixels,
                    pixel_size,
                }))
            }
        }
        Err(e) => {
            set_error(format!("Image decode failed: {e}"));
            None
        }
    }
}

// ---------------------------------------------------------------------------
// C ABI exports (for Scala Native @extern and JVM Panama FFM)
// ---------------------------------------------------------------------------

/// Decode encoded image bytes into raw pixels.
///
/// # Safety
///
/// `data` must point to at least `offset + len` bytes of valid memory.
/// Returns a pointer to an SgeImageResult, or null on failure.
/// Caller must free the result with `sge_image_free`.
#[no_mangle]
pub unsafe extern "C" fn sge_image_decode(
    data: *const u8,
    offset: i32,
    len: i32,
) -> *mut SgeImageResult {
    if data.is_null() || len <= 0 {
        set_error("Null data pointer or non-positive length".to_string());
        return std::ptr::null_mut();
    }
    let slice = unsafe {
        core::slice::from_raw_parts(data.add(offset as usize), len as usize)
    };
    match decode_image(slice) {
        Some(result) => Box::into_raw(result),
        None => std::ptr::null_mut(),
    }
}

/// Free a previously decoded image result.
///
/// # Safety
///
/// `result` must be a pointer returned by `sge_image_decode`, or null (no-op).
#[no_mangle]
pub unsafe extern "C" fn sge_image_free(result: *mut SgeImageResult) {
    if !result.is_null() {
        let boxed = unsafe { Box::from_raw(result) };
        if !boxed.pixels.is_null() && boxed.pixel_size > 0 {
            // Reconstruct the Vec to free the pixel data
            let _ = unsafe {
                Vec::from_raw_parts(boxed.pixels, boxed.pixel_size as usize, boxed.pixel_size as usize)
            };
        }
        // boxed is dropped here, freeing the SgeImageResult struct
    }
}

/// Get the failure reason from the last failed decode call.
/// Returns a pointer to a null-terminated C string. The string is valid
/// until the next call to `sge_image_decode` on the same thread.
///
/// # Safety
///
/// The returned pointer is valid only until the next decode call.
#[no_mangle]
pub unsafe extern "C" fn sge_image_failure() -> *const libc::c_char {
    LAST_ERROR.with(|e| {
        let s = e.borrow();
        // We need a null-terminated string that outlives this closure.
        // Use a thread-local CString buffer.
        thread_local! {
            static BUF: RefCell<std::ffi::CString> = RefCell::new(
                std::ffi::CString::new("No error").unwrap()
            );
        }
        BUF.with(|buf| {
            *buf.borrow_mut() = std::ffi::CString::new(s.as_str()).unwrap_or_else(|_| {
                std::ffi::CString::new("Unknown error").unwrap()
            });
            buf.borrow().as_ptr()
        })
    })
}

// ---------------------------------------------------------------------------
// Tests
// ---------------------------------------------------------------------------

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn decode_invalid_data_returns_none() {
        let garbage = vec![0u8, 1, 2, 3, 4, 5];
        let result = decode_image(&garbage);
        assert!(result.is_none());
    }

    #[test]
    fn decode_empty_returns_none() {
        let result = decode_image(&[]);
        assert!(result.is_none());
    }

    // Minimal valid 1x1 red PNG (67 bytes)
    fn tiny_red_png() -> Vec<u8> {
        // 1x1 RGBA PNG: red pixel (255, 0, 0, 255)
        let mut png_data = Vec::new();
        let encoder = image::codecs::png::PngEncoder::new(&mut png_data);
        use image::ImageEncoder;
        encoder.write_image(
            &[255, 0, 0, 255], // RGBA red pixel
            1, 1,
            image::ExtendedColorType::Rgba8,
        ).unwrap();
        png_data
    }

    #[test]
    fn decode_tiny_png() {
        let data = tiny_red_png();
        let result = decode_image(&data).expect("should decode");
        assert_eq!(result.width, 1);
        assert_eq!(result.height, 1);
        assert_eq!(result.format, 4); // RGBA8888
        assert_eq!(result.pixel_size, 4);

        // Check pixel data
        let pixels = unsafe { core::slice::from_raw_parts(result.pixels, 4) };
        assert_eq!(pixels[0], 255); // R
        assert_eq!(pixels[1], 0);   // G
        assert_eq!(pixels[2], 0);   // B
        assert_eq!(pixels[3], 255); // A

        // Clean up
        unsafe {
            let _ = Vec::from_raw_parts(result.pixels, 4, 4);
        }
    }

    #[test]
    fn c_abi_null_data_returns_null() {
        let result = unsafe { sge_image_decode(std::ptr::null(), 0, 0) };
        assert!(result.is_null());
    }

    #[test]
    fn c_abi_decode_and_free() {
        let data = tiny_red_png();
        let result = unsafe { sge_image_decode(data.as_ptr(), 0, data.len() as i32) };
        assert!(!result.is_null());

        let r = unsafe { &*result };
        assert_eq!(r.width, 1);
        assert_eq!(r.height, 1);
        assert_eq!(r.format, 4);

        // Free should not crash
        unsafe { sge_image_free(result) };
    }

    #[test]
    fn c_abi_free_null_is_noop() {
        // Should not crash
        unsafe { sge_image_free(std::ptr::null_mut()) };
    }
}
