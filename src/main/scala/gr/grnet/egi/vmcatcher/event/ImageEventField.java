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

package gr.grnet.egi.vmcatcher.event;

import static gr.grnet.egi.vmcatcher.event.EventFieldSection.Image;

/**
 *
 */
public enum ImageEventField implements IEventField {
    VMCATCHER_EVENT_DC_DESCRIPTION("dc:description"),
    VMCATCHER_EVENT_DC_IDENTIFIER("dc:identifier"),
    VMCATCHER_EVENT_DC_TITLE("dc:title"),
    VMCATCHER_EVENT_HV_HYPERVISOR("hv:hypervisor"),
    VMCATCHER_EVENT_HV_SIZE("hv:size"),
    VMCATCHER_EVENT_HV_URI("hv:uri"),
    VMCATCHER_EVENT_HV_VERSION("hv:version"),
    VMCATCHER_EVENT_HV_FORMAT("hv:format"),
    VMCATCHER_EVENT_SL_ARCH("sl:arch"),
    VMCATCHER_EVENT_SL_CHECKSUM_SHA512("sl:checksum:sha512"),
    VMCATCHER_EVENT_SL_COMMENTS("sl:comments"),
    VMCATCHER_EVENT_SL_OS("sl:os"),
    VMCATCHER_EVENT_SL_OSVERSION("sl:osversion");

    public final String jsonField;

    ImageEventField(String jsonField) {
        this.jsonField = jsonField;
    }

    public EventFieldSection section() { return Image; }

    public String jsonField() { return jsonField; }
}
