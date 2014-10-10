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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 *
 */
public enum EventFieldSection {
    External (Arrays.asList(ExternalEventField.values())),
    ImageList(Arrays.asList(ImageListEventField.values())),
    Image    (Arrays.asList(ImageEventField.values()));

    private List<IEventField> fields;

    EventFieldSection(List<? extends IEventField> fields) {
        this.fields = Collections.unmodifiableList(fields);
    }

    public List<IEventField> getFields() {
        return fields;
    }

    public List<String> getJsonFieldNames() {
        final List<String> names = new ArrayList<String>(fields.size());
        for(final IEventField field : fields) {
            names.add(field.jsonField());
        }
        return names;
    }

    public static List<IEventField> allFields() {
        final List<IEventField> all = new ArrayList<IEventField>(External.fields.size() + ImageList.fields.size() + Image.fields.size());
        all.addAll(External.fields);
        all.addAll(ImageList.fields);
        all.addAll(Image.fields);

        return all;
    }
}
