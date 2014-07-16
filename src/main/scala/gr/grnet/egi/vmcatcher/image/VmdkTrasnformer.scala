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
 *
 * @author Christos KK Loverdos <loverdos@gmail.com>
 */
class VmdkTrasnformer extends ImageTransformerSkeleton {
  protected def canTransformImpl(
    formatOpt: Option[String],
    extension: String,
    file: File
  ): Boolean = extension == ".vmdk"


  protected def transformImpl(
    log: Logger,
    registry: ImageTransformers,
    formatOpt: Option[String],
    extension: String,
    imageFile: File
  ): Option[File] = {
    val tmpFile = Sys.createTempFile("." + imageFile.getName + ".raw")
    log.info(s"Converting $imageFile from vmdk to raw at $tmpFile")
    Sys.qemuImgConvert(log, "vmdk", "raw", imageFile, tmpFile) match {
      case n if n != 0 ⇒
        val msg = s"Could not convert image $imageFile to raw format"
        log.error(msg)
        None

      case _ ⇒
        Some(tmpFile)
    }
  }
}
