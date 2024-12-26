package com.cordovaplugincamerapreview;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

/**
 * PhotoPostProcessor handles the post-processing pipeline for RAW images.
 * It allows adding multiple processing steps that are applied sequentially.
 */
public class PhotoPostProcessor {
    private static final String TAG = "PhotoPostProcessor";

    /**
     * Interface for processing steps.
     */
    public interface ProcessingStep {
        /**
         * Process the input bitmap and return the processed bitmap.
         *
         * @param input The input Bitmap.
         * @return The processed Bitmap.
         */
        Bitmap process(Bitmap input);
    }

    private List<ProcessingStep> processingSteps;

    public PhotoPostProcessor() {
        processingSteps = new ArrayList<>();
        // Add the initial RAW to JPEG conversion step
        processingSteps.add(new RawToJpegStep());
    }

    /**
     * Adds a new processing step to the pipeline.
     *
     * @param step The processing step to add.
     */
    public void addProcessingStep(ProcessingStep step) {
        processingSteps.add(step);
    }

    /**
     * Processes the RAW image data through the pipeline.
     *
     * @param rawData The RAW image data as byte array.
     * @return The processed image data as byte array (JPEG format).
     */
    public byte[] process(byte[] rawData) {
        Bitmap bitmap = BitmapFactory.decodeByteArray(rawData, 0, rawData.length);
        if (bitmap == null) {
            Log.e(TAG, "Failed to decode RAW image data.");
            return null;
        }

        for (ProcessingStep step : processingSteps) {
            bitmap = step.process(bitmap);
            if (bitmap == null) {
                Log.e(TAG, "Processing step returned null.");
                return null;
            }
        }

        // Convert the final bitmap to JPEG byte array
        byte[] jpegData = bitmapToJpeg(bitmap, 85); // Default quality
        bitmap.recycle(); // Clean up memory

        return jpegData;
    }

    /**
     * Converts a Bitmap to JPEG byte array.
     *
     * @param bitmap The Bitmap to convert.
     * @param quality The JPEG quality (0-100).
     * @return The JPEG image as byte array.
     */
    private byte[] bitmapToJpeg(Bitmap bitmap, int quality) {
        Bitmap rotatedBitmap = rotateBitmapIfNeeded(bitmap);
        java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
        rotatedBitmap.compress(Bitmap.CompressFormat.JPEG, quality, baos);
        return baos.toByteArray();
    }

    /**
     * Rotates the bitmap based on orientation metadata if necessary.
     * Placeholder for handling EXIF rotation. Implement as needed.
     *
     * @param bitmap The Bitmap to rotate.
     * @return The rotated Bitmap.
     */
    private Bitmap rotateBitmapIfNeeded(Bitmap bitmap) {
        // Implement rotation based on EXIF data if available
        // For now, return the bitmap as-is
        return bitmap;
    }

    /**
     * Initial processing step: Converts RAW image to JPEG format.
     */
    private class RawToJpegStep implements ProcessingStep {
        @Override
        public Bitmap process(Bitmap input) {
            // Since the input is already a Bitmap, we assume it's in a compatible format.
            // Additional RAW processing can be implemented here if needed.
            return input;
        }
    }
}