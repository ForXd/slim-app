package cn.qingji.diary;

import android.animation.ValueAnimator;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.Uri;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.view.animation.DecelerateInterpolator;
import android.widget.*;
import java.util.Locale;
import org.json.JSONObject;

/** Explicit two-step plan builder. Opening or previewing a plan never changes saved data. */
final class PlanScreen {
    private final MainActivity a;
    private final JournalStyle style;
    private final JSONObject savedProfile;
    private LinearLayout stage,choices,customPanel;
    private EditText height,weight,age,custom;
    private Spinner sex,activity;
    private String draftHeight,draftWeight,draftAge,customText;
    private int draftSex,draftActivity;
    private PlanMath.Estimate estimate;
    private PlanMath.Mode selected=PlanMath.Mode.MAINTAIN;
    private boolean showingResult;

    PlanScreen(MainActivity app){
        a=app;style=app.style;savedProfile=readProfile();
        draftHeight=savedProfile.optString("heightCm","");draftWeight=savedProfile.optString("weightKg","");
        draftAge=savedProfile.optString("age","");
        draftSex=enumIndex(savedProfile.optString("sex"),PlanMath.Sex.values());
        draftActivity=enumIndex(savedProfile.optString("activity"),PlanMath.Activity.values());
        for(JSONObject row:a.store.sorted())if(row.optString("kind").equals("weight")&&JournalStore.known(row,"kg")){
            draftWeight=a.n(row.optDouble("kg"));break;
        }
        JSONObject old=a.store.plan();customText=JournalStore.known(old,"target")?a.n(old.optDouble("target")):"";
    }

    void render(){
        if(!showingResult&&height!=null){
            draftHeight=height.getText().toString();draftWeight=weight.getText().toString();draftAge=age.getText().toString();
            draftSex=sex.getSelectedItemPosition();draftActivity=activity.getSelectedItemPosition();
        }
        currentPlan();
        LinearLayout tools=a.row(a.body);
        compactButton(tools,"直接设置",this::directSettings);
        compactButton(tools,"估算说明",this::explanation);
        stage=new LinearLayout(a);stage.setOrientation(LinearLayout.VERTICAL);a.body.addView(stage);
        if(showingResult&&estimate!=null)result();else bodyForm(false);
    }

    private JSONObject readProfile(){
        try{return new JSONObject(a.store.prefs.getString("profile","{}"));}
        catch(Exception ignored){return new JSONObject();}
    }

    private static int enumIndex(String value,Enum<?>[] values){
        for(int i=0;i<values.length;i++)if(values[i].name().equals(value))return i+1;
        return 0;
    }

    private void currentPlan(){
        JSONObject old=a.store.plan();
        LinearLayout card=a.card(a.body);
        card.setPadding(a.dp(14),a.dp(10),a.dp(14),a.dp(10));
        card.addView(style.label("当前计划",12,true,style.muted));
        if(JournalStore.known(old,"target")&&old.optDouble("target")>0){
            LinearLayout line=a.row(card);
            line.addView(style.number(a.n(old.optDouble("target")),28,a.ink));
            TextView unit=style.label(" 千卡 / 天",12,false,style.muted);line.addView(unit);
            String detail=old.optString("name","自定义目标");
            if(JournalStore.known(old,"base")&&old.optDouble("base")>0)detail+=" · 维持 "+a.n(old.optDouble("base"))+" 千卡";
            card.addView(style.label(detail,12,false,style.muted));
        }else card.addView(style.label("还没有每日摄入目标",16,false,a.ink));
    }

    private void bodyForm(boolean animate){
        showingResult=false;
        stage.removeAllViews();LinearLayout card=a.card(stage);
        card.addView(style.heading("1 · 身体信息",20,a.ink));
        LinearLayout measurements=a.row(card);
        height=a.input(half(measurements,true),"身高（cm）",draftHeight,true);
        weight=a.input(half(measurements,false),"体重（kg）",draftWeight,true);
        LinearLayout details=a.row(card);
        age=a.input(half(details,true),"年龄",draftAge,true);age.setInputType(InputType.TYPE_CLASS_NUMBER);age.setHint("18–78 岁");
        sex=a.spinner(half(details,false),"公式性别",new String[]{"请选择","女性公式","男性公式"},draftSex);
        activity=a.spinner(card,"日常活动",new String[]{"请选择","久坐为主","日常轻活动","日常较多活动"},draftActivity);
        TextView hint=style.label("不含在 App 另记的专项运动。",11,false,style.muted);hint.setPadding(0,a.dp(4),0,a.dp(5));card.addView(hint);
        primary(card,"计算我的计划",this::calculate);
        if(animate)transition(false);
    }

