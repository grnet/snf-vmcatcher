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
import gr.grnet.egi.vmcatcher.event.{Events, ImageEventField}
import gr.grnet.egi.vmcatcher.util.UsernamePassword
import net.liftweb.mapper.By

import scala.annotation.tailrec

/**
 *
 */
class StdVMCatcher(config: Config) extends VMCatcher {
  try MDB.init(config.getDbConfig)
  catch {
    case e: NullPointerException if (e.getMessage ne null) && e.getMessage.startsWith("Looking for Connection Identifier") ⇒
      if(config.getDbConfig.getJdbcURL.startsWith("jdbc:mysql")) {
        throw new VMCatcherException(CannotAccessDB, s"Maybe the database is inaccessible ... Did you start MySQL?")
      }
      throw new VMCatcherException(CannotAccessDB, s"Maybe the database is inaccessible ... ")
  }
  
  import Main.Log

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
    Log.info(s"Found boundary $boundary")

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
  
  def parseImagesFromJson(ref: MImageListRef, json: String): List[MImage] = {
    try {
      val events = Events.ofJson(json, Map())

      val images =
        for {
          event ← events
        } yield {
          val image = MImage.create.
            json(event.originalJson.orNull).
            dcIdentifier(event(ImageEventField.VMCATCHER_EVENT_DC_IDENTIFIER)).
            dcTitle     (event(ImageEventField.VMCATCHER_EVENT_DC_TITLE)).
            adMpuri     (event(ImageEventField.VMCATCHER_EVENT_AD_MPURI)).
            hvFormat    (event(ImageEventField.VMCATCHER_EVENT_HV_FORMAT)).
            hvHypervisor(event(ImageEventField.VMCATCHER_EVENT_HV_HYPERVISOR)).
            hvSize      (event(ImageEventField.VMCATCHER_EVENT_HV_SIZE).toLong).
            hvUri       (event(ImageEventField.VMCATCHER_EVENT_HV_URI)).
            slOs        (event(ImageEventField.VMCATCHER_EVENT_SL_OS)).
            slOsName    (event(ImageEventField.VMCATCHER_EVENT_SL_OSNAME)).
            slOsVersion (event(ImageEventField.VMCATCHER_EVENT_SL_OSVERSION))

          Log.info(s"Created $image")
          image
        }

      images
    }
    catch {
      case e: Throwable ⇒
        val name = ref.name.get
        val url = ref.url.get
        throw new VMCatcherException(CannotParseImages, s"Cannot parse images from image list $name at $url")
    }
  }

  def accessImageList(ref: MImageListRef): (MImageListAccess, List[MImage]) = {
    val url = new URL(ref.url.get)
    val upOpt = ref.credentialsOpt
    Sys.downloadRawImageList(url, upOpt) match {
      case Left(t) ⇒
        // Record the failure
        MImageListAccess.create.f_imageListRef(ref).saveNotRetrieved()

        val name = ref.name.get
        throw new VMCatcherException(CannotAccessImageList, s"Cannot access image list $name at $url", t)

      case Right(rawText) ⇒
        // Record the access, one step at a time
        val access = MImageListAccess.create.f_imageListRef(ref).saveRetrieved(rawText)

        // Parse json out of rawText
        val json = getImageListJsonFromRaw(rawText)
        access.saveParsed(json)
        val images = parseImagesFromJson(ref, json)

        (access, images)
    }
  }

  def updateImages(name: String): Unit =
    findImageListRefByName(name) match {
      case None ⇒
        throw new VMCatcherException(ImageListNotFound, s"Image list $name not found")

      case Some(ref) ⇒
        // 1. Access the image list
        val (access, images) = accessImageList(ref)

        // 2. Save new images
        images.foreach(_.save())

        // 3. Update the current view
        val currentImages = MCurrentImage.findAllOfImageListRef(ref)
        currentImages.foreach(_.delete_!)
        val newCurrentImages =
          for {
            image ← images
          } yield {
            MCurrentImage.create.
              f_image(image).
              f_imageListRef(ref).
              f_imageListAccess(access)
          }
        newCurrentImages.foreach(_.save())
    }

  def currentImageList(name: String): List[MCurrentImage] =
    findImageListRefByName(name) match {
      case None ⇒
        throw new VMCatcherException(ImageListNotFound, s"Image list $name not found")

      case Some(ref) ⇒
        MCurrentImage.findAll(By(MCurrentImage.f_imageListRef, ref))
    }
}