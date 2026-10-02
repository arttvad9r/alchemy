package com.artt.alchemy.ui;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

/** A document provider in the test APK that can take time opening a document. */
public final class TransferTestProvider extends ContentProvider {
    @Override
    public boolean onCreate() {
        return true;
    }

    @Override
    public ParcelFileDescriptor openFile(Uri uri, String mode) throws FileNotFoundException {
        String delayMillis = uri.getQueryParameter("delayMillis");
        try {
            Thread.sleep(delayMillis == null ? 0L : Long.parseLong(delayMillis));
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            throw new FileNotFoundException("Document opening interrupted");
        }
        File file = new File(getContext().getCacheDir(), uri.getLastPathSegment());
        String text = uri.getQueryParameter("text");
        if (text != null) {
            try (FileOutputStream output = new FileOutputStream(file)) {
                output.write(text.getBytes(StandardCharsets.UTF_8));
            } catch (IOException error) {
                throw new FileNotFoundException(error.getMessage());
            }
        }
        return ParcelFileDescriptor.open(file, ParcelFileDescriptor.parseMode(mode));
    }

    @Override
    public String getType(Uri uri) {
        return "application/json";
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        return null;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        return null;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        return 0;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        return 0;
    }
}
