package cn.qingji.diary;

import android.app.AlertDialog;
import android.graphics.Color;
import android.content.res.ColorStateList;
import android.text.*;
import android.view.*;
import android.view.inputmethod.InputMethodManager;
import android.widget.*;
import org.json.*;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;

final class ChallengeScreen {
    private final MainActivity app;private final ChallengeStore store;
    String selected,query="";boolean archived=false;
    LinearLayout results,detail;TextView position;ChallengePager pager;Button previousButton,nextButton;
    List<JSONObject> visible=new ArrayList<>();
    final android.os.Handler searchHandler=new android.os.Handler(android.os.Looper.getMainLooper());
    final Runnable searchRefresh=()->{if(results!=null&&results.isAttachedToWindow())refresh();};
    ChallengeScreen(MainActivity app){this.app=app;store=new ChallengeStore(app.getSharedPreferences("challenges-v1",0));}
    void render(){
        searchHandler.removeCallbacks(searchRefresh);
        TextView intro=app.text(app.body,"每一次都算数 · 左右滑动切换挑战",12,false);intro.setTextColor(app.style.muted);
        app.horizontalButtons(app.body,"新建挑战",()->editTitle(null),archived?"查看进行中":"查看已归档",()->{archived=!archived;selected=null;app.show("百次");});
        EditText search=new EditText(app);search.setText(query);search.setHint("搜索挑战名称");search.setContentDescription("搜索挑战名称");search.setSingleLine(true);search.setTextSize(15);search.setBackgroundTintList(ColorStateList.valueOf(app.accent));app.body.addView(search,new LinearLayout.LayoutParams(-1,app.dp(48)));search.setCompoundDrawables(app.style.icon("magnify",app.style.muted,20),null,null,null);search.setCompoundDrawablePadding(app.dp(8));search.setImeOptions(android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH);
        search.setOnEditorActionListener((v,id,e)->{search.clearFocus();((InputMethodManager)app.getSystemService(android.content.Context.INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(search.getWindowToken(),0);return true;});
        results=new LinearLayout(app);results.setOrientation(LinearLayout.VERTICAL);app.body.addView(results);
        search.addTextChangedListener(new TextWatcher(){public void beforeTextChanged(CharSequence s,int st,int c,int a){}public void onTextChanged(CharSequence s,int st,int before,int count){query=s.toString();searchHandler.removeCallbacks(searchRefresh);searchHandler.postDelayed(searchRefresh,120);}public void afterTextChanged(Editable e){}});
        refresh();
    }
    void refresh(){
        try{
            results.removeAllViews();visible.clear();JSONArray items=store.all();
            for(int i=0;i<items.length();i++){JSONObject item=items.getJSONObject(i);if(matches(item,archived,query))visible.add(item);}
            if(visible.isEmpty()){app.text(results,query.trim().isEmpty()?(archived?"这里会保留你归档的每一次努力。":"试试「散步100次」或「自己做饭100次」。"):"没有找到这个挑战，换个关键词试试。",15,false);return;}
            int active=0;for(int i=0;i<visible.size();i++)if(visible.get(i).getString("id").equals(selected))active=i;
            selected=visible.get(active).getString("id");
            pager=new ChallengePager(app);results.addView(pager,new LinearLayout.LayoutParams(-1,app.dp(96)));
            for(int i=0;i<visible.size();i++){
                JSONObject item=visible.get(i);final int index=i;LinearLayout card=app.column();card.setPadding(app.dp(18),app.dp(10),app.dp(18),app.dp(10));
                LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(app.dp(280),-1);p.setMargins(0,app.dp(7),0,app.dp(7));pager.strip.addView(card,p);
                TextView name=app.style.label(item.getString("title"),18,true,app.ink);card.addView(name);name.setMaxLines(1);name.setEllipsize(TextUtils.TruncateAt.END);
                app.style.space(card,7);card.addView(app.style.label(item.getJSONObject("cells").length()+" / 100"+(item.optBoolean("archived")?"  ·  已归档":"  ·  次修炼"),14,false,app.style.muted));
                card.setContentDescription(item.getString("title")+"，"+item.getJSONObject("cells").length()+"次，点击切换");card.setOnClickListener(v->pager.select(index));
            }
            LinearLayout navigation=app.row(results);
            Button previous=previousButton=app.button(navigation,"上一个",()->pager.select(pager.selected-1));previous.setLayoutParams(new LinearLayout.LayoutParams(app.dp(72),app.dp(42)));previous.setTextSize(11);previous.setPadding(0,0,0,0);
            position=app.text(navigation,"",12,false);position.setGravity(Gravity.CENTER);position.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);position.setLayoutParams(new LinearLayout.LayoutParams(0,-2,1));
            Button next=nextButton=app.button(navigation,"下一个",()->pager.select(pager.selected+1));next.setLayoutParams(new LinearLayout.LayoutParams(app.dp(72),app.dp(42)));next.setTextSize(11);next.setPadding(0,0,0,0);
            detail=new LinearLayout(app);detail.setOrientation(LinearLayout.VERTICAL);results.addView(detail);
            pager.configure(visible.size(),active,index->{try{selected=visible.get(index).getString("id");renderDetail();}catch(Exception e){app.error(e.getMessage());}});
            renderDetail();
        }catch(Exception e){app.error("无法读取挑战："+e.getMessage());}
    }
    static boolean matches(JSONObject item,boolean archived,String query){return item.optBoolean("archived")==archived&&item.optString("title").toLowerCase(Locale.ROOT).contains(query.trim().toLowerCase(Locale.ROOT));}
    void renderDetail()throws Exception{
        detail.removeAllViews();JSONObject active=store.get(selected);final String id=selected;int activeIndex=0;
        for(int i=0;i<visible.size();i++){
            boolean chosen=visible.get(i).getString("id").equals(id);if(chosen)activeIndex=i;
            LinearLayout card=(LinearLayout)pager.strip.getChildAt(i);card.setBackground(new android.graphics.drawable.InsetDrawable(app.style.ripple(chosen?0xfff2e2cf:0x55ffffff,12,true),0,0,app.dp(10),0));
            card.setPadding(app.dp(18),app.dp(10),app.dp(24),app.dp(10));
            ((TextView)card.getChildAt(0)).setTextColor(chosen?app.accent:app.ink);
        }
        position.setText((activeIndex+1)+" / "+visible.size()+" 个挑战");previousButton.setEnabled(activeIndex>0);nextButton.setEnabled(activeIndex<visible.size()-1);
        LinearLayout card=app.card(detail);card.setPadding(app.dp(8),app.dp(12),app.dp(8),app.dp(12));
        int count=active.getJSONObject("cells").length();app.text(card,count==100?"一百次完成，真好。":"已完成 "+count+" 次 · 还差 "+(100-count)+" 次",16,true);
        ProgressBar progress=new ProgressBar(app,null,android.R.attr.progressBarStyleHorizontal);progress.setMax(100);progress.setProgress(count);progress.setProgressTintList(ColorStateList.valueOf(app.accent));card.addView(progress);
        app.text(card,archived?"已归档，重新开启后可继续记录。":"轻点格子，记录这一次的小小收获。",11,false);
        JSONObject cells=active.getJSONObject("cells");
        for(int row=0;row<10;row++){
            LinearLayout line=new LinearLayout(app);card.addView(line);
            for(int col=0;col<10;col++){
                final int index=row*10+col;boolean done=cells.has(String.valueOf(index));
                Button cell=app.button(line,String.valueOf(index+1),()->cell(id,index));cell.setTextSize(10);cell.setMinWidth(0);cell.setMinimumWidth(0);cell.setMinHeight(0);cell.setMinimumHeight(0);cell.setPadding(0,0,0,0);cell.setTypeface(android.graphics.Typeface.DEFAULT);
                cell.setContentDescription("第"+(index+1)+"格，"+(done?"已完成":"未完成"));
                LinearLayout.LayoutParams params=new LinearLayout.LayoutParams(0,app.dp(31),1);params.setMargins(app.dp(1),app.dp(2),app.dp(1),app.dp(2));cell.setLayoutParams(params);
                cell.setBackground(app.style.ripple(done?app.accent:0xfff2e8da,5,false));cell.setTextColor(done?Color.WHITE:app.style.muted);
            }
        }
        int next=ChallengeStore.next(active);
        if(!archived&&next>=0){Button complete=app.button(detail,"完成下一次 · 第"+(next+1)+"格",()->cell(id,next));complete.setBackground(app.style.ripple(app.accent,10,false));complete.setTextColor(Color.WHITE);}
        app.horizontalButtons(detail,"重命名",()->editTitle(id),archived?"重新开启":"归档挑战",()->{
            if(archived)changeArchive(id,false);else new AlertDialog.Builder(app).setMessage("归档后保留100格和全部备注，随时可以重新开启。").setNegativeButton("取消",null).setPositiveButton("归档",(d,w)->changeArchive(id,true)).show();
        });
    }
    void changeArchive(String id,boolean value){try{store.archive(id,value);selected=null;refresh();}catch(Exception e){app.error(e.getMessage());}}
    void editTitle(String id){try{
        LinearLayout form=app.column();EditText name=app.input(form,"挑战名称",id==null?"":store.get(id).getString("title"),false);name.setFilters(new InputFilter[]{new InputFilter.LengthFilter(60)});
        AlertDialog dialog=app.dialog(id==null?"新建100次挑战":"重命名",form);
        app.button(form,"保存挑战",()->{try{if(id==null){selected=store.create(name.getText().toString());archived=false;query="";}else store.rename(id,name.getText().toString());dialog.dismiss();app.show("百次");}catch(Exception e){name.setError(e.getMessage());}});
    }catch(Exception e){app.error(e.getMessage());}}
    void cell(String id,int index){try{
        JSONObject challenge=store.get(id),old=challenge.getJSONObject("cells").optJSONObject(String.valueOf(index));LinearLayout form=app.column();
        if(old!=null)app.text(form,"完成于 "+DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").withZone(ZoneId.systemDefault()).format(Instant.parse(old.getString("at"))),14,false);
        EditText note=app.input(form,"备注（可留空）",old==null?"":old.optString("note"),false);note.setSingleLine(false);note.setMinLines(2);note.setFilters(new InputFilter[]{new InputFilter.LengthFilter(500)});
        AlertDialog dialog=app.dialog("第"+(index+1)+"次",form);if(challenge.optBoolean("archived")){note.setEnabled(false);return;}
        app.button(form,old==null?"确认完成":"保存备注",()->{try{store.setCell(id,index,true,note.getText().toString());dialog.dismiss();refreshKeepingScroll();int count=store.get(id).getJSONObject("cells").length();app.toast(count==100?"100次完成，恭喜你！":"已完成 "+count+" / 100 次");}catch(Exception e){app.error(e.getMessage());}});
        if(old!=null)app.button(form,"撤销这一次",()->new AlertDialog.Builder(app).setMessage("撤销第"+(index+1)+"格及其备注？").setNegativeButton("保留",null).setPositiveButton("撤销",(d,w)->{try{store.setCell(id,index,false,"");dialog.dismiss();refreshKeepingScroll();}catch(Exception e){app.error(e.getMessage());}}).show());
    }catch(Exception e){app.error(e.getMessage());}}
    void refreshKeepingScroll(){int y=app.activeScroll.getScrollY();refresh();app.activeScroll.post(()->app.activeScroll.scrollTo(0,y));}
}
