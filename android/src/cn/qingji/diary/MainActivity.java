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
    int ink=Color.rgb(63,44,34),accent=Color.rgb(178,57,36),paper=Color.rgb(250,244,234);
    final ExecutorService worker=Executors.newSingleThreadExecutor();
    JournalStore store;
    JournalStyle style;
    String selectedPhoto="";
    final android.util.LruCache<String,Bitmap> photoCache=new android.util.LruCache<String,Bitmap>(8*1024*1024){protected int sizeOf(String k,Bitmap b){return b.getByteCount();}};
    ChallengeScreen challenges;
    TrendScreen trendScreen;
    PlanScreen plans;
    AppUpdater updater;
    Runnable updatePanelRefresh;
    FoodDatabase foods;
    OfflineFood model;
    LinearLayout root,body,tabs;
    ScrollView activeScroll;
    final Map<String,Integer> scrollPositions=new HashMap<>();
    WebView migration;
    String selectedDate=JournalStore.today(),screen="今日";
    Bitmap photo;
    String photoRegion="整张";
    boolean busy=false;
    int chartDays=7;
    String chartMetric="kcal";
    final String[] groups={"早餐","午餐","晚餐","加餐"};
    final String[] planNames={"维持与观察","温和减量","标准减量","自定义目标"};

    @Override public void onCreate(Bundle saved){
        super.onCreate(saved);
        if(saved!=null){selectedDate=saved.getString("selectedDate",selectedDate);screen=saved.getString("screen",screen);}
        getWindow().setStatusBarColor(paper);getWindow().setNavigationBarColor(paper);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR|View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
        style=new JournalStyle(this);store=new JournalStore(this);model=new OfflineFood(this);
        challenges=new ChallengeScreen(this);trendScreen=new TrendScreen(this);plans=new PlanScreen(this);updater=AppUpdater.get(this);
        root=column();root.setPadding(0,0,0,0);root.setBackground(style.paper());setContentView(root);
        TextView opening=style.heading("靓女修炼笔记",28,ink);root.addView(opening);text(root,"正在加载…",16,false);
        worker.execute(()->{try{foods=new FoodDatabase(this);PhotoFiles.prune(this,store.rows());runOnUiThread(()->{if(store.migrated())show(screen);else migrate();});}
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
        TextView t=style.label(label,size,bold,ink);t.setPadding(0,dp(5),0,dp(5));
        parent.addView(t);return t;
    }
    Button button(LinearLayout parent,String label,Runnable action){
        Button b=new Button(this);b.setText(label);b.setAllCaps(false);b.setTextColor(accent);b.setMinHeight(dp(48));b.setTextSize(15);b.setPadding(dp(12),dp(8),dp(12),dp(8));b.setStateListAnimator(null);
        b.setTypeface(style.medium);b.setLetterSpacing(0);b.setIncludeFontPadding(false);b.setBackground(style.ripple(0x55ffffff,10,true));
        LinearLayout.LayoutParams layout=new LinearLayout.LayoutParams(-1,-2);layout.setMargins(dp(3),dp(4),dp(3),dp(4));
        parent.addView(b,layout);b.setOnClickListener(v->{v.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK);action.run();});return b;
    }
    void horizontalButtons(LinearLayout parent,String a,Runnable aa,String b,Runnable bb){
        LinearLayout line=row(parent);Button x=button(line,a,aa),y=button(line,b,bb);
        LinearLayout.LayoutParams left=new LinearLayout.LayoutParams(0,-2,1),right=new LinearLayout.LayoutParams(0,-2,1);left.setMargins(dp(3),dp(4),dp(3),dp(4));right.setMargins(dp(3),dp(4),dp(3),dp(4));x.setLayoutParams(left);y.setLayoutParams(right);
    }
    LinearLayout card(LinearLayout parent){
        LinearLayout v=column();GradientDrawable d=new GradientDrawable();d.setColor(0x55ffffff);d.setStroke(1,style.line);d.setCornerRadius(dp(12));v.setBackground(d);
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
        boolean pageChanged=!screen.equals(which);
        if(activeScroll!=null)scrollPositions.put(screen,activeScroll.getScrollY());
        if(activeScroll!=null)activeScroll.animate().cancel();
        screen=which;root.removeAllViews();
        if(!which.equals("今日")){
            LinearLayout header=column();root.addView(header);
            LinearLayout titleRow=row(header);
            TextView title=style.heading(which.equals("百次")?"百次修炼":which,26,ink);
            titleRow.addView(title,new LinearLayout.LayoutParams(0,-2,1));
            updateEntry(titleRow);
        }
        ScrollView scroll=new ScrollView(this);activeScroll=scroll;scroll.setSmoothScrollingEnabled(true);scroll.setFillViewport(true);root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));body=column();body.setPadding(dp(14),which.equals("今日")?0:dp(4),dp(14),dp(12));scroll.setClipToPadding(false);scroll.setVerticalScrollBarEnabled(false);scroll.addView(body);
        tabs=new LinearLayout(this);tabs.setPadding(dp(8),dp(4),dp(8),dp(3));tabs.setBackgroundColor(paper);root.addView(tabs);
        String[] tabNames={"今日","食物库","百次","趋势","计划"},icons={"book_open_page_variant_outline","bowl_mix_outline","sprout_outline","chart_line","bullseye_arrow"};
        for(int i=0;i<tabNames.length;i++){
            final String title=tabNames[i];boolean active=title.equals(which);
            LinearLayout tab=new LinearLayout(this);tab.setOrientation(LinearLayout.VERTICAL);tab.setGravity(Gravity.CENTER);tabs.addView(tab,new LinearLayout.LayoutParams(0,dp(54),1));
            tab.setBackground(style.ripple(active?(0x12000000|(accent&0xffffff)):Color.TRANSPARENT,10,false));
            tab.addView(style.image(icons[i],active?accent:style.muted,22),new LinearLayout.LayoutParams(dp(22),dp(22)));
            TextView caption=style.label(title,10,active,active?accent:style.muted);caption.setGravity(Gravity.CENTER);caption.setPadding(0,dp(4),0,0);tab.addView(caption);
            tab.setContentDescription(title);tab.setSelected(active);tab.setOnClickListener(v->{if(!screen.equals(title)){v.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK);show(title);}});
        }
        if(which.equals("今日"))home();else if(which.equals("食物库"))library(body,"",null);else if(which.equals("百次"))challenges.render();else if(which.equals("趋势"))charts();else plan();
        int savedY=scrollPositions.containsKey(which)?scrollPositions.get(which):0;
        scroll.post(()->scroll.scrollTo(0,savedY));
        if(pageChanged&&android.animation.ValueAnimator.areAnimatorsEnabled()){
            scroll.setAlpha(1);scroll.setTranslationY(dp(4));
            scroll.animate().translationY(0).withLayer().setDuration(180).setInterpolator(new android.view.animation.DecelerateInterpolator()).start();
        }
    }
    List<JSONObject> dayRows(String day){List<JSONObject> out=new ArrayList<>();for(JSONObject r:store.sorted())if(r.optString("date").equals(day))out.add(r);return out;}
    void home(){new HomeScreen(this).render();}
    static void put(JSONObject o,String key,Object value){try{o.put(key,value);}catch(JSONException e){throw new IllegalArgumentException(e);}}
    void entry(LinearLayout parent,JSONObject r){
        String kind=r.optString("kind");LinearLayout c=card(parent);
        text(c,kind.equals("meal")?r.optString("food"):kind.equals("weight")?"体重 "+r.optString("kg")+" kg":r.optString("sport"),17,true);
        String detail=kind.equals("meal")?r.optString("portion")+" · "+(JournalStore.known(r,"kcal")?n(r.optDouble("kcal"))+" 千卡":"热量待补"):kind.equals("exercise")?r.optString("minutes")+" 分钟 · "+r.optString("burn")+" 千卡":"";
        if(!detail.isEmpty())text(c,detail,14,false);
        if(!kind.equals("meal")){button(c,"修改记录",()->edit(r));return;}
        horizontalButtons(c,kind.equals("meal")?"再吃一次":"查看 / 修改",()->{if(kind.equals("meal")){JSONObject copy=copy(r);copy.remove("id");put(copy,"date",selectedDate);meal(copy);}else edit(r);},"修改",()->edit(r));
    }
    JSONObject copy(JSONObject object){try{return new JSONObject(object.toString());}catch(Exception e){return new JSONObject();}}
    void edit(JSONObject r){switch(r.optString("kind")){case "exercise":exercise(r);break;case "weight":weight(r);break;default:meal(r);}}
    void history(){new JournalCalendar(this).show();}
    void openDay(String day){
        try{java.time.LocalDate date=java.time.LocalDate.parse(day);if(date.isAfter(java.time.LocalDate.now()))return;
            selectedDate=day;if(activeScroll!=null)activeScroll.scrollTo(0,0);scrollPositions.put("今日",0);show("今日");
        }catch(RuntimeException e){toast("日期无效");}
    }
    AlertDialog dialog(String title,LinearLayout content){
        ScrollView scroll=new ScrollView(this);scroll.addView(content);
        AlertDialog d=new AlertDialog.Builder(this).setTitle(title).setView(scroll).setNegativeButton("关闭",null).create();
        d.show();d.getWindow().setBackgroundDrawable(style.surface(paper,18,true));d.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(accent);return d;
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
        if(!record.optString("photo").isEmpty()){ImageView image=new ImageView(this);image.setScaleType(ImageView.ScaleType.CENTER_CROP);form.addView(image,new LinearLayout.LayoutParams(-1,dp(130)));loadMealPhoto(image,record.optString("photo"));}
        Spinner group=spinner(form,"餐次",groups,Arrays.asList(groups).indexOf(record.optString("group","午餐")));
        EditText name=input(form,"吃了什么",record.optString("food"),false),portion=input(form,"份量",record.optString("portion"),false);
        LinearLayout suggestions=column();form.addView(suggestions);
        EditText grams=input(form,"食物库份量（克）","100",true);
        TextView reference=text(form,record.optString("source"),12,false);
        EditText kcal=input(form,"能量（千卡，可留空）",record.optString("kcal"),true);
        EditText protein=input(form,"蛋白质（克，可留空）",record.optString("protein"),true),carbs=input(form,"碳水（克，可留空）",record.optString("carbs"),true),fat=input(form,"脂肪（克，可留空）",record.optString("fat"),true);
        final JSONObject[] selected={null};final boolean[] filling={false};
        Runnable calculate=()->{if(selected[0]==null)return;try{
            double g=Double.parseDouble(grams.getText().toString());if(!Double.isFinite(g)||g<=0||g>3000)throw new Exception();
            EditText[] fields={kcal,protein,carbs,fat};String[] keys={"kcal","protein","carbs","fat"};
            for(int i=0;i<4;i++)fields[i].setText(n(Math.round(selected[0].optDouble(keys[i])*g)/100.0));
            portion.setText(n(g)+" 克");grams.setError(null);
        }catch(Exception e){grams.setError("请输入 1–3000 克");for(EditText f:new EditText[]{kcal,protein,carbs,fat})f.setText("");}};
        Handler lookupHandler=new Handler(Looper.getMainLooper());
        Runnable lookup=()->{suggestions.removeAllViews();String q=name.getText().toString().trim();if(q.isEmpty())return;
            int count=0;for(JSONObject item:foods.search(q)){if(count++==5)break;
                Button b=button(suggestions,item.optString("name")+" · "+n(item.optDouble("kcal"))+" 千卡/100克",()->{
                    selected[0]=item;filling[0]=true;name.setText(item.optString("name"));filling[0]=false;
                    suggestions.removeAllViews();put(draft,"source",item.optString("source")+" · "+item.optString("id"));reference.setText(draft.optString("source"));calculate.run();
                });b.setTextSize(13);
            }
        };
        name.addTextChangedListener(new TextWatcher(){public void beforeTextChanged(CharSequence s,int st,int c,int a){}public void afterTextChanged(Editable e){}public void onTextChanged(CharSequence s,int st,int b,int c){if(filling[0])return;selected[0]=null;draft.remove("source");reference.setText("");lookupHandler.removeCallbacks(lookup);lookupHandler.postDelayed(lookup,180);}});
        grams.addTextChangedListener(new TextWatcher(){public void beforeTextChanged(CharSequence s,int st,int c,int a){}public void afterTextChanged(Editable e){}public void onTextChanged(CharSequence s,int st,int b,int c){calculate.run();}});
        AlertDialog d=dialog(record.has("id")?"修改饮食":"核对饮食记录",form);
        d.setOnDismissListener(v->lookupHandler.removeCallbacks(lookup));
        button(form,"保存记录",()->{try{
            if(selected[0]!=null)number(grams,1,3000);
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
        int initial=record.has("sport")?ExerciseCatalog.index(record.optString("sport")):0;
        Spinner sport=spinner(form,"运动项目",ExerciseCatalog.NAMES,initial);
        EditText custom=input(form,"自定义名称",initial==ExerciseCatalog.NAMES.length-1?record.optString("sport",""):"",false);
        custom.setVisibility(initial==ExerciseCatalog.NAMES.length-1?View.VISIBLE:View.GONE);
        EditText minutes=input(form,"时长（分钟）",record.optString("minutes"),true);
        String latest="";for(JSONObject r:store.sorted())if(r.optString("kind").equals("weight")){latest=r.optString("kg");break;}
        EditText kg=input(form,"运动时体重（kg）",record.optString("kg",latest),true);
        EditText distance=input(form,"距离（公里，选填）",record.optString("distanceKm"),true);
        distance.setEnabled(ExerciseCatalog.distanceSport(initial));
        TextView reference=text(form,"",13,false),preview=text(form,"填写时长与体重后估算",17,true);
        Switch manual=new Switch(this);manual.setText("手动填写额外消耗");manual.setChecked(record.has("id"));form.addView(manual);
        EditText burn=input(form,"额外消耗（千卡）",record.optString("burn"),true);burn.setEnabled(true);
        final boolean[] updatingBurn={false};
        Runnable calculate=()->{try{
            long value=ExerciseCatalog.estimate(ExerciseCatalog.intensity(sport.getSelectedItemPosition(),distance.getText().toString().trim().isEmpty()?0:Double.parseDouble(distance.getText().toString()),Double.parseDouble(minutes.getText().toString())),Double.parseDouble(kg.getText().toString()),Double.parseDouble(minutes.getText().toString()));
            preview.setText("参考额外消耗 · "+value+" 千卡");if(!manual.isChecked()){updatingBurn[0]=true;burn.setText(String.valueOf(value));updatingBurn[0]=false;}
        }catch(Exception e){preview.setText("填写有效的体重、时长和距离后估算");if(!manual.isChecked()){updatingBurn[0]=true;burn.setText("");updatingBurn[0]=false;}}};
        TextWatcher watcher=new TextWatcher(){public void beforeTextChanged(CharSequence s,int st,int c,int a){}public void afterTextChanged(Editable e){}public void onTextChanged(CharSequence s,int st,int b,int c){calculate.run();}};
        TextWatcher parameterWatcher=new TextWatcher(){public void beforeTextChanged(CharSequence s,int st,int c,int a){}public void afterTextChanged(Editable e){}public void onTextChanged(CharSequence s,int st,int b,int c){manual.setChecked(false);calculate.run();}};
        minutes.addTextChangedListener(parameterWatcher);kg.addTextChangedListener(parameterWatcher);distance.addTextChangedListener(parameterWatcher);
        manual.setOnCheckedChangeListener((v,on)->calculate.run());
        burn.addTextChangedListener(new TextWatcher(){public void beforeTextChanged(CharSequence s,int st,int c,int a){}public void afterTextChanged(Editable e){}public void onTextChanged(CharSequence s,int st,int b,int c){if(!updatingBurn[0]&&!manual.isChecked())manual.setChecked(true);}});
        button(form,"使用参考估算",()->{manual.setChecked(false);calculate.run();});
        reference.setText("距离选填；填写后按平均速度估算，未填按项目估算。");
        sport.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener(){int previous=initial;
            public void onNothingSelected(AdapterView<?> p){}
            public void onItemSelected(AdapterView<?> p,View v,int pos,long id){custom.setVisibility(pos==ExerciseCatalog.NAMES.length-1?View.VISIBLE:View.GONE);distance.setEnabled(ExerciseCatalog.distanceSport(pos));if(pos!=previous){previous=pos;manual.setChecked(false);}calculate.run();}
        });
        text(form,"额外消耗已扣除静息消耗。平路速度估值不含坡度、风阻；可按设备记录修改。",12,false);
        button(form,"参考值来源",()->{try{startActivity(new Intent(Intent.ACTION_VIEW,android.net.Uri.parse("https://pacompendium.com/adult-compendium/")));}catch(Exception e){toast("无法打开浏览器");}});
        AlertDialog d=dialog("记录运动",form);calculate.run();d.setButton(AlertDialog.BUTTON_POSITIVE,"保存运动",(android.content.DialogInterface.OnClickListener)null);d.getButton(AlertDialog.BUTTON_POSITIVE).setVisibility(View.VISIBLE);d.getButton(AlertDialog.BUTTON_POSITIVE).setText("保存运动");d.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v->{try{
            double duration=number(minutes,1,1440),mass=number(kg,1,600),distanceKm=distance.isEnabled()&&!distance.getText().toString().trim().isEmpty()?number(distance,0,2000):0,intensity=ExerciseCatalog.intensity(sport.getSelectedItemPosition(),distanceKm,duration);
            String name=sport.getSelectedItemPosition()==ExerciseCatalog.NAMES.length-1?custom.getText().toString().trim():sport.getSelectedItem().toString();
            if(name.isEmpty()){custom.setError("请填写运动名称");return;}
            double energy=manual.isChecked()?number(burn,0,50000):ExerciseCatalog.estimate(intensity,mass,duration);
            put(draft,"distanceKm",distanceKm);put(draft,"minutes",duration);put(draft,"kg",mass);put(draft,"sport",name);put(draft,"met",intensity);put(draft,"burnMode",manual.isChecked()?"manual":"estimate");put(draft,"burn",energy);save(draft,d);
        }catch(Exception e){toast(e.getMessage());}});deleteButton(form,record,d);
    }
    void library(LinearLayout parent,String initial,AlertDialog owner){library(parent,initial,owner,"");}
    void library(LinearLayout parent,String initial,AlertDialog owner,String photoPath){
        text(parent,"8,886 条离线食物 · 中文家常菜优先",15,true);
        EditText query=input(parent,"搜索菜名 / 食材 / 英文",initial,false);
        LinearLayout list=column();parent.addView(list);list.setPadding(0,0,0,0);
        button(parent,"清空搜索",()->query.setText(""));
        Runnable search=()->{
            list.removeAllViews();List<JSONObject> results=foods.search(query.getText().toString());
            text(list,results.isEmpty()?"没有匹配条目，可手动记录。":"显示 "+Math.min(30,results.size())+" 条"+(results.size()>30?"，可继续输入缩小范围":""),12,false);
            int visible=0;for(JSONObject item:results){if(visible++>=30){text(list,"继续输入菜名可缩小结果范围",12,false);break;}
                Button b=button(list,item.optString("name")+"\n"+n(item.optDouble("kcal"))+" 千卡/100克 · "+(item.optString("id").startsWith("fdc:")?"USDA":"食谱估值"),()->{if(owner!=null)owner.dismiss();portion(item,photoPath);});b.setGravity(Gravity.START|Gravity.CENTER_VERTICAL);b.setTextSize(14);
            }
            button(list,"手动填写餐食",()->{if(owner!=null)owner.dismiss();JSONObject r=new JSONObject();if(!photoPath.isEmpty())put(r,"photo",photoPath);meal(r);});
        };
        Handler searchHandler=new Handler(Looper.getMainLooper());
        query.addTextChangedListener(new TextWatcher(){public void beforeTextChanged(CharSequence s,int st,int c,int a){}public void onTextChanged(CharSequence s,int st,int before,int count){searchHandler.removeCallbacks(search);searchHandler.postDelayed(search,150);}public void afterTextChanged(Editable e){}});
        search.run();
    }
    void searchDialog(String query){
        LinearLayout form=column();AlertDialog d=dialog("选择营养参考",form);library(form,query,d,selectedPhoto);
    }
    void portion(JSONObject item,String photoPath){
        LinearLayout form=column();text(form,item.optString("name"),23,true);text(form,item.optString("source"),13,false);
        text(form,"每100克："+n(item.optDouble("kcal"))+" 千卡 · 蛋白质 "+n(item.optDouble("protein"))+" g · 碳水 "+n(item.optDouble("carbs"))+" g · 脂肪 "+n(item.optDouble("fat"))+" g",14,false);
        text(form,item.optString("ingredients"),13,false);
        text(form,"参考配方与实际用油、熟重可能不同，保存前可修改营养数值。",13,false);
        EditText grams=input(form,"实际吃了多少克","",true);TextView estimate=text(form,"填写克重后计算",18,true);
        grams.addTextChangedListener(new TextWatcher(){public void beforeTextChanged(CharSequence s,int st,int c,int a){}public void onTextChanged(CharSequence s,int st,int b,int c){try{double g=Double.parseDouble(s.toString());estimate.setText("约 "+n(item.optDouble("kcal")*g/100)+" 千卡");}catch(Exception ignored){estimate.setText("填写克重后计算");}}public void afterTextChanged(Editable e){}});
        button(form,"参考一份 "+n(item.optDouble("portion",100))+" 克",()->grams.setText(n(item.optDouble("portion",100))));
        AlertDialog d=dialog("确认份量",form);
        button(form,"下一步 · 核对记录",()->{try{
            double g=number(grams,1,3000);JSONObject r=new JSONObject();if(!photoPath.isEmpty())put(r,"photo",photoPath);put(r,"food",item.optString("name"));put(r,"portion",n(g)+" 克");put(r,"source",item.optString("source")+" · "+item.optString("id"));
            for(String key:new String[]{"kcal","protein","carbs","fat"})put(r,key,Math.round(item.optDouble(key)*g)/100.0);
            d.dismiss();meal(r);
        }catch(Exception e){toast(e.getMessage());}});
    }
    void plan(){
        plans.render();
        button(body,"主题与语录",this::appearance);
        button(body,"应用更新 · "+updater.currentVersion(),this::checkUpdates);
        button(body,"数据来源",()->{try{LinearLayout f=column();text(f,FoodDatabase.read(this,"sources.txt")+"\n\n"+FoodDatabase.read(this,"NOTICE.txt")+"\n\n"+FoodDatabase.read(this,"EveryDay-Food-LICENSE.txt")+"\n\n"+FoodDatabase.read(this,"ui/SOURCES.txt"),13,false);dialog("数据来源",f);}catch(Exception e){error("无法读取来源");}});
    }
    void charts(){trendScreen.render();}
    static final String DEFAULT_QUOTE="慢慢来，每一次都算数。";
    android.content.SharedPreferences preferences(){return getSharedPreferences("ui-preferences",0);}
    String quote(){return preferences().getString("quote",DEFAULT_QUOTE);}
    void editQuote(){
        LinearLayout form=column();EditText value=input(form,"我的激励语录",quote(),false);
        value.setSingleLine(false);value.setMinLines(2);value.setMaxLines(4);value.setFilters(new InputFilter[]{new InputFilter.LengthFilter(80)});
        AlertDialog d=dialog("写给自己",form);
        button(form,"保存语录",()->{String text=value.getText().toString().trim();if(text.isEmpty()){value.setError("写一句喜欢的话吧");return;}
            if(!preferences().edit().putString("quote",text).commit()){toast("保存失败，请重试");return;}d.dismiss();show(screen);});
        button(form,"恢复默认",()->{if(preferences().edit().remove("quote").commit()){d.dismiss();show(screen);}});
    }
    void appearance(){
        LinearLayout form=column();String pref=preferences().getString("theme","auto");
        String theme=SeasonTheme.resolve(pref,java.time.LocalDate.now()).title;
        AlertDialog d=dialog("主题与语录",form);
        button(form,(pref.equals("auto")?"自动 · ":"")+theme,()->{d.dismiss();chooseTheme();});
        button(form,"编辑激励语录",()->{d.dismiss();editQuote();});
    }
    void chooseTheme(){
        String[] values={"auto","spring","summer","autumn","winter"};String[] labels={"自动随四季","春日田野","夏日湖泊","秋日柿子","冬日雪花"};
        String pref=preferences().getString("theme","auto");int current=Math.max(0,Arrays.asList(values).indexOf(pref));
        new AlertDialog.Builder(this).setTitle("四季主题").setSingleChoiceItems(labels,current,(d,which)->{
            if(preferences().edit().putString("theme",values[which]).commit()){style.refreshSeason();d.dismiss();show(screen);}else toast("保存失败，请重试");
        }).setNegativeButton("取消",null).show();
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
            String savedPhoto=PhotoFiles.save(this,image);
            runOnUiThread(()->{photo=image;selectedPhoto=savedPhoto;photoPreview();});
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
            Bitmap current=photo;String currentPhoto=selectedPhoto;worker.execute(()->{try{JSONObject r=CloudVision.recognize(current,service,api);put(r,"photo",currentPhoto);runOnUiThread(()->meal(r));}catch(Exception e){runOnUiThread(()->error("云端识别失败，请检查网络、密钥或额度。"));}});
        });
    }

    void loadMealPhoto(ImageView view,String name){
        if(worker.isShutdown())return;
        view.setTag(name);Bitmap cached=photoCache.get(name);if(cached!=null){view.setImageBitmap(cached);return;}
        worker.execute(()->{Bitmap image=PhotoFiles.load(this,name);if(image!=null){photoCache.put(name,image);runOnUiThread(()->{if(!isDestroyed()&&name.equals(view.getTag()))view.setImageBitmap(image);});}});
    }
    /** Explicit manual entry: open status immediately, then request a fresh check. */
    void checkUpdates(){updates();updater.check(true);}
    Button updateEntry(LinearLayout parent){
        Button check=button(parent,"检查更新",this::checkUpdates);
        check.setTextSize(13);check.setSingleLine(true);
        check.setMinWidth(0);check.setMinimumWidth(0);
        check.setPadding(dp(10),dp(8),dp(10),dp(8));
        check.setBackground(style.ripple(0x55ffffff,9,true));
        LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-2,-2);
        p.leftMargin=dp(10);check.setLayoutParams(p);
        return check;
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
        text(form,"更新保留本地记录，安装需系统确认。",13,false);
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
        if(style!=null&&style.refreshSeason()&&foods!=null&&migration==null)show(screen);
        if(updater!=null){
            updater.observe(()->{if(updatePanelRefresh!=null)updatePanelRefresh.run();});
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
