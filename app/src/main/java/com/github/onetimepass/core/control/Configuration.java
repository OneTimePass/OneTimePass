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

/**
 * The type Configuration.
 */
public class Configuration {
    /**
     * The Tag.
     */
    public final String  tag;        // [a-z][_a-z0-9]+
    /**
     * The Name.
     */
    public final int     name;       // [a-z][_a-z0-9]+
    /**
     * The Title.
     */
    public final int     title;      // freeform
    /**
     * The Layout.
     */
    public final int     layout;     // R.id.*
    /**
     * The Menu group.
     */
    public final int     menu_group; // R.id.*
    /**
     * The Opt group.
     */
    public final int     opt_group;  // R.id.*
    /**
     * The Show home.
     */
    public final boolean show_home;  // icon vs back navigation
    /**
     * The Handle uri.
     */
    public final boolean handle_uri; // screen receives Uri intent data
    /**
     * The Back quits.
     */
    public final boolean back_quits; // back press quits instead of default

    /**
     * Instantiates a new Configuration.
     *
     * @param tag        the tag
     * @param name       the name
     * @param title      the title
     * @param layout     the layout
     * @param menu_group the menu group
     * @param opt_group  the opt group
     * @param show_home  the show home
     * @param handle_uri the handle uri
     * @param back_quits the back quits
     */
    public Configuration(String tag, int name, int title,
                         int layout, int menu_group, int opt_group,
                         boolean show_home, boolean handle_uri, boolean back_quits) {
        this.tag        = tag;
        this.name       = name;
        this.title      = title;
        this.layout     = layout;
        this.menu_group = menu_group;
        this.opt_group  = opt_group;
        this.show_home  = show_home;
        this.handle_uri = handle_uri;
        this.back_quits = back_quits;
    }
}
