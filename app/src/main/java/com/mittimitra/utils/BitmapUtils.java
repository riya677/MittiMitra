package com.mittimitra.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.util.Base64;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Utility class for memory-safe bitmap operations.
 * Helps prevent OutOfMemoryError by properly managing bitmap lifecycle.
 */
public final class BitmapUtils {

    private static final String TAG = "BitmapUtils";

    private BitmapUtils() {
        // Prevent instantiation
    }

    /**
     * Scale a bitmap to the specified dimensions while maintaining aspect ratio.
     * Recycles the original bitmap if requested.
     */
    @NonNull
    public static Bitmap scaleBitmap(@NonNull Bitmap source, int targetWidth, int targetHeight, 
                                      boolean recycleSource) {
        Bitmap scaled = Bitmap.createScaledBitmap(source, targetWidth, targetHeight, true);
        if (recycleSource && scaled != source) {
            source.recycle();
        }
        return scaled;
    }

    /**
     * Convert bitmap to JPEG byte array with specified quality.
     */
    @NonNull
    public static byte[] bitmapToJpegBytes(@NonNull Bitmap bitmap, int quality) {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, quality, stream);
        return stream.toByteArray();
    }

    /**
     * Convert bitmap to Base64 data URL for API requests.
     */
    @NonNull
    public static String bitmapToBase64DataUrl(@NonNull Bitmap bitmap, int quality) {
        byte[] bytes = bitmapToJpegBytes(bitmap, quality);
        String base64 = Base64.encodeToString(bytes, Base64.NO_WRAP);
        return "data:image/jpeg;base64," + base64;
    }

    /**
     * Load bitmap from URI with memory-efficient sampling.
     * Prevents loading full-resolution images that could cause OOM.
     */
    @Nullable
    public static Bitmap loadBitmapFromUri(@NonNull Context context, @NonNull Uri uri, 
                                            int maxWidth, int maxHeight) {
        try {
            // First, decode bounds only
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            
            InputStream inputStream = context.getContentResolver().openInputStream(uri);
            if (inputStream == null) return null;
            BitmapFactory.decodeStream(inputStream, null, options);
            inputStream.close();

            // Calculate sample size
            options.inSampleSize = calculateInSampleSize(options, maxWidth, maxHeight);
            options.inJustDecodeBounds = false;

            // Decode with sample size
            inputStream = context.getContentResolver().openInputStream(uri);
            if (inputStream == null) return null;
            Bitmap bitmap = BitmapFactory.decodeStream(inputStream, null, options);
            inputStream.close();

            return bitmap;
        } catch (IOException e) {
            ErrorHandler.logError(TAG, "Failed to load bitmap from URI", e);
            return null;
        }
    }

    /**
     * Calculate optimal sample size for bitmap loading.
     */
    public static int calculateInSampleSize(@NonNull BitmapFactory.Options options, 
                                             int reqWidth, int reqHeight) {
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {
            final int halfHeight = height / 2;
            final int halfWidth = width / 2;

            while ((halfHeight / inSampleSize) >= reqHeight
                    && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2;
            }
        }

        return inSampleSize;
    }

    /**
     * Safely recycle a bitmap if it's not null and not already recycled.
     */
    public static void recycleSafely(@Nullable Bitmap bitmap) {
        if (bitmap != null && !bitmap.isRecycled()) {
            bitmap.recycle();
        }
    }

    /**
     * Create a scaled bitmap for ML classification (typically 224x224).
     */
    @NonNull
    public static Bitmap prepareForClassification(@NonNull Bitmap source, int size) {
        return scaleBitmap(source, size, size, false);
    }
}
