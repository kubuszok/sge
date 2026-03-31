/*
 * Ported from VisUI - https://github.com/kotcrab/vis-ui
 * Original authors: Kotcrab
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package visui
package util
package form

import scala.language.implicitConversions

import java.io.File

import sge.files.FileHandle
import sge.scenes.scene2d.ui.Label
import sge.scenes.scene2d.utils.Disableable
import sge.utils.Nullable
import sge.visui.widget.{ VisTextField, VisValidatableTextField }

/** Utility class made for creating input forms that requires inputting various information and that information cannot be wrong. For example user registration form.
  *
  * FormValidator is not GWT compatible, if you need that see [[SimpleFormValidator]].
  * @author
  *   Kotcrab
  */
class FormValidator(targetToDisable: Disableable, messageLabel: Nullable[Label], style: SimpleFormValidator.FormValidatorStyle)(using Sge)
    extends SimpleFormValidator(targetToDisable, messageLabel, style) {

  def this(targetToDisable: Disableable)(using Sge) =
    this(targetToDisable, Nullable.empty, VisUI.getSkin.get[SimpleFormValidator.FormValidatorStyle])

  def this(targetToDisable: Disableable, messageLabel: Label)(using Sge) =
    this(targetToDisable, Nullable(messageLabel), VisUI.getSkin.get[SimpleFormValidator.FormValidatorStyle])

  def this(targetToDisable: Disableable, messageLabel: Label, styleName: String)(using Sge) =
    this(targetToDisable, Nullable(messageLabel), VisUI.getSkin.get[SimpleFormValidator.FormValidatorStyle](styleName))

  /** Validates if absolute path entered in text field points to an existing file. */
  def fileExists(field: VisValidatableTextField, errorMsg: String): FormInputValidator = {
    val validator = new FormValidator.FileExistsValidator(errorMsg)
    field.addValidator(validator)
    add(field)
    validator
  }

  def fileExists(field: VisValidatableTextField, relativeTo: VisTextField, errorMsg: String): FormInputValidator = {
    val validator = new FormValidator.FileExistsValidator(Nullable(relativeTo), errorMsg)
    field.addValidator(validator)
    add(field)
    validator
  }

  def fileExists(field: VisValidatableTextField, relativeTo: VisTextField, errorMsg: String, errorIfRelativeEmpty: Boolean): FormInputValidator = {
    val validator = new FormValidator.FileExistsValidator(Nullable(relativeTo), errorMsg, mustNotExist = false, errorIfRelativeEmpty = errorIfRelativeEmpty)
    field.addValidator(validator)
    add(field)
    validator
  }

  def fileExists(field: VisValidatableTextField, relativeTo: File, errorMsg: String): FormInputValidator = {
    val validator = new FormValidator.FileExistsValidator(Nullable(relativeTo), errorMsg)
    field.addValidator(validator)
    add(field)
    validator
  }

  def fileExists(field: VisValidatableTextField, relativeTo: FileHandle, errorMsg: String)(using Sge): FormInputValidator = {
    val validator = new FormValidator.FileExistsValidator(Nullable(relativeTo.file), errorMsg)
    field.addValidator(validator)
    add(field)
    validator
  }

  /** Validates if relative path entered in text field points to a non existing file. */
  def fileNotExists(field: VisValidatableTextField, errorMsg: String): FormInputValidator = {
    val validator = new FormValidator.FileExistsValidator(errorMsg, mustNotExist = true)
    field.addValidator(validator)
    add(field)
    validator
  }

  def fileNotExists(field: VisValidatableTextField, relativeTo: VisTextField, errorMsg: String): FormInputValidator = {
    val validator = new FormValidator.FileExistsValidator(Nullable(relativeTo), errorMsg, mustNotExist = true)
    field.addValidator(validator)
    add(field)
    validator
  }

  def fileNotExists(field: VisValidatableTextField, relativeTo: File, errorMsg: String): FormInputValidator = {
    val validator = new FormValidator.FileExistsValidator(Nullable(relativeTo), errorMsg, mustNotExist = true)
    field.addValidator(validator)
    add(field)
    validator
  }

  def fileNotExists(field: VisValidatableTextField, relativeTo: FileHandle, errorMsg: String)(using Sge): FormInputValidator = {
    val validator = new FormValidator.FileExistsValidator(Nullable(relativeTo.file), errorMsg, mustNotExist = true)
    field.addValidator(validator)
    add(field)
    validator
  }

  /** Validates if relative path entered in text field points to an existing directory. */
  def directory(field: VisValidatableTextField, errorMsg: String): FormInputValidator = {
    val validator = new FormValidator.DirectoryValidator(errorMsg)
    field.addValidator(validator)
    add(field)
    validator
  }

  /** Validates if relative path entered in text field points to an existing and empty directory. */
  def directoryEmpty(field: VisValidatableTextField, errorMsg: String): FormInputValidator = {
    val validator = new FormValidator.DirectoryContentValidator(errorMsg, mustBeEmpty = true)
    field.addValidator(validator)
    add(field)
    validator
  }

  /** Validates if relative path entered in text field points to an existing and non empty directory. */
  def directoryNotEmpty(field: VisValidatableTextField, errorMsg: String): FormInputValidator = {
    val validator = new FormValidator.DirectoryContentValidator(errorMsg, mustBeEmpty = false)
    field.addValidator(validator)
    add(field)
    validator
  }
}

