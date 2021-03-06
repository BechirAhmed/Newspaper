package com.github.ayltai.newspaper;

import java.io.Closeable;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

import android.app.Activity;
import android.content.Context;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;

import com.github.ayltai.newspaper.item.ItemPresenter;
import com.github.ayltai.newspaper.item.ItemScreen;
import com.github.ayltai.newspaper.list.ListScreen;
import com.github.ayltai.newspaper.main.MainPresenter;
import com.github.ayltai.newspaper.main.MainScreen;
import com.github.ayltai.newspaper.setting.Settings;

import flow.Flow;
import flow.KeyDispatcher;
import flow.KeyParceler;
import flow.State;
import flow.TraversalCallback;
import io.reactivex.Flowable;
import io.reactivex.disposables.CompositeDisposable;
import io.realm.Realm;

final class FlowController {
    private final CompositeDisposable subscriptions = new CompositeDisposable();

    //region Cached presenters and views

    private final Map<Class<?>, Presenter.View>  screens    = new HashMap<>();
    private final Map<Presenter.View, Presenter> presenters = new HashMap<>();

    //endregion

    //region Variables

    private final Activity      activity;
    private final Realm         realm;
    private final MainComponent component;

    //endregion

    @Inject
    FlowController(@NonNull final Activity activity) {
        this.activity  = activity;
        this.realm     = Realm.getDefaultInstance();
        this.component = DaggerMainComponent.builder().mainModule(new MainModule(this.activity)).build();
    }

    @NonNull
    Context attachNewBase(@NonNull final Context newBase) {
        return Flow.configure(newBase, this.activity)
            .keyParceler(new KeyParceler() {
                @NonNull
                @Override
                public Parcelable toParcelable(@NonNull final Object key) {
                    return (Parcelable)key;
                }

                @NonNull
                @Override
                public Object toKey(@NonNull final Parcelable parcelable) {
                    return parcelable;
                }
            })
            .dispatcher(KeyDispatcher.configure(this.activity, (outgoingState, incomingState, direction, incomingContexts, callback) -> {
                if (outgoingState != null) outgoingState.save(((ViewGroup)this.activity.findViewById(android.R.id.content)).getChildAt(0));

                final Presenter.View view;
                final Presenter      presenter;

                if (this.screens.containsKey(incomingState.getKey().getClass())) {
                    view      = this.screens.get(incomingState.getKey().getClass());
                    presenter = this.presenters.get(view);
                } else {
                    if (incomingState.getKey() instanceof ItemScreen.Key) {
                        view      = this.component.itemView();
                        presenter = this.component.itemPresenter();

                        final Flowable<Object> attachments = view.attachments();
                        final Flowable<Object> detachments = view.detachments();

                        if (attachments != null) this.subscriptions.add(attachments.subscribe(dummy -> presenter.onViewAttached(view), error -> Log.e(this.getClass().getSimpleName(), error.getMessage(), error)));
                        if (detachments != null) this.subscriptions.add(detachments.subscribe(dummy -> presenter.onViewDetached(), error -> Log.e(this.getClass().getSimpleName(), error.getMessage(), error)));
                    } else {
                        view      = this.component.mainView();
                        presenter = this.component.mainPresenter();

                        final Flowable<Object> attachments = view.attachments();
                        final Flowable<Object> detachments = view.detachments();

                        if (attachments != null) this.subscriptions.add(attachments.subscribe(dummy -> {
                            presenter.onViewAttached(view);
                            ((MainPresenter)presenter).bind();
                        }, error -> Log.e(this.getClass().getSimpleName(), error.getMessage(), error)));

                        if (detachments != null) this.subscriptions.add(detachments.subscribe(dummy -> presenter.onViewDetached(), error -> Log.e(this.getClass().getSimpleName(), error.getMessage(), error)));
                    }

                    this.screens.put(incomingState.getKey().getClass(), view);
                    this.presenters.put(view, presenter);
                }

                if (incomingState.getKey() instanceof ItemScreen.Key) {
                    final ItemScreen.Key key = incomingState.getKey();

                    ((ItemPresenter)presenter).bind((ListScreen.Key)key.getParentKey(), key.getItem(), Settings.getListViewType(this.activity), true);
                }

                this.dispatch((View)view, incomingState, callback);
            }).build())
            .defaultKey(Constants.KEY_SCREEN_MAIN)
            .install();
    }

    boolean onBackPressed() {
        if (!Flow.get(this.activity).goBack()) {
            final View view = ((ViewGroup)this.activity.findViewById(android.R.id.content)).getChildAt(0);

            if (view instanceof MainScreen) {
                if (!((MainScreen)view).goBack()) return false;
            } else if (view instanceof DrawerLayout) {
                final DrawerLayout drawerLayout = (DrawerLayout)view;

                if (drawerLayout.isDrawerOpen(GravityCompat.END)) {
                    drawerLayout.closeDrawer(GravityCompat.END);

                    return true;
                } else {
                    if (!((MainScreen)((ViewGroup)drawerLayout.getChildAt(0)).getChildAt(0)).goBack()) return false;
                }
            }
        }

        return true;
    }

    void onDestroy() {
        if (!this.subscriptions.isDisposed() && this.subscriptions.size() > 0) this.subscriptions.dispose();

        for (final Presenter.View view : this.screens.values()) {
            if (view instanceof Closeable) {
                try {
                    ((Closeable)view).close();
                } catch (final IOException e) {
                    Log.e(this.getClass().getSimpleName(), e.getMessage(), e);
                }
            }
        }

        if (!this.realm.isClosed()) this.realm.close();
    }

    private void dispatch(@NonNull final View view, @NonNull final State incomingState, @NonNull final TraversalCallback callback) {
        incomingState.restore(view);

        this.activity.setContentView(view);

        callback.onTraversalCompleted();
    }
}
