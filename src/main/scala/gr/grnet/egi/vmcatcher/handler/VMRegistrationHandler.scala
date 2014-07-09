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

package gr.grnet.egi.vmcatcher.handler

import java.io.File
import java.util.Locale

import gr.grnet.egi.vmcatcher.Sys
import gr.grnet.egi.vmcatcher.cmdline.Args.Cmd
import gr.grnet.egi.vmcatcher.image.extract.ImageExtractor
import org.slf4j.Logger

/**
 *
 * @author Christos KK Loverdos <loverdos@gmail.com>
 */
class VMRegistrationHandler extends DequeueHandler {
  def expireVM(log: Logger, map: Map[String, String]): Unit = {
    // vmcatcher moves the image file to the $VMCATCHER_CACHE_DIR_EXPIRE folder
    log.info("Expiring VM (nothing to do)")
  }

  def availableVM(log: Logger, map: Map[String, String]): Unit = {
    // vmcatcher has downloaded the image in $VMCATCHER_CACHE_DIR_CACHE.
    // the image filename is $VMCATCHER_EVENT_FILENAME
    // the full path to the image file is $VMCATCHER_CACHE_DIR_CACHE/$VMCATCHER_EVENT_FILENAME
    log.info("Available VM")
    // image file
    val folderPath = map.getOrElse("VMCATCHER_CACHE_DIR_CACHE", "")
    log.info(s"VMCATCHER_CACHE_DIR_CACHE = $folderPath")
    if(folderPath.isEmpty) {
      log.warn("VMCATCHER_CACHE_DIR_CACHE is empty. Aborting")
      return
    }

    val filename = map.getOrElse("VMCATCHER_EVENT_FILENAME", "")
    log.info(s"VMCATCHER_EVENT_FILENAME = $filename")
    if(filename.isEmpty) {
      log.warn("VMCATCHER_EVENT_FILENAME is empty. Aborting")
      return
    }

    val imageFile = new File(folderPath, filename)
    if(!imageFile.isFile) {
      log.warn(s"$imageFile does not exist")
      return
    }

    log.info(s"imageFile = $imageFile")

    // format
    val format = map.getOrElse("VMCATCHER_EVENT_HV_FORMAT", "")
    log.info(s"VMCATCHER_EVENT_HV_FORMAT = $format")
    if(format.isEmpty) {
      log.warn("VMCATCHER_EVENT_HV_FORMAT is empty. Aborting")
      return
    }

    val imageExtractorOpt = ImageExtractor.findExtractor(format)
    imageExtractorOpt match {
      case None ⇒
        log.error(s"Image $imageFile has unknown format $format. Aborting")
        return

      case Some(imageExtractor) ⇒
        val readyImageFile = imageExtractor.extract(log, map, format, imageFile)
        log.info(s"Transformed $imageFile to $readyImageFile")

        try {
          val rcCloudName = Cmd.globalOptions.kamakiCloud
          val mkimageExitCode = Sys.snfMkimage(log, rcCloudName, readyImageFile.getName, readyImageFile)
          if(mkimageExitCode != 0) {
            log.warn(s"Could not register image $imageFile to $rcCloudName")
          }
        }
        finally {
          if(imageFile.getAbsolutePath != readyImageFile.getAbsolutePath) {
            log.info(s"Deleting temporary $readyImageFile")
            readyImageFile.delete()
          }
        }
    }
  }

  def handleOther(log: Logger, verb: String, map: Map[String, String]): Unit = {
    log.info(s"verb = $verb (nothing to do)")
  }

  def handle(log: Logger, json: String, map: Map[String, String]): Unit = {
    val sh = new JustLogHandler
    sh.handle(log, json, map)

    val eventType = map.getOrElse("VMCATCHER_EVENT_TYPE", "")
    val eventTypeL = eventType.toLowerCase(Locale.ENGLISH)
    if(!eventTypeL.endsWith("postfix")) {
      log.info(s"Ignoring VMCATCHER_EVENT_TYPE = $eventType")
      return
    }

    eventTypeL.stripSuffix("postfix") match {
      case "expire" ⇒
        expireVM(log, map)

      case "available" ⇒
        availableVM(log, map)

      case other ⇒
        handleOther(log, other, map)
    }
  }
}
