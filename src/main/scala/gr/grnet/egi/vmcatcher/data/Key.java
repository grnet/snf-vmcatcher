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

package gr.grnet.egi.vmcatcher.data;

/**
 * @author Christos KK Loverdos <loverdos@gmail.com>
 */
public enum Key {
    // Event
    VMCATCHER_EVENT_DC_DESCRIPTION,
    VMCATCHER_EVENT_DC_IDENTIFIER,
    VMCATCHER_EVENT_DC_TITLE,
    VMCATCHER_EVENT_HV_HYPERVISOR,
    VMCATCHER_EVENT_HV_SIZE,
    VMCATCHER_EVENT_HV_URI,
    VMCATCHER_EVENT_SL_ARCH,
    VMCATCHER_EVENT_SL_CHECKSUM_SHA512,
    VMCATCHER_EVENT_SL_COMMENTS,
    VMCATCHER_EVENT_SL_OS,
    VMCATCHER_EVENT_SL_OSVERSION,
    VMCATCHER_EVENT_TYPE,
    VMCATCHER_EVENT_FILENAME,
    VMCATCHER_EVENT_IL_DC_IDENTIFIER,
    VMCATCHER_EVENT_HV_FORMAT,

    // Configuration
    VMCATCHER_RDBMS,
    VMCATCHER_CACHE_EVENT,
    VMCATCHER_LOG_CONF,
    VMCATCHER_DIR_CERT,
    VMCATCHER_CACHE_DIR_CACHE,
    VMCATCHER_CACHE_DIR_DOWNLOAD,
    VMCATCHER_CACHE_DIR_EXPIRE,
    VMCATCHER_CACHE_ACTION_DOWNLOAD,
    VMCATCHER_CACHE_ACTION_CHECK,
    VMCATCHER_CACHE_ACTION_EXPIRE
}
