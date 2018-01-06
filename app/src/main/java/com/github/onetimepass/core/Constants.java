package com.github.onetimepass.core;
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

import android.view.Gravity;

/**
 * Constant values for configuring / tuning application behaviors.
 */
public class Constants {

    /**
     * The type Version.
     */
    public static final class Version {
        /**
         * The constant MAJOR.
         */
        public static final int MAJOR = 1;
        /**
         * The constant MAX_MAGIC.
         */
        public static final int MAX_MAGIC = 10;
    }

    /**
     * The constant IDLE_TIMEOUT.
     */
    public static final long IDLE_TIMEOUT = (3 * (60 * 1000)); // 3 minutes
    /**
     * The constant IDLE_EXPIRE.
     */
    public static final long IDLE_EXPIRE = (30 * 1000); // 30 seconds
    /**
     * The constant IDLE_INTERVAL.
     */
    public static final long IDLE_INTERVAL = (1 * 1000); // 1 second

    /**
     * The constant REQ_PERM_CAMERA.
     */
    public static final int REQ_PERM_CAMERA = 111;
    /**
     * The constant REQ_PERM_RWFILE.
     */
    public static final int REQ_PERM_RWFILE = 222;

    /**
     * The constant TOAST_GRAVITY.
     */
    public static final int TOAST_GRAVITY = Gravity.CENTER;
    /**
     * The constant TOAST_OFFSET_X.
     */
    public static final int TOAST_OFFSET_X = 0;
    /**
     * The constant TOAST_OFFSET_Y.
     */
    public static final int TOAST_OFFSET_Y = -64;

    /**
     * The constant ACTION_ALARM.
     */
    public static final String ACTION_ALARM = "com.github.onetimepass.ACTION_ALARM";
}
