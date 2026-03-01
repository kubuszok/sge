package sge.platform;

/** JNI bridge to Rust ETC1 codec. Loaded via System.loadLibrary("sge_native_ops"). */
public class ETC1Bridge {
    static { System.loadLibrary("sge_native_ops"); }

    public static native int getCompressedDataSize(int width, int height);
    public static native void formatHeader(byte[] header, int offset, int width, int height);
    public static native int getWidthPKM(byte[] header, int offset);
    public static native int getHeightPKM(byte[] header, int offset);
    public static native boolean isValidPKM(byte[] header, int offset);
    public static native void decodeImage(byte[] compressedData, int compressedOffset,
        byte[] decodedData, int decodedOffset, int width, int height, int pixelSize);
    public static native byte[] encodeImage(byte[] imageData, int offset,
        int width, int height, int pixelSize);
    public static native byte[] encodeImagePKM(byte[] imageData, int offset,
        int width, int height, int pixelSize);
}
