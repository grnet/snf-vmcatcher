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

package gr.grnet.egi.vmcatcher.image.convert

import java.io.File
import java.util.Locale

import gr.grnet.egi.vmcatcher.Sys
import org.slf4j.Logger

/**
 * Converts a `vmdk` image to `raw` using `qemu` utilities.
 */
class VmdkConverter extends ImageConverter {
  def canConvert(imageFile: File): Boolean =
    imageFile.getName.toLowerCase(Locale.ENGLISH).endsWith(".vmdk")

  def convert(log: Logger, imageFile: File): File = {
    val tmpFile = Sys.createTempFile("." + imageFile.getName + ".raw")
    Sys.qemuImgConvert(log, "vmdk", "raw", imageFile, tmpFile) match {
      case n if n != 0 ⇒
        val msg = s"Could not convert image $imageFile to raw format"
        throw new Exception(msg)

      case _ ⇒
        tmpFile
    }
  }
}
