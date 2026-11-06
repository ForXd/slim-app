package cn.qingji.diary;

import android.content.SharedPreferences;
import org.json.*;
import java.time.Instant;
import java.util.UUID;

final class ChallengeStore {
    private final SharedPreferences prefs;
    ChallengeStore(SharedPreferences prefs){this.prefs=prefs;}
    JSONArray all() throws JSONException {return new JSONArray(prefs.getString("items","[]"));}
    JSONObject get(String id) throws JSONException {
        JSONArray items=all();
        for(int i=0;i<items.length();i++)if(items.getJSONObject(i).getString("id").equals(id))return items.getJSONObject(i);
        throw new IllegalArgumentException("挑战不存在");
    }
    private void save(JSONObject item)throws Exception{
        JSONArray before=all(),after=new JSONArray();boolean replaced=false;
        for(int i=0;i<before.length();i++){
            JSONObject current=before.getJSONObject(i);
            if(current.getString("id").equals(item.getString("id"))){after.put(item);replaced=true;}else after.put(current);
        }
        if(!replaced){if(after.length()>=100)throw new IllegalArgumentException("最多保留100个挑战");after.put(item);}
        if(!prefs.edit().putString("items",after.toString()).commit())throw new Exception("挑战保存失败，请检查存储空间");
    }
    String create(String title)throws Exception{
        String id=UUID.randomUUID().toString();
        save(new JSONObject().put("id",id).put("title",title(title)).put("created",Instant.now().toString()).put("archived",false).put("cells",new JSONObject()));
        return id;
    }
    private String title(String text){
        String value=text.trim();
        if(value.isEmpty()||value.length()>60)throw new IllegalArgumentException("挑战名称需为1–60个字");
        return value;
    }
    void rename(String id,String text)throws Exception{JSONObject item=get(id);item.put("title",title(text));save(item);}
    void archive(String id,boolean value)throws Exception{JSONObject item=get(id);item.put("archived",value);save(item);}
    void setCell(String id,int index,boolean done,String note)throws Exception{
        if(index<0||index>=100||note.length()>500)throw new IllegalArgumentException("格子或备注无效");
        JSONObject item=get(id);if(item.optBoolean("archived"))throw new IllegalArgumentException("请先重新开启此挑战");
        JSONObject cells=item.getJSONObject("cells");String key=String.valueOf(index);
        if(done){
            String at=cells.has(key)?cells.getJSONObject(key).getString("at"):Instant.now().toString();
            cells.put(key,new JSONObject().put("at",at).put("note",note));
        }else cells.remove(key);
        save(item);
    }
    static int next(JSONObject item)throws JSONException{
        JSONObject cells=item.getJSONObject("cells");
        for(int i=0;i<100;i++)if(!cells.has(String.valueOf(i)))return i;
        return -1;
    }
}
