package com.mittimitra.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Utility class for compressing images before network upload.
 * Reduces data usage and speeds up API calls, especially important for rural users.
 */
public class ImageCompressor {

    private static final String TAG = "ImageCompressor";

    /**
     * Maximum dimension for compressed images (width or height).
     */
    public static final int MAX_DIMENSION_DEFAULT = 1024;
    public static final int MAX_DIMENSION_THUMBNAIL = 256;

    /**
     * Quality settings for JPEG compression.
     */
    public static final int QUALITY_HIGH = 85;
    public static final int QUALITY_MEDIUM = 70;
    public static final int QUALITY_LOW = 50;

    private ImageCompressor() {
        // Prevent instantiation
    }

    /**
     * Compress an image from URI with default settings.
     * Suitable for API uploads.
     */
    @Nullable
    public static byte[] compressFromUri(@NonNull Context context, @NonNull Uri imageUri) {
        return compressFromUri(context, imageUri, MAX_DIMENSION_DEFAULT, QUALITY_MEDIUM);
    }

    /**
     * Compress an image from URI with custom settings.
     *
     * @param context      Application context
     * @param imageUri     URI of the image to compress
     * @param maxDimension Maximum width or height (maintains aspect ratio)
     * @param quality      JPEG quality (0-100)
     * @return Compressed image as byte array, or null if failed
     */
    @Nullable
    public static byte[] compressFromUri(@NonNull Context context, @NonNull Uri imageUri,
                                          int maxDimension, int quality) {
        try {
            // 1. Decode bounds only first
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            
            InputStream is = context.getContentResolver().openInputStream(imageUri);
            if (is == null) return null;
            BitmapFactory.decodeStream(is, null, options);
            is.close();

            // 2. Calculate sample size
            options.inSampleSize = calculateInSampleSize(options, maxDimension, maxDimension);
            options.inJustDecodeBounds = false;

            // 3. Decode with sampling
            is = context.getContentResolver().openInputStream(imageUri);
            if (is == null) return null;
            Bitmap bitmap = BitmapFactory.decodeStream(is, null, options);
            is.close();

            if (bitmap == null) return null;

            // 4. Scale to exact max dimension if needed
            bitmap = scaleBitmapToMaxDimension(bitmap, maxDimension);

            // 5. Compress to JPEG
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality, baos);
            
            byte[] result = baos.toByteArray();
            bitmap.recycle();
            
            return result;

        } catch (IOException e) {
            ErrorHandler.logError(TAG, "Failed to compress image", e);
            return null;
        }
    }

    /**
     * Compress a Bitmap directly.
     */
    @NonNull
    public static byte[] compressBitmap(@NonNull Bitmap bitmap, int maxDimension, int quality) {
        Bitmap scaled = scaleBitmapToMaxDimension(bitmap, maxDimension);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        scaled.compress(Bitmap.CompressFormat.JPEG, quality, baos);
        if (scaled != bitmap) {
            scaled.recycle();
        }
        return baos.toByteArray();
    }

    /**
     * Get approximate file size in KB after compression.
     */
    public static int getCompressedSizeKb(@NonNull byte[] compressedData) {
        return compressedData.length / 1024;
    }

    private static int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
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

    private static Bitmap scaleBitmapToMaxDimension(Bitmap bitmap, int maxDimension) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();

        if (width <= maxDimension && height <= maxDimension) {
            return bitmap;
        }

        float ratio = (float) width / height;
        int newWidth, newHeight;

        if (width > height) {
            newWidth = maxDimension;
            newHeight = (int) (maxDimension / ratio);
        } else {
            newHeight = maxDimension;
            newWidth = (int) (maxDimension * ratio);
        }

        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true);
    }
}
