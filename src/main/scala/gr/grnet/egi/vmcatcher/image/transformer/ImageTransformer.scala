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

package gr.grnet.egi.vmcatcher.image.transformer

import java.io.File

import gr.grnet.egi.vmcatcher.{Main, Sys}
import org.slf4j.Logger

/**
 * Transform an image to raw format, so that `snf-image-creator` can pick it up.
 *
 * There are two parameters that govern the selection of the appropriate transformer:
 * a) `formatOpt`, which is an optional format and b) `file`, which is the actual file we
 * want to transform. By design, the `file` extension is interpreted as a format and takes
 * precedence over `formatOpt`.
 */
trait ImageTransformer {
  def log: Logger = Main.Log

  protected def canTransformImpl(fixedFormat: String): Boolean

  def canTransform(format: String): Boolean = {
    val fixedFormat = Sys.fixFormat(format)
    canTransformImpl(fixedFormat)
  }

  def transform(registry: ImageTransformers, formatOpt: Option[String], file: File): Option[File] = {
    val myClass = this.getClass.getSimpleName
    val callInfo = s"$myClass.transform($formatOpt, $file)"
    log.info(s"BEGIN $callInfo")

    try {
      val extension = Sys.fixFormat(Sys.fileExtension(file))

      if(ImageTransformers.isRaw(extension)) {
        log.info(s"Is already raw by extension. Returning same file")
        Some(file)
      }
      else if(canTransform(extension)) {
        log.info(s"Transforming by extension '$extension'")
        transformImpl(registry, extension, file)
      }
      else {
        formatOpt match {
          case None ⇒
            log.info(s"No auxiliary format, cannot proceed further, not transformed")
            None

          case Some(format0) ⇒
            val format = Sys.fixFormat(format0)
            log.info(s"Could not transform by extension '$extension', see if can transform by format '$format'")

            if(ImageTransformers.isRaw(format)) {
              log.info(s"Is already in raw format. Returning same file")
              Some(file)
            }
            else if(canTransform(format)) {
              log.info(s"Transforming by format '$format'")
              transformImpl(registry, format, file)
            }
            else {
              log.info(s"Could not transform by extension '$extension' or format '$format'")
              None
            }
        }
      }
    }
    finally log.info(s"END   $callInfo")
  }

  private[image] def transformImpl(registry: ImageTransformers, format: String, file: File): Option[File]

  def suggestTransformedFilename(formatOpt: Option[String], filename: String): String =
    Sys.dropFileExtension(formatOpt, filename)

  def suggestTransformedFilename(formatOpt: Option[String], file: File): String =
    this.suggestTransformedFilename(formatOpt, file.getName)

  override def toString: String = getClass.getSimpleName
}
