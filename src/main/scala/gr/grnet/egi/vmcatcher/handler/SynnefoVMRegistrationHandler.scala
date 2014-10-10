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
import java.net.URL
import java.util.Locale

import gr.grnet.egi.vmcatcher.Sys
import gr.grnet.egi.vmcatcher.event.{Event, ExternalEventField, ImageEventField}
import gr.grnet.egi.vmcatcher.image.ImageTransformers
import org.slf4j.Logger

/**
 * Registers a VM to Synnefo, using `snf-mkimage`
 *
 */
class SynnefoVMRegistrationHandler extends DequeueHandler {
  def expireVM(
    log: Logger,
    event: Event,
    kamakiCloud: String,
    imageTransformers: ImageTransformers
  ): Unit = {
    // vmcatcher moves the image file to the $VMCATCHER_CACHE_DIR_EXPIRE folder
    log.info("Expiring VM (nothing to do)")
  }

  def availableVM(
    log: Logger,
    event: Event,
    kamakiCloud: String,
    imageTransformers: ImageTransformers
  ): Unit = {
    // vmcatcher has downloaded the image in $VMCATCHER_CACHE_DIR_CACHE.
    // the image filename is $VMCATCHER_EVENT_FILENAME
    // the full path to the image file is $VMCATCHER_CACHE_DIR_CACHE/$VMCATCHER_EVENT_FILENAME
    log.info("Available VM")
    // image file
    val folderPath = event.apply(ExternalEventField.VMCATCHER_CACHE_DIR_CACHE)
    log.info(s"VMCATCHER_CACHE_DIR_CACHE = $folderPath")
    if(folderPath.isEmpty) {
      log.warn("VMCATCHER_CACHE_DIR_CACHE is empty. Aborting")
      return
    }

    val filename = event.apply(ExternalEventField.VMCATCHER_EVENT_FILENAME)
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
    val format = event.apply(ImageEventField.VMCATCHER_EVENT_HV_FORMAT)
    log.info(s"VMCATCHER_EVENT_HV_FORMAT = $format")
    if(format.isEmpty) {
      log.warn("VMCATCHER_EVENT_HV_FORMAT is empty. Aborting")
      return
    }

    // osfamily
    val osfamily = event.apply(ImageEventField.VMCATCHER_EVENT_SL_OS)
    log.info(s"VMCATCHER_EVENT_SL_OS = $osfamily")
    if(osfamily.isEmpty) {
      log.warn("VMCATCHER_EVENT_SL_OS is empty. Aborting")
      return
    }

    // user
    val users = "root"
    val rootPartition = "1"

    // FIXME add some
    val vmCatcherProperties = Map()

    val synnefoProperties = Sys.minimumImageProperties(osfamily, users, rootPartition)

    val properties = synnefoProperties ++ vmCatcherProperties

    Sys.publishVmImageFile(
      log,
      Some(format),
      properties,
      imageFile,
      kamakiCloud,
      imageTransformers,
      false
    )
  }

  def handleOther(log: Logger, verb: String, event: Event, kamakiCloud: String): Unit = {
    log.info(s"verb = $verb (nothing to do)")
  }

  def handleVmCatcherScriptJSON(
    log: Logger,
    event: Event,
    eventType: String,
    kamakiCloud: String,
    imageTransformers: ImageTransformers
  ): Unit = {
    log.info("#> handleVmCatcherScriptJSON")
    val eventTypeL = eventType.toLowerCase(Locale.ENGLISH)
    if(!eventTypeL.endsWith("postfix")) {
      log.info(s"Ignoring VMCATCHER_EVENT_TYPE = $eventType")
      return
    }

    eventTypeL.stripSuffix("postfix") match {
      case "available" ⇒
        // "available" is the interesting message
        availableVM(log, event, kamakiCloud, imageTransformers)

      case "expire" ⇒
        expireVM(log, event, kamakiCloud, imageTransformers)

      case other ⇒
        handleOther(log, other, event, kamakiCloud)
    }
    log.info("#< handleVmCatcherScriptJSON")
  }

  def handleImageJSON(
    log: Logger,
    event: Event,
    kamakiCloud: String,
    imageTransformers: ImageTransformers
  ): Unit = {
    log.info("#> handleImageJSON")

    val url = new URL(event(ImageEventField.VMCATCHER_EVENT_HV_URI))
    val formatOpt = Some(event(ImageEventField.VMCATCHER_EVENT_HV_FORMAT))


    val properties = Sys.minimumImageProperties(
      event.apply(ImageEventField.VMCATCHER_EVENT_SL_OS),
      "root"
    )

    Sys.downloadAndPublishImageFile(
      log,
      formatOpt,
      properties,
      kamakiCloud,
      url,
      imageTransformers
    )

    log.info("#< handleImageJSON")
  }

  def handle(
    log: Logger,
    event: Event,
    kamakiCloud: String,
    imageTransformers: ImageTransformers
  ): Unit = {
    val sh = new JustLogHandler
    sh.handle(log, event, kamakiCloud, imageTransformers)

    // There are two types of JSON messages going in the queue:
    //  1) The JSON message that vmcatcher creates
    //  2) The JSON message that snf-vmcatcher creates, using the enqueue-from-image-list command
    // So, we have to discover which one is it

    event.apply(ExternalEventField.VMCATCHER_EVENT_TYPE) match {
      case "" ⇒
        // not from vmcatcher since it always sets VMCATCHER_EVENT_TYPE
        handleImageJSON(log, event, kamakiCloud, imageTransformers)

      case eventType ⇒
        handleVmCatcherScriptJSON(log, event, eventType, kamakiCloud, imageTransformers)
    }
  }
}
