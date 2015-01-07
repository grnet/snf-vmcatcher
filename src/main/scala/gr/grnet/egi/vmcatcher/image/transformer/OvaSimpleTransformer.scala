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
import java.util.Locale

import gr.grnet.egi.vmcatcher.Sys

/**
 * Given an OVA file, extracts the VM image file which is included.
 * The OVF descriptor (.ovf), any manifests (.mf), certificates (.cert) and utility
 * files (.xml) are ignored.
 * If there are more than one VM images, only one is selected but we do not
 * specify which one.
 *
 */
class OvaSimpleTransformer extends ImageTransformer {
  protected def canTransformImpl(fixedFormat: String): Boolean = fixedFormat == ".ova"

  private[image] def transformImpl(registry: ImageTransformers, format: String, file: File, workingFolder: String): Option[File] = {
    val tmpDir    = Sys.createTempDirectory(workingFolder)
    log.info(s"Extract $file to $tmpDir")
    val untarCode = Sys.untar(log, file, tmpDir)

    if(untarCode != 0) {
      log.error(s"EXEC exit code $untarCode")
      log.error(s"IGNORE $file")
      return None
    }

    // Based on
    //   http://dmtf.org/sites/default/files/standards/documents/DSP0243_1.1.0.pdf
    //   page 10, section 5.1
    // we ignore .ovf, .mf, .cert and .xml files
    // and assume everything else is an image.
    val extsToIgnore = Set(".ovf", ".mf", ".cert", ".xml")
    val ovaFiles = tmpDir.listFiles().toSet
    val imageFileCandidates =
      for {
        file ← tmpDir.listFiles() if !extsToIgnore(Sys.fileExtension(file).toLowerCase(Locale.ENGLISH))
      } yield file

    val ignoredFiles = ovaFiles -- imageFileCandidates
    for { ignored ← ignoredFiles } {
      log.info(s"Ignoring non-image file: $ignored")
    }
    for { candidate ← imageFileCandidates } {
      log.info(s"Candidate image file: $candidate")
    }

    // We just use one image!
    val imageFileOpt = imageFileCandidates.headOption
    try {
      imageFileOpt match {
        case None ⇒
          val msg = s"Could not find image in archive $file"
          log.error(msg)
          None

        case Some(imageFile) ⇒
          log.info(s"Found image $imageFile")
          registry.transform(None, imageFile, workingFolder) match {
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
