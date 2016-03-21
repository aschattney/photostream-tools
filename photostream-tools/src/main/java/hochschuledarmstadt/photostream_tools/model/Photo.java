/*
 * The MIT License
 *
 * Copyright (c) 2016 Andreas Schattney
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package hochschuledarmstadt.photostream_tools.model;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.util.Base64;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class Photo implements Parcelable, Id{

    private static final String TAG = Photo.class.getName();
    public static final String FILENAME_FORMAT = "%s.jpg";

    @SerializedName("image")
    @Expose
    private String imageFilePath;
    @SerializedName("comment")
    @Expose
    private String comment;
    @SerializedName("favorite")
    @Expose
    private int liked;
    @SerializedName("photo_id")
    @Expose
    private int id;
    @SerializedName("deleteable")
    @Expose
    private boolean deleteable;
    @SerializedName("comment_count")
    @Expose
    private int commentCount;

    /**
     *
     * @return
     * The imageFilePath
     */
    public String getImageFilePath() {
        return imageFilePath;
    }

    /**
     *
     * @return
     * The comment
     */
    public String getComment() {
        return comment;
    }

    /**
     *
     * @return
     * The liked
     */
    public boolean isLiked() {
        return liked == 1;
    }

    public void setLiked(boolean liked) {
        this.liked = liked ? 1 : 0;
    }

    /**
     * Use this flag to determine if the photo can be deleted by the user
     * @return
     * true if the photo can be deleted
     */
    public boolean isDeleteable() {
        return deleteable;
    }

    public int getCommentCount() {
        return commentCount;
    }

    protected Photo(Parcel in) {
        id = in.readInt();
        imageFilePath = in.readString();
        comment = in.readString();
        liked = in.readInt();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeInt(id);
        dest.writeString(imageFilePath);
        dest.writeString(comment);
        dest.writeInt(liked);
    }

    public static final Creator<Photo> CREATOR = new Creator<Photo>() {
        @Override
        public Photo createFromParcel(Parcel in) {
            return new Photo(in);
        }

        @Override
        public Photo[] newArray(int size) {
            return new Photo[size];
        }
    };

    public Integer getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    private static String getImageFileName(int id){
        return String.format(FILENAME_FORMAT, id);
    }

    private static File concatImageFilePath(Context context, String imageFileName){
        return new File(context.getFilesDir(), imageFileName);
    }

    public boolean saveToImageToCache(Context context) throws IOException {

        boolean inCache = false;

        String filename = getImageFileName(id);
        if (!imageExistsOnFileSystem(context, filename)) {
            FileOutputStream outputStream = null;
            try {
                outputStream = context.openFileOutput(filename, Context.MODE_PRIVATE);
                byte[] data = Base64.decode(imageFilePath, Base64.DEFAULT);
                Bitmap bitmap = BitmapFactory.decodeByteArray(data, 0, data.length);
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream);
                bitmap.recycle();
                imageFilePath = concatImageFilePath(context, filename).getAbsolutePath();
                inCache = true;
            } catch (Exception e) {

            } finally {
                if (outputStream != null)
                    outputStream.close();
            }
        }else{
            inCache = true;
            imageFilePath = concatImageFilePath(context, filename).getAbsolutePath();
        }

        return inCache;

    }

    private boolean imageExistsOnFileSystem(Context context, String filename) {
        File file = context.getFileStreamPath(filename);
        if(file == null || !file.exists()) {
            return false;
        }
        return true;
    }

    public static File getImageFilePathForPhotoId(Context context, int photoId) {
        return concatImageFilePath(context, getImageFileName(photoId));
    }
}