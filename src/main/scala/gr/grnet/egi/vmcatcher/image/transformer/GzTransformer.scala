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
class GzTransformer extends ImageTransformer {
  protected def canTransformImpl(fixedFormat: String): Boolean = fixedFormat == ".gz"

  private[image] def transformImpl(registry: ImageTransformers, format: String, file: File, workingFolder: String): Option[File] = {
    val dropGz = Sys.dropFileExtension(file.getName)
    val tmpFile =  Sys.createTempFile("." + dropGz, workingFolder)
    val exitCode = Sys.gunzip(log, file, tmpFile)

    if(exitCode != 0) {
      log.error(s"EXEC exit code $exitCode")
      log.error(s"IGNORE $file")
      return None
    }

    Some(tmpFile)
  }
}