    private LinearLayout half(LinearLayout row,boolean first){
        LinearLayout column=new LinearLayout(a);column.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,-2,1);
        p.setMargins(first?0:a.dp(6),0,first?a.dp(6):0,0);row.addView(column,p);return column;
    }

    private void calculate(){
        try{
            double cm=a.number(height,100,250),kg=a.number(weight,25,350),years=a.number(age,18,78);
            if(years!=Math.floor(years)){age.setError("请填写整数年龄");return;}
            if(sex.getSelectedItemPosition()==0){a.toast("请选择公式性别");sex.requestFocus();return;}
            if(activity.getSelectedItemPosition()==0){a.toast("请选择日常活动程度");activity.requestFocus();return;}
            draftHeight=height.getText().toString().trim();draftWeight=weight.getText().toString().trim();
            draftAge=String.valueOf((int)years);draftSex=sex.getSelectedItemPosition();draftActivity=activity.getSelectedItemPosition();
            estimate=PlanMath.estimate(cm,kg,(int)years,PlanMath.Sex.values()[draftSex-1],PlanMath.Activity.values()[draftActivity-1]);
            selected=estimate.allowsReduction&&estimate.maintenance>estimate.minimumTarget?PlanMath.Mode.MILD:PlanMath.Mode.MAINTAIN;
            ((InputMethodManager)a.getSystemService(android.content.Context.INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(stage.getWindowToken(),0);
            result();transition(true);
        }catch(Exception error){a.toast(error.getMessage());}
    }

    private void result(){
        showingResult=true;
        stage.removeAllViews();LinearLayout card=a.card(stage);
        LinearLayout heading=a.row(card);
        heading.addView(style.heading("2 · 选择计划",20,a.ink),new LinearLayout.LayoutParams(0,-2,1));
        Button edit=a.button(heading,"修改信息",()->bodyForm(true));edit.setTextSize(12);
        edit.setPadding(a.dp(6),0,0,0);edit.setBackground(style.ripple(Color.TRANSPARENT,8,false));edit.setLayoutParams(new LinearLayout.LayoutParams(-2,-2));
        card.addView(style.label("预计维持热量",12,true,style.muted));
        LinearLayout baseline=a.row(card);baseline.addView(style.number(a.n(estimate.maintenance),32,a.ink));baseline.addView(style.label(" 千卡 / 天",12,false,style.muted));
        card.addView(style.label("日常活动已计入，另记运动单独计算。",11,false,style.muted));
        if(!estimate.allowsReduction){
            TextView note=style.label("BMI 低于 18.5，暂不提供自动减量方案。",12,false,a.accent);note.setPadding(0,a.dp(8),0,0);card.addView(note);
        }else if(estimate.maintenance<=estimate.minimumTarget){
            TextView note=style.label("当前维持估算较低，自动方案不再下调。",12,false,style.muted);note.setPadding(0,a.dp(8),0,0);card.addView(note);
        }
        style.space(card,8);choices=new LinearLayout(a);choices.setOrientation(LinearLayout.VERTICAL);card.addView(choices);
        customPanel=new LinearLayout(a);customPanel.setOrientation(LinearLayout.VERTICAL);card.addView(customPanel);
        custom=a.input(customPanel,"每日摄入目标（千卡）",customText,true);
        custom.addTextChangedListener(new TextWatcher(){
            public void beforeTextChanged(CharSequence value,int start,int count,int after){}
            public void onTextChanged(CharSequence value,int start,int before,int count){customText=value.toString();refreshChoices();}
            public void afterTextChanged(Editable value){}
        });
        card.addView(style.label("初期理论参考，不代表实际体重变化。",11,false,style.muted));
        primary(card,"保存每日计划",this::saveCalculated);
        refreshChoices();
    }

    private void refreshChoices(){
        choices.removeAllViews();
        boolean reduce=estimate.allowsReduction&&estimate.maintenance>estimate.minimumTarget;
        PlanMath.Mode[] modes=reduce?PlanMath.Mode.values():new PlanMath.Mode[]{PlanMath.Mode.MAINTAIN,PlanMath.Mode.CUSTOM};
        LinearLayout row=null;
        for(int i=0;i<modes.length;i++){
            if(i%2==0)row=a.row(choices);
            option(row,modes[i],i%2==0);
        }
        customPanel.setVisibility(selected==PlanMath.Mode.CUSTOM?View.VISIBLE:View.GONE);
    }

    private void option(LinearLayout row,PlanMath.Mode mode,boolean first){
        boolean chosen=selected==mode;PlanMath.Choice choice=null;
        try{choice=PlanMath.choice(estimate,mode,mode==PlanMath.Mode.CUSTOM?Double.parseDouble(customText):0);}catch(Exception ignored){}
        LinearLayout card=new LinearLayout(a);card.setOrientation(LinearLayout.VERTICAL);card.setPadding(a.dp(10),a.dp(10),a.dp(10),a.dp(10));
        card.setBackground(style.ripple(chosen?(0x20000000|(a.accent&0xffffff)):0x55ffffff,10,true));
        LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,-2,1);p.setMargins(first?0:a.dp(4),a.dp(4),first?a.dp(4):0,a.dp(4));row.addView(card,p);
        card.addView(style.label(shortName(mode),14,true,chosen?a.accent:a.ink));
        card.addView(style.number(choice==null?"—":a.n(choice.target),24,chosen?a.accent:a.ink));
        card.addView(style.label("千卡 / 天",10,false,style.muted));
        style.space(card,5);card.addView(style.label("初期理论参考",10,false,style.muted));
        String reference;
        if(choice==null)reference="填写目标后查看";
        else if(Math.abs(choice.deficit)<.0001)reference="每周约持平\n4 周约持平";
        else reference="每周 "+signed(-choice.weekKg)+" kg\n4 周 "+signed(-choice.fourWeeksKg)+" kg";
        card.addView(style.label(reference,11,false,style.muted));
        card.setSelected(chosen);card.setFocusable(true);
        card.setContentDescription(shortName(mode)+(chosen?"，已选":"，未选")+(choice==null?"，填写目标": "，每日 "+a.n(choice.target)+" 千卡，理论参考 "+reference.replace('\n','，')));
        card.setOnClickListener(v->{
            v.performHapticFeedback(android.view.HapticFeedbackConstants.CLOCK_TICK);
            selected=mode;refreshChoices();
            if(mode==PlanMath.Mode.CUSTOM){custom.requestFocus();((InputMethodManager)a.getSystemService(android.content.Context.INPUT_METHOD_SERVICE)).showSoftInput(custom,InputMethodManager.SHOW_IMPLICIT);}
        });
    }

    private static String signed(double value){return String.format(Locale.CHINESE,"%+.2f",value);}
    private static String shortName(PlanMath.Mode mode){switch(mode){case MILD:return "温和减量";case STANDARD:return "标准减量";case CUSTOM:return "自定义";default:return "维持与观察";}}
    private static String savedName(PlanMath.Mode mode){return mode==PlanMath.Mode.CUSTOM?"自定义目标":shortName(mode);}

    private void saveCalculated(){
        try{
            double manual=selected==PlanMath.Mode.CUSTOM?a.number(custom,1,6500):0;
            PlanMath.Choice choice=PlanMath.choice(estimate,selected,manual);
            JSONObject profile=new JSONObject().put("heightCm",Double.parseDouble(draftHeight)).put("weightKg",Double.parseDouble(draftWeight))
                    .put("age",Integer.parseInt(draftAge)).put("sex",PlanMath.Sex.values()[draftSex-1].name()).put("activity",PlanMath.Activity.values()[draftActivity-1].name());
            persist(savedName(selected),choice.target,estimate.maintenance,profile);
        }catch(Exception error){a.toast(error.getMessage());}
    }

    private void directSettings(){
        JSONObject old=a.store.plan();LinearLayout form=a.column();
        EditText baseline=a.input(form,"维持热量（千卡，可留空）",old.optString("base",""),true);
        EditText target=a.input(form,"每日摄入目标（千卡）",old.optString("target",""),true);
        AlertDialog dialog=a.dialog("直接设置",form);
        primary(form,"保存每日计划",()->{
            try{
                double value=PlanMath.customTarget(a.number(target,1,6500));
                double base=baseline.getText().toString().trim().isEmpty()?Double.NaN:PlanMath.manualMaintenance(a.number(baseline,1,6500));
                persist("自定义目标",value,base,null);dialog.dismiss();
            }catch(Exception error){a.toast(error.getMessage());}
        });
    }

    private void persist(String name,double target,double base,JSONObject profile)throws Exception{
        JSONObject plan=new JSONObject(a.store.plan().toString()).put("name",name).put("target",target);
        if(Double.isFinite(base)&&base>0)plan.put("base",base);else plan.remove("base");
        SharedPreferences.Editor edit=a.store.prefs.edit().putString("plan",plan.toString());
        if(profile!=null)edit.putString("profile",profile.toString());
        if(!edit.commit())throw new IllegalStateException("保存失败，请重试");
        a.show("今日");a.toast("计划已更新");
    }

    private void transition(boolean forward){
        stage.animate().cancel();stage.setAlpha(1);stage.setTranslationX(0);
        if(ValueAnimator.areAnimatorsEnabled()){
            stage.setAlpha(0);stage.setTranslationX(a.dp(forward?18:-18));
            stage.animate().alpha(1).translationX(0).setDuration(180).setInterpolator(new DecelerateInterpolator()).start();
        }
        stage.post(()->{if(stage.isAttachedToWindow())a.activeScroll.smoothScrollTo(0,Math.max(0,stage.getTop()-a.dp(8)));});
    }

    private Button primary(LinearLayout parent,String text,Runnable action){
        Button button=a.button(parent,text,action);button.setBackground(style.ripple(a.accent,10,false));button.setTextColor(Color.WHITE);return button;
    }
    private void compactButton(LinearLayout row,String text,Runnable action){
        Button button=a.button(row,text,action);button.setTextSize(13);button.setBackground(style.ripple(Color.TRANSPARENT,8,false));
        button.setLayoutParams(new LinearLayout.LayoutParams(0,-2,1));
    }

    private void explanation(){
        LinearLayout form=a.column();
        a.text(form,"维持热量用 Mifflin–St Jeor 静息公式结合日常活动估算，适用年龄 18–78 岁。另记运动不计入活动档位，避免重复计算。",13,false);
        a.text(form,"温和与标准方案原始日差值为 250 / 500 千卡。自动方案采用女性 1200、男性 1500 千卡的保守下限；维持估算低于下限时不再下调。BMI 低于 18.5 不提供自动减量方案。",13,false);
        a.text(form,"每周与 4 周的数值按实际热量差及约 7700 千卡 / kg 换算，仅是初期理论参考。实际变化受代谢适应、水分等影响，不能线性预测长期效果或达标日期。",13,false);
        a.text(form,"孕哺期、未成年人或需要疾病饮食管理时，请个别制定目标。已有专业建议可通过「直接设置」保存。",13,false);
        source(form,"公式来源 · Mifflin–St Jeor","https://pubmed.ncbi.nlm.nih.gov/2305711/");
        source(form,"摄入范围 · AHA / ACC / TOS 指南","https://pmc.ncbi.nlm.nih.gov/articles/PMC5819889/");
        source(form,"活动系数 · Texas HHS","https://www.hhs.texas.gov/sites/default/files/documents/common-comparative-standards-for-rds-ltc-settings.pdf");
        source(form,"体重变化 · NIH Body Weight Planner","https://www.niddk.nih.gov/health-information/professionals/diabetes-discoveries-practice/nih-body-weight-planner");
        a.dialog("估算说明",form);
    }

    private void source(LinearLayout parent,String title,String url){
        Button button=a.button(parent,title,()->{try{a.startActivity(new Intent(Intent.ACTION_VIEW,Uri.parse(url)));}catch(Exception error){a.toast("未找到可打开链接的浏览器");}});
        button.setTextSize(12);button.setGravity(Gravity.START|Gravity.CENTER_VERTICAL);button.setBackground(style.ripple(Color.TRANSPARENT,8,false));
    }

}
