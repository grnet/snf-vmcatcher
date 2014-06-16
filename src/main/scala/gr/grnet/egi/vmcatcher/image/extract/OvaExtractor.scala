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
 * Given an OVA file, extracts the VM image file.
 * In the OVA archive we assume a) no directory structure and b) only two files present.
 * One of the present files is an .ovf metadata file and the other one is the image itself.
 *
 */
class OvaExtractor extends ImageExtractor {
  def canExtract(format: String): Boolean =
    format.toLowerCase(Locale.ENGLISH).endsWith(".ova")

  def untar(log: Logger, imageFile: File, tmpDir: File): Int = {
    Sys.exec(
      log,
      "tar",
     "xf",
      imageFile.getAbsolutePath,
      "-C",
      tmpDir.getAbsolutePath
    )
  }

  def extract(log: Logger, map: Map[String, String], format: String, imageFile: File): File = {
    val tmpDir =  Sys.createTempDirectory()
    val untarCode = untar(log, imageFile, tmpDir)

    if(untarCode != 0) {
      log.error(s"EXEC exit code $untarCode")
      log.warn(s"IGNORE $imageFile $map")
      throw new Exception(s"EXEC exit code $untarCode. IGNORE $imageFile $map")
    }

    // Now, inside the dir, get the file that is not .OVF
    val imageFile2Opt = tmpDir.
      listFiles().
      find { child ⇒ !child.getName.toLowerCase(Locale.ENGLISH).endsWith(".ovf") }

    try {
      imageFile2Opt match {
        case None ⇒
          val msg = s"Could not find image in archive $imageFile"
          throw new Exception(msg)

        case Some(imageFile2) ⇒
          val name = imageFile2.getName.toLowerCase(Locale.ENGLISH)
          // FIXME
          ???
      }
    }
    finally {
      Sys.rmrf(log, tmpDir)
    }
  }
}
