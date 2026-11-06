package cn.qingji.diary;
import android.content.*;
import android.database.*;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.provider.OpenableColumns;
import java.io.*;

public final class UpdatePackage extends ContentProvider {
    static final Uri URI=Uri.parse("content://cn.qingji.diary.updates/ready.apk");
    private File file(Uri uri){
        if(!URI.equals(uri))throw new IllegalArgumentException("Unknown package");
        return new File(getContext().getFilesDir(),"updates/ready.apk");
    }
    @Override public boolean onCreate(){return true;}
    @Override public String getType(Uri uri){file(uri);return "application/vnd.android.package-archive";}
    @Override public ParcelFileDescriptor openFile(Uri uri,String mode)throws FileNotFoundException{
        if(!mode.equals("r"))throw new FileNotFoundException("Read only");
        return ParcelFileDescriptor.open(file(uri),ParcelFileDescriptor.MODE_READ_ONLY);
    }
    @Override public Cursor query(Uri uri,String[] columns,String selection,String[] args,String sort){
        File f=file(uri);String[] names=columns==null?new String[]{OpenableColumns.DISPLAY_NAME,OpenableColumns.SIZE}:columns;
        MatrixCursor cursor=new MatrixCursor(names);MatrixCursor.RowBuilder row=cursor.newRow();
        for(String name:names)row.add(name.equals(OpenableColumns.DISPLAY_NAME)?f.getName():name.equals(OpenableColumns.SIZE)?f.length():null);
        return cursor;
    }
    @Override public Uri insert(Uri uri,ContentValues v){throw new UnsupportedOperationException();}
    @Override public int delete(Uri uri,String s,String[] a){throw new UnsupportedOperationException();}
    @Override public int update(Uri uri,ContentValues v,String s,String[] a){throw new UnsupportedOperationException();}
}
