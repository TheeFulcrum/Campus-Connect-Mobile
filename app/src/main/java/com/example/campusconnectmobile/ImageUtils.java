package com.example.campusconnectmobile;

import android.content.ContentResolver;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Shared helper for turning a picked image Uri into compressed JPEG bytes
 * ready for multipart upload (avatar and listing photos both go through this).
 */
public final class ImageUtils {

    private ImageUtils() {
    }

    /** Decodes, auto-rotates (per EXIF), downsizes to maxDimension, and JPEG-encodes an image Uri. */
    public static byte[] readAndCompress(Context context, Uri uri, int maxDimension, int quality) throws IOException {
        ContentResolver resolver = context.getContentResolver();
        Bitmap bitmap = decodeSampled(resolver, uri, maxDimension);
        if (bitmap == null) {
            throw new IOException("Unable to decode the selected image.");
        }
        bitmap = applyExifRotation(resolver, uri, bitmap);
        bitmap = scaleDown(bitmap, maxDimension);

        ByteArrayOutputStream output = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, quality, output);
        bitmap.recycle();
        return output.toByteArray();
    }

    private static Bitmap decodeSampled(ContentResolver resolver, Uri uri, int maxDimension) throws IOException {
        BitmapFactory.Options boundsOptions = new BitmapFactory.Options();
        boundsOptions.inJustDecodeBounds = true;
        try (InputStream stream = resolver.openInputStream(uri)) {
            BitmapFactory.decodeStream(stream, null, boundsOptions);
        }

        int sampleSize = 1;
        int halfWidth = boundsOptions.outWidth / 2;
        int halfHeight = boundsOptions.outHeight / 2;
        while ((halfWidth / sampleSize) >= maxDimension && (halfHeight / sampleSize) >= maxDimension) {
            sampleSize *= 2;
        }

        BitmapFactory.Options decodeOptions = new BitmapFactory.Options();
        decodeOptions.inSampleSize = sampleSize;
        try (InputStream stream = resolver.openInputStream(uri)) {
            return BitmapFactory.decodeStream(stream, null, decodeOptions);
        }
    }

    private static Bitmap applyExifRotation(ContentResolver resolver, Uri uri, Bitmap bitmap) {
        try (InputStream stream = resolver.openInputStream(uri)) {
            if (stream == null) return bitmap;
            ExifInterface exif = new ExifInterface(stream);
            int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
            int degrees;
            switch (orientation) {
                case ExifInterface.ORIENTATION_ROTATE_90:
                    degrees = 90;
                    break;
                case ExifInterface.ORIENTATION_ROTATE_180:
                    degrees = 180;
                    break;
                case ExifInterface.ORIENTATION_ROTATE_270:
                    degrees = 270;
                    break;
                default:
                    degrees = 0;
            }
            if (degrees == 0) return bitmap;
            Matrix matrix = new Matrix();
            matrix.postRotate(degrees);
            Bitmap rotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
            if (rotated != bitmap) bitmap.recycle();
            return rotated;
        } catch (IOException exception) {
            return bitmap;
        }
    }

    private static Bitmap scaleDown(Bitmap bitmap, int maxDimension) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        float scale = Math.min(1f, ((float) maxDimension) / Math.max(width, height));
        if (scale >= 1f) return bitmap;
        int newWidth = Math.round(width * scale);
        int newHeight = Math.round(height * scale);
        Bitmap scaled = Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true);
        if (scaled != bitmap) bitmap.recycle();
        return scaled;
    }

    /** Square-crops a bitmap to its center, used for the avatar crop step. */
    public static Bitmap centerCropSquare(Bitmap source) {
        int size = Math.min(source.getWidth(), source.getHeight());
        int x = (source.getWidth() - size) / 2;
        int y = (source.getHeight() - size) / 2;
        return Bitmap.createBitmap(source, x, y, size, size);
    }
}
