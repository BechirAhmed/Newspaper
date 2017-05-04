package com.github.ayltai.newspaper.model;

import java.util.Date;
import java.util.List;

import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.github.ayltai.newspaper.data.RealmString;

import io.realm.RealmList;
import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;

public class Item extends RealmObject implements Comparable<Item>, Parcelable {
    //region Constants

    public static final String FIELD_SOURCE       = "source";
    public static final String FIELD_CATEGORY     = "category";
    public static final String FIELD_PUBLISH_DATE = "publishDate";
    public static final String FIELD_BOOKMARKED   = "bookmarked";

    //endregion

    //region Fields

    private String title;

    private String  description;
    private boolean isFullDescription;

    @PrimaryKey
    private String link;

    private long publishDate;

    private String source;
    private String category;

    private RealmList<RealmString> mediaUrls;

    private Boolean bookmarked;

    //endregion

    public Item() {
    }

    //region Properties

    @NonNull
    public String getTitle() {
        return this.title;
    }

    @NonNull
    public String getDescription() {
        return this.description;
    }

    public void setDescription(@NonNull final String description) {
        this.description       = description;
        this.isFullDescription = true;
    }

    public boolean isFullDescription() {
        return this.isFullDescription;
    }

    @NonNull
    public String getLink() {
        return this.link;
    }

    @Nullable
    public Date getPublishDate() {
        if (this.publishDate == 0) return null;

        return new Date(this.publishDate);
    }

    @NonNull
    public String getSource() {
        return this.source;
    }

    @NonNull
    public String getCategory() {
        return this.category;
    }

    @NonNull
    public RealmList<RealmString> getMediaUrls() {
        return this.mediaUrls;
    }

    @Nullable
    public Boolean isBookmarked() {
        return this.bookmarked;
    }

    public void setBookmarked(final boolean bookmarked) {
        this.bookmarked = bookmarked;
    }

    //endregion

    @Override
    public final int compareTo(@NonNull final Item item) {
        if (this.publishDate != 0 && item.publishDate != 0) return (int)(item.publishDate - this.publishDate);

        return this.title.compareTo(item.title);
    }

    @Override
    public final boolean equals(final Object obj) {
        if (this == obj) return true;

        if (obj instanceof Item) {
            final Item item = (Item)obj;

            return this.link.equals(item.link);
        }

        return false;
    }

    @Override
    public final int hashCode() {
        return this.link.hashCode();
    }

    //region Parcelable

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull final Parcel dest, final int flags) {
        dest.writeString(this.title);
        dest.writeString(this.description);
        dest.writeInt(this.isFullDescription ? 1 : 0);
        dest.writeString(this.link);
        dest.writeLong(this.publishDate);
        dest.writeString(this.source);
        dest.writeString(this.category);
        dest.writeTypedList(this.mediaUrls);
        dest.writeInt(this.bookmarked ? 1 : 0);
    }

    protected Item(@NonNull final Parcel in) {
        this.title             = in.readString();
        this.description       = in.readString();
        this.isFullDescription = in.readInt() == 1;
        this.link              = in.readString();
        this.publishDate       = in.readLong();
        this.source            = in.readString();
        this.category          = in.readString();
        this.mediaUrls         = new RealmList<>();
        this.bookmarked        = in.readInt() == 1;

        final List<RealmString> mediaUrls = in.createTypedArrayList(RealmString.CREATOR);
        for (final RealmString mediaUrl : mediaUrls) this.mediaUrls.add(mediaUrl);
    }

    public static final Parcelable.Creator<Item> CREATOR = new Parcelable.Creator<Item>() {
        @NonNull
        @Override
        public Item createFromParcel(@NonNull final Parcel source) {
            return new Item(source);
        }

        @NonNull
        @Override
        public Item[] newArray(final int size) {
            return new Item[size];
        }
    };

    //endregion
}