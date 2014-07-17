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

import gr.grnet.egi.vmcatcher.image.ImageTransformers
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

  /**
   * Registers a raw image file with Synnefo, using the `snf-mkimage` command-line tool.
   *
   * Please check the documentation for the [[https://www.synnefo.org/docs/snf-image-creator/latest/usage.html snf-mkimage]]
   * tool of [[http://synnefo.org Synnefo]] for more info.
   *
   * @param rcCloudName The cloud section in `~/.kamakirc` that is used by `snf-mkimage`
   * @param name The name of the image when it is uploaded to Synnefo
   * @param imageFile The raw image file that is the input to `snf-mkimage`
   */
  def snfMkimage(log: Logger, rcCloudName: String, name: String, imageFile: File): Int = {
    val exe = "snf-mkimage"
    log.info(s"Running $exe on $imageFile")
    exec(
      log,
      exe,
      "-c", rcCloudName,
      "-u", name,
      "-r", name,
      "--no-sysprep",
      "--no-shrink",
      "--public",
      imageFile.getAbsolutePath
    )
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

  def publishVmImageFile(
    log: Logger,
    formatOpt: Option[String],
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

        try {
          val mkimageExitCode = Sys.snfMkimage(
            log,
            kamakiCloud,
            transformedImageFile.getName,
            transformedImageFile
          )

          if(mkimageExitCode != 0) {
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

  def downloadAndPublishImageFile(
    log: Logger,
    formatOpt: Option[String],
    kamakiCloud: String,
    url: URL,
    imageTransformers: ImageTransformers
  ) = {
    // We want to preserve the remote filename
    val filename = new File(url.getFile).getName
    val imageFile = Sys.createTempFile("." + filename)
    log.info(s"Downloading $url to $imageFile")
    Http.downloadToFile(url, imageFile)
    Sys.publishVmImageFile(log, formatOpt, imageFile, kamakiCloud, imageTransformers, true)
  }
}

object Sys extends Sys
