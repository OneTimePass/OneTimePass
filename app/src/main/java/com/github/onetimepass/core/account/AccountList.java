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

import android.graphics.Canvas;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.ItemTouchHelper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.github.onetimepass.R;
import com.github.onetimepass.core.Notify;
import com.github.onetimepass.core.SupportBar;

import java.util.ArrayList;
import java.util.Collections;

/**
 * The Account list view manager
 */
public class AccountList
        extends RecyclerView.Adapter<AccountList.ViewHolder>
        implements AccountInterface.OnDragInteraction
{
    private ArrayList<AccountEntry> mAccountEntries;
    private ArrayList<AccountEntry> mVisibleEntries;
    private AccountInterface.OnDragListener mOnDragListener;
    private AccountInterface.OnListInteraction mOnListInteraction;

    /**
     * Instantiates a new Account list.
     *
     * @param entries     the entries
     * @param listener    the listener
     * @param interaction the interaction
     */
    public AccountList(ArrayList<AccountEntry> entries,
                       AccountInterface.OnDragListener listener,
                       AccountInterface.OnListInteraction interaction) {
        Notify.Debug();
        if (entries != null)
            mAccountEntries = entries;
        else
            mAccountEntries = new ArrayList<AccountEntry>();
        mVisibleEntries = new ArrayList<AccountEntry>();
        reloadVisibleEntries();
        mOnDragListener = listener;
        mOnListInteraction = interaction;
        notifyDataSetChanged();
    }

    private void reloadVisibleEntries() {
        mVisibleEntries.clear();
        for (int i=0; i < mAccountEntries.size(); i++) {
            mVisibleEntries.add(mAccountEntries.get(i));
        }
    }

    @Override
    public int getItemCount() {
        return mVisibleEntries.size();
    }

    /**
     * Gets full item count.
     *
     * @return the full item count
     */
    public int getFullItemCount() {
        if (mAccountEntries != null)
            return mAccountEntries.size();
        return -1;
    }

    @Override
    public AccountList.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater
                .from(parent.getContext())
                .inflate(
                        R.layout.fragment_account_list_item,
                        parent,
                        false
                );
        return new AccountList.ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(final AccountList.ViewHolder holder, int position) {
        AccountEntry ae = mVisibleEntries.get(position);
        holder.mLabelView.setText(ae.getLabel());
        holder.mIssuerView.setText(ae.getIssuer());
        holder.Initialize(ae);
        holder.mView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mOnListInteraction.onListInteraction(holder.mItem);
            }
        });
        holder.mView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                if (!isSearching)
                    mOnDragListener.onDragBegin(holder);
                return false;
            }
        });
    }

    /*************************************************************************
     DRAG INTERACTION IMPLEMENTATION
     */

    @Override
    public void onItemDismiss(int position) {
        Notify.Debug();
        mAccountEntries.remove(position);
        reloadVisibleEntries();
        mOnListInteraction.onListInteraction(mAccountEntries);
        notifyItemRemoved(position);
    }

    @Override
    public boolean onItemMove(int src, int dst) {
        Notify.Debug();
        Collections.swap(mAccountEntries, src, dst);
        reloadVisibleEntries();
        mOnListInteraction.onListInteraction(mAccountEntries);
        notifyItemMoved(src, dst);
        return true;
    }

    /*************************************************************************
     SEARCH FILTER
     */

    private boolean isSearching = false;

    /**
     * Search filter.
     *
     * @param text the text
     */
    public void SearchFilter(String text) {
        if (text.isEmpty()) {
            ClearSearchFilter();
        } else {
            isSearching = true;
            mVisibleEntries.clear();
            text = text.toLowerCase();
            for (int i=0; i < mAccountEntries.size(); i++) {
                AccountEntry ae = mAccountEntries.get(i);
                if (ae.getLabel().toLowerCase().contains(text) || ae.getIssuer().toLowerCase().contains(text)) {
                    mVisibleEntries.add(ae);
                }
            }
        }
        notifyDataSetChanged();
    }

    /**
     * Clear search filter.
     */
    public void ClearSearchFilter() {
        Notify.Debug();
        isSearching = false;
        reloadVisibleEntries();
    }

    /*************************************************************************
     *RECYCLER VIEW HOLDER
     */
    public class ViewHolder
            extends RecyclerView.ViewHolder
            implements AccountInterface.OnTouchInteraction
    {
        /**
         * The M view.
         */
        public final View mView;
        /**
         * The M label view.
         */
        public final TextView mLabelView;
        /**
         * The M issuer view.
         */
        public final TextView mIssuerView;
        /**
         * The M icon view.
         */
        public final ImageView mIconView;
        /**
         * The M item.
         */
        public AccountEntry mItem;

        /**
         * Instantiates a new View holder.
         *
         * @param view the view
         */
        public ViewHolder(View view) {
            super(view);
            mView = view;
            mLabelView = view.findViewById(R.id.account_label_text);
            mIssuerView = view.findViewById(R.id.account_issuer_text);
            mIconView = view.findViewById(R.id.account_icon_view);
        }

        /**
         * Initialize.
         *
         * @param ae the ae
         */
        public void Initialize(AccountEntry ae) {
            mItem = ae;
            mIconView.setImageDrawable(mItem.MakeIconDrawable());
        }

        @Override
        public String toString() {
            return super.toString() + " '" + mItem.toString() + "'";
        }

        @Override
        public void onItemSelected() {
            mView.setBackgroundResource(R.drawable.account_list_row_select);
        }

        @Override
        public void onItemClear() {
            mView.setBackgroundResource(R.drawable.account_list_row);
        }
    }

    /*************************************************************************
     *ITEM TOUCh HELPER CALLBACK
     * @param view the view
     * @param adapter the adapter
     * @return the item touch helper
     */
    public ItemTouchHelper AttachTouchCallback(RecyclerView view, AccountList adapter) {
        TouchCallback tc = new TouchCallback(adapter);
        ItemTouchHelper ith = new ItemTouchHelper(tc);
        ith.attachToRecyclerView(view);
        return ith;
    }

    /**
     * The type Touch callback.
     */
    public class TouchCallback extends ItemTouchHelper.Callback {
        /**
         * The Alpha full.
         */
        static final float ALPHA_FULL = 1.0f;

        private final AccountInterface.OnDragInteraction mAdapter;

        /**
         * Instantiates a new Touch callback.
         *
         * @param adapter the adapter
         */
        TouchCallback(AccountInterface.OnDragInteraction adapter) {
            mAdapter = adapter;
        }

        @Override
        public boolean isLongPressDragEnabled() {
            // if searching, don't allow re-ordering
            return !isSearching;
        }

        @Override
        public boolean isItemViewSwipeEnabled() {
            // if searching, don't allow swipe-delete
            return !isSearching;
        }

        @Override
        public int getMovementFlags(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder) {
            if (recyclerView.getLayoutManager() instanceof GridLayoutManager) {
                final int dragFlags = ItemTouchHelper.UP | ItemTouchHelper.DOWN | ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT;
                final int swipeFlags = 0;
                return makeMovementFlags(dragFlags, swipeFlags);
            } else {
                final int dragFlags = ItemTouchHelper.UP | ItemTouchHelper.DOWN;
                final int swipeFlags = ItemTouchHelper.START | ItemTouchHelper.END;
                return makeMovementFlags(dragFlags, swipeFlags);
            }
        }

        @Override
        public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder source, RecyclerView.ViewHolder target) {
            if (source.getItemViewType() != target.getItemViewType()) {
                return false;
            }
            mAdapter.onItemMove(source.getAdapterPosition(), target.getAdapterPosition());
            return true;
        }

        @Override
        public void onSwiped(RecyclerView.ViewHolder viewHolder, int i) {
            final RecyclerView.ViewHolder vh = viewHolder;
            SupportBar.getInstance()
                .ShowYesNoBox(R.string.delete, R.string.account_delete_prompt,
                        new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                mAdapter.onItemDismiss(vh.getAdapterPosition());
                            }
                        },
                        new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                reloadVisibleEntries();
                                notifyDataSetChanged();
                            }
                        }
                );

        }

        @Override
        public void onChildDraw( Canvas c, RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, float dX, float dY, int actionState, boolean isCurrentlyActive) {
            if (actionState == ItemTouchHelper.ACTION_STATE_SWIPE) {
                final float alpha = ALPHA_FULL - Math.abs(dX) / (float) viewHolder.itemView.getWidth();
                viewHolder.itemView.setAlpha(alpha);
                viewHolder.itemView.setTranslationX(dX);
            } else {
                super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
            }
        }

        @Override
        public void onSelectedChanged(RecyclerView.ViewHolder viewHolder, int actionState) {
            if (actionState != ItemTouchHelper.ACTION_STATE_IDLE) {
                if (viewHolder instanceof AccountInterface.OnTouchInteraction) {
                    AccountInterface.OnTouchInteraction itemViewHolder = (AccountInterface.OnTouchInteraction) viewHolder;
                    itemViewHolder.onItemSelected();
                }
            }
            super.onSelectedChanged(viewHolder, actionState);
        }

        @Override
        public void clearView(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder) {
            super.clearView(recyclerView, viewHolder);
            viewHolder.itemView.setAlpha(ALPHA_FULL);
            if (viewHolder instanceof AccountInterface.OnTouchInteraction) {
                // Tell the view holder it's time to restore the idle state
                AccountInterface.OnTouchInteraction itemViewHolder = (AccountInterface.OnTouchInteraction) viewHolder;
                itemViewHolder.onItemClear();
            }
        }
    }
}
