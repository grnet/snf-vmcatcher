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

import java.net.URL
import java.util.Scanner

import gr.grnet.egi.vmcatcher.ErrorCode._
import gr.grnet.egi.vmcatcher.config.Config
import gr.grnet.egi.vmcatcher.db._
import gr.grnet.egi.vmcatcher.event.{ImageEnvField, ImageEvent}
import gr.grnet.egi.vmcatcher.util.UsernamePassword
import net.liftweb.common.Full
import net.liftweb.mapper.{Ascending, By, OrderBy}

import scala.annotation.tailrec

/**
 *
 */
class StdVMCatcher(config: Config) extends VMCatcher {
  try MDB.init(config.getDbConfig)
  catch {
    case e: NullPointerException if (e.getMessage ne null) && e.getMessage.startsWith("Looking for Connection Identifier") ⇒
      if(config.getDbConfig.getJdbcURL.toString.startsWith("jdbc:mysql")) {
        throw new VMCatcherException(CannotAccessDB, s"Maybe the database is inaccessible ... Did you start MySQL?")
      }
      throw new VMCatcherException(CannotAccessDB, s"Maybe the database is inaccessible ... ")
  }
  
  import gr.grnet.egi.vmcatcher.Main.{Log => log}

  protected def findImageListRefByName(name: String) = MImageListRef.findByName(name)

  def saveImageList(ref: MImageListRef): MImageListRef = ref.saveMe()

  def registerImageList(
    name: String, 
    url: URL,
    isActive: Boolean,
    upOpt: Option[UsernamePassword]
  ): MImageListRef = {
    findImageListRefByName(name) match {
      case Some(_) ⇒
        throw new VMCatcherException(ImageListAlreadyRegistered, s"Image list $name already registered")

      case None ⇒
        MImageListRef.create.
          name(name).
          url(url.toExternalForm).
          isActive(isActive).
          setCredentials(upOpt).
          saveMe()
    }
  }


  def getImageLists(): List[MImageListRef] =
    MImageListRef.findAll(
      OrderBy(MImageListRef.name, Ascending)
    )

  def activateImageList(name: String): Boolean =
    findImageListRefByName(name) match {
      case None ⇒
        throw new VMCatcherException(ImageListNotFound, s"Image list $name not found")

      case Some(ref) ⇒
        val previousStatus = ref.isActive.get
        ref.activate()
        previousStatus
    }

  def deactivateImageList(name: String): Boolean =
    findImageListRefByName(name) match {
      case None ⇒
        throw new VMCatcherException(ImageListNotFound, s"Image list $name not found")

      case Some(ref) ⇒
        val previousStatus = ref.isActive.get
        ref.deactivate()
        previousStatus
    }

  def updateCredentials(name: String, upOpt: Option[UsernamePassword]): Unit =
    findImageListRefByName(name) match {
      case None ⇒
        throw new VMCatcherException(ImageListNotFound, s"Image list $name not found")

      case Some(ref) ⇒
        upOpt match {
          case None ⇒
            ref.username(null).password(null).save()

          case Some(UsernamePassword(username, password)) ⇒
            ref.username(username).password(password).save()
        }
    }

  def getImageListJsonFromRaw(rawText: String): String = {
    val scanner = new Scanner(rawText)
    scanner.nextLine() // Ignore "MIME-Version: 1.0" first line
    scanner.useDelimiter("boundary=\"")
    val boundaryPart = scanner.findInLine("boundary=\"(.+?)\"")
    val boundary = "--" + boundaryPart.substring("boundary=\"".length, boundaryPart.length - 1)
    log.info(s"Found boundary $boundary")

    val buffer = new java.lang.StringBuilder

    @tailrec def scanUntilFirstBoundary(): Unit = {
      val nextLine = scanner.nextLine()
      if(nextLine != boundary) scanUntilFirstBoundary()
    }

    @tailrec def scanUntilLastBoundary(): Unit = {
      val nextLine = scanner.nextLine()
      if(nextLine != boundary) {
        buffer.append(nextLine + System.getProperty("line.separator"))
        scanUntilLastBoundary()
      }
    }

    scanUntilFirstBoundary()
    scanUntilLastBoundary()

    buffer.toString
  }
  