object FormValidator {

  /** Validates if entered absolute path points to existing directory. */
  class DirectoryValidator(errorMsg: String)(using Sge) extends FormInputValidator(errorMsg) {
    override protected def validate(input: String): Boolean = {
      val file = Sge().files.absolute(input)
      file.exists() && file.isDirectory()
    }
  }

  class DirectoryContentValidator(errorMsg: String, var mustBeEmpty: Boolean)(using Sge) extends FormInputValidator(errorMsg) {
    override protected def validate(input: String): Boolean = {
      val file = Sge().files.absolute(input)
      if (!file.exists() || !file.isDirectory()) false
      else if (mustBeEmpty) file.list().length == 0
      else file.list().length != 0
    }
  }

  class FileExistsValidator(errorMsg: String, var mustNotExist: Boolean = false)(using Sge) extends FormInputValidator(errorMsg) {
    var relativeTo:           Nullable[VisTextField] = Nullable.empty
    var relativeToFile:       Nullable[File]         = Nullable.empty
    var errorIfRelativeEmpty: Boolean                = false

    def this(relativeToField: Nullable[VisTextField], errorMsg: String)(using Sge) = {
      this(errorMsg)
      this.relativeTo = relativeToField
    }

    def this(relativeToField: Nullable[VisTextField], errorMsg: String, mustNotExist: Boolean)(using Sge) = {
      this(errorMsg, mustNotExist)
      this.relativeTo = relativeToField
    }

    def this(relativeToField: Nullable[VisTextField], errorMsg: String, mustNotExist: Boolean, errorIfRelativeEmpty: Boolean)(using Sge) = {
      this(errorMsg, mustNotExist)
      this.relativeTo = relativeToField
      this.errorIfRelativeEmpty = errorIfRelativeEmpty
    }

    def this(relativeToFile: Nullable[File], errorMsg: String)(using Sge) = {
      this(errorMsg)
      this.relativeToFile = relativeToFile
    }

    def this(relativeToFile: Nullable[File], errorMsg: String, mustNotExist: Boolean)(using Sge) = {
      this(errorMsg, mustNotExist)
      this.relativeToFile = relativeToFile
    }

    override protected def validate(input: String): Boolean =
      if (relativeTo.isDefined && relativeTo.get.text.isEmpty && !errorIfRelativeEmpty) true
      else {
        val file: File =
          if (relativeTo.isDefined) {
            new File(relativeTo.get.text, input)
          } else if (relativeToFile.isDefined) {
            new File(relativeToFile.get, input)
          } else {
            new File(input)
          }

        if (mustNotExist) !file.exists()
        else file.exists()
      }
  }
}
