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

package gr.grnet.egi.vmcatcher.shell

import java.net.URL
import java.util.Locale

import com.typesafe.config.{Config, ConfigRenderOptions, ConfigValueFactory}
import gr.grnet.egi.vmcatcher.db.MImageListAccess
import gr.grnet.egi.vmcatcher.util.UsernamePassword
import gr.grnet.egi.vmcatcher.{IaaS, LogHelper, VMCatcher}

import scala.collection.JavaConverters._

/**
 * The Shell implements the CLI commands.
 * Some are directly delegated to [[gr.grnet.egi.vmcatcher.VMCatcher VMCatcher]] and others may need extra business logic.
 * One thing the [[gr.grnet.egi.vmcatcher.shell.Shell Shell]] does is to communicate the outcome using `stderr` or `stdout`
 * accordingly.
 *
 * Gathering all the command implementations here makes them independent of any command-line
 * parsing technology, since here we just have the requirements.
 */
object Shell extends LogHelper {
  object IaaS {
    def describe(iaas: IaaS): Unit = {
      val (fromVmCatcher, otherImages) = iaas.listImages()
      INFO(s"Found ${fromVmCatcher.size} snf-vmcatcher registered images")
      INFO(s"Found ${otherImages.size} other registered images")
    }

    def ls(iaas: IaaS, vmCatcherOnly: Boolean): Unit = {
      def lsImage(config: Config): Unit = {
        val id = config.getString("id")
        val updated_at = config.getString("updated_at")
        val size = config.getString("size")
        val properties = config.getConfig("properties")
        val propertiesRoot = properties.root()
        val vmcatcherPropsMap =
          propertiesRoot.asScala.filter { case (k, v) ⇒
            k.toLowerCase(Locale.ENGLISH).startsWith("vmcatcher_")
          }. map { case (k, v) ⇒
            val vv = String.valueOf(propertiesRoot.get(k).unwrapped())
            k → vv
          }

        val vmcatcherPropsConfig = ConfigValueFactory.fromAnyRef(vmcatcherPropsMap.asJava)
        val jsonVmcatcherProps = vmcatcherPropsConfig.render(ConfigRenderOptions.concise())

        INFO(s"$id $updated_at $size $jsonVmcatcherProps")
      }

      def lsImages(images: List[Config]): Unit = {
        INFO(s"IAAS_UUID IAAS_UPDATED_AT SIZE JSON_PROPERTIES")
        images.foreach(lsImage)
      }

      // main stuff
      val (fromVmCatcher, otherImages) = iaas.listImages()
      INFO(s"Found ${fromVmCatcher.size} snf-vmcatcher registered images")
      lsImages(fromVmCatcher)

      if(!vmCatcherOnly) {
        INFO("")
        INFO(s"Found ${otherImages.size} other registered images")
        lsImages(otherImages)
      }
    }
  }

  object ImageList {
    def register(
      vmcatcher: VMCatcher,
      username: String,
      password: String,
      name: String,
      url: String,
      activate: Boolean
    ): Unit = {
      val upOpt = UsernamePassword.optional(username, password)
      val ref = vmcatcher.registerImageList(name, new URL(url), activate, upOpt)
      INFO(s"Registered $ref")
    }

    def showLists(vmcatcher: VMCatcher): Unit = {
      val ils = vmcatcher.listImageLists()
      val nameLengths = ils.map(_.name.get.length)
      val maxNameLen = nameLengths.foldLeft(0)(_ max _)

      if(ils.nonEmpty) {
        INFO(s"NAME DATE TIME ACTIVE HTTP_USERNAME URL")
      }

      for(il ← ils) {
        val name = il.name.get
        val when = il.whenRegistered.get
        val isActive = il.isActive.get
        val username = il.username.get
        val url = il.url.get

        val nameSlack = " " * (maxNameLen - name.length)
        val isActiveSlack = if(isActive) "  " else " "

        INFO(s"$name$nameSlack $when $isActive$isActiveSlack $username $url")
      }
    }

    def showAccess(vmcatcher: VMCatcher, name: String): Unit = {
      vmcatcher.forImageListByName(name) { _ ⇒
        val accesses = MImageListAccess.findAllByRefName(name)
        for {
          access ← accesses
        } {
          val whenAccessed = access.whenAccessed.get
          val isOK = access.isOK
          val isOKStr = if (isOK) "true " else "false"
          val statusCode = access.httpStatusCode.get
          val msg = if (!isOK) {
            val xmsg = scala.Option(access.exceptionMsg.get).getOrElse("")
            val body = access.f_rawText.obj.map(_.textData.get).openOr("")

            if (!xmsg.isEmpty) xmsg else body
          } else ""

          INFO(s"$whenAccessed $isOKStr $statusCode $msg")
        }
      }
    }

    def ls(vmcatcher: VMCatcher, name: String): Unit = {
      val images = vmcatcher.listImages(name)
      if(images.lengthCompare(0) == 0) { return }

      if(images.nonEmpty) {
        INFO("IDENTIFIER REVISION")
      }
      for { image ← images } {
        val (ident, rev) = image.identifierAndRevision

        INFO(s"$ident $rev")
      }
    }

    def activate(vmcatcher: VMCatcher, name: String): Unit = {
      val wasActive = vmcatcher.activateImageList(name)
      if(wasActive) { INFO(s"Already active") }
      else          { INFO(s"Activated") }
    }

    def deactivate(vmcatcher: VMCatcher, name: String): Unit = {
      val wasActive = vmcatcher.deactivateImageList(name)
      if(wasActive) { INFO(s"Deactivated") }
      else          { INFO(s"Already deactive") }
    }

    def fetch(vmcatcher: VMCatcher, name: String): Unit = {
      val result = vmcatcher.fetchImageList(name)
      val access = result.imageListAccess
      val newImages = result.newImages

      if(access.isOK) {
        INFO(s"Fetched image list $name, found ${newImages.size} new image(s)")
        for {
          image ← newImages
        } {
          INFO(s"Fetched image (identifier:revision) ${image.repr}")
        }
      }
      else {
        val statusCode = access.httpStatusCode.get
        val statusText = access.httpStatusText.get
        val body = access.f_rawText.obj.map(_.textData.get).getOrElse("")
        ERROR(s"Cannot fetch image list $name. HTTP returned status [$statusCode $statusText] and body [$body]")
      }
    }

    def credentials(vmcatcher: VMCatcher, username: String, password: String, name: String): Unit = {
      val upOpt = UsernamePassword.optional(username, password)
      vmcatcher.updateCredentials(name, upOpt)
      if(upOpt.isDefined) { INFO(s"Credentials have been set") }
      else                { INFO(s"Credentials have been cleared") }
    }
  }
}
