/*
 * Stub implementations for GLFW native window handle functions that are
 * unavailable on the current platform.
 *
 * Scala Native requires ALL @extern symbols to be resolvable at link time,
 * even if guarded by runtime platform checks. These stubs satisfy the linker
 * on platforms where the real implementations don't exist.
 *
 * The _GLFW_COCOA / _GLFW_X11 / _GLFW_WIN32 defines are set by build.rs
 * based on the cargo target OS, matching the GLFW build configuration.
 */

#include <stddef.h>

#if !defined(_GLFW_COCOA)
void* glfwGetCocoaWindow(void* window) {
    (void)window;
    return NULL;
}
#endif

#if !defined(_GLFW_X11)
unsigned long long glfwGetX11Window(void* window) {
    (void)window;
    return 0;
}
#endif

#if !defined(_GLFW_WIN32)
void* glfwGetWin32Window(void* window) {
    (void)window;
    return NULL;
}
#endif