  def parseImagesFromJson(ref: MImageListRef, access: MImageListAccess, imageListJson: String): List[MImage] = {
    try {
      val events = ImageEvent.parseImageListJson(imageListJson)

      val images =
        for {
          event ← events
        } yield {
          val image = MImage.create.
            f_imageListRef(ref).
            f_imageListAccess(access).
            json(event.imageJsonView.json).
            envJson(event.envFieldsView.json).

            adMpuri       (event(ImageEnvField.VMCATCHER_EVENT_AD_MPURI, "")).
            adUserFullName(event(ImageEnvField.VMCATCHER_EVENT_AD_USER_FULLNAME, "")).
            adUserGuid    (event(ImageEnvField.VMCATCHER_EVENT_AD_USER_GUID, "")).
            adUserUri     (event(ImageEnvField.VMCATCHER_EVENT_AD_USER_URI, "")).

            dcIdentifier(event(ImageEnvField.VMCATCHER_EVENT_DC_IDENTIFIER, "")).
            dcTitle     (event(ImageEnvField.VMCATCHER_EVENT_DC_TITLE, "")).

            hvUri       (event(ImageEnvField.VMCATCHER_EVENT_HV_URI, "")).
            hvHypervisor(event(ImageEnvField.VMCATCHER_EVENT_HV_HYPERVISOR, "")).
            hvFormat    (event(ImageEnvField.VMCATCHER_EVENT_HV_FORMAT, "")).
            hvSize      (event(ImageEnvField.VMCATCHER_EVENT_HV_SIZE, "-1").toLong).

            slOs         (event(ImageEnvField.VMCATCHER_EVENT_SL_OS, "")).
            slOsName     (event(ImageEnvField.VMCATCHER_EVENT_SL_OSNAME, "")).
            slOsVersion  (event(ImageEnvField.VMCATCHER_EVENT_SL_OSVERSION, "")).
            slArch       (event(ImageEnvField.VMCATCHER_EVENT_SL_ARCH, "")).
            slChecksum512(event(ImageEnvField.VMCATCHER_EVENT_SL_CHECKSUM_SHA512, ""))

          image.uniqueID(image.computeUniqueID)

          log.info(s"Created $image")
          image.saveMe()
        }

      images
    }
    catch {
      case e: Exception ⇒
        val name = ref.name.get
        val url = ref.url.get
        throw new VMCatcherException(CannotParseImages, s"Cannot parse images from image list $name at $url")
    }
  }

  def accessImageList(ref: MImageListRef): (MImageListAccess, List[MImage]) = {
    val name = ref.name.get
    val url = new URL(ref.url.get)
    val upOpt = ref.credentialsOpt
    upOpt match {
      case Some(UsernamePassword(username, _)) ⇒
        log.info(s"Fetching $url with Basic Auth username = $username")
      case None ⇒
        log.info(s"Fetching $url")
    }

    Sys.downloadRawImageList(url, upOpt) match {
      case Left(t) ⇒
        log.error(s"Error retrieving $url", t)
        // Record the failure
        MImageListAccess.createNotRetrieved(ref, t).save()

        throw new VMCatcherException(CannotAccessImageList, s"Cannot access image list $name at $url [${t.getMessage}]", t)

      case Right(r) if !r.is2XX ⇒
        val statusLine = r.statusLine
        log.error(s"Error [$statusLine] retrieving $url")
        // Record the failure
        val _ = MImageListAccess.createErrorStatus(ref, r).saveMe()

        throw new VMCatcherException(CannotAccessImageList, s"Cannot access image list $name at $url [$statusLine]")

      case Right(r) ⇒
        log.info(s"Retrieved $url")
        // Record the access, one step at a time
        val access = MImageListAccess.createRetrieved(ref, r)
        // Parse json out of the response body
        val imageListJson = getImageListJsonFromRaw(r.getUtf8)
        access.setParsed(imageListJson)
        access.save()

        val images = parseImagesFromJson(ref, access, imageListJson)

        (access, images)
    }
  }

  def fetchNewImageRevisions(name: String): (MImageListRef, MImageListAccess, List[MImageRevision]) =
    findImageListRefByName(name) match {
      case None ⇒
        throw new VMCatcherException(ImageListNotFound, s"Image list $name not found")

      case Some(ref) ⇒
        // 1. Access the image list
        val (access, images) = accessImageList(ref)

        if(images.lengthCompare(0) == 0) {
          return (ref, access, Nil)
        }

        // 2. Save new images
        images.foreach(_.save())

        // 3. Update DB for new revisions
        val newRevisions =
          for {
            image ← images if !MImageRevision.existsByUniqueID(image)
          } yield {
            image.createRevision.saveMe()
          }

        (ref, access, newRevisions)
    }

  def listImageList(name: String): List[MImage] = {
    findImageListRefByName(name) match {
      case None ⇒
        throw new VMCatcherException(ImageListNotFound, s"Image list $name not found")

      case Some(ref) ⇒
        for {
          mci ← MCurrentImage.findAll(By(MCurrentImage.f_imageListRef, ref))
          mi ← mci.f_image.obj
        } yield {
          mi
        }
    }
  }

  def currentImageList(name: String): List[MCurrentImage] =
    findImageListRefByName(name) match {
      case None ⇒
        throw new VMCatcherException(ImageListNotFound, s"Image list $name not found")

      case Some(ref) ⇒
        MCurrentImage.findAll(By(MCurrentImage.f_imageListRef, ref))
    }

  def getImage(dcIdentifier: String): MImage = {
    val mImageBox =
      for {
        mci ← MCurrentImage.find(By(MCurrentImage.dcIdentifier, dcIdentifier))
        mi  ← mci.f_image.obj
      } yield mi

    mImageBox match {
      case Full(mi) ⇒ mi
      case _ ⇒
        throw new VMCatcherException(ImageListNotFound, s"Image $dcIdentifier not found")
    }
  }
}
