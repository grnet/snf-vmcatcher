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

package gr.grnet.egi.vmcatcher.queue

/**
 *
 */
sealed trait QueueConnectAttempt {
  def isFirstAttempt: Boolean = false
  def firstAttemptMillis: Long
  def failedAttempts: Long
  def toFailed = QueueConnectFailedAttempt(firstAttemptMillis, failedAttempts + 1)
}

case class QueueConnectFirstAttempt(firstAttemptMillis: Long) extends QueueConnectAttempt {
  override def isFirstAttempt: Boolean = true

  def failedAttempts: Long = 0L
}

case class QueueConnectFailedAttempt(firstAttemptMillis: Long, failedAttempts: Long) extends QueueConnectAttempt

case class QueueConnectSuccess(firstAttemptMillis: Long, failedAttempts: Long) extends QueueConnectAttempt
