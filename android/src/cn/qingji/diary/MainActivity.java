package cn.qingji.diary;

import android.app.*;
import android.os.*;
import android.content.*;
import android.content.res.ColorStateList;
import android.graphics.*;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.provider.MediaStore;
import android.text.*;
import android.view.*;
import android.view.inputmethod.InputMethodManager;
import android.webkit.*;
import android.widget.*;
import org.json.*;
import java.io.*;
import java.text.*;
import java.util.*;
import java.util.concurrent.*;

public final class MainActivity extends Activity {
    final int ink=Color.rgb(63,44,34),accent=Color.rgb(158,65,40),paper=Color.rgb(250,245,236);
    final ExecutorService worker=Executors.newSingleThreadExecutor();
    JournalStore store;
    ChallengeScreen challenges;
    AppUpdater updater;
    TextView updateStatus;
    Runnable updatePanelRefresh;
    FoodDatabase foods;
    OfflineFood model;
    LinearLayout root,body,tabs;
    WebView migration;
    String selectedDate=JournalStore.today(),screen="今日";
    Bitmap photo;
    String photoRegion="整张";
    boolean busy=false;
    int chartDays=7;
    String chartMetric="kcal";
    final String[] groups={"早餐","午餐","晚餐","加餐"};
    final String[] sports={"平地中速步行","轻松骑车","平地慢走"};
    final double[] mets={3.8,4,2.8};
    final String[] planNames={"维持与观察","温和减量","标准减量","自定义目标"};

