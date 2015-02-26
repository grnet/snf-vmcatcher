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
 * Where an event field belongs to.
 */
public enum EnvFieldCategory {
    External (Arrays.asList(ExternalEnvField.values())),
    ImageList(Arrays.asList(ImageListEnvField.values())),
    Image    (Arrays.asList(ImageEnvField.values()));

    private List<IEnvField> fields;

    EnvFieldCategory(List<? extends IEnvField> fields) {
        this.fields = Collections.unmodifiableList(fields);
    }

    public List<IEnvField> getFields() {
        return fields;
    }

    public List<String> getJsonFieldNames() {
        final List<String> names = new ArrayList<String>(fields.size());
        for(final IEnvField field : fields) {
            names.add(field.imageListJsonAttribute());
        }
        return names;
    }

    public static List<IEnvField> allFields() {
        final List<IEnvField> all = new ArrayList<IEnvField>(External.fields.size() + ImageList.fields.size() + Image.fields.size());
        all.addAll(External.fields);
        all.addAll(ImageList.fields);
        all.addAll(Image.fields);

        return all;
    }

    public static String imageListJsonAttributeOfEnvField(String envFieldName) {
        try {
            return ImageEnvField.valueOf(envFieldName).imageListJsonAttribute();
        }
        catch(IllegalArgumentException e) {
            try {
                return ImageListEnvField.valueOf(envFieldName).imageListJsonAttribute();
            }
            catch(IllegalArgumentException e1) {
                return ExternalEnvField.valueOf(envFieldName).imageListJsonAttribute();
            }
        }
    }

    public static IEnvField envFieldOfImageListJsonAttribute(String jsonAttr) {
        try {
            return ImageEnvField.ofImageListJsonAttribute(jsonAttr);
        }
        catch(IllegalArgumentException e) {
            try {
                return ImageListEnvField.ofImageListJsonAttribute(jsonAttr);
            }
            catch(IllegalArgumentException e1) {
                return ExternalEnvField.valueOf(jsonAttr);
            }
        }
    }

    public static boolean existsImageListJsonAttribute(String jsonAttr) {
        try {
            envFieldOfImageListJsonAttribute(jsonAttr);
            return true;
        }
        catch(IllegalArgumentException e) {
            return false;
        }
    }
}
