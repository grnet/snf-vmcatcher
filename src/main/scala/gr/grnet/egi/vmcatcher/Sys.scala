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

package gr.grnet.egi.vmcatcher

import java.io.File
import java.net.{MalformedURLException, URL}
import java.nio.file.Files

import gr.grnet.egi.vmcatcher.event.{ImageEventField, Event}
import gr.grnet.egi.vmcatcher.image.ImageTransformers
import okio.{ByteString, Buffer, Sink, Okio}
import org.slf4j.Logger
import org.zeroturnaround.exec.ProcessExecutor
import org.zeroturnaround.exec.stream.slf4j.Slf4jStream

import scala.annotation.tailrec

class Sys {
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

  def createTempFile(suffix: String): File = Files.createTempFile("snf", suffix).toFile.getAbsoluteFile

  def createTempDirectory(): File = Files.createTempDirectory("snf").toFile.getAbsoluteFile

  def rmrf(log: Logger, dir: File): Unit = {
    if(dir.isDirectory && dir.isAbsolute) {
      exec(log, "rm", "-rf", dir.getAbsolutePath)
    }
  }

  def qemuImgConvert(log: Logger, inFormat: String, outFormat: String, inFile: File, outFile: File): Int = {
    exec(
      log,
      "qemu-img",
      "convert",
      "-f", inFormat,
      "-O", outFormat,
      inFile.getAbsolutePath,
      outFile.getAbsolutePath
    )
  }

  def createRegistrationMetafile(log: Logger, properties: Map[String, String]): File = {
    val metaMap = Map("properties" → properties)
    val metaJson = Json.jsonOfMap(metaMap)
    log.info(s"Image meta       (json) = $metaJson")
    val metaFile = createTempFile(".meta")
    val metaSink = Okio.sink(metaFile)
    val metaString = ByteString.encodeUtf8(metaJson)
    val buffer = new Buffer()
    buffer.write(metaString)
    metaSink.write(buffer, metaString.size())
    metaSink.close()

    metaFile
  }

  def kamakiRegisterRawImage(log: Logger, rcCloudName: String, properties: Map[String, String], imageFile: File, name: String): Int = {
    // Make the file for --metafile parameter
    log.info(s"Image properties (map ) = $properties")
    val metaFile = createRegistrationMetafile(log, properties)
    val kamaki = "kamaki"
    log.info(s"Registering $imageFile using $kamaki")

    val result =
      exec(
        log,
        kamaki,
        "image", "register",
        "--cloud", rcCloudName,
        "--public",
        "--metafile", metaFile.getAbsolutePath,
        "--upload-image-file", imageFile.getAbsolutePath,
        "--name", name,
        "--location", s"/images/$name"
      )

    log.info(s"Deleting tmp metafile $metaFile")
    metaFile.delete()

    result
  }

  def untar(log: Logger, tarFile: File, where: File): Int = {
    Sys.exec(
      log,
      "tar",
      "xf",
      tarFile.getAbsolutePath,
      "-C",
      where.getAbsolutePath
    )
  }

  def gunzip(log: Logger, from: File, to: File): Int =
    exec(
      log,
      "/bin/sh",
      "-c",
      s"""gunzip < "${from.getAbsolutePath}" > "${to.getAbsolutePath}""""
    )

  def bunzip2(log: Logger, from: File, to: File): Int =
    exec(
      log,
      "/bin/sh",
      "-c",
      s"""bunzip2 < "${from.getAbsolutePath}" > "${to.getAbsolutePath}""""
    )

  /**
   * Computes the file extension (dot (`.`) included).
   */
  def fileExtension(name: String): String =
    name.lastIndexOf('.') match {
      case -1 ⇒ ""
      case  i ⇒ name.substring(i)
    }

  /**
   * Computes the file extension (dot `.` included).
   */
  def fileExtension(file: File): String = fileExtension(file.getName)

  /**
   * Computes the filename without the extension
   */
  def dropFileExtension(filename: String): String = {
    filename.lastIndexOf('.') match {
      case -1 ⇒ filename
      case  i ⇒ filename.substring(0, i)
    }
  }