    @Override public void onCreate(Bundle saved){
        super.onCreate(saved);
        if(saved!=null){selectedDate=saved.getString("selectedDate",selectedDate);screen=saved.getString("screen",screen);}
        getWindow().setStatusBarColor(paper);getWindow().setNavigationBarColor(paper);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR|View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
        store=new JournalStore(this);model=new OfflineFood(this);
        challenges=new ChallengeScreen(this);updater=AppUpdater.get(this);
        root=column();root.setPadding(0,0,0,0);root.setBackgroundColor(paper);setContentView(root);
        text(root,"歪脖树修炼笔记",32,true);text(root,"正在准备离线食物库…",16,false);
        worker.execute(()->{try{foods=new FoodDatabase(this);runOnUiThread(()->{if(store.migrated())show(screen);else migrate();});}
        catch(Exception e){runOnUiThread(()->error("食物库加载失败，请重新打开应用。"));}});
    }
    void migrate(){
        migration=new WebView(this);
        migration.getSettings().setJavaScriptEnabled(true);migration.getSettings().setDomStorageEnabled(true);
        migration.setWebViewClient(new WebViewClient(){
            @Override public void onPageFinished(WebView view,String url){
                view.evaluateJavascript("JSON.stringify({records:localStorage.getItem('qingji.records.v1'),plan:localStorage.getItem('qingji.energyPlan.v1')})",value->{
                    try{Object decoded=new JSONTokener(value).nextValue();store.migrate(new JSONObject((String)decoded));migration.destroy();migration=null;show("今日");}
                    catch(Exception e){text(root,"旧版记录尚未迁移，请重试。原数据已保留。",16,false);button(root,"重试迁移",()->{migration.destroy();migrate();});}
                });
            }
        });
        // Same file origin as releases 0.1–0.3. WebView exists only for this one-time import.
        migration.loadUrl("file:///android_asset/migrate.html");
    }
    int dp(float n){return (int)(getResources().getDisplayMetrics().density*n+.5f);}
    LinearLayout column(){LinearLayout v=new LinearLayout(this);v.setOrientation(LinearLayout.VERTICAL);v.setPadding(dp(18),dp(10),dp(18),dp(10));return v;}
    LinearLayout row(LinearLayout parent){LinearLayout v=new LinearLayout(this);v.setGravity(Gravity.CENTER_VERTICAL);parent.addView(v,new LinearLayout.LayoutParams(-1,-2));return v;}
    TextView text(LinearLayout parent,String label,int size,boolean bold){
        TextView t=new TextView(this);t.setText(label);t.setTextSize(size);t.setTextColor(ink);t.setPadding(0,dp(5),0,dp(5));if(bold)t.setTypeface(null,Typeface.BOLD);
        parent.addView(t);return t;
    }
    Button button(LinearLayout parent,String label,Runnable action){
        Button b=new Button(this);b.setText(label);b.setAllCaps(false);b.setTextColor(accent);b.setMinHeight(dp(48));b.setTextSize(15);b.setPadding(dp(12),dp(8),dp(12),dp(8));b.setStateListAnimator(null);
        GradientDrawable surface=new GradientDrawable();surface.setColor(0xffefe2d2);surface.setCornerRadius(dp(12));
        b.setBackground(new android.graphics.drawable.RippleDrawable(ColorStateList.valueOf(0x229e4128),surface,null));
        LinearLayout.LayoutParams layout=new LinearLayout.LayoutParams(-1,-2);layout.setMargins(dp(3),dp(4),dp(3),dp(4));
        parent.addView(b,layout);b.setOnClickListener(v->action.run());return b;
    }
    void horizontalButtons(LinearLayout parent,String a,Runnable aa,String b,Runnable bb){
        LinearLayout line=row(parent);Button x=button(line,a,aa),y=button(line,b,bb);
        LinearLayout.LayoutParams left=new LinearLayout.LayoutParams(0,-2,1),right=new LinearLayout.LayoutParams(0,-2,1);left.setMargins(dp(3),dp(4),dp(3),dp(4));right.setMargins(dp(3),dp(4),dp(3),dp(4));x.setLayoutParams(left);y.setLayoutParams(right);
    }
    LinearLayout card(LinearLayout parent){
        LinearLayout v=column();GradientDrawable d=new GradientDrawable();d.setColor(Color.WHITE);d.setCornerRadius(dp(16));v.setBackground(d);
        LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.setMargins(0,dp(8),0,dp(8));parent.addView(v,p);return v;
    }
    EditText input(LinearLayout parent,String label,String value,boolean numeric){
        text(parent,label,13,false);EditText e=new EditText(this);e.setSingleLine(true);e.setText(value);e.setTextColor(ink);e.setTextSize(16);e.setContentDescription(label);
        if(numeric)e.setInputType(android.text.InputType.TYPE_CLASS_NUMBER|android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL);
        e.setBackgroundTintList(ColorStateList.valueOf(accent));e.setMinHeight(dp(48));parent.addView(e,new LinearLayout.LayoutParams(-1,-2));return e;
    }
    Spinner spinner(LinearLayout parent,String label,String[] values,int selection){
        text(parent,label,13,false);Spinner s=new Spinner(this);ArrayAdapter<String>a=new ArrayAdapter<>(this,android.R.layout.simple_spinner_item,values);a.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);s.setAdapter(a);s.setSelection(Math.max(0,selection));s.setContentDescription(label);parent.addView(s,new LinearLayout.LayoutParams(-1,dp(48)));return s;
    }
    interface DateChange{void set(String date);}
    Button dateButton(LinearLayout parent,String current,DateChange change){
        return button(parent,current+" · 选择日期",()->{
            String[] parts=current.split("-");DatePickerDialog d=new DatePickerDialog(this,(view,y,m,day)->change.set(String.format(Locale.US,"%04d-%02d-%02d",y,m+1,day)),Integer.parseInt(parts[0]),Integer.parseInt(parts[1])-1,Integer.parseInt(parts[2]));
            d.getDatePicker().setMaxDate(System.currentTimeMillis());d.show();
        });
    }
    void error(String message){new AlertDialog.Builder(this).setMessage(message).setPositiveButton("知道了",null).show();}
    void toast(String message){Toast.makeText(this,message,Toast.LENGTH_SHORT).show();}
    String n(double value){return new DecimalFormat("0.#").format(value);}
    void show(String which){
        if(isFinishing()||isDestroyed())return;
        screen=which;updateStatus=null;root.removeAllViews();
        LinearLayout header=column();root.addView(header);text(header,"歪脖树修炼笔记",23,true);text(header,which,14,false);
        ScrollView scroll=new ScrollView(this);scroll.setFillViewport(true);root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));body=column();scroll.addView(body);
        tabs=new LinearLayout(this);root.addView(tabs);
        for(String title:new String[]{"今日","食物库","百次","趋势","计划"}){
            Button b=button(tabs,title,()->show(title));b.setLayoutParams(new LinearLayout.LayoutParams(0,dp(54),1));b.setTextSize(13);b.setTypeface(null,title.equals(which)?Typeface.BOLD:Typeface.NORMAL);
            b.setBackgroundTintList(ColorStateList.valueOf(title.equals(which)?0xff9e4128:paper));b.setTextColor(title.equals(which)?Color.WHITE:ink);
        }
        if(which.equals("今日"))home();else if(which.equals("食物库"))library(body,"",null);else if(which.equals("百次"))challenges.render();else if(which.equals("趋势"))charts();else plan();
    }
    List<JSONObject> dayRows(String day){List<JSONObject> out=new ArrayList<>();for(JSONObject r:store.sorted())if(r.optString("date").equals(day))out.add(r);return out;}
    void home(){
        updateStatus=text(body,updater.message,12,false);updateStatus.setOnClickListener(v->updates());
        dateButton(body,selectedDate,date->{selectedDate=date;show("今日");});
        List<JSONObject> day=dayRows(selectedDate),meals=new ArrayList<>();int missing=0;
        for(JSONObject r:day)if(r.optString("kind").equals("meal")){meals.add(r);if(!JournalStore.known(r,"kcal"))missing++;}
        LinearLayout summary=card(body);
        text(summary,"已记摄入",14,false);text(summary,meals.size()==missing?"— 千卡":n(JournalStore.sum(meals,"kcal"))+" 千卡",34,true);
        JSONObject plan=store.plan();double target=plan.optDouble("target",0),burn=JournalStore.sum(day,"burn");
        if(target>0){
            ProgressBar progress=new ProgressBar(this,null,android.R.attr.progressBarStyleHorizontal);progress.setMax((int)target);progress.setProgress((int)JournalStore.sum(meals,"kcal"));progress.setProgressTintList(ColorStateList.valueOf(accent));summary.addView(progress,new LinearLayout.LayoutParams(-1,dp(8)));
            text(summary,"每日目标 "+n(target)+" · "+(target>=JournalStore.sum(meals,"kcal")?"尚余 ":"已超 ")+n(Math.abs(target-JournalStore.sum(meals,"kcal")))+" 千卡",14,false);
        }else button(summary,"设置每日计划",()->show("计划"));
        if(missing>0)text(summary,missing+" 条餐食热量待补",13,false);
        text(summary,"运动额外消耗 "+n(burn)+" 千卡",14,false);
        if(plan.optDouble("base",0)>0)text(summary,"估计全天消耗 "+n(plan.optDouble("base")+burn)+" 千卡",14,false);
        StringBuilder macros=new StringBuilder();
        for(String field:new String[]{"protein","carbs","fat"}){
            int known=0;for(JSONObject r:meals)if(JournalStore.known(r,field))known++;
            if(known>0){if(macros.length()>0)macros.append("  ·  ");macros.append(field.equals("protein")?"蛋白质":field.equals("carbs")?"碳水":"脂肪").append(" ").append(n(JournalStore.sum(meals,field))).append(" g").append(known<meals.size()?"（部分）":"");}
        }
        if(macros.length()>0)text(summary,macros.toString(),13,false);
        horizontalButtons(body,"拍照识餐",()->photoMenu(),"搜索记餐",()->show("食物库"));
        horizontalButtons(body,"记体重",()->weight(new JSONObject()),"记运动",()->exercise(new JSONObject()));
        for(JSONObject r:store.sorted())if(r.optString("kind").equals("weight")){text(body,"最近体重 "+r.optString("kg")+" kg  ·  "+r.optString("date"),17,true);break;}
        text(body,"饮食日记",21,true);
        for(String group:groups){
            text(body,group,16,true);boolean any=false;
            for(JSONObject r:meals)if(r.optString("group").equals(group)){entry(body,r);any=true;}
            if(!any)button(body,"添加"+group,()->{JSONObject draft=new JSONObject();put(draft,"group",group);meal(draft);});
        }
        for(JSONObject r:day)if(!r.optString("kind").equals("meal"))entry(body,r);
        button(body,"全部历史记录",()->history());
    }
    static void put(JSONObject o,String key,Object value){try{o.put(key,value);}catch(JSONException e){throw new IllegalArgumentException(e);}}
    void entry(LinearLayout parent,JSONObject r){
        String kind=r.optString("kind");LinearLayout c=card(parent);
        text(c,kind.equals("meal")?r.optString("food"):kind.equals("weight")?"体重 "+r.optString("kg")+" kg":r.optString("sport"),17,true);
        String detail=kind.equals("meal")?r.optString("portion")+" · "+(JournalStore.known(r,"kcal")?n(r.optDouble("kcal"))+" 千卡":"热量待补"):kind.equals("exercise")?r.optString("minutes")+" 分钟 · "+r.optString("burn")+" 千卡":"";
        text(c,detail,14,false);
        horizontalButtons(c,kind.equals("meal")?"再吃一次":"查看 / 修改",()->{if(kind.equals("meal")){JSONObject copy=copy(r);copy.remove("id");put(copy,"date",selectedDate);meal(copy);}else edit(r);},"修改",()->edit(r));
    }
    JSONObject copy(JSONObject object){try{return new JSONObject(object.toString());}catch(Exception e){return new JSONObject();}}
    void edit(JSONObject r){switch(r.optString("kind")){case "exercise":exercise(r);break;case "weight":weight(r);break;default:meal(r);}}
    void history(){
        LinearLayout content=column();AlertDialog dialog=dialog("全部历史",content);
        for(JSONObject r:store.sorted()){
            button(content,r.optString("date")+" · "+r.optString("food",r.optString("sport",r.optString("kg")+" kg")),()->{dialog.dismiss();edit(r);});
        }
        if(store.sorted().isEmpty())text(content,"还没有记录",16,false);
    }
    AlertDialog dialog(String title,LinearLayout content){
        ScrollView scroll=new ScrollView(this);scroll.addView(content);
        AlertDialog d=new AlertDialog.Builder(this).setTitle(title).setView(scroll).setNegativeButton("关闭",null).create();
        d.show();return d;
    }
    void save(JSONObject r,AlertDialog d){try{store.save(r);d.dismiss();selectedDate=r.optString("date",selectedDate);show("今日");toast("已保存");}catch(Exception e){error(e.getMessage());}}
    String dateField(LinearLayout form,JSONObject r,JSONObject draft){
        String value=r.optString("date",selectedDate);put(draft,"date",value);
        Button b=new Button(this);b.setText(value+" · 选择日期");form.addView(b);
        b.setOnClickListener(v->{String[] p=draft.optString("date").split("-");DatePickerDialog picker=new DatePickerDialog(this,(view,y,m,day)->{String s=String.format(Locale.US,"%04d-%02d-%02d",y,m+1,day);put(draft,"date",s);b.setText(s+" · 选择日期");},Integer.parseInt(p[0]),Integer.parseInt(p[1])-1,Integer.parseInt(p[2]));picker.getDatePicker().setMaxDate(System.currentTimeMillis());picker.show();});
        return value;
    }
    double number(EditText field,double min,double max)throws Exception{
        double value;try{value=Double.parseDouble(field.getText().toString());}catch(Exception e){field.setError("请输入数字");throw new Exception("请检查输入");}
        if(!Double.isFinite(value)||value<min||value>max){field.setError("范围 "+n(min)+"–"+n(max));throw new Exception("请检查输入范围");}return value;
    }
    void deleteButton(LinearLayout form,JSONObject record,AlertDialog d){
        if(!record.has("id"))return;
        button(form,"删除记录",()->new AlertDialog.Builder(this).setMessage("删除这条记录？").setNegativeButton("保留",null).setPositiveButton("删除",(v,w)->{try{store.delete(record);d.dismiss();show("今日");}catch(Exception e){error(e.getMessage());}}).show());
    }
    void meal(JSONObject record){
        JSONObject draft=copy(record);put(draft,"kind","meal");LinearLayout form=column();dateField(form,record,draft);
        Spinner group=spinner(form,"餐次",groups,Arrays.asList(groups).indexOf(record.optString("group","午餐")));
        EditText name=input(form,"吃了什么",record.optString("food"),false),portion=input(form,"份量",record.optString("portion"),false);
        if(record.has("source"))text(form,record.optString("source"),12,false);
        EditText kcal=input(form,"能量（千卡，可留空）",record.optString("kcal"),true);
        EditText protein=input(form,"蛋白质（克，可留空）",record.optString("protein"),true),carbs=input(form,"碳水（克，可留空）",record.optString("carbs"),true),fat=input(form,"脂肪（克，可留空）",record.optString("fat"),true);
        AlertDialog d=dialog(record.has("id")?"修改饮食":"核对饮食记录",form);
        button(form,"保存记录",()->{try{
            if(name.getText().toString().trim().isEmpty()){name.setError("请填写菜名");return;}
            put(draft,"food",name.getText().toString().trim());put(draft,"portion",portion.getText().toString());put(draft,"group",group.getSelectedItem());
            EditText[] fields={kcal,protein,carbs,fat};String[] keys={"kcal","protein","carbs","fat"};double[] limits={15000,1000,2000,1000};
            for(int i=0;i<4;i++)put(draft,keys[i],fields[i].getText().toString().trim().isEmpty()?"":number(fields[i],0,limits[i]));
            save(draft,d);
        }catch(Exception e){toast(e.getMessage());}});
        deleteButton(form,record,d);
    }
    void weight(JSONObject record){
        JSONObject draft=copy(record);put(draft,"kind","weight");LinearLayout form=column();dateField(form,record,draft);
        EditText kg=input(form,"体重（kg）",record.optString("kg"),true);AlertDialog d=dialog("记录体重",form);
        button(form,"保存体重",()->{try{put(draft,"kg",number(kg,1,600));save(draft,d);}catch(Exception e){toast(e.getMessage());}});deleteButton(form,record,d);
    }
    void exercise(JSONObject record){
        JSONObject draft=copy(record);put(draft,"kind","exercise");LinearLayout form=column();dateField(form,record,draft);
        Spinner sport=spinner(form,"运动项目",sports,(record.optString("sport").startsWith("轻松骑车")?1:Arrays.asList(sports).indexOf(record.optString("sport"))));
        EditText minutes=input(form,"时长（分钟）",record.optString("minutes"),true);
        String latest="";for(JSONObject r:store.sorted())if(r.optString("kind").equals("weight")){latest=r.optString("kg");break;}
        EditText kg=input(form,"运动时体重（kg）",record.optString("kg",latest),true);
        text(form,"MET 参考成人活动汇编。按 (MET−1) × 3.5 × 体重 ÷ 200 × 分钟计算额外消耗，扣除同时间静息消耗。只记日常维持热量以外的专项运动。",13,false);
        AlertDialog d=dialog("记录运动",form);button(form,"保存运动",()->{try{
            double duration=number(minutes,1,1440),mass=number(kg,1,600);put(draft,"minutes",duration);put(draft,"kg",mass);put(draft,"sport",sport.getSelectedItem());put(draft,"burn",Math.round((mets[sport.getSelectedItemPosition()]-1)*3.5*mass/200*duration));save(draft,d);
        }catch(Exception e){toast(e.getMessage());}});deleteButton(form,record,d);
    }
    void library(LinearLayout parent,String initial,AlertDialog owner){
        text(parent,"8,886 条离线食物 · 中文家常菜优先",15,true);
        EditText query=input(parent,"搜索菜名 / 食材 / 英文",initial,false);
        LinearLayout list=column();parent.addView(list);list.setPadding(0,0,0,0);
        button(parent,"清空搜索",()->query.setText(""));
        Runnable search=()->{
            list.removeAllViews();List<JSONObject> results=foods.search(query.getText().toString());
            text(list,results.isEmpty()?"没有匹配条目，可手动记录。":"显示 "+results.size()+" 条"+(results.size()==80?"，可继续输入缩小范围":""),12,false);
            for(JSONObject item:results){
                Button b=button(list,item.optString("name")+"\n"+n(item.optDouble("kcal"))+" 千卡/100克 · "+(item.optInt("priority")==0?"食谱估值":"USDA"),()->{if(owner!=null)owner.dismiss();portion(item);});b.setGravity(Gravity.START|Gravity.CENTER_VERTICAL);b.setTextSize(14);
            }
            button(list,"手动填写餐食",()->{if(owner!=null)owner.dismiss();meal(new JSONObject());});
        };
        query.addTextChangedListener(new TextWatcher(){public void beforeTextChanged(CharSequence s,int st,int c,int a){}public void onTextChanged(CharSequence s,int st,int before,int count){search.run();}public void afterTextChanged(Editable e){}});
        search.run();
    }
    void searchDialog(String query){
        LinearLayout form=column();AlertDialog d=dialog("选择营养参考",form);library(form,query,d);
    }
    void portion(JSONObject item){
        LinearLayout form=column();text(form,item.optString("name"),23,true);text(form,item.optString("source"),13,false);
        text(form,"每100克："+n(item.optDouble("kcal"))+" 千卡 · 蛋白质 "+n(item.optDouble("protein"))+" g · 碳水 "+n(item.optDouble("carbs"))+" g · 脂肪 "+n(item.optDouble("fat"))+" g",14,false);
        text(form,item.optString("ingredients"),13,false);
        text(form,"参考配方与实际用油、熟重可能不同，保存前可修改营养数值。",13,false);
        EditText grams=input(form,"实际吃了多少克","",true);TextView estimate=text(form,"填写克重后计算",18,true);
        grams.addTextChangedListener(new TextWatcher(){public void beforeTextChanged(CharSequence s,int st,int c,int a){}public void onTextChanged(CharSequence s,int st,int b,int c){try{double g=Double.parseDouble(s.toString());estimate.setText("约 "+n(item.optDouble("kcal")*g/100)+" 千卡");}catch(Exception ignored){estimate.setText("填写克重后计算");}}public void afterTextChanged(Editable e){}});
        button(form,"参考一份 "+n(item.optDouble("portion",100))+" 克",()->grams.setText(n(item.optDouble("portion",100))));
        AlertDialog d=dialog("确认份量",form);
        button(form,"下一步 · 核对记录",()->{try{
            double g=number(grams,1,3000);JSONObject r=new JSONObject();put(r,"food",item.optString("name"));put(r,"portion",n(g)+" 克");put(r,"source",item.optString("source")+" · "+item.optString("id"));
            for(String key:new String[]{"kcal","protein","carbs","fat"})put(r,key,Math.round(item.optDouble(key)*g)/100.0);
            d.dismiss();meal(r);
        }catch(Exception e){toast(e.getMessage());}});
    }
    void plan(){
        JSONObject old=store.plan();text(body,"每日摄入计划",23,true);
        text(body,"面向一般成年人。未成年人、孕哺期及需疾病饮食管理者应个别制定目标。",13,false);
        Spinner type=spinner(body,"计划类型",planNames,Arrays.asList(planNames).indexOf(old.optString("name")));
        EditText base=input(body,"无专项运动日的维持热量（千卡）",old.optString("base"),true);
        EditText target=input(body,"每日摄入目标（千卡）",old.optString("target"),true);
        TextView info=text(body,"",14,false);
        Runnable update=()->{
            int index=type.getSelectedItemPosition();target.setEnabled(index==3);
            int deficit=index==1?250:index==2?500:0;
            info.setText(index==3?"填写自己的每日目标。":"每日目标 = 维持热量 − "+deficit+" 千卡");
            if(index!=3){try{target.setText(n(Double.parseDouble(base.getText().toString())-deficit));}catch(Exception e){target.setText("");}}
        };
        type.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener(){public void onItemSelected(AdapterView<?> p,View v,int pos,long id){update.run();}public void onNothingSelected(AdapterView<?> p){}});
        base.addTextChangedListener(new TextWatcher(){public void beforeTextChanged(CharSequence s,int st,int c,int a){}public void onTextChanged(CharSequence s,int st,int b,int c){update.run();}public void afterTextChanged(Editable e){}});
        text(body,"温和档减少250千卡，标准档减少500千卡。摄入目标支持1500千卡以下的自定义值；请按自身情况设定，维持热量可按既有测算结果填写。运动额外消耗单独记录。",14,false);
        button(body,"保存每日计划",()->{try{
            double intake=number(target,1,6500);JSONObject p=new JSONObject();put(p,"name",type.getSelectedItem());put(p,"target",intake);
            if(type.getSelectedItemPosition()!=3||!base.getText().toString().isEmpty())put(p,"base",number(base,1200,6500));
            if(!store.prefs.edit().putString("plan",p.toString()).commit())throw new Exception("保存失败");
            show("今日");toast("计划已更新");
        }catch(Exception e){toast(e.getMessage());}});
        button(body,"应用更新 · "+updater.currentVersion(),()->updates());
        button(body,"营养数据库与模型来源",()->{try{LinearLayout f=column();text(f,FoodDatabase.read(this,"sources.txt")+"\n\n"+FoodDatabase.read(this,"NOTICE.txt")+"\n\n"+FoodDatabase.read(this,"EveryDay-Food-LICENSE.txt"),13,false);dialog("数据来源",f);}catch(Exception e){error("无法读取来源");}});
    }
    void charts(){
        horizontalButtons(body,"近7天",()->{chartDays=7;show("趋势");},"近30天",()->{chartDays=30;show("趋势");});
        String[] labels={"摄入","运动","体重"},keys={"kcal","burn","kg"};
        LinearLayout types=row(body);
        for(int i=0;i<3;i++){final String key=keys[i];Button b=button(types,labels[i],()->{chartMetric=key;show("趋势");});b.setLayoutParams(new LinearLayout.LayoutParams(0,-2,1));}
        text(body,(chartMetric.equals("kg")?"体重 · kg":chartMetric.equals("burn")?"运动额外消耗 · 千卡":"饮食摄入 · 千卡")+" / 近"+chartDays+"天",20,true);
        List<Double> values=new ArrayList<>();List<String> dates=new ArrayList<>();List<Boolean> partial=new ArrayList<>();
        Calendar date=Calendar.getInstance();date.add(Calendar.DATE,1-chartDays);
        for(int i=0;i<chartDays;i++){
            String day=new SimpleDateFormat("yyyy-MM-dd",Locale.US).format(date.getTime());dates.add(day);date.add(Calendar.DATE,1);
            List<JSONObject> rows=dayRows(day);Double value=null;boolean incomplete=false;
            for(JSONObject r:rows){
                if(chartMetric.equals("kg")&&r.optString("kind").equals("weight")&&value==null)value=r.optDouble("kg");
                else if(!chartMetric.equals("kg")&&JournalStore.known(r,chartMetric))value=(value==null?0:value)+r.optDouble(chartMetric);
                if(chartMetric.equals("kcal")&&r.optString("kind").equals("meal")&&!JournalStore.known(r,"kcal"))incomplete=true;
            }
            values.add(value);partial.add(incomplete);
        }
        body.addView(new TrendView(this,values,dates,chartMetric.equals("kg")),new LinearLayout.LayoutParams(-1,dp(220)));
        text(body,"空白日期没有记录，＊表示当天还有热量待补。体重取当日最后录入的一次。",13,false);
        for(int i=dates.size()-1;i>=0;i--)if(values.get(i)!=null)text(body,dates.get(i)+"    "+n(values.get(i))+(partial.get(i)?"＊":""),16,false);
    }
    void photoMenu(){
        new AlertDialog.Builder(this).setTitle("离线拍照识餐").setItems(new String[]{"拍摄一道菜","从相册选择","云端识别（可选）"},(d,which)->{
            if(which==2){cloudMenu();return;}
            Intent intent;
            if(which==0){
                new File(getCacheDir(),"capture.jpg").delete();
                intent=new Intent(MediaStore.ACTION_IMAGE_CAPTURE);intent.putExtra(MediaStore.EXTRA_OUTPUT,MealPhoto.URI);intent.setClipData(ClipData.newRawUri("meal",MealPhoto.URI));intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION|Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
            }else intent=new Intent(Intent.ACTION_OPEN_DOCUMENT).setType("image/*").addCategory(Intent.CATEGORY_OPENABLE);
            try{startActivityForResult(intent,which==0?41:42);}catch(ActivityNotFoundException e){error("未找到可用应用，请使用相册选图。");}
        }).show();
    }
    @Override protected void onActivityResult(int request,int result,Intent data){
        super.onActivityResult(request,result,data);if(result!=RESULT_OK)return;
        Uri uri=request==41?MealPhoto.URI:data==null?null:data.getData();if(uri==null)return;
        worker.execute(()->{try{
            ImageDecoder.Source source=ImageDecoder.createSource(getContentResolver(),uri);
            Bitmap image=ImageDecoder.decodeBitmap(source,(decoder,info,s)->{android.util.Size size=info.getSize();float scale=Math.min(1,1024f/Math.max(size.getWidth(),size.getHeight()));decoder.setTargetSize(Math.max(1,(int)(size.getWidth()*scale)),Math.max(1,(int)(size.getHeight()*scale)));decoder.setAllocator(ImageDecoder.ALLOCATOR_SOFTWARE);});
            runOnUiThread(()->{photo=image;photoPreview();});
        }catch(Exception e){runOnUiThread(()->error("无法读取图片，请选择其他照片。"));}});
    }
    void photoPreview(){
        LinearLayout form=column();ImageView preview=new ImageView(this);preview.setImageBitmap(photo);preview.setAdjustViewBounds(true);preview.setMaxHeight(dp(240));form.addView(preview,new LinearLayout.LayoutParams(-1,dp(220)));
        text(form,"对准一道菜。候选分数不代表营养准确度，确认参考配方后再计算。",14,false);
        Spinner region=spinner(form,"识别区域",new String[]{"整张","左半边","右半边","上半边","下半边"},0);
        AlertDialog d=dialog("照片预览",form);
        Button start=button(form,"离线识别",()->{if(busy)return;busy=true;toast("正在手机上识别…");
            int area=region.getSelectedItemPosition();Bitmap input=photo;
            if(area>0){boolean horizontal=area<=2;int width=horizontal?Math.max(1,photo.getWidth()/2):photo.getWidth(),height=horizontal?photo.getHeight():Math.max(1,photo.getHeight()/2);input=Bitmap.createBitmap(photo,area==2?photo.getWidth()-width:0,area==4?photo.getHeight()-height:0,width,height);}
            final Bitmap selected=input;
            worker.execute(()->{try{JSONArray results=model.predict(selected);runOnUiThread(()->{busy=false;if(d.isShowing()){d.dismiss();candidates(results);}});}
            catch(Exception e){runOnUiThread(()->{busy=false;error("离线识别失败，请重试或搜索菜名。");});}});
        });
    }
    void candidates(JSONArray results){
        LinearLayout form=column();text(form,results.length()==0?"未识别出明确菜品，可搜索确认。":results.optJSONObject(0).optDouble("score")<0.5?"识别把握较低，请核对或搜索中文菜名。":"选择与照片相符的菜名",16,true);
        AlertDialog d=dialog("识别候选",form);
        for(int i=0;i<results.length();i++){
            JSONObject result=results.optJSONObject(i);String label=result.optString("name"),chinese=foods.chinese(label);
            button(form,chinese+" · "+n(result.optDouble("score")*100)+"% 模型分数"+(foods.search(chinese).isEmpty()?" · 暂无对应营养条目":""),()->{d.dismiss();searchDialog(chinese);});
        }
        button(form,"都不对 · 搜索菜名",()->{d.dismiss();searchDialog("");});
    }
    void cloudMenu(){
        if(photo==null){error("请先从相册或相机选择照片，再使用云端识别。");return;}
        LinearLayout form=column();Spinner provider=spinner(form,"识别服务",new String[]{"阿里云百炼","Google Gemini"},0);
        EditText key=input(form,"API Key（仅用于本次请求）","",false);key.setInputType(129);
        text(form,"点击发送后，照片会传给所选服务。百炼新人额度有期限，Gemini受地区和额度限制，请检查服务商计费设置。",14,false);
        AlertDialog d=dialog("云端识别",form);
        Button send=button(form,"发送当前照片",()->{
            if(key.getText().toString().trim().isEmpty()){key.setError("请输入密钥");return;}
            String api=key.getText().toString().trim();int service=provider.getSelectedItemPosition();key.setText("");d.dismiss();toast("正在识别…");
            Bitmap current=photo;worker.execute(()->{try{JSONObject r=CloudVision.recognize(current,service,api);runOnUiThread(()->meal(r));}catch(Exception e){runOnUiThread(()->error("云端识别失败，请检查网络、密钥或额度。"));}});
        });
    }

    void updates(){
        LinearLayout form=column();
        text(form,"当前版本 "+updater.currentVersion(),18,true);
        Switch auto=new Switch(this);auto.setText("每天自动检查更新");auto.setChecked(updater.automatic());form.addView(auto);
        auto.setOnCheckedChangeListener((button,enabled)->updater.automatic(enabled));
        TextView state=text(form,updater.message,16,true),notes=text(form,"",14,false);
        ProgressBar progress=new ProgressBar(this);form.addView(progress);
        Button check=button(form,"立即检查",()->updater.check(true));
        Button download=button(form,"下载新版",()->updater.download());
        Button install=button(form,"安装新版",()->updater.install(this));
        Button cancel=button(form,"取消下载",()->updater.cancel());
        text(form,"通过 GitHub Releases 检查和下载。验证文件摘要、包名、版本及签名后，由 Android 确认覆盖安装。",13,false);
        AlertDialog dialog=dialog("应用更新",form);
        updatePanelRefresh=()->{
            state.setText(updater.message);
            notes.setText(updater.release==null?"":updater.release.notes);
            progress.setVisibility(updater.busy?View.VISIBLE:View.GONE);
            check.setEnabled(!updater.busy&&updater.ready==null);
            download.setVisibility(updater.release!=null&&updater.ready==null?View.VISIBLE:View.GONE);download.setEnabled(!updater.busy);
            install.setVisibility(updater.ready!=null?View.VISIBLE:View.GONE);install.setEnabled(!updater.busy);
            cancel.setVisibility(updater.busy&&updater.release!=null?View.VISIBLE:View.GONE);
        };
        dialog.setOnDismissListener(d->updatePanelRefresh=null);updatePanelRefresh.run();
    }
    @Override protected void onResume(){
        super.onResume();
        if(updater!=null){
            updater.observe(()->{if(updateStatus!=null)updateStatus.setText(updater.message);if(updatePanelRefresh!=null)updatePanelRefresh.run();});
            updater.check(false);
        }
    }
    @Override protected void onPause(){if(updater!=null)updater.observe(null);super.onPause();}
    @Override protected void onSaveInstanceState(Bundle out){super.onSaveInstanceState(out);out.putString("selectedDate",selectedDate);out.putString("screen",screen);}
    @Override protected void onDestroy(){
        if(migration!=null)migration.destroy();
        worker.execute(()->{if(foods!=null)foods.close();model.close();});worker.shutdown();
        super.onDestroy();
    }
}
