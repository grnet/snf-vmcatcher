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

import gr.grnet.egi.vmcatcher.event.{Events, ImageEventField}
import org.junit.Test

/**
 *
 */
class ImageListConfigTest {

  val data =
    """
      |{
      |   "hv:imagelist": {
      |      "dc:date:created": "2014-06-16T12:45:37Z",
      |      "dc:date:expires": "2499-12-31T22:00:00Z",
      |      "dc:description": "Description",
      |      "dc:identifier": "FOOBAR",
      |      "dc:source": "https://appdb.egi.eu/",
      |      "dc:title": "CernVM",
      |      "ad:swid": "810",
      |      "hv:endorser": {
      |         "hv:x509": {
      |            "dc:creator": "EGI Applications Database",
      |            "hv:ca": "",
      |            "hv:dn": "",
      |            "hv:email": "_@_"
      |         }
      |      },
      |      "hv:images": [
      |         {
      |            "hv:image": {
      |               "dc:description": "Description",
      |               "dc:identifier": "FOOBAR_",
      |               "ad:mpuri": "https://FOOBAR_/7f1c5d25-614e-4409-bfa6-625d3ae7b9d4:619/",
      |               "dc:title": "Image [Scientific Linux/6.0/KVM]",
      |               "ad:group": "General group",
      |               "hv:hypervisor": "KVM",
      |               "hv:format": "OVA",
      |               "hv:ram_minimum": "512",
      |               "ad:ram_recommended": "1024",
      |               "hv:core_minimum": "1",
      |               "ad:core_recommended": "4",
      |               "hv:size": "121243136",
      |               "hv:uri": "http://FOO/FOO.ova",
      |               "hv:version": "3.3.0-1",
      |               "sl:arch": "x86_64",
      |               "sl:checksum:sha512": "CHECKSUM",
      |               "sl:comments": "",
      |               "sl:os": "Linux",
      |               "sl:osname": "Scientific Linux",
      |               "sl:osversion": "6.0",
      |               "ad:user:fullname": "FOO BAR",
      |               "ad:user:guid": "FOOBAR__",
      |               "ad:user:uri": "https://FOO_/person/FOO%20BAR"
      |            }
      |         }
      |      ],
      |      "hv:uri": "https://FOO_/image.list",
      |      "hv:version": "3.3.0"
      |   }
      |}
      |
    """.stripMargin
  @Test def test(): Unit = {
    Events.ofImageListContainer(data, Map())
  }

  @Test def test2(): Unit = {
    val eventList = Events.ofImageListContainer(data, Map())
    for {
      event ← eventList
    } {
      val dcIdentifier = event(ImageEventField.VMCATCHER_EVENT_DC_IDENTIFIER)
      println(dcIdentifier)
      println(event.toJson)
    }
  }
}
