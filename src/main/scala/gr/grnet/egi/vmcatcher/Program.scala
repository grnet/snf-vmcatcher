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

import org.slf4j.LoggerFactory

/**
 *
 */
trait Program {
  type Cmd = Runnable

  val t0 = System.currentTimeMillis()
  var _args = Array[String]()
  lazy val argsDebugStr = _args.mkString(" ")

  final val ProgName = getClass.getName.stripSuffix("$")
  final val Log = LoggerFactory.getLogger(getClass)

  def beginSequence(args: Array[String]): Unit = {
    _args = args
    Log.info("=" * 30)
    Log.info(s"BEGIN snf-vmcatcher ($t0) [$argsDebugStr]")
  }

  def endSequence(): Unit = {
    val t1 = System.currentTimeMillis()
    val dtms = t1 - t0
    Log.info(s"END snf-vmcatcher ($dtms ms) [$argsDebugStr]")
    Log.info("=" * 30)
  }

  def ERROR(s: String): Unit = {
    System.err.println(s)
    Log.error(s)
  }

  def INFO(s: String): Unit = {
    System.err.println(s)
    Log.info(s)
  }

  def EXIT(status: Int, alsoDo: () ⇒ Any = () ⇒ ()): Nothing = {
    Log.warn(s"Exiting with status $status")
    alsoDo()
    sys.exit(status)
  }
}
