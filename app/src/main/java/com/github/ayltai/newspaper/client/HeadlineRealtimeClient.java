package com.github.ayltai.newspaper.client;

import java.io.IOException;
import java.io.InputStream;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.apache.commons.io.IOUtils;

import com.github.ayltai.newspaper.BuildConfig;
import com.github.ayltai.newspaper.model.Image;
import com.github.ayltai.newspaper.model.Item;
import com.github.ayltai.newspaper.model.Source;
import com.github.ayltai.newspaper.net.HttpClient;
import com.github.ayltai.newspaper.util.LogUtils;
import com.github.ayltai.newspaper.util.StringUtils;

import io.reactivex.Maybe;
import io.reactivex.Single;

final class HeadlineRealtimeClient extends Client {
    //region Constants

    private static final String BASE_URI  = "http://hd.stheadline.com";
    private static final String TAG_LINK  = "</a>";
    private static final String TAG_QUOTE = "\"";
    private static final String TAG_CLOSE = "\">";
    private static final String HTTP      = "http:";

    //endregion

    private static final ThreadLocal<DateFormat> DATE_FORMAT = new ThreadLocal<DateFormat>() {
        @Override
        protected DateFormat initialValue() {
            return new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.ENGLISH);
        }
    };

    HeadlineRealtimeClient(@NonNull final HttpClient client, @Nullable final Source source) {
        super(client, source);
    }

    @NonNull
    @Override
    public Single<List<Item>> getItems(@NonNull final String url) {
        return Single.create(emitter -> {
            try {
                final InputStream inputStream = this.client.download(url);

                if (inputStream == null) {
                    emitter.onSuccess(Collections.emptyList());
                } else {
                    final String     html         = IOUtils.toString(inputStream, Client.ENCODING);
                    final String[]   sections     = StringUtils.substringsBetween(html, "<div class=\"topic\">", "<div class=\"col-xs-12 instantnews-list-1\">");
                    final List<Item> items        = new ArrayList<>(sections.length);
                    final String     categoryName = this.getCategoryName(url);

                    for (final String section : sections) {
                        final Item   item  = new Item();
                        final String title = StringUtils.substringBetween(section, "<h4>", "</h4>");

                        item.setTitle(StringUtils.substringBetween(title, HeadlineRealtimeClient.TAG_CLOSE, HeadlineRealtimeClient.TAG_LINK));
                        item.setLink(HeadlineRealtimeClient.BASE_URI + StringUtils.substringBetween(title, "<a href=\"", HeadlineRealtimeClient.TAG_CLOSE));
                        item.setDescription(StringUtils.substringBetween(section, "<p class=\"text\">", "</p>"));
                        item.setSource(this.source.getName());
                        item.setCategory(categoryName);

                        final String image = StringUtils.substringBetween(section, "<img src=\"", HeadlineRealtimeClient.TAG_QUOTE);
                        if (image != null) item.getImages().add(new Image(HeadlineRealtimeClient.HTTP + image));

                        try {
                            item.setPublishDate(HeadlineRealtimeClient.DATE_FORMAT.get().parse(StringUtils.substringBetween(section, "<i class=\"fa fa-clock-o\"></i>", "</span>")));

                            items.add(item);
                        } catch (final ParseException e) {
                            LogUtils.getInstance().w(this.getClass().getSimpleName(), e.getMessage(), e);
                        }
                    }

                    emitter.onSuccess(this.filters(url, items));
                }
            } catch (final IOException e) {
                this.handleError(emitter, e);
            }
        });
    }

    @NonNull
    @Override
    public Maybe<Item> updateItem(@NonNull final Item item) {
        return Maybe.create(emitter -> {
            if (BuildConfig.DEBUG) LogUtils.getInstance().d(this.getClass().getSimpleName(), item.getLink());

            try {
                final String html = IOUtils.toString(this.client.download(item.getLink()), Client.ENCODING);

                HeadlineRealtimeClient.extractImages(StringUtils.substringsBetween(html, "<a class=\"fancybox\" rel=\"gallery\"", HeadlineRealtimeClient.TAG_LINK), item);
                HeadlineRealtimeClient.extractImages(StringUtils.substringsBetween(html, "<figure ", "</figure>"), item);

                item.setDescription(StringUtils.substringBetween(html, "<div id=\"news-content\" class=\"set-font-aera\" style=\"visibility: visible;\">", "</div>"));
                item.setIsFullDescription(true);

                emitter.onSuccess(item);
            } catch (final IOException e) {
                this.handleError(emitter, e);
            }
        });
    }

    private static void extractImages(@NonNull final String[] imageContainers, @NonNull final Item item) {
        final List<Image> images = new ArrayList<>();

        for (final String imageContainer : imageContainers) {
            final String imageUrl         = StringUtils.substringBetween(imageContainer, "href=\"", HeadlineRealtimeClient.TAG_QUOTE);
            final String imageDescription = StringUtils.substringBetween(imageContainer, "title=\"", HeadlineRealtimeClient.TAG_QUOTE);

            if (imageUrl != null) images.add(new Image(HeadlineRealtimeClient.HTTP + imageUrl, imageDescription));
        }

        if (!images.isEmpty()) {
            item.getImages().clear();
            item.getImages().addAll(images);
        }
    }
}
