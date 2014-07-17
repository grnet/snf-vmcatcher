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
 *
 * @author Christos KK Loverdos <loverdos@gmail.com>
 */
class Bz2Transformer extends ImageTransformerSkeleton {
  protected def canTransformImpl(
    formatOpt: Option[String],
    extension: String,
    file: File
  ): Boolean =
    formatOpt match {
      case Some(format) ⇒
        Sys.fileExtension(format).toLowerCase(Locale.ENGLISH) == ".bz2"

      case _ ⇒
        extension == ".bz"
    }

  protected def transformImpl(
    log: Logger,
    registry: ImageTransformers,
    formatOpt: Option[String],
    extension: String,
    bz2File: File
  ): Option[File] = {
    val dropBz2 = Sys.dropFileExtension(bz2File)
    val tmpFile =  Sys.createTempFile("." + dropBz2)
    val exitCode = Sys.bunzip2(log, bz2File, tmpFile)

    if(exitCode != 0) {
      log.error(s"EXEC exit code $exitCode")
      log.error(s"IGNORE $bz2File")
      return None
    }

    Some(tmpFile)
  }
}
