package cn.qingji.diary;
import android.app.Instrumentation;
import android.app.Activity;
import android.os.Bundle;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.content.Intent;
import org.json.*;
import java.util.*;
import java.io.*;

public final class NativeCheck extends Instrumentation {
    @Override public void onCreate(Bundle args){super.onCreate(args);start();}
    @Override public void onStart(){
        Bundle output=new Bundle();
        try {
            FoodDatabase foods=new FoodDatabase(getTargetContext());
            for(String query:new String[]{"麻婆豆腐","宫保鸡丁","红烧肉","西红柿炒鸡蛋","米饭","tofu"}){
                if(foods.search(query).isEmpty())throw new AssertionError("No match for "+query);
            }
            if(foods.search("' OR 1=1 --").size()!=0)throw new AssertionError("Search injection");
            OfflineFood model=new OfflineFood(getTargetContext());
            JSONArray report=new JSONArray();
            for(String file:new String[]{"kung-pao.jpg","mapo-tofu.jpg","braised-pork.jpg"}){
                try(InputStream in=getContext().getAssets().open(file)){
                    Bitmap original=BitmapFactory.decodeStream(in);float scale=Math.min(1,1024f/Math.max(original.getWidth(),original.getHeight()));
                    Bitmap image=Bitmap.createScaledBitmap(original,(int)(original.getWidth()*scale),(int)(original.getHeight()*scale),true);
                    long start=System.nanoTime();JSONArray ranks=model.predict(image);
                    JSONObject item=new JSONObject().put("photo",file).put("milliseconds",(System.nanoTime()-start)/1000000).put("candidates",ranks);
                    for(int i=0;i<ranks.length();i++){
                        JSONObject rank=ranks.getJSONObject(i);String label=foods.chinese(rank.getString("name"));
                        rank.put("chinese",label).put("matches",foods.search(label).size());
                    }
                    report.put(item);if(image!=original)image.recycle();original.recycle();
                }
            }
            model.close();foods.close();
            output.putString("photoReport",report.toString());
            JournalStore store=new JournalStore(getTargetContext());
            output.putString("records",store.rows().toString());output.putBoolean("migrated",store.migrated());
            output.putString("result","PASS");
            finish(Activity.RESULT_OK,output);
        } catch(Throwable e){output.putString("result","FAIL "+android.util.Log.getStackTraceString(e));finish(Activity.RESULT_CANCELED,output);}
    }
}
