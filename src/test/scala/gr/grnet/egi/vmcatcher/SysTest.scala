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

import org.junit.{Assert, Test}

/**
 *
 */
class SysTest {
  @Test
  def fileExt0(): Unit = {
    val ext = ".bar"

    Assert.assertEquals(ext, Sys.fileExtension(ext))
  }

  @Test
  def fileExt(): Unit = {
    val ext = ".bar"
    val name = s"foo$ext"

    Assert.assertEquals(ext, Sys.fileExtension(name))
  }

  @Test
  def filePreExt(): Unit = {
    val pre = ".pre"
    val ext = ".ext"
    val name = s"foo${pre}$ext"
    Assert.assertEquals(pre, Sys.filePreExtension(name))
  }

  @Test
  def dropExt0(): Unit = {
    val ext = ".bar"
    val foo = ext // No file extension to drop if the filename is the extension
    val name = s"$foo$ext"

    Assert.assertEquals(foo, Sys.dropFileExtension(name))
  }

  @Test
  def dropExt(): Unit = {
    val ext = ".bar"
    val foo = "foo"
    val name = s"$foo$ext"

    Assert.assertEquals(foo, Sys.dropFileExtension(name))
  }

  @Test
  def dropExts(): Unit = {
    val exts = ".foo.bar.z"
    val game = "game"
    val name = s"$game$exts"

    Assert.assertEquals(game, Sys.dropFileExtensions(name))
  }
}
