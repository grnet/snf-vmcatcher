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
import java.nio.file.Files

import gr.grnet.egi.vmcatcher.image.extract.ImageExtractor
import org.slf4j.Logger
import org.zeroturnaround.exec.ProcessExecutor
import org.zeroturnaround.exec.stream.slf4j.Slf4jStream

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

  def createTempFile(suffix: String): File = Files.createTempFile("snf-vmcatcher.", suffix).toFile.getAbsoluteFile

  def createTempDirectory(): File = Files.createTempDirectory("snf-vmcatcher").toFile.getAbsoluteFile

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
}

object Sys extends Sys
