package cn.qingji.diary;

import android.content.Context;
import android.content.SharedPreferences;
import org.json.*;
import java.text.SimpleDateFormat;
import java.util.*;

final class JournalStore {
    final SharedPreferences prefs;
    JournalStore(Context context){prefs=context.getSharedPreferences("native-journal",0);}
    static String today(){return new SimpleDateFormat("yyyy-MM-dd",Locale.US).format(new Date());}
    JSONArray rows(){
        try{return new JSONArray(prefs.getString("records","[]"));}
        catch(JSONException e){throw new IllegalStateException("记录数据无法读取，请保留应用数据。",e);}
    }
    JSONObject plan(){try{return new JSONObject(prefs.getString("plan","{}"));}catch(Exception e){return new JSONObject();}}
    boolean migrated(){return prefs.getBoolean("migrated",false);}
    void migrate(JSONObject old) throws Exception{
        JSONArray records=old.isNull("records")?new JSONArray():new JSONArray(old.getString("records"));
        String plan=old.isNull("plan")?"{}":old.getString("plan");
        new JSONObject(plan);
        if(!prefs.edit().putString("records",records.toString()).putString("plan",plan).putBoolean("migrated",true).commit())throw new Exception("保存迁移数据失败");
    }
    void save(JSONObject row) throws Exception{
        if(!row.has("id"))row.put("id",System.currentTimeMillis()+"-"+UUID.randomUUID());
        JSONArray before=rows(),after=new JSONArray();
        for(int i=0;i<before.length();i++){JSONObject old=before.getJSONObject(i);if(!old.optString("id").equals(row.getString("id")))after.put(old);}
        after.put(row);
        if(!prefs.edit().putString("records",after.toString()).commit())throw new Exception("无法保存，请检查存储空间");
    }
    void delete(JSONObject row) throws Exception{
        JSONArray before=rows(),after=new JSONArray();
        for(int i=0;i<before.length();i++){JSONObject old=before.getJSONObject(i);if(!old.optString("id").equals(row.getString("id")))after.put(old);}
        if(!prefs.edit().putString("records",after.toString()).commit())throw new Exception("无法删除");
    }
    List<JSONObject> sorted(){
        List<JSONObject> result=new ArrayList<>();JSONArray array=rows();
        for(int i=0;i<array.length();i++)result.add(array.optJSONObject(i));
        result.sort((a,b)->{int date=b.optString("date").compareTo(a.optString("date"));return date!=0?date:b.optString("id").compareTo(a.optString("id"));});
        return result;
    }
    static boolean known(JSONObject row,String field){return row.has(field)&&!row.isNull(field)&&!row.optString(field).isEmpty()&&Double.isFinite(row.optDouble(field,Double.NaN));}
    static double sum(List<JSONObject> rows,String field){double sum=0;for(JSONObject row:rows)if(known(row,field))sum+=row.optDouble(field);return sum;}
}
