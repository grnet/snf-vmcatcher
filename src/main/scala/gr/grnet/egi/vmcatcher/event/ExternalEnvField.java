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

import static gr.grnet.egi.vmcatcher.event.EnvFieldCategory.External;

/**
 *
 */
public enum ExternalEnvField implements IEnvField {
    VMCATCHER_EVENT_TYPE,
    VMCATCHER_EVENT_AD_MPURI,
    VMCATCHER_EVENT_FILENAME,
    VMCATCHER_CACHE_DIR_CACHE,
    VMCATCHER_EVENT_UUID_SESSION,
    VMCATCHER_EVENT_VO,
    VMCATCHER_X_EVENT_IMAGE_LIST_URL; // Custom, not present in original vmcatcher

    public EnvFieldCategory section() { return External; }

    public String imageListJsonAttribute() { return ""; }
}
