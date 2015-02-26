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

import static gr.grnet.egi.vmcatcher.event.EnvFieldCategory.Image;

/**
 *
 */
public enum ImageEnvField implements IEnvField {
    VMCATCHER_EVENT_DC_DESCRIPTION("dc:description"),
    VMCATCHER_EVENT_DC_IDENTIFIER("dc:identifier"),
    VMCATCHER_EVENT_DC_TITLE("dc:title"),
    VMCATCHER_EVENT_HV_HYPERVISOR("hv:hypervisor"),
    VMCATCHER_EVENT_HV_SIZE("hv:size"),
    VMCATCHER_EVENT_HV_URI("hv:uri"),
    VMCATCHER_EVENT_HV_VERSION("hv:version"),
    VMCATCHER_EVENT_HV_FORMAT("hv:format"),
    VMCATCHER_EVENT_SL_CHECKSUM_SHA512("sl:checksum:sha512"),
    VMCATCHER_EVENT_SL_COMMENTS("sl:comments"),
    VMCATCHER_EVENT_SL_OS("sl:os"),
    VMCATCHER_EVENT_SL_ARCH("sl:arch"),
    VMCATCHER_EVENT_SL_OSVERSION("sl:osversion"),
    VMCATCHER_EVENT_SL_OSNAME("sl:osname"),
    VMCATCHER_EVENT_AD_GROUP("ad:group"),
    VMCATCHER_EVENT_AD_MPURI("ad:mpuri"),
    VMCATCHER_EVENT_AD_USER_FULLNAME("ad:user:fullname"),
    VMCATCHER_EVENT_AD_USER_GUID("ad:user:guid"),
    VMCATCHER_EVENT_AD_USER_URI("ad:user:uri"),
    ;

    private static final Map<String, ImageEnvField> reverseMap;
    static {
        reverseMap = new HashMap<String, ImageEnvField>();
        for(ImageEnvField eventField: ImageEnvField.values()) {
            reverseMap.put(eventField.imageListJsonAttribute(), eventField);
        }
    }

    public final String imageListJsonAttribute;

    ImageEnvField(String imageListJsonAttribute) {
        this.imageListJsonAttribute = imageListJsonAttribute;
    }

    public EnvFieldCategory section() { return Image; }

    public String imageListJsonAttribute() { return imageListJsonAttribute; }

    public static ImageEnvField ofImageListJsonAttribute(String jsonAttribute) throws IllegalArgumentException {
        ImageEnvField field = reverseMap.get(jsonAttribute);
        if(field != null) { return field; }
        throw new IllegalArgumentException("Unknown json attribute '" + jsonAttribute + "'");
    }
}
