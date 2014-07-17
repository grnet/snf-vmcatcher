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

import scala.annotation.tailrec

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
 * @author Christos KK Loverdos <loverdos@gmail.com>
 */
trait ImageTransformer {
  def canTransform(formatOpt: Option[String], file: File): Boolean

  def transform(log: Logger, registry: ImageTransformers, formatOpt: Option[String], file: File): Option[File]

  // returns the new formatOpt and the suggested filename
  def suggestTransformedFilename(formatOpt: Option[String], filename: String): (Option[String], String) =
    formatOpt match {
      case None ⇒
        None → Sys.dropFileExtension(filename)
        
      case Some(format) ⇒
        val newFormat = Sys.dropFileExtension(format)
        val newFilename = Sys.dropFileExtensions(filename) + newFormat
        Some(newFormat) → newFilename
    }

  def suggestTransformedFilename(formatOpt: Option[String], file: File): (Option[String], String) =
    suggestTransformedFilename(formatOpt, file.getName)

  override def toString: String = getClass.getSimpleName
}

abstract class ImageTransformerSkeleton extends ImageTransformer {

  // A format is made to resemble an extension, that is with a preceding dot.
  // See the doc of ImageTransformer
  @tailrec
  final def fixFormat(format: String): String =
    if(format.isEmpty)
      ""
    else if(format.startsWith("."))
      format.toLowerCase(Locale.ENGLISH)
    else
      fixFormat("." + format)

  def fixFormatOpt(formatOpt: Option[String]): Option[String] =
    formatOpt.map(fixFormat)

  final def canTransform(formatOpt: Option[String], file: File): Boolean = {
    val extension = Sys.fileExtension(file).toLowerCase(Locale.ENGLISH)
    val fixedFormatOpt = fixFormatOpt(formatOpt)
    canTransformImpl(fixedFormatOpt, extension, file)
  }

  final def transform(log: Logger, registry: ImageTransformers, formatOpt: Option[String], file: File): Option[File] = {
    if(!canTransform(formatOpt, file))
      None
    else {
      val extension = Sys.fileExtension(file).toLowerCase(Locale.ENGLISH)
      val fixedFormatOpt = fixFormatOpt(formatOpt)
      transformImpl(log, registry, fixedFormatOpt, extension, file)
    }
  }

  protected def canTransformImpl(
    formatOpt: Option[String],
    extension: String,
    file: File
  ): Boolean

  protected def transformImpl(
    log: Logger,
    registry: ImageTransformers,
    formatOpt: Option[String],
    extension: String,
    file: File
  ): Option[File]
}
