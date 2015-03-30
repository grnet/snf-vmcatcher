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

package gr.grnet.egi.vmcatcher.db

import gr.grnet.egi.vmcatcher.Digest
import net.liftweb.common.{Box, Full}
import net.liftweb.mapper._

/**
 * Holds the text for image lists, image json etc.
 */
class MText extends LongKeyedMapper[MText] with IdPK {
  def getSingleton = MText

  object sha256 extends MappedString(this, 32 * 2) {
    override def dbColumnName = "sha_256"
    override def dbNotNull_? = true
  }

  object textData extends MappedText(this) {
    override def dbColumnName = "text_data"
    override def dbNotNull_? = true
  }
}

object MText extends MText with LongKeyedMetaMapper[MText] {
  override def dbTableName = "TEXT_DATA"

  def getOrCreate(textData: String): Box[MText] = {
    val hash = Digest.hexSha256(textData)

    MText.find(
      By(MText.sha256, hash)
    ) match {
      case f @ Full(_) ⇒ f
      case _ ⇒ Full(
        MText.create.
        sha256(hash).
        textData(textData).
        saveMe())
    }
  }
}


