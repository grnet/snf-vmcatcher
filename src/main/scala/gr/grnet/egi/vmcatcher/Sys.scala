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
import java.net.URL
import java.nio.file.Files
import java.util.Locale

import gr.grnet.egi.vmcatcher.event.{Event, ImageEventField}
import gr.grnet.egi.vmcatcher.image.transformer.ImageTransformers
import okio.{Buffer, ByteString, Okio}
import org.slf4j.Logger
import org.zeroturnaround.exec.ProcessExecutor
import org.zeroturnaround.exec.stream.slf4j.Slf4jStream

import scala.annotation.tailrec

class Sys {
  def exec(log: Logger, args: String*): Int = {
    val pe = new ProcessExecutor().
      command(args:_*).
      exitValueAny().
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
        "--force",
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
  def dropFileExtension(extOpt: Option[String], filename: String): String = {
    filename.lastIndexOf('.') match {
      case -1 | 0 ⇒ filename
      case  i ⇒
        extOpt match {
          case None ⇒
            filename.substring(0, i)

          case Some(extension) ⇒
            val realExtension = filename.substring(i)
            if(extension.toLowerCase(Locale.ENGLISH) == realExtension.toLowerCase(Locale.ENGLISH)) {
              filename.substring(0, i)
            }
            else {
              filename
            }
        }
    }
  }

  def dropFileExtension(filename: String): String = dropFileExtension(None, filename)

  def dropFileExtension(file: File): String = dropFileExtension(None, file.getName)

  def dropSpecificFileExtension(extension: String, file: File): String = dropFileExtension(Some(extension), file.getName)

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

  def filePreExtension(name: String): String = {
    name.lastIndexOf('.') match {
      case -1 ⇒ ""
      case  i ⇒
        val newName = name.substring(0, i)
        fileExtension(newName)
    }
  }

  def filePreExtension(file: File): String = filePreExtension(file.getName)

  // A format is made to resemble an extension, that is with a preceding dot.
  // See the doc of ImageTransformer
  def fixFormat(format: String): String =
    if(format.isEmpty)
      ""
    else if(format.startsWith("."))
      format.toLowerCase(Locale.ENGLISH)
    else
      "." + format.toLowerCase(Locale.ENGLISH)


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
    imageTransformers: ImageTransformers
  ): Unit = {

    val transformedImageFileOpt = imageTransformers.transform(formatOpt, imageFile)
    transformedImageFileOpt match {
      case None ⇒
        log.error(s"publishVmImageFile(): Unknown (unexpected) transformer for $imageFile")

      case Some(transformedImageFile) ⇒
        if(imageFile == transformedImageFile) {
          log.info(s"publishVmImageFile(): No transformation needed for $imageFile")
        }
        else {
          log.info(s"publishVmImageFile(): Transformed $imageFile to $transformedImageFile")
        }

        // Now make a better name
        val tname0 = transformedImageFile.getName
        val tname1 =
          if(tname0.startsWith("snf"))
            tname0.substring(tname0.indexOf('.') + 1)
          else
            tname0
        val tnamePrefix = "vmcatcher-" // TODO Make it configurable
        val tname = tnamePrefix + Sys.dropFileExtension(tname1)
        log.info(s"publishVmImageFile(): Name of image to upload is $tname")

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

        if(transformedImageFile != imageFile) {
          log.info(s"Deleting transformed published file $transformedImageFile")
          transformedImageFile.delete()
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
    finally {
      in.close()

      // not the best efficiency but the best of fun
      val length = file.length()
      val sizeB = length → "bytes"
      val sizeKB = (length / 1024) → "KB"
      val sizeMB = (length / (1024 * 1024)) → "MB"
      val sizeGB = (length / (1024 * 1024 * 1024)) → "GB"

      val sizeOpt = List(sizeGB, sizeMB, sizeKB, sizeB).find(_._1 > 0)
      val size = sizeOpt.getOrElse(sizeB)
      val sizeStr = size.productIterator.mkString(" ")

      log.info(s"Size of $file is $sizeStr")
    }
  }

  def createTempImageFile(filename: String): File = Sys.createTempFile("." + filename)

  def createTempImageFile(imageURL: URL): File = {
    // We want to preserve the remote filename
    val filename = new File(imageURL.getFile).getName
    Sys.createTempImageFile(filename)
  }

  def getImage(log: Logger, url: URL): GetImage = {
    url.getProtocol match {
      case "file" ⇒
        val file = new File(url.getFile)
        if(!file.isFile) {
          throw new Exception(s"Image file $file is not a file!")
        }
        GetImage(isTemporary = false, file = file)

      case _ ⇒
        val imageFile = Sys.createTempImageFile(url)
        Sys.downloadToFile(log, url, imageFile)
        GetImage(isTemporary = true, imageFile)
    }
  }

  def downloadAndPublishImageFile(
    log: Logger,
    formatOpt: Option[String],
    properties: Map[String, String],
    kamakiCloud: String,
    url: URL,
    imageTransformers: ImageTransformers
  ): Unit = {
    val GetImage(isTemporary, imageFile) = Sys.getImage(log, url)

    try     Sys.publishVmImageFile(log, formatOpt, properties, imageFile, kamakiCloud, imageTransformers)
    finally {
      if(isTemporary) {
        log.info(s"Deleting temporary $imageFile")
        imageFile.delete()
      }
    }
  }
}

object Sys extends Sys
