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

package gr.grnet.egi.vmcatcher.image.extract

import java.io.File
import java.util.Locale

import gr.grnet.egi.vmcatcher.Sys
import org.slf4j.Logger

/**
 *
 * @author Christos KK Loverdos <loverdos@gmail.com>
 */
class CpioBz2Extractor extends ImageExtractor {
  val exec = new Sys

  def canExtract(format: String): Boolean =
    format.toLowerCase(Locale.ENGLISH).endsWith(".cpio.bz2")

  def bunzip2(log: Logger, from: File, to: File): Int =
    exec.exec(
      log,
      "/bin/sh",
      "-c",
      s"""bunzip2 < "${from.getAbsolutePath}" > "${to.getAbsolutePath}""""
    )

  def extract(log: Logger, map: Map[String, String], format: String, imageFile: File): Option[File] = {
    if(!canExtract(format)) {
      return None
    }

    val tmpFile =  Sys.createTempFile(imageFile.getName+".bunzip2")
    val exitCode = bunzip2(log, imageFile, tmpFile)

    if(exitCode != 0) {
      log.error(s"EXEC exit code $exitCode")
      log.warn(s"IGNORE $imageFile $map")
      throw new Exception(s"EXEC exit code $exitCode. IGNORE $imageFile $map")
    }

    Some(tmpFile)
  }
}
