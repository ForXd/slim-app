package cn.qingji.diary;
import android.content.Context;
import android.graphics.*;
import java.io.*;
import java.util.UUID;
final class PhotoFiles {
    static String save(Context c,Bitmap image)throws IOException{
        File dir=new File(c.getFilesDir(),"meal-photos");if(!dir.exists()&&!dir.mkdirs())throw new IOException("无法保存照片");
        String name=UUID.randomUUID()+".jpg";File file=new File(dir,name);
        try(OutputStream out=new FileOutputStream(file)){if(!image.compress(Bitmap.CompressFormat.JPEG,85,out))throw new IOException("照片保存失败");}
        return name;
    }
    static void prune(Context c,org.json.JSONArray records){
        java.util.Set<String> used=new java.util.HashSet<>();for(int i=0;i<records.length();i++)used.add(records.optJSONObject(i).optString("photo"));
        File[] files=new File(c.getFilesDir(),"meal-photos").listFiles();if(files==null)return;
        for(File f:files)if(!used.contains(f.getName())&&System.currentTimeMillis()-f.lastModified()>7L*86400000)f.delete();
    }
    static Bitmap load(Context c,String name){
        if(!name.matches("[a-f0-9-]{36}\\.jpg"))return null;
        BitmapFactory.Options o=new BitmapFactory.Options();o.inSampleSize=2;
        return BitmapFactory.decodeFile(new File(new File(c.getFilesDir(),"meal-photos"),name).getPath(),o);
    }
}
