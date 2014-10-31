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

/**
 * Transforms the majority of qemu-supported formats to `raw` by using `qemu-img`.
 *
 */
class AllQemuTransformer extends ImageTransformer {
  protected def canTransformImpl(fixedFormat: String): Boolean = AllQemuTransformer.SupportedExtensions(fixedFormat)

  private[image] def transformImpl(registry: ImageTransformers, format: String, file: File): Option[File] = {
    val inFormatStripped = Sys.fixFormat(format).substring(1) // remove '.'
    val outFormatStripped = "raw"
    val tmpFile = Sys.createTempFile("." + Sys.dropFileExtension(file) + "." + outFormatStripped)
    log.info(s"Transforming $file from $inFormatStripped to $outFormatStripped at $tmpFile")
    Sys.qemuImgConvert(log, inFormatStripped, outFormatStripped, file, tmpFile) match {
      case n if n != 0 ⇒
        val msg = s"Could not transform image $file from $inFormatStripped to $outFormatStripped"
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
