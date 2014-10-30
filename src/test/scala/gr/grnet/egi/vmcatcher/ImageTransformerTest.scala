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

import gr.grnet.egi.vmcatcher.image.{IdentityTransformer, ImageTransformers}
import org.junit.{Assert, Test}
import org.slf4j.LoggerFactory

/**
 *
 */
class ImageTransformerTest {
  final val Log = LoggerFactory.getLogger("test")
  final val Identity = new IdentityTransformer

  @Test
  def findOne(): Unit = {
    val file = new File("treeThreader.qcow2.gz")
    assert(ImageTransformers.findForFile(Log, file).isDefined)
  }

  @Test
  def suggestTransformedFilename2(): Unit = {
    val filename = "foo.cow.gz"
    val expected = "foo.cow"
    val suggestedFilename = Identity.suggestTransformedFilename(None, filename)

    Assert.assertEquals(expected, suggestedFilename)
  }

  @Test
  def suggestTransformedFilename1(): Unit = {
    val filename = "foo.cow"
    val expected = "foo"
    val suggestedFilename = Identity.suggestTransformedFilename(None, filename)

    Assert.assertEquals(expected, suggestedFilename)
  }

  @Test
  def suggestTransformedFilename1Format(): Unit = {
    val filename = "random.irrelevant"
    val formatOpt = Some(".cow")
    val expected = filename
    val suggestedFilename = Identity.suggestTransformedFilename(formatOpt, filename)

    Assert.assertEquals(expected, suggestedFilename)
  }

  @Test
  def suggestTransformedFilename2Format(): Unit = {
    val filename = "random.irrelevant"
    val formatOpt = None
    val expected = "random"
    val suggestedFilename = Identity.suggestTransformedFilename(formatOpt, filename)

    Assert.assertEquals(expected, suggestedFilename)
  }

  @Test
  def suggestTransformedFilename3Format(): Unit = {
    val filename = "random.irrelevant"
    val formatOpt = Some(".cow.gz")
    val expected = filename
    val suggestedFilename = Identity.suggestTransformedFilename(formatOpt, filename)

    Assert.assertEquals(expected, suggestedFilename)
  }
}
