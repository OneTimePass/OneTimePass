package com.github.onetimepass.core.account;
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

import java.util.List;

/**
 * The Account-related interfaces necessary for handling drag'n'drop and other
 * end-user interactions.
 */
public class AccountInterface {

    /**
     * The interface On drag listener.
     */
    public interface OnDragListener {
        /**
         * On drag begin.
         *
         * @param holder the holder
         */
        void onDragBegin(AccountList.ViewHolder holder);
    }

    /**
     * The interface On drag interaction.
     */
    public interface OnDragInteraction {
        /**
         * On item move boolean.
         *
         * @param src the src
         * @param dst the dst
         * @return the boolean
         */
        boolean onItemMove(int src, int dst);

        /**
         * On item dismiss.
         *
         * @param position the position
         */
        void onItemDismiss(int position);
    }

    /**
     * The interface On touch interaction.
     */
    public interface OnTouchInteraction {
        /**
         * On item selected.
         */
        void onItemSelected();

        /**
         * On item clear.
         */
        void onItemClear();
    }

    /**
     * The interface On list interaction.
     */
    public interface OnListInteraction {
        /**
         * On list interaction.
         *
         * @param entry the entry
         */
        void onListInteraction(AccountEntry entry);

        /**
         * On list interaction.
         *
         * @param entries the entries
         */
        void onListInteraction(List<AccountEntry> entries);
    }
}
