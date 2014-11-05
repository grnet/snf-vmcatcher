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

import gr.grnet.egi.vmcatcher.Sys

/**
 *
 */
trait ImageTransformers extends ImageTransformer {
  def transformers: List[ImageTransformer]

  protected def find(format: String): Option[ImageTransformer] =
    transformers.find(_.canTransform(format))

  def find(formatOpt: Option[String], file: File): Option[ImageTransformer] = {
    val myClass = this.getClass.getSimpleName
    val callInfo = s"$myClass.find($formatOpt, $file)"
    log.info(s"BEGIN $callInfo")

    try {
      val extension = Sys.fileExtension(file)
      find(extension) match {
        case None ⇒
          log.info(s"No transformer found from extension '$extension' of file $file")
          formatOpt match {
            case None ⇒
              log.info(s"No transformer found at all, since auxiliary format does not exist")
              None

            case Some(format) ⇒
              log.info(s"Trying to find transformer from auxiliary format '$format'")
              find(format) match {
                case None ⇒
                  log.info(s"No transformer found for format '$format' or extension '$extension'")
                  None
                case some @ Some(transformer) ⇒
                  log.info(s"Found transformer $transformer from format '$format'")
                  some
              }
          }

        case some @ Some(transformer) ⇒
          log.info(s"Found transformer $transformer from extension '$extension' of file $file")
          some
      }
    }
    finally log.info(s"END   $callInfo")
  }

  protected def canTransformImpl(fixedFormat: String): Boolean = find(fixedFormat).isDefined
  
  private[image] def transformImpl(registry: ImageTransformers, format: String, file: File): Option[File] =
    transformImpl(registry, format, file, deleteFileAfterTransform = false)

  private[image] def transformImpl(
    registry: ImageTransformers,
    format: String,
    file: File,
    deleteFileAfterTransform: Boolean
  ): Option[File] = {
    find(format) match {
      case None ⇒
        None

      case Some(transformer) ⇒
        transformer.transformImpl(registry, format, file) match {
          case None ⇒
            log.error(s"$transformer should have transformed $file via format '$format'")
            log.error(s"Not deleting untransformed $file")
            None

          case some0 @ Some(transformed0) ⇒
            val extension = Sys.fixFormat(Sys.fileExtension(transformed0))
            transformImpl(registry, extension, transformed0, deleteFileAfterTransform = true) match {
              case None ⇒
                if(deleteFileAfterTransform) {
                  log.info(s"Deleting file $file")
                  file.delete()
                }
                some0

              case some1 @ Some(transformed1) ⇒
                if(deleteFileAfterTransform) {
                  log.info(s"Deleting file $file")
                  file.delete()
                }
                if(transformed0 != file) {
                  log.info(s"Deleting transformed0 $file")
                  transformed0.delete()
                }
                some1
            }
        }
    }
  }

  def transform(formatOpt: Option[String], file: File): Option[File] =
    transform(this, formatOpt, file)
}

object ImageTransformers extends ImageTransformers {
  final val transformers = List(
    new AllQemuTransformer,
    new GzTransformer,
    new Bz2Transformer,
    new OvaSimpleTransformer
  )

  def isRaw(format: String): Boolean = Sys.fixFormat(format) == ".raw"
}
