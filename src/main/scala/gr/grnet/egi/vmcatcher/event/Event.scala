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

package gr.grnet.egi.vmcatcher.event

import com.typesafe.config.{Config ⇒ TConfig}
import gr.grnet.egi.vmcatcher.{Config, Json}

import scala.collection.JavaConverters._

/**
 *
 */
sealed trait Event {
  def originator: EventOriginator

  def has(field: IEventField): Boolean

  def apply(field: IEventField, default: String = ""): String

  def get(field: IEventField): Option[String] = if(has(field)) Some(apply(field)) else None

  def toMap: Map[String, String]

  def toJson: String = Json.jsonOfMap(toMap)

  override def toString = toJson
}

/**
 * An [[gr.grnet.egi.vmcatcher.event.Event]] produced by a map.
 * @param map
 */
class MapEvent(val originator: EventOriginator, map: Map[String, String]) extends Event {
  def has(field: IEventField) = map.contains(field.name)

  def apply(field: IEventField, default: String = "") = map.getOrElse(field.name, default)

  override def get(field: IEventField): Option[String] = map.get(field.name)

  def toMap: Map[String, String] = map
}

object Event {
  def SysEnv = sys.env
  def AllFieldNames = EventFieldSection.allFields().asScala.map(_.name()).toSeq
  def ExistingFieldPairs = AllFieldNames.flatMap(name ⇒ SysEnv.get(name).map(value ⇒ (name, value)))
  def ExistingFields = Map(ExistingFieldPairs:_*)

  def ofSysEnv: Event = new MapEvent(EventOriginator.vmcatcher_sysenv, ExistingFields)

  /**
   *
   * @param json As submitted to the queue
   * @return
   */
  def ofJson(json: String): Event = {
    val map = Json.stringMapOfJson(json)
    new MapEvent(EventOriginator.vanilla_json, map)
  }


  // Fix the Key.
  // _       _
  // the Config library does not like the ':' token inside keys, unless it is quoted.
  def fk(key: String) = key.replaceAll(":", "\":\"")
}

object Events {
  import Event.fk
  def ofImageList(
    json: String,
    externalFieldsMap: Map[ExternalEventField, String]
  ): List[Event] = {

    val imageListConfig = Config.ofString(json).getConfig(fk("hv:imagelist"))
    val jconfigList = imageListConfig.getConfigList(fk("hv:images"))
    val imageConfigList = jconfigList.asScala.map(_.getConfig(fk("hv:image"))).toList

    imageConfigList.map(
      new ImageConfigEvent(
        EventOriginator.image_list_json,
        imageListConfig,
        _,
        externalFieldsMap
      )
    )
  }
}

/**
 * An [[gr.grnet.egi.vmcatcher.event.Event]] produced by an image list.
 *
 * @param originator
 * @param imageListConfig
 * @param imageConfig
 * @param externalFields
 */
class ImageConfigEvent(
  val originator: EventOriginator,
  imageListConfig: TConfig,
  imageConfig: TConfig,
  externalFields: Map[ExternalEventField, String]
) extends Event {

  def has(field: IEventField) = {
    lazy val path = Event.fk(field.jsonField)

    field.section match {
      case EventFieldSection.ImageList ⇒
        imageListConfig.hasPath(path)

      case EventFieldSection.Image ⇒
        imageConfig.hasPath(path)

      case EventFieldSection.External ⇒
        field match {
          case external: ExternalEventField ⇒
            externalFields.contains(external)

          case _ ⇒
            false
        }
    }
  }


  def apply(field: IEventField, default: String = "") = {
    lazy val path = Event.fk(field.jsonField)
    def getOrElse(config: TConfig) = if(config.hasPath(path)) config.getString(path) else default

    field.section match {
      case EventFieldSection.ImageList ⇒
        getOrElse(imageListConfig)

      case EventFieldSection.Image ⇒
        getOrElse(imageConfig)

      case EventFieldSection.External ⇒
        field match {
          case external: ExternalEventField ⇒
            externalFields.getOrElse(external, default)

          case _ ⇒
            default
        }
    }
  }

  def toMap: Map[String, String] = {
    val imageMap = Config.toMap(imageConfig)
    val imageListMap = Config.toMap(imageListConfig)
    val externalMap = externalFields.map{ case (k, v) ⇒ (k.name, v) }

    imageMap ++ imageListMap ++ externalMap
  }
}
