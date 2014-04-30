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

import java.util.Locale
import java.io.File
import org.slf4j.Logger
import org.zeroturnaround.exec.ProcessExecutor
import org.zeroturnaround.exec.stream.slf4j.Slf4jStream

/**
 *
 * @author Christos KK Loverdos <loverdos@gmail.com>
 */
class VMRegistrationHandler extends DequeueHandler {
  def expireVM(log: Logger, map: Map[String, String]): Unit = {
    // vmcatcher moves the image file to the $VMCATCHER_CACHE_DIR_EXPIRE folder
    log.info("Expiring VM (nothing to do)")
  }

  def exec(log: Logger, args: String*): Int = {
    val pe = new ProcessExecutor().
      command(args:_*).
      exitValueAny().
      readOutput(true).
      redirectErrorStream(true).
      redirectOutput(System.out).
      redirectOutputAlsoTo(Slf4jStream.of(log).asInfo())

    val pr = pe.execute()
    val exitCode = pr.getExitValue

    exitCode
  }

  def bunzip2(log: Logger, from: File, to: File): Int =
    exec(
      log,
      "/bin/sh",
      "-c",
      s"""bunzip2 < "${from.getAbsolutePath}" > "${to.getAbsolutePath}""""
    )

  def snf_mkimage(log: Logger, image: File): Int = {
    val exe = "snf-mkimage"
    log.info(s"Running $exe on $image")
    exec(
      log,
      exe,
      "--print-syspreps",
      "--no-sysprep",
      "--no-shrink",
      "--public",
      image.getAbsolutePath
    )
  }

  def undoBz2(log: Logger, imageFile: File, format: String, map: Map[String, String]): File = {
    val tmpFile =  File.createTempFile(imageFile.getName, ".bunzip2").getAbsoluteFile

    val exitCode = bunzip2(log, imageFile, tmpFile)

    if(exitCode != 0) {
      log.error(s"EXEC exit code $exitCode")
      log.warn(s"IGNORE $imageFile $map")
      throw new Exception(s"EXEC exit code $exitCode. IGNORE $imageFile $map")
    }

    tmpFile
  }

  def undoGz(log: Logger, imageFile: File, format: String, map: Map[String, String]): File = {
    throw new Exception(".gz format for image not supported yet")
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
    val formatL = format.toLowerCase(Locale.ENGLISH)

    val readyImageFileOpt =
      if(formatL.endsWith(".bz2"))
        Some(undoBz2(log, imageFile, format, map))
      else if(format.endsWith(".gz"))
        Some(undoGz(log, imageFile, format, map))
      else
        None

    readyImageFileOpt match {
      case None ⇒
        log.error(s"Image $imageFile has unknown format $format. Aborting")
        return

      case Some(readyImageFile) ⇒
        log.info(s"Transformed $imageFile to $readyImageFile")
        val mkimageExitCode = snf_mkimage(log, readyImageFile)
        if(mkimageExitCode != 0) {
          log.warn(s"Could not register image $imageFile to ~okeanos")
        }
        if(imageFile != readyImageFile) {
          log.info(s"Deleting temporary $readyImageFile")
          readyImageFile.delete()
        }
    }
  }

  def handleOther(log: Logger, verb: String, map: Map[String, String]): Unit = {
    log.info(s"verb = $verb (nothing to do)")
  }

  def handle0(log: Logger, json: String, map: Map[String, String]): Unit = {
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

  def handle(log: Logger, json: String, map: Map[String, String]): Unit = {
    handle0(log, json, map)
  }
}
