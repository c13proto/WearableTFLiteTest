package com.sonymobile.generalobjectdetector;

import android.content.Context;
import android.content.res.AssetManager;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;

public final class GeneralObjectAssetsUnpacker {
    private static final String SOD_FILE_NAME = GeneralObjectDetector.SOD_NAME + ".sod";

    private static final String SOD_FILE_ASSET_PATH = "sod/" + SOD_FILE_NAME;

    private static final String UNPACKED_SOD_DIRECTORY_NAME = "sod";

    private static final long TRANSFERED_LENGTH = 1 * 1000 * 1000;

    private final Context mContext;

    public GeneralObjectAssetsUnpacker(Context context) {
        mContext = context;
    }

    private File getUnpackedSodDirectory() {
        return mContext.getDir(UNPACKED_SOD_DIRECTORY_NAME, Context.MODE_PRIVATE);
    }

    private File getUnpackedSodFile() {
        return new File(getUnpackedSodDirectory(), SOD_FILE_NAME);
    }

    private void unpackAsset(String assetPath, File outputFile) throws IOException {
        AssetManager assetManager = mContext.getAssets();

        try (BufferedInputStream inputStream = new BufferedInputStream(assetManager.open(assetPath));
             ReadableByteChannel inputChannel = Channels.newChannel(inputStream);
             FileOutputStream outputStream = new FileOutputStream(outputFile);
             FileChannel outputChannel = outputStream.getChannel()) {

            long position = 0;
            while (0 < inputStream.available()) {
                long writtenLength = outputChannel.transferFrom(inputChannel, position, TRANSFERED_LENGTH);

                position += writtenLength;
            }
        }
    }

    public File unpackSodFile() throws IOException {
        getUnpackedSodDirectory().delete();

        File unpackedSodFile = getUnpackedSodFile();
        unpackAsset(SOD_FILE_ASSET_PATH, unpackedSodFile);

        return unpackedSodFile;
    }
}
