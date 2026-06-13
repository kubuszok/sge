// java.util.zip.DataFormatException for Scala.js.
//
// Scala.js's javalib does not provide java.util.zip; SGE supplies the exception
// type thrown by the pure-Scala Inflater on malformed compressed data
// (ISS-533 / ISS-651), matching java.util.zip.DataFormatException.
package java.util.zip

class DataFormatException(message: String) extends Exception(message)
