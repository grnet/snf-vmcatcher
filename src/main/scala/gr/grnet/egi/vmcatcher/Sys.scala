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
import java.net.{URL, URLConnection}
import java.nio.file.Files
import java.security.cert.X509Certificate
import java.util.Locale
import javax.net.ssl._

import com.squareup.okhttp.Credentials
import gr.grnet.egi.vmcatcher.event.{ImageEvent, ImageEnvField}
import gr.grnet.egi.vmcatcher.image.handler.HandlerData
import gr.grnet.egi.vmcatcher.util.{GetImage, UsernamePassword}
import okio.{Buffer, ByteString, Okio}
import org.slf4j.Logger
import org.zeroturnaround.exec.ProcessExecutor
import org.zeroturnaround.exec.stream.slf4j.Slf4jStream

import scala.annotation.tailrec
import scala.collection.immutable.Seq

class Sys {
  final val TmpFilePrefix = "snf" + java.lang.Long.toString(System.currentTimeMillis(), Character.MAX_RADIX)

  final val OSFAMILY = "OSFAMILY"
  final val ROOT_PARTITION = "ROOT_PARTITION"
  final val USERS = "USERS"

  def exec(log: Logger, args: String*): Int = {
    val cmdline = args.mkString("$ ", " ", "")
    log.info(cmdline)

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

  def createTempFile(suffix: String, folderPath: String): File = {
    val folder = new File(folderPath)
    Files.createTempFile(folder.toPath, TmpFilePrefix, suffix).toFile.getAbsoluteFile
  }

  def createTempDirectory(folderPath: String): File = {
    val folder = new File(folderPath)
    Files.createTempDirectory(folder.toPath, TmpFilePrefix).toFile.getAbsoluteFile
  }

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

  def createRegistrationMetafile(log: Logger, properties: Map[String, String], workingFolder: String): File = {
    val metaMap = Map("properties" → properties)
    val metaJson = Json.jsonOfMap(metaMap)
    log.info(s"Image meta       (json) = $metaJson")
    val metaFile = createTempFile(".meta", workingFolder)
    val metaSink = Okio.sink(metaFile)
    val metaString = ByteString.encodeUtf8(metaJson)
    val buffer = new Buffer().write(metaString)
    metaSink.write(buffer, metaString.size())
    metaSink.close()

    metaFile
  }

  val SnfExcludeTasks = Seq(
    "EXCLUDE_TASK_DELETESSHKEYS",
    "EXCLUDE_TASK_FILESYSTEMRESIZEMOUNTED",
    "EXCLUDE_TASK_FIXPARTITIONTABLE",
    "EXCLUDE_TASK_FILESYSTEMRESIZEUNMOUNTED",
    "EXCLUDE_TASK_SELINUXAUTORELABEL",
    "EXCLUDE_TASK_ASSIGNHOSTNAME",
    "EXCLUDE_TASK_ADDSWAP",
    "EXCLUDE_TASK_CHANGEPASSWORD"
  )

  // The command-line form, in case you would like to try it directly:
  // -m EXCLUDE_TASK_DELETESSHKEYS=yes
  // -m EXCLUDE_TASK_FILESYSTEMRESIZEMOUNTED=yes
  // -m EXCLUDE_TASK_FIXPARTITIONTABLE=yes
  // -m EXCLUDE_TASK_FILESYSTEMRESIZEUNMOUNTED=yes
  // -m EXCLUDE_TASK_SELINUXAUTORELABEL=yes
  // -m EXCLUDE_TASK_ASSIGNHOSTNAME=yes
  // -m EXCLUDE_TASK_ADDSWAP=yes
  // -m EXCLUDE_TASK_CHANGEPASSWORD=yes
  val SnfExcludeTasksParams = (for(task ← SnfExcludeTasks) yield Seq("-m", s"$task=yes")).flatten

  def snfMkImageRegister(
    data: HandlerData,
    properties: Map[String, String],
    imageFile: File,
    name: String
  ): RegistrationResult = {
    val log = data.log
    val rcCloudName = data.kamakiCloud
    // Beware, it needs to be run as root.
    val snfmkimage = "snf-mkimage"
    log.info(s"Registering $imageFile using $snfmkimage")

    val paramsFront = Seq(
      "sudo",
      snfmkimage,
      "--public",
      "--force", // always update existing images
      "-c", rcCloudName,
      "-u", name,
      "-r", name
    )
    val paramsBack = Seq(imageFile.getAbsolutePath)

    val propertiesParams =
      (for {
        (k, v) ← properties
      } yield Seq("-m", s"$k=$v")).flatten

    val cmdline = paramsFront ++ SnfExcludeTasksParams ++ propertiesParams ++ paramsBack
    val result = exec(log, cmdline:_*)

    result match {
      case 0 ⇒ ImageRegistered
      case n ⇒ ImageNotRegistered(result, "")
    }
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
  def minimumImageProperties(osfamily: String, users: String, rootPartition: String = "1"): Map[String, String] =
    Map(
//      OSFAMILY       → osfamily,
//      USERS          → users,
//      ROOT_PARTITION → rootPartition
    )

  def newImageProperties(event: ImageEvent, users: String, rootPartition: String = "1") =
    minimumImageProperties(
      event(ImageEnvField.VMCATCHER_EVENT_SL_OS, "linux"),
      users,
      rootPartition
    ) ++ event.envFieldsView.map

  def mkImageNameToUpload(imageFile: File): String = {
    val name0 = imageFile.getName
    val name1 =
    // IN snf7708633662105303961.CentOS-6.5-20141029.ova
      if(name0.startsWith(TmpFilePrefix))
        name0.dropWhile(_ != '.').dropWhile(_ == '.')
      else
        name0
    // OUT CentOS-6.5-20141029.ova
    val name2 = Sys.dropFileExtension(name1)
    val now = System.currentTimeMillis()
    val now36 = java.lang.Long.toString(now / 1000, Character.MAX_RADIX)
    val name = s"vmcatcher-$now36-$name2"

    name
  }

  def publishVmImageFile(
    formatOpt: Option[String],
    properties: Map[String, String],
    imageFile: File,
    data: HandlerData,
    eventOpt: Option[ImageEvent]
  ): RegistrationResult = {
    val log = data.log
    val imageTransformers = data.imageTransformers
    val kamakiCloud = data.kamakiCloud
    val workingFolder = data.workingFolder
    val nameToUpload = mkImageNameToUpload(imageFile)

    log.info(s"If all goes well, image will be uploaded as $nameToUpload")

    val transformedImageFileOpt = imageTransformers.transform(formatOpt, imageFile, workingFolder)
    transformedImageFileOpt match {
      case None ⇒
        log.error(s"publishVmImageFile(): Unknown (unexpected) transformer for $imageFile")
        NoTransformerFound

      case Some(transformedImageFile) ⇒
        if(imageFile == transformedImageFile) {
          log.info(s"publishVmImageFile(): No transformation needed for $imageFile")
        }
        else {
          log.info(s"publishVmImageFile(): Transformed $imageFile to $transformedImageFile")
        }

        log.info(s"publishVmImageFile(): Name of image to upload is $nameToUpload")

        val registrationResult = Sys.snfMkImageRegister(data, properties, transformedImageFile, nameToUpload)

        val uriInfo =
          eventOpt match {
            case None ⇒ ""
            case Some(event) ⇒
              val hvURI = ImageEnvField.VMCATCHER_EVENT_HV_URI
              val mpURI = ImageEnvField.VMCATCHER_EVENT_AD_MPURI
              val v1 = event(hvURI, "")
              val v2 = event(mpURI, "")

              val mapStr = Map(hvURI → v1, mpURI → v2).mkString("{", ", ", "}")
              s" from $mapStr"
          }

        if(registrationResult.isSuccess) {
          log.info(s"publishVmImageFile(): Registered image $imageFile$uriInfo to $kamakiCloud as $nameToUpload")
        }
        else {
          log.error(s"publishVmImageFile(): Could not register image $imageFile$uriInfo to $kamakiCloud")
        }

        if(transformedImageFile != imageFile) {
          log.info(s"publishVmImageFile(): Deleting transformed published file $transformedImageFile")
          transformedImageFile.delete()
        }

        registrationResult
    }
  }

  val NotSoTrustManager = new X509TrustManager {
    def checkClientTrusted(chain: Array[X509Certificate], authType: String): Unit = {}

    def getAcceptedIssuers: Array[X509Certificate] = Array()

    def checkServerTrusted(chain: Array[X509Certificate], authType: String): Unit = {
      for(cert ← chain) {
        val principal = cert.getIssuerX500Principal
        Main.Log.warn(s"X509TrustManager: Force server trusted: $principal")
      }
    }
  }

  val HostnameNoVerifier = new HostnameVerifier {
    def verify(hostname: String, session: SSLSession): Boolean = {
      Main.Log.warn(s"Not verifying hostname $hostname")
      true
    }
  }

  def urlConnection(url: URL, insecureSSL: Boolean): URLConnection = {
    val urlConn = url.openConnection()

    if(insecureSSL) {
      urlConn match {
        case httpsConn: HttpsURLConnection ⇒
          val ctx = SSLContext.getInstance("TLS")
          ctx.init(null, Array(NotSoTrustManager), null)
          val factory = ctx.getSocketFactory
          httpsConn.setSSLSocketFactory(factory)
          httpsConn.setHostnameVerifier(HostnameNoVerifier)

        case _ ⇒
      }
    }

    urlConn
  }

  def downloadToFile(log: Logger, url: URL, file: File, insecureSSL: Boolean): Unit = {
    log.info(s"Downloading $url to $file")
    val urlConn = urlConnection(url, insecureSSL)
    log.info(s"Got connection to $url")
    val in = urlConn.getInputStream
    try {
      val urlSource = Okio.source(in)
      val urlBuffer = Okio.buffer(urlSource)
      val fileSink = Okio.sink(file)

      urlBuffer.readAll(fileSink)

      // not the best of efficiency ...
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
    finally in.close()
  }

  def createTempImageFile(filename: String, workingFolder: String): File = Sys.createTempFile("." + filename, workingFolder)

  def createTempImageFile(imageURL: URL, workingFolder: String): File = {
    // We want to preserve the remote filename
    val filename = new File(imageURL.getFile).getName
    Sys.createTempImageFile(filename, workingFolder)
  }

  def getImage(url: URL, data: HandlerData): GetImage = {
    val log = data.log
    val insecureSSL = data.insecureSSL
    val workingFolder = data.workingFolder

    url.getProtocol match {
      case "file" ⇒
        val file = new File(url.getFile)
        if(!file.isFile) {
          throw new Exception(s"Image file $file is not a file!")
        }
        GetImage(isTemporary = false, file = file)

      case _ ⇒
        val imageFile = Sys.createTempImageFile(url, workingFolder)
        Sys.downloadToFile(log, url, imageFile, insecureSSL)
        val imageSize = imageFile.length()
        if(imageSize == 0L) {
          log.warn(s"Downloaded image file $imageFile is 0 bytes!")
        }
        GetImage(isTemporary = true, file = imageFile)
    }
  }

  def downloadAndPublishImageFile(
    formatOpt: Option[String],
    properties: Map[String, String],
    url: URL,
    data: HandlerData,
    eventOpt: Option[ImageEvent]
  ): RegistrationResult = {
    val log = data.log
    val GetImage(isTemporary, imageFile) = Sys.getImage(url, data)

    try Sys.publishVmImageFile(formatOpt, properties, imageFile, data, eventOpt)
    finally {
      if(isTemporary) {
        log.info(s"Deleting temporary $imageFile")
        imageFile.delete()
      }
    }
  }

  def downloadUtf8(url: URL, upOpt: Option[UsernamePassword]): Either[Throwable, String] = {
    try {
      val urlConnection = url.openConnection()
      for(up ← upOpt) {
        // The password seems to be fixed, as described in
        //   https://wiki.appdb.egi.eu/main:faq:how_to_subscribe_to_a_private_image_list_using_the_vmcatcher
        val username = up.username
        val password = Option(up.password).getOrElse("x-oauth-basic")

        val credential = Credentials.basic(username, password)
        urlConnection.setRequestProperty("Authorization", credential)
      }

      urlConnection.connect()
      val stream = urlConnection.getInputStream

      val source = Okio.source(stream)
      val buffer = Okio.buffer(source)
      val str = buffer.readUtf8()
      stream.close()
      Right(str)
    }
    catch {
      case t: Throwable ⇒
        Left(t)
    }
  }

  def downloadRawImageList(url: URL, upOpt: Option[UsernamePassword]): Either[Throwable, String] = {
    ////////////////////////////////////////////////////////////
    // This is the image list JSON with a header and a footer.
    // The header is like:
    ////////////////////////////////////////////////////////////
    // MIME-Version: 1.0
    // Content-Type: multipart/signed; protocol="application/x-pkcs7-signature"; micalg="sha1"; boundary="----842099D61D9D967FA11C0562C70A8E03"
    //
    // This is an S/MIME signed message
    //
    // ------842099D61D9D967FA11C0562C70A8E03
    //
    ////////////////////////////////////////////////////////////

    ////////////////////////////////////////////////////////////
    // and the footer is like:
    ////////////////////////////////////////////////////////////
    // ------842099D61D9D967FA11C0562C70A8E03
    // Content-Type: application/x-pkcs7-signature; name="smime.p7s"
    // Content-Transfer-Encoding: base64
    // Content-Disposition: attachment; filename="smime.p7s"

    // MIIHbQYJKoZIhvcNAQcCoIIHXjCCB1oCAQExCzAJBgUrDgMCGgUAMAsGCSqGSIb3
    // DQEHAaCCBOQwggTgMIIDyKADAgECAgEjMA0GCSqGSIb3DQEBCwUAMEoxEzARBgoJ
    // kiaJk/IsZAEZFgNPUkcxGDAWBgoJkiaJk/IsZAEZFghTRUUtR1JJRDEZMBcGA1UE
    //
    // ------842099D61D9D967FA11C0562C70A8E03--
    ////////////////////////////////////////////////////////////
    downloadUtf8(url, upOpt)
  }
}

object Sys extends Sys
