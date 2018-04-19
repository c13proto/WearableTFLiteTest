package com.sonymobile.generalobjectdetector;

import android.graphics.RectF;

import java.util.Locale;

public final class GeneralDetectedObject {
    private final RectF mNormalizedArea;

    private final GeneralObjectLabel mLabel;

    private final float mScore;

    public GeneralDetectedObject(RectF normalizedArea, GeneralObjectLabel label, float score) {
        mNormalizedArea = new RectF(normalizedArea);
        mLabel = label;
        mScore = score;
    }

    public GeneralObjectLabel getLabel() {
        return mLabel;
    }

    public float getScore() {
        return mScore;
    }

    public RectF getNormalizedArea() {
        return new RectF(mNormalizedArea);
    }

    @Override
    public String toString() {
        return String.format(
                Locale.US,
                "%s(number=%d, score=%f): left=%f, top=%f, right=%f, bottom=%f, width=%f, height=%f",
                mLabel.name,
                mLabel.number,
                mScore,
                mNormalizedArea.left,
                mNormalizedArea.top,
                mNormalizedArea.right,
                mNormalizedArea.bottom,
                mNormalizedArea.width(),
                mNormalizedArea.height());
    }
}
