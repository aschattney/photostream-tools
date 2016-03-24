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

package hochschuledarmstadt.photostream_tools;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.drawable.BitmapDrawable;
import android.media.ExifInterface;
import android.net.Uri;
import android.provider.MediaStore;
import android.widget.ImageView;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

public class BitmapUtils {

    private static final String TAG = BitmapUtils.class.getName();

    private static final int TYPE_ASSET = -1;
    private static final int TYPE_FILE = -2;
    private static final int TYPE_OTHER = -3;

    /**
     * Entfernt ein zur ImageView zugeordnetes Bitmap aus dem Speicher
     * @param imageView ImageView welches das Bitmap anzeigt
     */
    public static void recycleBitmapFromImageView(ImageView imageView) {
        if (imageView != null && imageView.getDrawable() != null && imageView.getDrawable() instanceof BitmapDrawable){
            final Bitmap bitmap = ((BitmapDrawable) imageView.getDrawable()).getBitmap();
            recycleBitmap(bitmap);
        }
    }

    /**
     * Entfernt ein Bitmap aus dem Speicher
     * @param bitmap das Bitmap
     */
    public static void recycleBitmap(Bitmap bitmap){
        if (bitmap != null && !bitmap.isRecycled())
            bitmap.recycle();
    }

    public static Bitmap decodeBitmapFromAssetFile(Context context, String assetFileName) throws FileNotFoundException {
        return internalDecodeBitmap(context, Uri.parse(String.format("assets://%s", assetFileName)), TYPE_ASSET);
    }

    public static Bitmap decodeBitmapFromFile(Context context, File file) throws FileNotFoundException {
        return internalDecodeBitmap(context, Uri.fromFile(file), TYPE_FILE);
    }

    /**
     * Lädt ein Bitmap anhand einer Uri
     * @param context Android Context
     * @param uri bitmap source
     * @return Bitmap
     * @throws FileNotFoundException wird geworfen, wenn der Uri nicht aufgelöst werden konnte
     */
    public static Bitmap decodeBitmapFromUri(Context context, Uri uri) throws FileNotFoundException {
        return internalDecodeBitmap(context, uri, TYPE_OTHER);
    }


    private static InputStream getInputStream(Context context, Uri uri, int type) throws IOException {
        switch(type){
            case TYPE_ASSET:
                return context.getAssets().open(uri.toString().replace("assets://", ""));
            case TYPE_FILE:
                return new FileInputStream(uri.toString());
            case TYPE_OTHER:
                return context.getContentResolver().openInputStream(uri);
        }
        return null;
    }

    private static Bitmap internalDecodeBitmap(Context context, Uri uri, int type) throws FileNotFoundException {
        Bitmap bm = null;
        try {
            BitmapFactory.Options options = lessResolution(getInputStream(context, uri, type), 400, 400);
            bm = BitmapFactory.decodeStream(getInputStream(context, uri, type), null, options);
            ExifInterface exif = new ExifInterface(getRealPathFromURI(context, uri));
            String orientString = exif.getAttribute(ExifInterface.TAG_ORIENTATION);
            int orientation = orientString != null ? Integer.parseInt(orientString) :  ExifInterface.ORIENTATION_NORMAL;

            int rotationAngle = 0;
            if (orientation == ExifInterface.ORIENTATION_ROTATE_90) rotationAngle = 90;
            if (orientation == ExifInterface.ORIENTATION_ROTATE_180) rotationAngle = 180;
            if (orientation == ExifInterface.ORIENTATION_ROTATE_270) rotationAngle = 270;

            if (rotationAngle != 0) {
                Matrix matrix = new Matrix();
                matrix.postRotate(rotationAngle);
                bm = Bitmap.createBitmap(bm, 0, 0, bm.getWidth(), bm.getHeight(), matrix, true);
            }
        } catch (IOException e) {
            Logger.log(TAG, LogLevel.ERROR, e.toString());
        }
        return bm;
    }

    private static BitmapFactory.Options lessResolution (InputStream is, int width, int height) {
        int reqHeight = height;
        int reqWidth = width;
        BitmapFactory.Options options = new BitmapFactory.Options();

        // First decode with inJustDecodeBounds=true to check dimensions
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeStream(is, null, options);
        // Calculate inSampleSize
        options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);
        // Decode bitmap with inSampleSize set
        options.inJustDecodeBounds = false;
        return options;
    }

    private static int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {

        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {
            // Calculate ratios of height and width to requested height and width
            final int heightRatio = Math.round((float) height / (float) reqHeight);
            final int widthRatio = Math.round((float) width / (float) reqWidth);

            // Choose the smallest ratio as inSampleSize value, this will guarantee
            // a final image with both dimensions larger than or equal to the
            // requested height and width.
            inSampleSize = heightRatio < widthRatio ? heightRatio : widthRatio;
        }
        return inSampleSize;
    }

    private static String getRealPathFromURI(Context context, Uri contentURI) {
        String result;
        Cursor cursor = context.getContentResolver().query(contentURI, null, null, null, null);
        if (cursor == null) { // Source is Dropbox or other similar local file path
            result = contentURI.getPath();
        } else {
            cursor.moveToFirst();
            int idx = cursor.getColumnIndex(MediaStore.Images.ImageColumns.DATA);
            result = cursor.getString(idx);
            cursor.close();
        }
        return result;
    }

    /**
     * Konvertiert ein Bitmap {@code bitmap} in ein Byte Array
     * @param bitmap das zu konvertierende Bitmap
     * @return Byte Array
     */
    public static byte[] bitmapToBytes(Bitmap bitmap) {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 60, bos);
        bitmap.recycle();
        return bos.toByteArray();
    }

}
