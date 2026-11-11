package cn.qingji.diary;

import android.app.*;
import android.content.res.ColorStateList;
import android.graphics.*;
import android.view.*;
import android.widget.*;
import org.json.*;
import java.util.*;
import java.time.*;
import java.time.format.DateTimeFormatter;

/** Content hierarchy follows the paper journal: date, quick actions, summaries, energy, meal timeline. */
final class HomeScreen {
    final MainActivity a;final JournalStyle s;final LinearLayout body;
    HomeScreen(MainActivity a){this.a=a;s=a.style;body=a.body;}
    void render(){
        hero();if(!a.selectedDate.equals(JournalStore.today()))a.button(body,"回到今天",()->a.openDay(JournalStore.today()));s.space(body,12);
        LinearLayout actions=a.row(body);
        Button food=s.action(actions,"记饮食","bowl_mix_outline",this::addMeal);
        Button weight=s.action(actions,"记体重","scale_bathroom",()->a.weight(new JSONObject()));
        LinearLayout.LayoutParams left=new LinearLayout.LayoutParams(0,a.dp(62),1),right=new LinearLayout.LayoutParams(0,a.dp(62),1);left.setMargins(0,0,a.dp(5),0);right.setMargins(a.dp(5),0,0,0);food.setLayoutParams(left);weight.setLayoutParams(right);
        s.space(body,14);
        List<JSONObject> day=a.dayRows(a.selectedDate),meals=new ArrayList<>();Set<String> groups=new HashSet<>();
        for(JSONObject r:day)if(r.optString("kind").equals("meal")){meals.add(r);groups.add(r.optString("group"));}
        summaries(groups.size());s.space(body,6);energy(day,meals);s.space(body,10);
        LinearLayout heading=a.row(body);TextView title=s.heading("饮食日记",22,a.ink);heading.addView(title);
        View rule=new View(a);rule.setBackgroundColor(0xffd9b39a);LinearLayout.LayoutParams rp=new LinearLayout.LayoutParams(0,a.dp(1),1);rp.setMargins(a.dp(12),0,a.dp(8),0);heading.addView(rule,rp);
        Button all=a.button(heading,"日历回顾",a::history);all.setBackground(s.ripple(Color.TRANSPARENT,8,false));all.setTextSize(13);all.setPadding(0,0,0,0);all.setLayoutParams(new LinearLayout.LayoutParams(a.dp(76*Math.max(1,a.getResources().getConfiguration().fontScale)),a.dp(44)));all.setCompoundDrawables(null,null,s.icon("chevron_right",a.accent,16),null);
        for(int i=0;i<a.groups.length;i++)timeline(a.groups[i],i,meals);
        boolean shownWeight=false;int weightCount=0;
        for(JSONObject r:day)if(r.optString("kind").equals("weight"))weightCount++;
        for(JSONObject r:day){
            if(r.optString("kind").equals("exercise"))a.entry(body,r);
            else if(r.optString("kind").equals("weight")&&!shownWeight){a.entry(body,r);shownWeight=true;}
        }
        if(weightCount>1)a.button(body,"查看当日全部体重（"+weightCount+"条）",()->{
            LinearLayout list=a.column();for(JSONObject r:day)if(r.optString("kind").equals("weight"))a.entry(list,r);
            a.dialog("当日体重记录",list);
        });
    }
    void hero(){
        float fontScale=Math.max(1,a.getResources().getConfiguration().fontScale);
        FrameLayout hero=new FrameLayout(a);body.addView(hero,new LinearLayout.LayoutParams(-1,a.dp(130*fontScale)));
        ImageView branch=new ImageView(a);branch.setImageBitmap(s.branch);branch.setScaleType(ImageView.ScaleType.FIT_END);branch.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        FrameLayout.LayoutParams bp=new FrameLayout.LayoutParams(a.dp(146),a.dp(116),Gravity.TOP|Gravity.RIGHT);bp.rightMargin=-a.dp(14);bp.topMargin=-a.dp(2);hero.addView(branch,bp);
        TextView brand=s.heading("靓女",32,0xff802917);FrameLayout.LayoutParams p=new FrameLayout.LayoutParams(-2,-2);p.topMargin=a.dp(9);hero.addView(brand,p);
        TextView subtitle=s.label("修炼笔记",12,false,0xff92583f);subtitle.setLetterSpacing(.18f);p=new FrameLayout.LayoutParams(-2,-2);p.topMargin=a.dp(57*fontScale);p.leftMargin=a.dp(2);hero.addView(subtitle,p);
        LinearLayout date=new LinearLayout(a);date.setGravity(Gravity.BOTTOM);p=new FrameLayout.LayoutParams(-2,a.dp(48*fontScale));p.topMargin=a.dp(82*fontScale);hero.addView(date,p);
        LocalDate day=LocalDate.parse(a.selectedDate);
        TextView value=s.heading(day.format(DateTimeFormatter.ofPattern("M月d日")),30,a.ink);date.addView(value);
        TextView weekday=s.label(day.format(DateTimeFormatter.ofPattern("EEE",Locale.CHINESE)),16,true,a.ink);LinearLayout.LayoutParams weekdayLayout=new LinearLayout.LayoutParams(-2,-2);weekdayLayout.leftMargin=a.dp(8);date.addView(weekday,weekdayLayout);
        date.setContentDescription("选择日期 "+a.selectedDate);date.setBackground(s.ripple(Color.TRANSPARENT,8,false));date.setOnClickListener(v->a.history());
        LinearLayout utility=a.row(body);
        TextView tagline=s.label(a.quote(),13,false,s.muted);
        tagline.setMaxLines(3);tagline.setEllipsize(android.text.TextUtils.TruncateAt.END);tagline.setMinHeight(a.dp(48));tagline.setGravity(Gravity.CENTER_VERTICAL);
        tagline.setContentDescription("编辑激励语录");tagline.setBackground(s.ripple(Color.TRANSPARENT,8,false));tagline.setCompoundDrawables(null,null,s.icon("pencil_outline",s.muted,14),null);tagline.setCompoundDrawablePadding(a.dp(6));tagline.setOnClickListener(v->a.editQuote());
        tagline.setBreakStrategy(android.text.Layout.BREAK_STRATEGY_BALANCED);
        utility.addView(tagline,new LinearLayout.LayoutParams(0,-2,1));
        a.updateEntry(utility);
    }
    void addMeal(){new AlertDialog.Builder(a).setTitle("记录这一餐").setItems(new String[]{"拍照 / 相册识餐","搜索离线食物库","手动填写"},(d,i)->{if(i==0)a.photoMenu();else if(i==1)a.show("食物库");else a.meal(new JSONObject());}).show();}
    void summaries(int count){
        LinearLayout row=a.row(body);LinearLayout weight=summary(row,true),food=summary(row,false);
        weight.addView(s.label("最新体重",12,true,s.muted));
        List<JSONObject> weights=new ArrayList<>();for(JSONObject r:a.store.sorted())if(r.optString("kind").equals("weight")&&r.optString("date").compareTo(a.selectedDate)<=0)weights.add(r);
        LinearLayout number=new LinearLayout(a);number.setGravity(Gravity.BOTTOM);weight.addView(number);
        TextView kg=s.number(weights.isEmpty()?"—":a.n(weights.get(0).optDouble("kg")),34,a.ink);number.addView(kg);TextView unit=s.label(" kg",12,false,a.ink);unit.setPadding(0,0,0,a.dp(5));number.addView(unit);
        String delta="记录身体的小变化";int color=s.muted;
        if(weights.size()>1){double change=weights.get(0).optDouble("kg")-weights.get(1).optDouble("kg");delta="较上次 "+(change>0?"+":"")+a.n(change)+" kg";color=change<=0?0xff65764c:s.muted;}
        weight.addView(s.label(delta,11,false,color));weight.setOnClickListener(v->a.show("趋势"));weight.setContentDescription("最新体重，查看趋势");
        food.addView(s.label((a.selectedDate.equals(JournalStore.today())?"今日饮食":"当日饮食"),12,true,s.muted));LinearLayout line=new LinearLayout(a);line.setGravity(Gravity.BOTTOM);food.addView(line);line.addView(s.number(String.valueOf(count),34,a.ink));TextView caption=s.label(" 餐已记录",12,false,a.ink);caption.setPadding(0,0,0,a.dp(5));line.addView(caption);
        food.addView(s.label(count==0?"记下第一餐":"",11,false,s.muted));food.setOnClickListener(v->addMeal());food.setContentDescription(count+"餐已记录，添加饮食");
    }
    LinearLayout summary(LinearLayout row,boolean first){LinearLayout c=a.column();c.setPadding(a.dp(14),a.dp(8),a.dp(10),a.dp(8));c.setBackground(s.ripple(0x42ffffff,12,true));LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,-2,1);p.setMargins(first?0:a.dp(5),0,first?a.dp(5):0,0);row.addView(c,p);return c;}
    void timeline(String group,int index,List<JSONObject> meals){
        LinearLayout row=new LinearLayout(a);row.setGravity(Gravity.TOP);body.addView(row,new LinearLayout.LayoutParams(-1,-2));
        List<JSONObject> matching=new ArrayList<>();for(JSONObject r:meals)if(r.optString("group").equals(group))matching.add(r);
        LinearLayout side=new LinearLayout(a);side.setOrientation(LinearLayout.HORIZONTAL);side.setPadding(0,a.dp(11),0,0);row.addView(side,new LinearLayout.LayoutParams(a.dp(64),-1));
        LinearLayout track=new LinearLayout(a);track.setOrientation(LinearLayout.VERTICAL);track.setGravity(Gravity.CENTER_HORIZONTAL);side.addView(track,new LinearLayout.LayoutParams(a.dp(11),-1));
        View dot=new View(a);android.graphics.drawable.GradientDrawable circle=new android.graphics.drawable.GradientDrawable();circle.setShape(android.graphics.drawable.GradientDrawable.OVAL);circle.setColor(matching.isEmpty()?a.paper:a.accent);circle.setStroke(1,matching.isEmpty()?0xffac8e78:a.accent);dot.setBackground(circle);LinearLayout.LayoutParams dotParams=new LinearLayout.LayoutParams(a.dp(7),a.dp(7));dotParams.topMargin=a.dp(6);track.addView(dot,dotParams);
        if(index<a.groups.length-1){View rule=new View(a);rule.setBackgroundColor(0xffdec5af);LinearLayout.LayoutParams rp=new LinearLayout.LayoutParams(1,0,1);rp.topMargin=a.dp(3);track.addView(rule,rp);}
        LinearLayout labels=new LinearLayout(a);labels.setOrientation(LinearLayout.VERTICAL);labels.setPadding(a.dp(4),0,0,0);side.addView(labels,new LinearLayout.LayoutParams(0,-2,1));
        TextView title=s.label(group,14,true,matching.isEmpty()?s.muted:a.accent);labels.addView(title);TextView hint=s.label(matching.isEmpty()?"待记录":matching.size()+"条记录",10,false,s.muted);labels.addView(hint);
        LinearLayout content=new LinearLayout(a);content.setOrientation(LinearLayout.VERTICAL);row.addView(content,new LinearLayout.LayoutParams(0,-2,1));
        if(matching.isEmpty()){
            Button empty=a.button(content,"未记录",()->{JSONObject r=new JSONObject();MainActivity.put(r,"group",group);a.meal(r);});empty.setGravity(Gravity.LEFT|Gravity.CENTER_VERTICAL);empty.setTextColor(s.muted);empty.setBackground(s.ripple(Color.TRANSPARENT,6,false));empty.setCompoundDrawables(null,null,s.icon("plus",0xffb79b83,17),null);empty.setLayoutParams(new LinearLayout.LayoutParams(-1,a.dp(53)));empty.setContentDescription("添加"+group);
        }else for(JSONObject r:matching)mealCard(content,r);
        if(!matching.isEmpty())a.button(content,"＋ 继续添加"+group,()->{JSONObject r=new JSONObject();MainActivity.put(r,"group",group);a.meal(r);});
        s.line(content);
    }
    void mealCard(LinearLayout parent,JSONObject r){
        LinearLayout c=new LinearLayout(a);c.setOrientation(LinearLayout.HORIZONTAL);c.setGravity(Gravity.CENTER_VERTICAL);c.setPadding(a.dp(8),a.dp(8),a.dp(8),a.dp(8));c.setBackground(s.surface(0x55ffffff,12,true));
        LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.setMargins(0,a.dp(6),0,a.dp(9));parent.addView(c,p);
        String photo=r.optString("photo","");
        if(!photo.isEmpty()){
            ImageView image=new ImageView(a);image.setScaleType(ImageView.ScaleType.CENTER_CROP);image.setBackground(s.surface(0xffeee0cf,9,false));image.setClipToOutline(true);c.addView(image,new LinearLayout.LayoutParams(a.dp(126),a.dp(96)));
            image.setContentDescription(r.optString("food")+"的照片");a.loadMealPhoto(image,photo);
        }
        LinearLayout info=new LinearLayout(a);info.setOrientation(LinearLayout.VERTICAL);info.setPadding(photo.isEmpty()?a.dp(3):a.dp(10),0,0,0);c.addView(info,new LinearLayout.LayoutParams(0,-2,1));
        TextView name=s.label(r.optString("food"),15,true,a.ink);name.setMaxLines(3);name.setEllipsize(android.text.TextUtils.TruncateAt.END);info.addView(name);s.space(info,5);
        info.addView(s.label(JournalStore.known(r,"kcal")?"约 "+a.n(r.optDouble("kcal"))+" kcal":"热量待补",12,false,s.muted));
        Button repeat=a.button(info,"再吃一次",()->{JSONObject copy=a.copy(r);copy.remove("id");MainActivity.put(copy,"date",a.selectedDate);a.meal(copy);});repeat.setTextSize(12);repeat.setMinHeight(a.dp(40));repeat.setMinimumHeight(a.dp(40));repeat.setPadding(a.dp(5),0,a.dp(5),0);repeat.setBackground(s.ripple(Color.TRANSPARENT,20,true));repeat.setCompoundDrawables(s.icon("repeat",a.accent,17),null,null,null);repeat.setLayoutParams(new LinearLayout.LayoutParams(-1,a.dp(40)));
        name.setMinHeight(a.dp(40));name.setOnClickListener(v->a.edit(r));name.setContentDescription("修改饮食 "+r.optString("food"));c.setOnClickListener(v->a.edit(r));
    }
    void energy(List<JSONObject> day,List<JSONObject> meals){
        double intake=JournalStore.sum(meals,"kcal"),target=a.store.plan().optDouble("target",0);
        List<JSONObject> exercises=new ArrayList<>();for(JSONObject row:day)if(row.optString("kind").equals("exercise"))exercises.add(row);
        double burn=JournalStore.sum(exercises,"burn"),net=intake-burn,base=a.store.plan().optDouble("base",0);
        int known=0,knownBurn=0;
        for(JSONObject r:meals)if(JournalStore.known(r,"kcal"))known++;
        for(JSONObject r:exercises)if(JournalStore.known(r,"burn"))knownBurn++;
        boolean hasTarget=Double.isFinite(target)&&target>0;
        boolean comparable=hasTarget&&known>0,over=comparable&&net>target;
        int stateColor=comparable?(net<target?0xff65764c:a.accent):s.muted;

        LinearLayout c=a.card(body);c.setPadding(a.dp(14),a.dp(8),a.dp(14),a.dp(10));
        c.setBackground(s.surface(0x88fffdf6,12,true));
        LinearLayout heading=a.row(c);
        heading.addView(s.heading(a.selectedDate.equals(JournalStore.today())?"今日能量":"当日能量",20,a.ink),new LinearLayout.LayoutParams(0,-2,1));
        Button edit=a.button(heading,"调整目标",()->a.show("计划"));edit.setTextSize(13);
        edit.setMinWidth(0);edit.setMinimumWidth(0);edit.setPadding(a.dp(8),0,0,0);
        edit.setBackground(s.ripple(Color.TRANSPARENT,6,false));edit.setLayoutParams(new LinearLayout.LayoutParams(-2,-2));

        s.space(c,6);
        LinearLayout breakdown=a.row(c);
        breakdown.addView(s.label("饮食摄入  "+(known==0?"—":a.n(intake))+" 千卡",13,false,a.ink),new LinearLayout.LayoutParams(0,-2,1));
        breakdown.addView(s.label("− 运动  "+a.n(burn)+" 千卡",13,true,0xff65764c),new LinearLayout.LayoutParams(0,-2,1));
        s.space(c,8);c.addView(s.label("净摄入 · 饮食减运动",12,true,s.muted));
        LinearLayout numbers=a.row(c);numbers.setGravity(Gravity.BOTTOM);
        TextView total=s.number(known==0?"—":a.n(net),28,stateColor);numbers.addView(total);
        TextView goal=s.label(hasTarget?" / "+a.n(target)+" 千卡":" 千卡 · 未设目标",14,false,s.muted);
        goal.setPadding(a.dp(4),0,0,a.dp(3));numbers.addView(goal,new LinearLayout.LayoutParams(0,-2,1));
        s.space(c,7);
        if(hasTarget){
            ProgressBar bar=new ProgressBar(a,null,android.R.attr.progressBarStyleHorizontal);
            bar.setMax(1000);bar.setProgress(known==0?0:(int)Math.max(0,Math.min(1000,net/target*1000)));
            bar.setProgressTintList(ColorStateList.valueOf(stateColor));bar.setProgressBackgroundTintList(ColorStateList.valueOf(s.line));
            bar.setContentDescription(known==0?"摄入热量待记录":"净摄入 "+a.n(net)+" 千卡，目标 "+a.n(target)+" 千卡");
            c.addView(bar,new LinearLayout.LayoutParams(-1,a.dp(8)));s.space(c,8);
        }

        if(over){
            LinearLayout notice=new LinearLayout(a);notice.setOrientation(LinearLayout.VERTICAL);
            notice.setPadding(a.dp(10),a.dp(8),a.dp(10),a.dp(8));notice.setBackground(s.surface(0x18b23924,8,false));
            notice.addView(s.label("超出 "+a.n(net-target)+" 千卡",15,true,a.accent));
            notice.addView(s.label("记得动一动，运动后记录一下。",12,false,a.accent));
            c.addView(notice,new LinearLayout.LayoutParams(-1,-2));
        }else{
            String status=!hasTarget?"设置每日目标后查看进度":known==0?(meals.isEmpty()?"记录饮食后查看进度":"等待补全热量"):
                    net<target?"还可摄入 "+a.n(target-net)+" 千卡":"已达今日目标";
            c.addView(s.label(status,14,true,stateColor));
        }
        if(knownBurn<exercises.size())c.addView(s.label("运动消耗待补，净摄入暂不完整",12,false,s.muted));
        if(known<meals.size()){
            TextView pending=s.label((meals.size()-known)+" 条热量待补，合计暂不完整",12,false,s.muted);
            pending.setPadding(0,a.dp(5),0,0);c.addView(pending);
        }

        s.space(c,7);LinearLayout activity=a.row(c);
        LinearLayout consumption=new LinearLayout(a);consumption.setOrientation(LinearLayout.VERTICAL);
        activity.addView(consumption,new LinearLayout.LayoutParams(0,-2,1));
        consumption.addView(s.label(knownBurn==0?"运动 · 未记录":"运动额外消耗 "+a.n(burn)+" 千卡",12,false,s.muted));
        if(Double.isFinite(base)&&base>0)consumption.addView(s.label("全天消耗约 "+a.n(base+burn)+" 千卡",12,false,s.muted));
        Button exercise=a.button(activity,"记录运动",()->a.exercise(new JSONObject()));exercise.setTextSize(13);
        exercise.setMinWidth(0);exercise.setMinimumWidth(0);exercise.setPadding(a.dp(10),a.dp(8),a.dp(10),a.dp(8));
        exercise.setLayoutParams(new LinearLayout.LayoutParams(-2,-2));exercise.setCompoundDrawablePadding(a.dp(4));
        exercise.setCompoundDrawables(s.icon("run",over?Color.WHITE:a.accent,20),null,null,null);
        if(over){exercise.setBackground(s.ripple(a.accent,10,false));exercise.setTextColor(Color.WHITE);}

        StringBuilder macros=new StringBuilder();
        for(String key:new String[]{"protein","carbs","fat"}){
            int n=0;for(JSONObject r:meals)if(JournalStore.known(r,key))n++;
            if(n>0){if(macros.length()>0)macros.append(" · ");macros.append(key.equals("protein")?"蛋白质 ":key.equals("carbs")?"碳水 ":"脂肪 ").append(a.n(JournalStore.sum(meals,key))).append("g").append(n<meals.size()?"（部分）":"");}
        }
        if(macros.length()>0)c.addView(s.label(macros.toString(),11,false,s.muted));
    }
}
