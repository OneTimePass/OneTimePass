package com.github.onetimepass.core.control;
/*
 This software is free software; you can redistribute it and/or
 modify it under the terms of the GNU Lesser General Public
 License as published by the Free Software Foundation; specifically
 version 2.1 of the License and not any other version.

 This software is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 Lesser General Public License for more details.

 You should have received a copy of the GNU Lesser General Public
 License along with this library; if not, write to the Free Software
 Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
*/

import android.content.Context;

/**
 * The interface Control action.
 */
public interface ControlAction {
    /**
     * Gets tag.
     *
     * @return the tag
     */
    String getTag();

    /**
     * Needs to be alive boolean.
     *
     * @return the boolean
     */
    boolean needsToBeAlive();

    /**
     * Perform action.
     *
     * @param context the context
     * @param data    the data
     */
    void performAction(final Context context, final String[] data);
}
