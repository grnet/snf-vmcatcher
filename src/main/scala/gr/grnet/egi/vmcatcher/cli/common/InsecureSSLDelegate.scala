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

package gr.grnet.egi.vmcatcher.cli.common

import com.beust.jcommander.Parameter

/**
 *
 */
class InsecureSSLDelegate {
  @Parameter(
    names = Array("-insecure-ssl", "-insecure-SSL"),
    description = "If set to true, SSL validation errors are ignored." +
      " This might be useful for images that are behind self-signed SSL certificates." +
      " Use at your own risk !",
    required = false
  )
  val insecureSSL = false
}
