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

import org.slf4j.Logger

import scala.annotation.tailrec

/**
 *
 */
trait ImageTransformers {
  def lastResortTransformerOption: Option[ImageTransformer]
  
  def transformers: List[ImageTransformer]

  def findForFile(log: Logger, file: File): Option[ImageTransformer] = {
    def logit(imageTransformer: ImageTransformer) =
      log.info(s"Found ${imageTransformer.getClass.getSimpleName} for $file")

    transformers.find(_.canTransform(None, file)) match {
      case some @ Some(imageTransformer) ⇒
        logit(imageTransformer)
        some

      case None ⇒
        ImageTransformers.lastResortTransformerOption match {
          case None ⇒ None
          case some @ Some(imageTransformer) ⇒
            logit(imageTransformer)
            some
        }
    }
  }

  def findForFormat(log: Logger, format: String, file: File): Option[ImageTransformer] = {
    def logit(imageTransformer: ImageTransformer) =
      log.info(s"Found ${imageTransformer.getClass.getSimpleName} for $file of format $format")

    transformers.find(_.canTransform(Some(format), file)) match {
      case some @ Some(imageTransformer) ⇒
        logit(imageTransformer)
        some

      case None ⇒
        ImageTransformers.lastResortTransformerOption match {
          case None ⇒ None
          case some @ Some(imageTransformer) ⇒
            logit(imageTransformer)
            some
        }
    }
  }

  def find(log: Logger, formatOpt: Option[String], file: File): Option[ImageTransformer] =
    formatOpt match {
      case Some(format) ⇒ findForFormat(log, format, file)
      case None ⇒ findForFile(log, file)
    }

  def pipelineTransform(
    log: Logger,
    originalFormatOpt: Option[String],
    originalFile: File,
    deleteOriginalFile: Boolean
  ): Option[File] = {

    log.info(s"BEGIN pipelineTransform($originalFormatOpt, $originalFile, $deleteOriginalFile)")
    log.info(s"IGNORE originalFormatOpt = $originalFormatOpt, will use only filename")

    @tailrec
    def runTransformationRound(
      round: Int,
      file: File,
      deleteFileAfterTransform: Boolean
    ): Option[File] = {
      def fileInfo = s"$file"

      def checkDelete() =
        if(deleteFileAfterTransform) {
          log.info(s"Deleting intermediate file $file")
          file.delete()
        }

      findForFile(log, file) match {
        case None ⇒
          if(round == 1)
            log.info(s"No transformer found for $file")
          else
            log.info(s"No further transformer found for $file")

          Some(file) // either we transformed or we just return the original

        case Some(imageTransformer) ⇒
          val suggestedFilename = imageTransformer.suggestTransformedFilename(file)
          log.info(s"Using $imageTransformer to transform $fileInfo to $suggestedFilename ...")

          // the final name after transform() must be the same as the value of `suggestedFilename`
          log.info(s"===BEGIN Round $round using $imageTransformer for $file")
          imageTransformer.transform(log, this, file) match {
            case None ⇒
              log.error(s"Could not transform $fileInfo using $imageTransformer")
              checkDelete()
              log.info(s"===END   Round $round using $imageTransformer for $file")
              None

            case Some(transformedFile) ⇒
              log.info(s"Transformed $fileInfo to $transformedFile")
              checkDelete()
              log.info(s"===END   Round $round using $imageTransformer for $file")
              runTransformationRound(round + 1, transformedFile, true)
          }
      }
    }

    val result =
      runTransformationRound(1, originalFile, false) match {
        case None ⇒ None
        case Some(transformedFile) if originalFile.getAbsolutePath == transformedFile.getAbsolutePath ⇒ None
        case some ⇒ some
      }

    if(deleteOriginalFile) {
      log.info(s"Deleting original file $originalFile")
      originalFile.delete()
    }

    log.info(s"END   pipelineTransform($originalFormatOpt, $originalFile, $deleteOriginalFile)")
    result
  }

  def pipelineTransform(
    log: Logger,
    originalFile: File,
    deleteOriginalFile: Boolean
  ): Option[File] =
    this.pipelineTransform(log, None, originalFile, deleteOriginalFile)
}

object ImageTransformers extends ImageTransformers {
  final val lastResortTransformerOption: Option[ImageTransformer] =
    None

  final val transformers = List(
    new AllQemuTransformer,
    new GzTransformer,
    new Bz2Transformer,
    new OvaSimpleTransformer
  )
}
