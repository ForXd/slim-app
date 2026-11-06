package cn.qingji.diary;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import org.json.JSONObject;
import java.io.*;
import java.util.*;

final class FoodDatabase {
    final SQLiteDatabase db;
    final JSONObject aliases;
    FoodDatabase(Context context) throws Exception {
        File file=new File(context.getFilesDir(),"foods-v4.sqlite");
        if(!file.exists()){
            File temp=new File(context.getFilesDir(),"foods-v4.tmp");
            try(InputStream in=context.getAssets().open("foods.sqlite");OutputStream out=new FileOutputStream(temp)){
                byte[] buffer=new byte[65536];int n;
                while((n=in.read(buffer))!=-1)out.write(buffer,0,n);
            }
            if(!temp.renameTo(file))throw new IOException("无法安装食物库");
        }
        db=SQLiteDatabase.openDatabase(file.getPath(),null,SQLiteDatabase.OPEN_READONLY);
        aliases=new JSONObject(read(context,"aliases.json"));
    }
    static String read(Context context,String asset) throws Exception{
        try(InputStream in=context.getAssets().open(asset);ByteArrayOutputStream out=new ByteArrayOutputStream()){
            byte[] bytes=new byte[8192];int n;while((n=in.read(bytes))!=-1)out.write(bytes,0,n);
            return out.toString("UTF-8");
        }
    }
    String chinese(String label){return aliases.optString(label,label);}
    List<JSONObject> search(String query){
        String q=query.trim();
        for(Iterator<String> keys=aliases.keys();keys.hasNext();){String key=keys.next();if(key.equalsIgnoreCase(q)){q=aliases.optString(key);break;}}
        if(q.equals("西红柿炒鸡蛋")||q.equals("西红柿炒蛋")||q.equals("番茄炒鸡蛋"))q="番茄炒蛋";
        String like="%"+q.replace("\\","\\\\").replace("%","\\%").replace("_","\\_")+"%";
        List<JSONObject> result=new ArrayList<>();
        try(Cursor c=db.rawQuery("SELECT * FROM foods WHERE name LIKE ? ESCAPE '\\' OR english LIKE ? ESCAPE '\\' ORDER BY CASE WHEN name=? THEN 0 WHEN english=? THEN 1 ELSE 2 END,priority,length(name) LIMIT 80",new String[]{like,like,q,q})){
            while(c.moveToNext()){
                JSONObject item=new JSONObject();
                for(int i=0;i<c.getColumnCount();i++){
                    try{item.put(c.getColumnName(i),c.getType(i)==Cursor.FIELD_TYPE_STRING?c.getString(i):c.getDouble(i));}
                    catch(Exception ignored){}
                }
                result.add(item);
            }
        }
        return result;
    }
    void close(){db.close();}
}
