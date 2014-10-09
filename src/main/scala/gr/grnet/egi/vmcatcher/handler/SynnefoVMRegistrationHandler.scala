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

import gr.grnet.egi.vmcatcher.image.ImageTransformers
import gr.grnet.egi.vmcatcher.message._
import gr.grnet.egi.vmcatcher.{Http, Sys}
import org.slf4j.Logger

/**
 * Registers a VM to Synnefo, using `snf-mkimage`
 *
 * @author Christos KK Loverdos <loverdos@gmail.com>
 */
class SynnefoVMRegistrationHandler extends DequeueHandler {
  def expireVM(
    log: Logger,
    map: Map[String, String],
    kamakiCloud: String,
    imageTransformers: ImageTransformers
  ): Unit = {
    // vmcatcher moves the image file to the $VMCATCHER_CACHE_DIR_EXPIRE folder
    log.info("Expiring VM (nothing to do)")
  }

  def availableVM(
    log: Logger,
    map: Map[String, String],
    kamakiCloud: String,
    imageTransformers: ImageTransformers
  ): Unit = {
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

    // osfamily
    val osfamily = map.getOrElse("VMCATCHER_EVENT_SL_OS", "")
    log.info(s"VMCATCHER_EVENT_SL_OS = $osfamily")
    if(osfamily.isEmpty) {
      log.warn("VMCATCHER_EVENT_SL_OS is empty. Aborting")
      return
    }

    // user
    val users = "root"
    val rootPartition = "1"

    val vmCatcherProperties = map -- List(
      "VMCATCHER_CACHE_EVENT",
      "VMCATCHER_HOME",
      "VMCATCHER_RDBMS",
      "VMCATCHER_CACHE_DIR_DOWNLOAD",
      "VMCATCHER_CACHE_DIR_CACHE",
      "VMCATCHER_CACHE_DIR_EXPIRE",
      "VMCATCHER_EVENT_TYPE"
    )

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

  def handleOther(log: Logger, verb: String, map: Map[String, String], kamakiCloud: String): Unit = {
    log.info(s"verb = $verb (nothing to do)")
  }

  def handleVmCatcherScriptJSON(
    log: Logger,
    json: String,
    map: Map[String, String],
    kamakiCloud: String,
    imageTransformers: ImageTransformers
  ): Unit = {
    log.info("#> handleVmCatcherScriptJSON")
    val eventType = map.getOrElse("VMCATCHER_EVENT_TYPE", "")
    val eventTypeL = eventType.toLowerCase(Locale.ENGLISH)
    if(!eventTypeL.endsWith("postfix")) {
      log.info(s"Ignoring VMCATCHER_EVENT_TYPE = $eventType")
      return
    }

    eventTypeL.stripSuffix("postfix") match {
      case "available" ⇒
        // "available" is the interesting message
        availableVM(log, map, kamakiCloud, imageTransformers)

      case "expire" ⇒
        expireVM(log, map, kamakiCloud, imageTransformers)

      case other ⇒
        handleOther(log, other, map, kamakiCloud)
    }
    log.info("#< handleVmCatcherScriptJSON")
  }

  def handleImageJSON(
    log: Logger,
    imageConfig: ImageConfig,
    json: String,
    map: Map[String, String],
    kamakiCloud: String,
    imageTransformers: ImageTransformers
  ): Unit = {
    log.info("#> handleImageJSON")

    val url = new URL(imageConfig.hvURI)
    val formatOpt = Some(imageConfig.hvFormat)


    val properties = Sys.minimumImageProperties(imageConfig.slOS, "root")

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
    json: String,
    map: Map[String, String],
    kamakiCloud: String,
    imageTransformers: ImageTransformers
  ): Unit = {
    val sh = new JustLogHandler
    sh.handle(log, json, map, kamakiCloud, imageTransformers)

    // There are two types of JSON messages going in the queue:
    //  1) The JSON message that vmcatcher creates
    //  2) The JSON message that snf-vmcatcher creates, using the enqueue-from-image-list command
    // So, we have to discover which one is it

    val msgType = Message.parseJson(json)
    log.info(s"Message is ${msgType.getClass.getSimpleName}")
    msgType match {
      case m @ UnparsedMessage(error) ⇒
        log.error(error)
        log.error(json)
        return

      case m @ VmCatcherScriptJSON(_) ⇒
        handleVmCatcherScriptJSON(log, json, map, kamakiCloud, imageTransformers)

      case m @ ImageJSON(imageConfig) ⇒
        handleImageJSON(log, imageConfig, json, map, kamakiCloud, imageTransformers)
    }
  }
}
