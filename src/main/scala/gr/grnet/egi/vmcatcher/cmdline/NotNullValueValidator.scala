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

package gr.grnet.egi.vmcatcher.cmdline

import com.beust.jcommander.{ParameterException, IValueValidator}

/**
 *
 */
class NotNullValueValidator[T <: AnyRef] extends IValueValidator[T] {
  def validate(name: String, value: T): Unit =
    value match {
      case null ⇒ throw new ParameterException(s"Parameter $name is null")
      case _    ⇒
    }
}
