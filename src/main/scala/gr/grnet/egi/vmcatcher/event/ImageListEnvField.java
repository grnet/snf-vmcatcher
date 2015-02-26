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

import java.util.HashMap;
import java.util.Map;

import static gr.grnet.egi.vmcatcher.event.EnvFieldCategory.ImageList;

/**
 *
 */
public enum ImageListEnvField implements IEnvField {
    VMCATCHER_EVENT_IL_DC_IDENTIFIER("dc:identifier");

    private static final Map<String, ImageListEnvField> reverseMap;
    static {
        reverseMap = new HashMap<String, ImageListEnvField>();
        for(ImageListEnvField eventField: ImageListEnvField.values()) {
            reverseMap.put(eventField.imageListJsonAttribute(), eventField);
        }
    }

    public final String imageListJsonAttribute;

    ImageListEnvField(String imageListJsonAttribute) {
        this.imageListJsonAttribute = imageListJsonAttribute;
    }

    public EnvFieldCategory section() { return ImageList; }

    public String imageListJsonAttribute() { return imageListJsonAttribute; }

    public static ImageListEnvField ofImageListJsonAttribute(String jsonAttribute) throws IllegalArgumentException {
        ImageListEnvField field = reverseMap.get(jsonAttribute);
        if(field != null) { return field; }
        throw new IllegalArgumentException("Unknown json field '" + jsonAttribute + "'");
    }
}
