package sge.platform;

/** JNI bridge to Rust buffer operations. Loaded via System.loadLibrary("sge_native_ops"). */
public class BufferOpsBridge {
    static { System.loadLibrary("sge_native_ops"); }

    public static native void copyBytes(byte[] src, int srcOffset, byte[] dst, int dstOffset, int numBytes);
    public static native void copyFloats(float[] src, int srcOffset, float[] dst, int dstOffset, int numFloats);

    public static native void transformV4M4(float[] data, int stride, int count, float[] matrix, int offset);
    public static native void transformV3M4(float[] data, int stride, int count, float[] matrix, int offset);
    public static native void transformV2M4(float[] data, int stride, int count, float[] matrix, int offset);
    public static native void transformV3M3(float[] data, int stride, int count, float[] matrix, int offset);
    public static native void transformV2M3(float[] data, int stride, int count, float[] matrix, int offset);

    public static native long find(float[] vertex, int vertexOffset, int stride,
        float[] vertices, int verticesOffset, int numVertices);
    public static native long findEpsilon(float[] vertex, int vertexOffset, int stride,
        float[] vertices, int verticesOffset, int numVertices, float epsilon);

    // Memory management (JVM only — uses native malloc/free)
    public static native void freeMemory(java.nio.ByteBuffer buffer);
    public static native java.nio.ByteBuffer newDisposableByteBuffer(int numBytes);
    public static native long getBufferAddress(java.nio.Buffer buffer);
    public static native void clear(java.nio.ByteBuffer buffer, int numBytes);
}
