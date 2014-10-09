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

import gr.grnet.egi.vmcatcher.Sys
import org.slf4j.Logger

/**
 * Transforms the majority of qemu-supported formats to `raw` by using `qemu-img`.
 *
 */
class AllQemuTransformer extends ImageTransformerSkeleton {
  protected def canTransformImpl(
    formatOpt: Option[String],
    extension: String,
    file: File
  ): Boolean =
    formatOpt match {
      case Some(format) if AllQemuTransformer.SupportedExtensions(format) ⇒
        true

      case _ ⇒
        AllQemuTransformer.SupportedExtensions(extension)
    }


  protected def transformImpl(
    log: Logger,
    registry: ImageTransformers,
    formatOpt: Option[String],
    extension: String,
    imageFile: File
  ): Option[File] = {
    val inFormat = extension.drop(1) // remove '.'
    val outFormat = "raw"
    val tmpFile = Sys.createTempFile("." + Sys.dropFileExtension(imageFile) + "." + outFormat)
    log.info(s"Transforming $imageFile from $inFormat to $outFormat at $tmpFile")
    Sys.qemuImgConvert(log, inFormat, outFormat, imageFile, tmpFile) match {
      case n if n != 0 ⇒
        val msg = s"Could not transform image $imageFile from $inFormat to $outFormat"
        log.error(msg)
        None

      case _ ⇒
        Some(tmpFile)
    }
  }
}

object AllQemuTransformer {
  final val SupportedExtensions = Set(
    ".vvfat",
    ".vpc",
    ".vmdk",
    ".vdi",
    ".host_cdrom",
    ".qed",
    ".qcow2",
    ".qcow",
    ".cow",
    ".parallels",
    ".nbd",
    ".dmg",
    ".cloop",
    ".bochs"
  )
}
