package com.example.textrecognition.processors;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.Image;

import java.nio.ByteBuffer;

public class ImageProcessor {
    private Image image;

    public ImageProcessor(Image image)
    {
        this.image = image;
    }

    public Bitmap processImage()
    {
        ByteBuffer buffer = image.getPlanes()[0].getBuffer();
        byte[] bytes = new byte[buffer.capacity()];
        buffer.get(bytes);
        Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
        int x = 3* bitmap.getWidth()/8;
        int y = 0;
        int croppedWidth = bitmap.getWidth()/4;
        int croppedHeight = bitmap.getHeight();
        int scaledWidth = croppedWidth/6;
        int scaledHeight = croppedHeight/6;
        Bitmap croppedBmp = Bitmap.createBitmap(bitmap, x, y, croppedWidth, croppedHeight);
        Bitmap scaledBitmap = Bitmap.createScaledBitmap(croppedBmp, scaledWidth, scaledHeight, false);
        Matrix matrix = new Matrix();
        matrix.postRotate(90);
        return Bitmap.createBitmap(scaledBitmap, 0, 0, scaledBitmap.getWidth(), scaledBitmap.getHeight(), matrix, true);
    }
}
