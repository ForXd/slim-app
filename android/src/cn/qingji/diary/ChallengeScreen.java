package cn.qingji.diary;

import android.app.AlertDialog;
import android.graphics.Color;
import android.content.res.ColorStateList;
import android.text.InputFilter;
import android.widget.*;
import org.json.*;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

final class ChallengeScreen {
    private final MainActivity app;
    private final ChallengeStore store;
    String selected;
    boolean archived=false;
    ChallengeScreen(MainActivity app){this.app=app;store=new ChallengeStore(app.getSharedPreferences("challenges-v1",0));}
    void render(){
        try{
            app.text(app.body,"每一次，都算数",23,true);
            app.text(app.body,"一格记录一次完成，同一天可以打卡多次。",14,false);
            app.horizontalButtons(app.body,"新建挑战",()->editTitle(null),archived?"查看进行中":"查看已归档",()->{archived=!archived;selected=null;app.show("百次");});
            JSONArray items=store.all();JSONObject active=null;
            for(int i=0;i<items.length();i++){
                JSONObject item=items.getJSONObject(i);
                if(item.optBoolean("archived")!=archived)continue;
                if(active==null||item.getString("id").equals(selected))active=item;
                String id=item.getString("id");
                app.button(app.body,item.getString("title")+"  "+item.getJSONObject("cells").length()+"/100",()->{selected=id;app.show("百次");});
            }
            if(active==null){app.text(app.body,archived?"归档后会保留完整打卡记录。":"可以从「散步100次」「自己做饭100次」开始。",16,false);return;}
            selected=active.getString("id");final String id=selected;
            LinearLayout card=app.card(app.body);
            app.text(card,active.getString("title"),21,true);
            int count=active.getJSONObject("cells").length();
            app.text(card,count+" / 100"+(count==100?" · 挑战完成！":""),30,true);
            ProgressBar progress=new ProgressBar(app,null,android.R.attr.progressBarStyleHorizontal);progress.setMax(100);progress.setProgress(count,android.animation.ValueAnimator.areAnimatorsEnabled());progress.setProgressTintList(ColorStateList.valueOf(app.accent));card.addView(progress);
            app.text(card,archived?"已归档，可重新开启继续记录。":"点击格子补打、修改备注或撤销。",13,false);
            JSONObject cells=active.getJSONObject("cells");
            for(int row=0;row<10;row++){
                LinearLayout line=new LinearLayout(app);card.addView(line);
                for(int col=0;col<10;col++){
                    final int index=row*10+col;
                    boolean done=cells.has(String.valueOf(index));
                    Button cell=app.button(line,String.valueOf(index+1),()->cell(id,index));
                    cell.setTextSize(11);cell.setMinWidth(0);cell.setMinimumWidth(0);cell.setMinHeight(0);cell.setMinimumHeight(0);cell.setPadding(0,0,0,0);
                    cell.setContentDescription("第"+(index+1)+"格，"+(done?"已完成":"未完成"));
                    LinearLayout.LayoutParams params=new LinearLayout.LayoutParams(0,app.dp(38),1);params.setMargins(app.dp(1),app.dp(2),app.dp(1),app.dp(2));cell.setLayoutParams(params);
                    cell.setBackgroundTintList(ColorStateList.valueOf(done?app.accent:0xffefe2d2));cell.setTextColor(done?Color.WHITE:app.ink);
                }
            }
            int next=ChallengeStore.next(active);
            if(!archived&&next>=0)app.button(app.body,"完成下一次 · 第"+(next+1)+"格",()->cell(id,next));
            app.horizontalButtons(app.body,"重命名",()->editTitle(id),archived?"重新开启":"归档挑战",()->{
                if(archived)changeArchive(id,false);
                else new AlertDialog.Builder(app).setMessage("归档后保留100格和全部备注，随时可以重新开启。").setNegativeButton("取消",null).setPositiveButton("归档",(d,w)->changeArchive(id,true)).show();
            });
        }catch(Exception e){app.error("无法读取挑战："+e.getMessage());}
    }
    void changeArchive(String id,boolean value){try{store.archive(id,value);selected=null;app.show("百次");}catch(Exception e){app.error(e.getMessage());}}
    void editTitle(String id){
        try{
            LinearLayout form=app.column();
            EditText name=app.input(form,"挑战名称",id==null?"":store.get(id).getString("title"),false);name.setFilters(new InputFilter[]{new InputFilter.LengthFilter(60)});
            AlertDialog dialog=app.dialog(id==null?"新建100次挑战":"重命名",form);
            app.button(form,"保存挑战",()->{try{
                if(id==null)selected=store.create(name.getText().toString());else store.rename(id,name.getText().toString());
                if(id==null)archived=false;dialog.dismiss();app.show("百次");
            }catch(Exception e){name.setError(e.getMessage());}});
        }catch(Exception e){app.error(e.getMessage());}
    }
    void cell(String id,int index){
        try{
            JSONObject challenge=store.get(id),old=challenge.getJSONObject("cells").optJSONObject(String.valueOf(index));
            LinearLayout form=app.column();
            if(old!=null)app.text(form,"完成于 "+DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").withZone(ZoneId.systemDefault()).format(Instant.parse(old.getString("at"))),14,false);
            EditText note=app.input(form,"备注（可留空）",old==null?"":old.optString("note"),false);note.setSingleLine(false);note.setMinLines(2);note.setFilters(new InputFilter[]{new InputFilter.LengthFilter(500)});
            AlertDialog dialog=app.dialog("第"+(index+1)+"次",form);
            if(challenge.optBoolean("archived")){note.setEnabled(false);return;}
            app.button(form,old==null?"确认完成":"保存备注",()->{try{
                store.setCell(id,index,true,note.getText().toString());dialog.dismiss();app.show("百次");
                int count=store.get(id).getJSONObject("cells").length();
                app.toast(count==100?"100次完成，恭喜你！":"已完成 "+count+" / 100 次");
            }catch(Exception e){app.error(e.getMessage());}});
            if(old!=null)app.button(form,"撤销这一次",()->new AlertDialog.Builder(app).setMessage("撤销第"+(index+1)+"格及其备注？").setNegativeButton("保留",null).setPositiveButton("撤销",(d,w)->{try{store.setCell(id,index,false,"");dialog.dismiss();app.show("百次");}catch(Exception e){app.error(e.getMessage());}}).show());
        }catch(Exception e){app.error(e.getMessage());}
    }
}
