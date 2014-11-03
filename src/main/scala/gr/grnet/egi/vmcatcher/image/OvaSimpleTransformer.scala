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

/**
 * Given an OVA file, extracts the VM image file.
 * In the OVA archive we assume a) no directory structure and b) only two files present.
 * One of the present files is an .ovf metadata file and the other one is the image itself.
 *
 */
class OvaSimpleTransformer extends ImageTransformer {
  protected def canTransformImpl(fixedFormat: String): Boolean = fixedFormat == ".ova"

  private[image] def transformImpl(registry: ImageTransformers, format: String, file: File): Option[File] = {
    val tmpDir    = Sys.createTempDirectory()
    log.info(s"Extract $file to $tmpDir")
    val untarCode = Sys.untar(log, file, tmpDir)

    if(untarCode != 0) {
      log.error(s"EXEC exit code $untarCode")
      log.error(s"IGNORE $file")
      return None
    }

    // For now, assume a simple structure:
    //  1) One .OVF file (metadata) and another one that is the VM image
    //  2) We do not parse the OVF.
    //  3) The image does not need some "extraction"

    // Now, inside the dir, get the file that is not .OVF
    val imageFileOpt = tmpDir.
      listFiles().
      find { child ⇒ !child.getName.toLowerCase(Locale.ENGLISH).endsWith(".ovf") }

    try {
      imageFileOpt match {
        case None ⇒
          val msg = s"Could not find image in archive $file"
          log.error(msg)
          None

        case Some(imageFile) ⇒
          log.info(s"Found image $imageFile")
          registry.transform(None, imageFile) match {
            case None ⇒
              log.error(s"Unknown transformer for $imageFile from OVA archive $file")
              None

            case some ⇒
              some
          }
      }
    }
    finally {
      log.info(s"Deleting temporary folder $tmpDir")
      Sys.rmrf(log, tmpDir)
    }
  }
}
