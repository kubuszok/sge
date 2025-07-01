package sge
package utils

enum SdeError extends Exception {
  case FileReadError(file: files.FileHandle, message: String, cause: Option[Throwable] = None)
  case FileWriteError(file: files.FileHandle, message: String, cause: Option[Throwable] = None)
  case MathError(message: String, cause: Option[Throwable] = None)
  case NetworkError(message: String, cause: Option[Throwable] = None)
  case SerializationError(message: String, cause: Option[Throwable] = None)
  case InvalidInput(message: String, cause: Option[Throwable] = None)
}
