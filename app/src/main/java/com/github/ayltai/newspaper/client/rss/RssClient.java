package com.github.ayltai.newspaper.client.rss;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.apache.commons.io.IOUtils;

import org.xmlpull.v1.XmlPullParserException;

import com.github.ayltai.newspaper.client.Client;
import com.github.ayltai.newspaper.model.Item;
import com.github.ayltai.newspaper.model.Source;
import com.github.ayltai.newspaper.net.HttpClient;

import io.reactivex.Single;
import io.realm.RealmList;

public abstract class RssClient extends Client {
    //@Inject
    protected RssClient(@NonNull final HttpClient client, @Nullable final Source source) {
        super(client, source);
    }

    @NonNull
    @Override
    public final Single<List<Item>> getItems(@NonNull final String url) {
        final String categoryName = this.getCategoryName(url);

        return Single.create(emitter -> {
            if (this.source == null) {
                emitter.onSuccess(Collections.emptyList());
            } else {
                InputStream inputStream = null;

                try {
                    final RealmList<Item> items = new RealmList<>(this.filters(url, Parser.parse(inputStream = this.client.download(url))).toArray(new Item[0]));

                    for (final Item item : items) {
                        item.setSource(this.source.getName());
                        item.setCategory(categoryName);
                    }

                    Collections.sort(items);

                    emitter.onSuccess(items);
                } catch (final XmlPullParserException | IOException e) {
                    this.handleError(emitter, e);
                } finally {
                    IOUtils.closeQuietly(inputStream);
                }
            }
        });
    }
}
