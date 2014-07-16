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
 * @author Christos KK Loverdos <loverdos@gmail.com>
 */
trait ImageTransformer {
  def canTransform(formatOpt: Option[String], file: File): Boolean
  def transform(log: Logger, registry: ImageTransformers, formatOpt: Option[String], file: File): Option[File]
}

abstract class ImageTransformerSkeleton extends ImageTransformer {
  final def canTransform(formatOpt: Option[String], file: File): Boolean = {
    val extension = Sys.fileExtension(file).toLowerCase(Locale.ENGLISH)
    val lowerFormatOpt = formatOpt.map(_.toLowerCase(Locale.ENGLISH))
    canTransformImpl(lowerFormatOpt, extension, file)
  }


  final def transform(log: Logger, registry: ImageTransformers, formatOpt: Option[String], file: File): Option[File] = {
    if(!canTransform(formatOpt, file))
      None
    else {
      val extension = Sys.fileExtension(file).toLowerCase(Locale.ENGLISH)
      transformImpl(log, registry, formatOpt, extension, file)
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
