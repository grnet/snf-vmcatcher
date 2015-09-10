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

package gr.grnet.egi.vmcatcher.api
package impl

import java.util.Locale

import com.typesafe.config.ConfigFactory
import gr.grnet.egi.vmcatcher.config.IaaSConfig
import gr.grnet.egi.vmcatcher.{ErrorCode, Sys, VMCatcherException}
import org.slf4j.LoggerFactory

import scala.collection.JavaConverters._

class KamakiBasedIaaS(config: IaaSConfig) extends IaaS {
  final val log = LoggerFactory.getLogger(getClass)
  val cloud = config.getCloud


  def listImages() = {
    val (exitCode, stdout) = Sys.execRead(log,
      "kamaki",
      "--cloud", cloud,
      "image", "list",
      "--details",
      "--output-format", "json"
    )

    if(exitCode != 0) {
      throw new VMCatcherException(ErrorCode.CannotGetRegisteredImages, s"Could not get registered images from IaaS")
    }

    val jsonObj = s"""{"k":$stdout}"""
    val config = ConfigFactory.parseString(jsonObj)
    val list = config.getConfigList("k").asScala.toList
    val (fromVmCatcher, other) = list.partition { c ⇒
      c.hasPath("properties") &&
        c.getConfig("properties").root().keySet().asScala.
          map(_.toLowerCase(Locale.ENGLISH)).
          exists(_.startsWith("vmcatcher_"))
    }

    (fromVmCatcher, other)
  }

  def listRegisteredImages(): List[(String, String)] = {
    val (exitCode, stdout) = Sys.execRead(log,
      "kamaki",
      "--cloud", cloud,
      "image", "list",
      "--details",
      "--output-format", "json"
    )

    if(exitCode != 0) {
      throw new VMCatcherException(ErrorCode.CannotGetRegisteredImages, s"Could not get registered images from IaaS")
    }

    // The result is a JSON array of objects
    // Each object describes a registered image and is of the form
    //
    //{
    //  "status": "AVAILABLE",
    //  "name": "Bitnami-joomla",
    //  "checksum": "4bb46f76c26716c372198612e32396b115615eb4570428c9c034edb74da4cb87",
    //  "created_at": "2015-02-17 14:03:20",
    //  "disk_format": "diskdump",
    //  "updated_at": "2015-02-17 14:03:20",
    //  "id": "7c8f6173-aba8-455d-afd1-c1968d31de19",
    //  "is_snapshot": false,
    //  "location": "pithos://bbab7241-fc27-4d30-bf30-bf361c60491c/images/bitnami-joomla.disklabel",
    //  "container_format": "bare",
    //  "owner": "bbab7241-fc27-4d30-bf30-bf361c60491c",
    //  "is_public": true,
    //  "deleted_at": "",
    //  "properties": {
    //    "partition_table": "msdos",
    //    "kernel": "3.13.0-36-generic",
    //    "osfamily": "linux",
    //    "description": "Ubuntu 14.04.1 LTS",
    //    "remote_connection": "ssh:port=22,user=bitnami",
    //    "gui": "No GUI",
    //    "sortorder": "7801404",
    //    "users": "bitnami",
    //    "os": "ubuntu",
    //    "root_partition": "1",
    //    "swap": "2:976"
    //  },
    //  "size": 1298296320
    //}
    val jsonObj = s"""{"k":$stdout}"""
    val config = ConfigFactory.parseString(jsonObj)
    val list = config.getConfigList("k").asScala.toList

    val vmCatcherImages =
      list.filter { image ⇒
        val propperties = image.getConfig("properties")
        val propMap = propperties.root().unwrapped()
        val propKeys = propMap.keySet().asScala

        // Keep those images for which there is at least one property starting with "vmcatcher_".
        // We need to cmp lowercase because we do not how the external tool handles case.
        propKeys.exists( key ⇒ key.toLowerCase(Locale.ENGLISH).startsWith("vmcatcher_"))
      }

    val imageInfo =
      for {
        image ← vmCatcherImages
      } yield {
        val id = image.getString("id")
        val name = image.getString("name")

        (id, name)
      }

    imageInfo

  }
}
