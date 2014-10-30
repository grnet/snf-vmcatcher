/*
 * Copyright (C) 2014 GRNET S.A.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package gr.grnet.egi.vmcatcher.image

import java.io.File
import java.util.Locale

import gr.grnet.egi.vmcatcher.Sys
import org.slf4j.Logger

/**
 * Transform an image to raw format, so that `snf-image-creator` can pick it up.
 *
 * There are two parameters that govern the selection of the appropriate transformer:
 * a) `formatOpt`, which is an optional format and b) `file`, which is the actual file we
 * want to transform. By design, the `formatOpt`, if defined, takes precedence. The rationale
 * for this precedence rule is that when an image is described in an image list, a field called
 * `hv:format` indicates the format, so this should be preferred over the filename. Our current usage
 * scenarios/patterns indicate that the `hv:format` field resembles a file extension (e.g. `.cpio.gz`),
 * so an implementation can rely on this fact and unify the handling of both `formatOpt` and `file` should
 * the filename extension of `file` be taken into account.
 *
 */
trait ImageTransformer {
  def canTransform(file: File): Boolean = {
    val extension = Sys.fileExtension(file).toLowerCase(Locale.ENGLISH)
    canTransform(extension)
  }

  // The file extension takes priority over the optionally provided format
  def canTransform(formatOpt: Option[String], file: File): Boolean =
    if(canTransform(file)) {
      true
    }
    else {
      formatOpt match {
        case None ⇒ false
        case Some(format) ⇒ canTransform(format)
      }
    }

  protected def canTransformImpl(format0: String): Boolean

  def canTransform(format0: String): Boolean = {
    val format = Sys.fixFormat(format0)
    canTransformImpl(format)
  }

  def transform(log: Logger, registry: ImageTransformers, formatOpt: Option[String], file: File): Option[File] = {
    val extension = Sys.fileExtension(file).toLowerCase(Locale.ENGLISH)
    if(canTransform(extension)) {
      transform(log, registry, extension, file)
    }
    else {
      formatOpt match {
        case None ⇒
          None

        case Some(format0) ⇒
          val format = Sys.fixFormat(format0)
          if(canTransform(format)) {
            transform(log, registry, format, file)
          }
          else {
            None
          }
      }
    }
  }

  def transform(log: Logger, registry: ImageTransformers, file: File): Option[File] =
    this.transform(log, registry, None, file)

  def transform(log: Logger, registry: ImageTransformers, format: String, file: File): Option[File]

  def suggestTransformedFilename(formatOpt: Option[String], filename: String): String =
    Sys.dropFileExtension(formatOpt, filename)

  def suggestTransformedFilename(formatOpt: Option[String], file: File): String =
    this.suggestTransformedFilename(formatOpt, file.getName)

  override def toString: String = getClass.getSimpleName
}