  def dropFileExtension(file: File): String = dropFileExtension(file.getName)

  def dropFileExtensions(filename: String): String = {
    @tailrec
    def drop(filename: String): String = {
      val dropped = dropFileExtension(filename)
      if(dropped == filename)
        filename
      else
        drop(dropped)
    }

    drop(filename)
  }

  def dropFileExtensions(file: File): String = dropFileExtensions(file.getName)

  def filePreExtension(name: String): String = {
    name.lastIndexOf('.') match {
      case -1 ⇒ ""
      case  i ⇒
        val newName = name.substring(0, i)
        fileExtension(newName)
    }
  }

  def filePreExtension(file: File): String = filePreExtension(file.getName)

  // Constructs the minimum set of image properties (--metafile) and their values.
  def minimumImageProperties(osfamily: String, users: String, rootPartition: String = "1") =
    Map(
      "osfamily" → osfamily,
      "users" → users,
      "ROOT_PARTITION" → rootPartition
    )

  def newImageProperties(event: Event, users: String, rootPartition: String = "1") =
    minimumImageProperties(
      event(ImageEventField.VMCATCHER_EVENT_SL_OS),
      users,
      rootPartition
    ) ++ event.toMap

  def publishVmImageFile(
    log: Logger,
    formatOpt: Option[String],
    properties: Map[String, String],
    imageFile: File,
    kamakiCloud: String,
    imageTransformers: ImageTransformers,
    deleteImageAfterTransform: Boolean
  ): Unit = {

    val transformedImageFileOpt = imageTransformers.pipelineTransform(log, formatOpt, imageFile, deleteImageAfterTransform)
    transformedImageFileOpt match {
      case None ⇒
        log.error(s"Unknown (unexpected) transformer for $imageFile")

      case Some(transformedImageFile) ⇒
        log.info(s"Transformed $imageFile to $transformedImageFile")

        // Now make a better name
        val tname0 = transformedImageFile.getName
        val tname1 =
          if(tname0.startsWith("snf"))
            tname0.substring(tname0.indexOf('.') + 1)
          else
            tname0
        val tnamePrefix = "vmcatcher-" // TODO Make it configurab
        val tname = tnamePrefix + Sys.dropFileExtension(tname1)
        log.info(s"Name of image to upload is $tname")

        try {
          val kamakiExitCode =
            Sys.kamakiRegisterRawImage(
              log,
              kamakiCloud,
              properties,
              transformedImageFile,
              tname
            )

          if(kamakiExitCode != 0) {
            log.error(s"Could not register image $imageFile to $kamakiCloud")
          }
        }
        finally {
          if(imageFile.getAbsolutePath != transformedImageFile.getAbsolutePath) {
            log.info(s"Deleting temporary $transformedImageFile")
            transformedImageFile.delete()
          }
        }
    }
  }

  def downloadToFile(log: Logger, url: URL, file: File): Unit = {
    log.info(s"Downloading $url to $file")
    val in = url.openStream()
    try {
      val urlSource = Okio.source(in)
      val urlBuffer = Okio.buffer(urlSource)
      val fileSink = Okio.sink(file)

      urlBuffer.readAll(fileSink)
    }
    finally in.close()
  }

  def createTempImageFile(filename: String): File = Sys.createTempFile("." + filename)

  def createTempImageFile(imageURL: URL): File = {
    // We want to preserve the remote filename
    val filename = new File(imageURL.getFile).getName
    Sys.createTempImageFile(filename)
  }

  def downloadAndPublishImageFile(
    log: Logger,
    formatOpt: Option[String],
    properties: Map[String, String],
    kamakiCloud: String,
    url: URL,
    imageTransformers: ImageTransformers
  ): Unit = {
    val imageFile = Sys.createTempImageFile(url)
    Sys.downloadToFile(log, url, imageFile)

    try     Sys.publishVmImageFile(log, formatOpt, properties, imageFile, kamakiCloud, imageTransformers, true)
    finally imageFile.delete()
  }
}

object Sys extends Sys
