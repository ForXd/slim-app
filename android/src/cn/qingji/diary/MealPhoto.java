package cn.qingji.diary;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.provider.OpenableColumns;
import java.io.File;
import java.io.FileNotFoundException;

// Isolated, short-lived camera output. Only this single file can be opened.
public final class MealPhoto extends ContentProvider {
    static final Uri URI = Uri.parse("content://cn.qingji.diary.photo/capture.jpg");
    private File file(Uri uri) {
        if (!URI.equals(uri)) throw new IllegalArgumentException("Unknown image");
        return new File(getContext().getCacheDir(), "capture.jpg");
    }
    public boolean onCreate() { return true; }
    public String getType(Uri uri) { return "image/jpeg"; }
    public ParcelFileDescriptor openFile(Uri uri, String mode) throws FileNotFoundException {
        return ParcelFileDescriptor.open(file(uri), ParcelFileDescriptor.parseMode(mode));
    }
    public Cursor query(Uri uri,String[] columns,String selection,String[] args,String order) {
        File f=file(uri);
        String[] names=columns==null?new String[]{OpenableColumns.DISPLAY_NAME,OpenableColumns.SIZE}:columns;
        MatrixCursor cursor=new MatrixCursor(names);
        MatrixCursor.RowBuilder row=cursor.newRow();
        for(String col:names) row.add(OpenableColumns.DISPLAY_NAME.equals(col)?f.getName():OpenableColumns.SIZE.equals(col)?f.length():null);
        return cursor;
    }
    public Uri insert(Uri uri,ContentValues values){throw new UnsupportedOperationException();}
    public int delete(Uri uri,String s,String[] args){throw new UnsupportedOperationException();}
    public int update(Uri uri,ContentValues values,String s,String[] args){throw new UnsupportedOperationException();}
}
