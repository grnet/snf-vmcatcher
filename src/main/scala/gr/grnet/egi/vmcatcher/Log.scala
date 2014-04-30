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

import org.slf4j.Logger

/**
 *
 * @author Christos KK Loverdos <loverdos@gmail.com>
 */
class Log(slf4j: Logger) {
  def DEBUG(s: String) = if(slf4j.isDebugEnabled) slf4j.debug("{}", s)
  def INFO (s: String) = if(slf4j.isInfoEnabled ) slf4j.info ("{}", s)
  def WARN (s: String) = if(slf4j.isWarnEnabled ) slf4j.warn ("{}", s)
  def ERROR(s: String) = if(slf4j.isErrorEnabled) slf4j.error("{}", s)
}
