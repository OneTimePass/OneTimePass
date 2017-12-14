package com.github.onetimepass.core.screen;
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

import android.view.MenuItem;

/**
 * The interface Screen interface.
 */
public interface ScreenInterface {
    /**
     * On options item selected boolean.
     *
     * @param item the item
     * @return the boolean
     */
    boolean onOptionsItemSelected(MenuItem item);
}
