/* This file is part of VoltDB.
 * Copyright (C) 2008-2015 VoltDB Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with VoltDB.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.voltdb.importer;

import java.util.NavigableSet;
import java.util.Set;

import com.google_voltpatches.common.collect.Sets;

public class ChannelAssignment {

    final Set<ChannelSpec> added;
    final Set<ChannelSpec> removed;
    final NavigableSet<ChannelSpec> channels;
    final int version;

    ChannelAssignment(NavigableSet<ChannelSpec> prev, NavigableSet<ChannelSpec> next, int version) {
        this.version  = version;
        this.added    = Sets.difference(next, prev);
        this.removed  = Sets.difference(prev, next);
        this.channels = next;
    }

    public Set<ChannelSpec> getAdded() {
        return added;
    }

    public Set<ChannelSpec> getRemoved() {
        return removed;
    }

    public NavigableSet<ChannelSpec> getChannels() {
        return channels;
    }

    public int getVersion() {
        return version;
    }

    public boolean hasChanges() {
        return !removed.isEmpty() || !added.isEmpty();
    }

    @Override
    public String toString() {
        return "ChannelAssignment [added=" + added + ", removed=" + removed
                + ", channels=" + channels + ", version=" + version + "]";
    }
}
