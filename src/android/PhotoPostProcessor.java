package com.cordovaplugincamerapreview;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.util.Log;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;

import androidx.exifinterface.media.ExifInterface;

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

    public PhotoPostProcessor(byte[] rawData, boolean isFrontCamera) {
        processingSteps = new ArrayList<>();


        // Add EXIF adjustment step
        processingSteps.add(new ExifAdjustmentsStep(rawData, isFrontCamera));


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
        java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, quality, baos);
        return baos.toByteArray();
    }

    /**
     * Processing step: Handles EXIF-based rotation and mirroring.
     */
    public static class ExifAdjustmentsStep implements ProcessingStep {
        private byte[] rawData;
        private boolean isFrontCamera;

        public ExifAdjustmentsStep(byte[] rawData, boolean isFrontCamera) {
            this.rawData = rawData;
            this.isFrontCamera = isFrontCamera;
        }

        @Override
        public Bitmap process(Bitmap input) {
            try {
                ExifInterface exif = new ExifInterface(new ByteArrayInputStream(rawData));
                int rotation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
                int rotationInDegrees = exifToDegrees(rotation);

                Matrix matrix = new Matrix();

                // Handle front camera mirroring
                if (isFrontCamera) {
                    matrix.preScale(1.0f, -1.0f);
                }

                // Apply rotation
                if (rotationInDegrees != 0) {
                    matrix.preRotate(rotationInDegrees);
                }

                // Apply matrix if needed
                if (!matrix.isIdentity()) {
                    Bitmap rotatedBitmap = Bitmap.createBitmap(input, 0, 0, input.getWidth(), input.getHeight(), matrix, true);
                    input.recycle(); // Recycle the original bitmap
                    return rotatedBitmap;
                }

            } catch (Exception e) {
                Log.e(TAG, "Error in ExifAdjustmentsStep: " + e.getMessage());
            }

            return input; // Return the input unchanged if no adjustments are needed
        }

        private int exifToDegrees(int exifOrientation) {
            switch (exifOrientation) {
                case ExifInterface.ORIENTATION_ROTATE_90:
                    return 90;
                case ExifInterface.ORIENTATION_ROTATE_180:
                    return 180;
                case ExifInterface.ORIENTATION_ROTATE_270:
                    return 270;
                default:
                    return 0;
            }
        }
    }

    /**
     * Initial processing step: Converts RAW image to JPEG format.
     */
    private static class RawToJpegStep implements ProcessingStep {
        @Override
        public Bitmap process(Bitmap input) {
            // Since the input is already a Bitmap, we assume it's in a compatible format.
            // Additional RAW processing can be implemented here if needed.
            return input;
        }
    }
}
