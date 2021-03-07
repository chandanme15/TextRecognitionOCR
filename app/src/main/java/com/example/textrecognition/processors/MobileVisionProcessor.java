package com.example.textrecognition.processors;
import android.content.Context;
import android.util.Log;
import android.util.SparseArray;
import android.widget.Toast;

import com.example.textrecognition.utils.TempString;
import com.google.android.gms.vision.Detector;
import com.google.android.gms.vision.text.TextBlock;

import java.util.Arrays;


public class MobileVisionProcessor implements Detector.Processor<TextBlock> {

    private Context context;
    public MobileVisionProcessor(Context context) {
        this.context = context;
    }

    @Override
    public void receiveDetections(Detector.Detections<TextBlock> detections) {
        Log.d("OcrDetectorProcessor", "inside receiveDetections! ");
        SparseArray<TextBlock> items = detections.getDetectedItems();
        if(items == null) {
            Toast.makeText(context,"Mobile Vision can't recognize text",Toast.LENGTH_SHORT).show();
        }
        else {

            int n = items.size();
            String[] strings = new String[n];
            for (int i = 0; i < items.size(); ++i) {
                TextBlock item = items.valueAt(i);
                if (item != null && item.getValue() != null) {
                    strings[i] = item.getValue();
                }
            }
            /*String string = Arrays.toString(strings);
            String finalData = string.replaceAll("[^A-Za-z0-9]", "");
            TempString.getInstance().saveTextDetails(finalData.toUpperCase());*/
            TempString.getInstance().saveTextDetails(Arrays.toString(strings));
        }
    }

    @Override
    public void release() {
    }

}
