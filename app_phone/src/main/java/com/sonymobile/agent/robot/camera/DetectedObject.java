/*
 * Copyright (C) 2016 Sony Mobile Communications Inc. 
 * All rights, including secret rights, reserved.
 */
package com.sonymobile.agent.robot.camera;

import android.os.Parcel;
import android.os.Parcelable;

import com.sonymobile.generalobjectdetector.GeneralDetectedObject;

public class DetectedObject implements Parcelable {
    private int mPositionX;
    private int mPositionY;
    private int mWidth;
    private int mHeight;
    private String mName;
    private float mScore;

    public DetectedObject(Parcel parcel) {
        readFromParcel(parcel);
    }

    public DetectedObject(int x, int y, int width, int height) {
        mPositionX = x;
        mPositionY = y;
        mWidth = width;
        mHeight = height;
    }

    public DetectedObject(GeneralDetectedObject result, int imageWidth, int imageHeight) {
        mPositionX = (int)(result.getNormalizedArea().left * imageWidth);
        mPositionY = (int) (result.getNormalizedArea().top * imageHeight);
        mWidth = (int)(result.getNormalizedArea().width() * imageWidth);
        mHeight = (int)(result.getNormalizedArea().height() * imageHeight);
        mName = result.getLabel().name;
        mScore = result.getScore();
    }

    public int xPosition() {
        return mPositionX;
    }
    public void setX(int x) {
        mPositionX=x;
    }
    public int yPosition() {
        return mPositionY;
    }
    public void setY(int y) {
        mPositionY=y;
    }
    public int width() {
        return mWidth;
    }
    public void setWidth(int w) {
        mWidth=w;
    }
    public int height() {
        return mHeight;
    }
    public void setHeight(int h) {
        mHeight=h;
    }
    public float centerX() {
        return (float)mPositionX + (float)mWidth / 2.0f;
    }

    public float centerY() {
        return (float)mPositionY + (float)mHeight / 2.0f;
    }

    public String name() { return mName; }
    public void setName(String name) { mName=name; }

    public float score() { return mScore; }

    public static final Creator<DetectedObject> CREATOR =
            new Creator<DetectedObject>() {
                @Override
                public DetectedObject createFromParcel(Parcel parcel) {
                    return new DetectedObject(parcel);
                }

                @Override
                public DetectedObject[] newArray(int i) {
                    return new DetectedObject[i];
                }
            };

    @Override
    public void writeToParcel(Parcel dest, int flags) {

    }

    public void readFromParcel(Parcel parcel) {

    }

    @Override
    public int describeContents() {
        return 0;
    }
}
