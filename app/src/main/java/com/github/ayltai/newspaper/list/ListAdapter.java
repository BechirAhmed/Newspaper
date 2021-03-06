package com.github.ayltai.newspaper.list;

import java.io.Closeable;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import com.github.ayltai.newspaper.Constants;
import com.github.ayltai.newspaper.R;
import com.github.ayltai.newspaper.RxBus;
import com.github.ayltai.newspaper.data.ItemManager;
import com.github.ayltai.newspaper.item.ItemPresenter;
import com.github.ayltai.newspaper.item.ItemUpdatedEvent;
import com.github.ayltai.newspaper.item.ItemViewHolder;
import com.github.ayltai.newspaper.item.ItemsUpdatedEvent;
import com.github.ayltai.newspaper.model.Item;
import com.github.ayltai.newspaper.util.LogUtils;
import com.jakewharton.rxbinding2.view.RxView;

import io.reactivex.disposables.CompositeDisposable;
import io.realm.Realm;

final class ListAdapter extends RecyclerView.Adapter<ItemViewHolder> implements Closeable {
    //region Variables

    private final Map<ItemViewHolder, ItemPresenter> map         = new HashMap<>();
    private final CompositeDisposable                disposables = new CompositeDisposable();
    private final Context                            context;
    private final ListScreen.Key                     parentKey;
    private final int                                listViewType;
    private final Realm                              realm;
    private final List<Item>                         items;

    private final Subscriber<ItemUpdatedEvent> subscriber = new Subscriber<ItemUpdatedEvent>() {
        @Override
        public void onComplete() {
        }

        @Override
        public void onSubscribe(final Subscription subscription) {
        }

        @Override
        public void onError(final Throwable e) {
            LogUtils.getInstance().e(this.getClass().getSimpleName(), e.getMessage(), e);
        }

        @Override
        public void onNext(final ItemUpdatedEvent itemUpdatedEvent) {
            new ItemManager(ListAdapter.this.realm).getItemsSingle(Collections.emptyList(), Collections.singletonList(ListAdapter.this.parentKey.getCategory()))
                .subscribe(items -> {
                    if (Constants.CATEGORY_BOOKMARK.equals(ListAdapter.this.parentKey.getCategory())) {
                        if (itemUpdatedEvent.getIndex() < 0) {
                            if (items.size() == 1) {
                                RxBus.getInstance().send(new ItemsUpdatedEvent(items));
                            } else {
                                ListAdapter.this.notifyItemInserted(items.indexOf(itemUpdatedEvent.getItem()));
                            }
                        } else {
                            if (items.isEmpty()) {
                                RxBus.getInstance().send(new ItemsUpdatedEvent(items));
                            } else {
                                ListAdapter.this.notifyItemRemoved(itemUpdatedEvent.getIndex());
                            }
                        }
                    } else {
                        final int index = ListAdapter.this.items.indexOf(itemUpdatedEvent.getItem());
                        if (index > -1) ListAdapter.this.notifyItemChanged(index);
                    }
                });
        }
    };

    private int fakeCount;

    //endregion

    ListAdapter(@NonNull final Context context, @NonNull final ListScreen.Key parentKey, @Constants.ListViewType final int listViewType, @Nullable final List<Item> items) {
        this.context      = context;
        this.parentKey    = parentKey;
        this.listViewType = listViewType;
        this.items        = items;
        this.realm        = Realm.getDefaultInstance();

        RxBus.getInstance().register(ItemUpdatedEvent.class, this.subscriber);
    }

    @Override
    public int getItemCount() {
        if (this.realm.isClosed()) return this.fakeCount;

        return this.fakeCount = this.items == null ? 0 : this.items.size();
    }

    @NonNull
    @Override
    public ItemViewHolder onCreateViewHolder(@NonNull final ViewGroup parent, final int viewType) {
        final View           view      = LayoutInflater.from(this.context).inflate(this.listViewType == Constants.LIST_VIEW_TYPE_COZY ? R.layout.view_item_cozy : R.layout.view_item_compact, parent, false);
        final ItemViewHolder holder    = new ItemViewHolder(view);
        final ItemPresenter  presenter = new ItemPresenter(this.realm);

        this.disposables.add(RxView.attaches(view).subscribe(dummy -> presenter.onViewAttached(holder)));
        this.disposables.add(RxView.detaches(view).subscribe(dummy -> presenter.onViewDetached()));

        this.map.put(holder, presenter);

        return holder;
    }

    @Override
    public void onBindViewHolder(@NonNull final ItemViewHolder holder, final int position) {
        if (this.realm.isClosed()) return;

        this.map.get(holder).bind(this.parentKey, this.items.get(position), this.listViewType, false);
    }

    @Override
    public void close() {
        RxBus.getInstance().unregister(ItemUpdatedEvent.class, this.subscriber);

        if (!this.disposables.isDisposed() && this.disposables.size() > 0) this.disposables.dispose();

        for (final ItemViewHolder holder : this.map.keySet()) holder.close();

        this.map.clear();

        if (!this.realm.isClosed()) this.realm.close();
    }
}
