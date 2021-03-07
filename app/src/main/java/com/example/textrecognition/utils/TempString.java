package com.example.textrecognition.utils;

public class TempString {
    private static TempString sInstance;
    private String text;

    public static TempString getInstance() {
        if (sInstance == null)
            sInstance = new TempString();
        return sInstance;
    }

    public void saveTextDetails(String text)
    {
        this.text = text;
    }

    public String getText()
    {
        return text;
    }
}
