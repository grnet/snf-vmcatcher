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

package gr.grnet.egi.vmcatcher.image.handler

import java.io.File
import java.net.URL
import java.util.Locale

import gr.grnet.egi.vmcatcher.Sys
import gr.grnet.egi.vmcatcher.event.{ImageEventField, Event}
import gr.grnet.egi.vmcatcher.event.ExternalEventField._
import gr.grnet.egi.vmcatcher.event.ImageEventField._

/**
 * Registers a VM to Synnefo, using `snf-mkimage`
 *
 */
class SynnefoVMRegistrationHandler extends DequeueHandler {
  def expireVM(event: Event, data: HandlerData): Unit = {
    // vmcatcher moves the image file to the $VMCATCHER_CACHE_DIR_EXPIRE folder
    data.log.info("Expiring VM (nothing to do)")
  }

  def availableVM(event: Event, data: HandlerData): Unit = {
    val log = data.log

    // vmcatcher has downloaded the image in $VMCATCHER_CACHE_DIR_CACHE.
    // the image filename is $VMCATCHER_EVENT_FILENAME
    // the full path to the image file is $VMCATCHER_CACHE_DIR_CACHE/$VMCATCHER_EVENT_FILENAME
    log.info("Available VM")
    // image file
    val folderPath = event(VMCATCHER_CACHE_DIR_CACHE)
    log.info(s"VMCATCHER_CACHE_DIR_CACHE = $folderPath")
    if(folderPath.isEmpty) {
      log.warn("VMCATCHER_CACHE_DIR_CACHE is empty. Aborting")
      return
    }

    val filename = event(VMCATCHER_EVENT_FILENAME)
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
    val format = Sys.fixFormat(event(VMCATCHER_EVENT_HV_FORMAT))
    log.info(s"VMCATCHER_EVENT_HV_FORMAT = $format")
    if(format.isEmpty) {
      log.warn("VMCATCHER_EVENT_HV_FORMAT is empty. Aborting")
      return
    }

    // osfamily
    val osfamily = event(VMCATCHER_EVENT_SL_OS)
    log.info(s"VMCATCHER_EVENT_SL_OS = $osfamily")
    if(osfamily.isEmpty) {
      log.warn("VMCATCHER_EVENT_SL_OS is empty. Aborting")
      return
    }

    val users = "root"
    val rootPartition = "1"

    val vmCatcherProperties = event.toMap
    val synnefoProperties = Sys.minimumImageProperties(osfamily, users, rootPartition)
    val properties = synnefoProperties ++ vmCatcherProperties

    Sys.publishVmImageFile(
      Some(format),
      properties,
      imageFile,
      data,
      Some(event)
    )
  }

  def handleOther(verb: String, event: Event, data: HandlerData): Unit = {
    data.log.info(s"verb = $verb (nothing to do)")
  }

  def handleVmCatcherScriptJSON(event: Event, eventType: String, data: HandlerData): Unit = {
    val log = data.log

    log.info("#> handleVmCatcherScriptJSON")
    log.info(s"eventType = $eventType")
    val eventTypeL = eventType.toLowerCase(Locale.ENGLISH)
    if(!eventTypeL.endsWith("postfix")) {
      log.info(s"Ignoring VMCATCHER_EVENT_TYPE = $eventType")
      return
    }

    eventTypeL.stripSuffix("postfix") match {
      case "available" ⇒
        // "available" is the interesting message
        availableVM(event, data)

      case "expire" ⇒
        expireVM(event, data)

      case other ⇒
        handleOther(other, event, data)
    }
    log.info("#< handleVmCatcherScriptJSON")
  }

  def handleImageJSON(event: Event, data: HandlerData): Unit = {
    val log = data.log
    val hvURI = event(ImageEventField.VMCATCHER_EVENT_HV_URI)
    log.info(s"#> handleImageJSON($hvURI)")

    val url = new URL(event(VMCATCHER_EVENT_HV_URI))
    val formatOpt = Some(Sys.fixFormat(event(VMCATCHER_EVENT_HV_FORMAT)))

    val users = "root"
    val rootPartition = "1"
    val properties = Sys.newImageProperties(event, users, rootPartition)

    val result = Sys.downloadAndPublishImageFile(
      formatOpt,
      properties,
      url,
      data,
      Some(event)
    )

    log.info(s"Registration result for $hvURI: $result")
    log.info(s"#< handleImageJSON($hvURI)")
  }

  def handle(event: Event, data: HandlerData): Unit = {
    val sh = new JustLogHandler
    sh.handle(event, data)

    // There are two types of JSON messages going in the queue:
    //  1) The JSON message that vmcatcher creates
    //  2) The JSON message that snf-vmcatcher creates, using the enqueue-from-image-list command
    // So, we have to discover which one is it

    event.get(VMCATCHER_EVENT_TYPE) match {
      case None ⇒
        // not from vmcatcher since it always sets VMCATCHER_EVENT_TYPE
        handleImageJSON(event, data)

      case Some(eventType) ⇒
        handleVmCatcherScriptJSON(event, eventType, data)
    }
  }
}
