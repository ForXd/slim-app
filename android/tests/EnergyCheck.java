package cn.qingji.diary;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.os.SystemClock;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/** Real native energy-card views with isolated plan data and no journal writes. */
public final class EnergyCheck extends Instrumentation {
    private static final int BELOW_TARGET=0xff65764c;
    private MainActivity activity;
    private SharedPreferences testPreferences;
    private final List<TextView> texts=new ArrayList<>();
    private final List<ProgressBar> bars=new ArrayList<>();

    @Override public void onCreate(Bundle args){super.onCreate(args);start();}

    @Override public void onStart(){
        Bundle result=new Bundle();
        Throwable failure=null;
        try{
            activity=(MainActivity)startActivitySync(new Intent(getTargetContext(),MainActivity.class)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            awaitReady();
            // One main-thread transaction keeps async activity callbacks outside the store swap.
            main(()->{
                JournalStore originalStore=activity.store;
                LinearLayout originalBody=activity.body;
                Map<String,?> originalPreferences=originalStore.prefs.getAll();
                testPreferences=getTargetContext().getSharedPreferences("energy-check",Context.MODE_PRIVATE);
                try{
                    check(testPreferences.edit().clear().commit(),"cannot prepare isolated preferences");
                    Context isolated=new ContextWrapper(activity){
                        @Override public SharedPreferences getSharedPreferences(String name,int mode){return testPreferences;}
                    };
                    activity.store=new JournalStore(isolated);
                    check(activity.store.prefs==testPreferences,"test store is not isolated");
                    activity.body=new LinearLayout(activity);
                    activity.body.setOrientation(LinearLayout.VERTICAL);

                    render(1200d,meal(1000d));
                    checkBar(833,BELOW_TARGET,"below target");
                    checkColor(activity.n(1000),BELOW_TARGET,"below-target total");
                    checkColor("还可摄入 "+activity.n(200)+" 千卡",BELOW_TARGET,"remaining allowance");
                    checkText(" / "+activity.n(1200)+" 千卡");
                    checkNoText("已达今日目标");

                    render(1200d,meal(1200d));
                    checkBar(1000,activity.accent,"exact target");
                    checkColor(activity.n(1200),activity.accent,"at-target total");
                    checkColor("已达今日目标",activity.accent,"at-target status");
                    checkNoPrefix("超出 ");

                    render(1200d,meal(1300d));
                    checkBar(1000,activity.accent,"over target");
                    checkColor(activity.n(1300),activity.accent,"over-target total");
                    checkColor("超出 "+activity.n(100)+" 千卡",activity.accent,"over-target amount");
                    checkText("记得动一动，运动后记录一下。");
                    TextView exercise=checkText("记录运动");
                    check(exercise instanceof Button&&exercise.isEnabled()&&exercise.hasOnClickListeners(),"exercise action missing");
                    check(exercise.getCurrentTextColor()==Color.WHITE,"over-target exercise action not emphasized");

                    render(1200d,meal(1300d),new JSONObject().put("kind","exercise").put("burn",100));
                    checkBar(1000,activity.accent,"exercise does not raise the intake allowance");
                    checkText("全天消耗约 "+activity.n(1900)+" 千卡");
                    checkText("运动额外消耗 "+activity.n(100)+" 千卡");
                    checkText(" / "+activity.n(1200)+" 千卡");
                    checkText("超出 "+activity.n(100)+" 千卡");

                    render(1200d,meal(null));
                    checkBar(0,activity.style.muted,"unknown calories");
                    checkColor("—",activity.style.muted,"unknown total must remain unknown");
                    checkText("等待补全热量");
                    checkText("1 条热量待补，合计暂不完整");
                    checkNoText(activity.n(0));
                    checkNoText("已达今日目标");
                    checkNoPrefix("还可摄入 ");

                    render(1200d);
                    checkBar(0,activity.style.muted,"empty meals");
                    checkText("—");
                    checkText("记录饮食后查看进度");
                    checkNoText("已达今日目标");
                    checkNoPrefix("超出 ");

                    render(null,meal(1300d));
                    check(bars.isEmpty(),"missing target must not show a full progress bar");
                    checkColor(activity.n(1300),activity.style.muted,"no-target total");
                    checkText(" 千卡 · 未设目标");
                    checkText("设置每日目标后查看进度");
                    checkNoText("已达今日目标");
                    checkNoPrefix("超出 ");

                    render(1200d,meal(0d));
                    checkBar(0,BELOW_TARGET,"explicit zero differs from unknown");
                    checkColor(activity.n(0),BELOW_TARGET,"known zero total");
                    checkText("还可摄入 "+activity.n(1200)+" 千卡");
                    checkNoText("—");
                }finally{
                    activity.store=originalStore;
                    activity.body=originalBody;
                    check(testPreferences.edit().clear().commit(),"cannot clear isolated preferences");
                    check(originalStore.prefs.getAll().equals(originalPreferences),"real journal preferences changed during the check");
                }
            });
            result.putString("result","PASS: real native energy-card progress/tints/text for below, exact and over target; exercise increases expenditure without changing allowance; unknown, empty, no-target and explicit-zero states; isolated plan data cleaned and original store/body restored");
        }catch(Throwable error){failure=error;}
        if(failure!=null)result.putString("result",android.util.Log.getStackTraceString(failure));
        finish(failure==null?Activity.RESULT_OK:Activity.RESULT_CANCELED,result);
    }

    private void awaitReady(){
        long end=SystemClock.uptimeMillis()+15000;
        boolean[] ready={false};
        while(SystemClock.uptimeMillis()<end){
            main(()->ready[0]=activity.style!=null&&activity.store!=null&&activity.body!=null&&activity.migration==null);
            if(ready[0]){waitForIdleSync();return;}
            SystemClock.sleep(50);
        }
        throw new AssertionError("MainActivity did not finish preparing its content");
    }

    private JSONObject meal(Double kcal)throws Exception{
        JSONObject item=new JSONObject().put("kind","meal").put("food","energy-check fixture");
        if(kcal!=null)item.put("kcal",kcal);
        return item;
    }

    private void render(Double target,JSONObject... records)throws Exception{
        JSONObject plan=new JSONObject().put("base",1800);
        if(target!=null)plan.put("target",target);
        check(testPreferences.edit().putString("plan",plan.toString()).commit(),"cannot save isolated plan");
        List<JSONObject> day=Arrays.asList(records),meals=new ArrayList<>();
        for(JSONObject record:day)if("meal".equals(record.optString("kind")))meals.add(record);
        activity.body.removeAllViews();
        new HomeScreen(activity).energy(day,meals);
        texts.clear();bars.clear();collect(activity.body);
        check(!texts.isEmpty(),"energy card did not produce native text views");
    }

    private void collect(View view){
        if(view instanceof TextView)texts.add((TextView)view);
        if(view instanceof ProgressBar)bars.add((ProgressBar)view);
        if(view instanceof ViewGroup){
            ViewGroup group=(ViewGroup)view;
            for(int i=0;i<group.getChildCount();i++)collect(group.getChildAt(i));
        }
    }

    private TextView checkText(String expected){
        for(TextView view:texts)if(expected.contentEquals(view.getText()))return view;
        throw new AssertionError("missing text: "+expected);
    }

    private void checkNoText(String unwanted){
        for(TextView view:texts)check(!unwanted.contentEquals(view.getText()),"unexpected text: "+unwanted);
    }

    private void checkNoPrefix(String unwanted){
        for(TextView view:texts)check(!view.getText().toString().startsWith(unwanted),"unexpected status: "+view.getText());
    }

    private void checkColor(String text,int expected,String label){
        check(checkText(text).getCurrentTextColor()==expected,label+": wrong text color");
    }

    private void checkBar(int progress,int color,String label){
        check(bars.size()==1,label+": expected exactly one progress bar");
        ProgressBar bar=bars.get(0);
        check(bar.getMax()==1000&&bar.getProgress()==progress,label+": wrong progress "+bar.getProgress());
        check(bar.getProgressTintList()!=null&&bar.getProgressTintList().getDefaultColor()==color,label+": wrong progress tint");
    }

    private interface Action {void run()throws Exception;}
    private void main(Action action){
        Throwable[] failure={null};
        runOnMainSync(()->{try{action.run();}catch(Throwable error){failure[0]=error;}});
        if(failure[0]!=null)throw new AssertionError("Main-thread energy check failed",failure[0]);
    }

    private static void check(boolean condition,String message){if(!condition)throw new AssertionError(message);}
}
