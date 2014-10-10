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

import static gr.grnet.egi.vmcatcher.event.EventFieldSection.ImageList;

/**
 *
 */
public enum ImageListEventField implements IEventField {
    VMCATCHER_EVENT_IL_DC_IDENTIFIER("dc:identifier");

    public final String jsonField;

    ImageListEventField(String jsonField) {
        this.jsonField = jsonField;
    }

    public EventFieldSection section() { return ImageList; }

    public String jsonField() { return jsonField; }
}
