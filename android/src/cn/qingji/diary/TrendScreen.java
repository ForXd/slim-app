package cn.qingji.diary;

import android.graphics.Color;
import android.view.Gravity;
import android.widget.*;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.*;

/** Compact selectors, period summary and an inspectable native chart. */
final class TrendScreen {
    private final MainActivity app;
    private String range="week",metric="kcal";
    private YearMonth month=YearMonth.now();
    private int year=LocalDate.now().getYear();
    TrendScreen(MainActivity app){this.app=app;}
    void render(){
        LocalDate today=LocalDate.now();
        if(month.isAfter(YearMonth.from(today)))month=YearMonth.from(today);if(year>today.getYear())year=today.getYear();
        segments(new String[]{"近7天","近30天","月度","年度"},new String[]{"week","recent","month","year"},range,true);
        app.style.space(app.body,6);
        segments(new String[]{"摄入","运动","体重"},new String[]{"kcal","burn","kg"},metric,false);
        LocalDate start,end;
        if(range.equals("month")){start=month.atDay(1);end=month.atEndOfMonth();}
        else if(range.equals("year")){start=LocalDate.of(year,1,1);end=LocalDate.of(year,12,31);}
        else {end=today;start=end.minusDays(range.equals("recent")?29:6);}
        app.style.space(app.body,12);
        period(start,end,today);
        TrendSeries series=TrendSeries.build(app.store.sorted(),metric,start,end,range.equals("year"),today);
        summary(series);
        LinearLayout chartCard=app.card(app.body);chartCard.setPadding(app.dp(12),app.dp(14),app.dp(12),app.dp(12));
        LinearLayout heading=app.row(chartCard);
        String title=metric.equals("kg")?(series.monthly?"月均体重":"体重变化"):metric.equals("burn")?(series.monthly?"月均运动":"运动消耗"):(series.monthly?"月均摄入":"饮食摄入");
        heading.addView(app.style.heading(title,19,app.ink),new LinearLayout.LayoutParams(0,-2,1));
        heading.addView(app.style.label(metric.equals("kg")?"kg":"千卡",12,false,app.style.muted));
        app.style.space(chartCard,5);
        LinearLayout detail=new LinearLayout(app);detail.setOrientation(LinearLayout.VERTICAL);
        TrendView chart=new TrendView(app,series,index->selection(detail,series,index));
        int height=app.dp(228*Math.max(1,app.getResources().getConfiguration().fontScale));
        chartCard.addView(chart,new LinearLayout.LayoutParams(-1,height));
        app.style.line(chartCard);app.style.space(chartCard,12);chartCard.addView(detail);
        selection(detail,series,series.latestIndex());
        TextView hint=app.style.label(series.monthly?"按有记录日计算月均值 · 点图查看":"点图查看每日记录",11,false,app.style.muted);
        hint.setPadding(0,app.dp(10),0,0);chartCard.addView(hint);
        if(metric.equals("kg"))app.text(app.body,"每日取最后一次体重",11,false).setTextColor(app.style.muted);
        if(series.partial)app.text(app.body,metric.equals("kcal")?"＊ 含热量待补记录，统计暂不完整":"＊ 含待补记录",12,false).setTextColor(app.style.muted);
    }
    private void segments(String[] labels,String[] keys,String active,boolean periods){
        LinearLayout row=app.row(app.body);row.setBackground(app.style.surface(0x40ffffff,11,true));row.setPadding(app.dp(3),app.dp(3),app.dp(3),app.dp(3));
        for(int i=0;i<labels.length;i++){
            final String key=keys[i];boolean chosen=key.equals(active);
            Button button=app.button(row,labels[i],()->{
                if(periods){if(range.equals(key))return;range=key;}else {if(metric.equals(key))return;metric=key;}
                redraw();
            });
            button.setSingleLine(true);button.setTextSize(12);button.setPadding(app.dp(3),app.dp(6),app.dp(3),app.dp(6));button.setMinHeight(0);button.setMinimumHeight(0);
            int height=app.dp(42*Math.max(1,app.getResources().getConfiguration().fontScale));
            LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,height,1);p.setMargins(app.dp(1),0,app.dp(1),0);button.setLayoutParams(p);
            button.setSelected(chosen);button.setBackground(app.style.ripple(chosen?(periods?app.accent:0xffeed9cc):Color.TRANSPARENT,8,false));
            button.setTextColor(chosen&&periods?0xfffff9ef:chosen?app.accent:app.style.muted);
        }
    }
    private void period(LocalDate start,LocalDate end,LocalDate today){
        LinearLayout line=app.row(app.body);
        if(range.equals("month")||range.equals("year")){
            Button previous=app.button(line,"‹",()->{if(range.equals("month"))month=month.minusMonths(1);else year--;redraw();});
            periodArrow(previous,"上一"+(range.equals("year")?"年":"月"));previous.setEnabled(range.equals("month")?month.getYear()>1900||month.getMonthValue()>1:year>1900);
            TextView title=app.style.heading(range.equals("month")?month.getYear()+"年"+month.getMonthValue()+"月":year+"年",20,app.ink);title.setGravity(Gravity.CENTER);line.addView(title,new LinearLayout.LayoutParams(0,-2,1));
            Button next=app.button(line,"›",()->{if(range.equals("month"))month=month.plusMonths(1);else year++;redraw();});periodArrow(next,"下一"+(range.equals("year")?"年":"月"));
            boolean canAdvance=range.equals("month")?month.isBefore(YearMonth.from(today)):year<today.getYear();next.setEnabled(canAdvance);next.setAlpha(canAdvance?1:.3f);previous.setAlpha(previous.isEnabled()?1:.3f);
        }else{
            DateTimeFormatter format=DateTimeFormatter.ofPattern("M月d日");
            TextView dates=app.style.label(start.format(format)+" — "+end.format(format),14,true,app.ink);dates.setPadding(app.dp(2),app.dp(10),0,app.dp(8));line.addView(dates);
        }
    }
    private void periodArrow(Button button,String description){
        button.setContentDescription(description);button.setTextSize(26);button.setPadding(0,0,0,0);button.setMinHeight(0);button.setMinimumHeight(0);button.setBackground(app.style.ripple(Color.TRANSPARENT,9,false));button.setLayoutParams(new LinearLayout.LayoutParams(app.dp(44),app.dp(48)));
    }
    private void summary(TrendSeries series){
        LinearLayout row=app.row(app.body);
        if(metric.equals("kg")){
            summaryCell(row,"最近体重",series.latest==null?"—":app.n(series.latest),"kg",app.ink,true);
            String change=series.change==null?"—":(series.change>0?"+":"")+app.n(series.change);
            summaryCell(row,"期间变化",change,"kg",series.change!=null&&series.change<0?0xff65764c:app.ink,false);
        }else{
            summaryCell(row,metric.equals("burn")?"日均运动":"日均摄入",series.average==null?"—":app.n(series.average),"千卡",app.ink,true);
            summaryCell(row,"有记录",String.valueOf(series.recordedDays),"天",app.ink,false);
        }
    }
    private void summaryCell(LinearLayout parent,String title,String value,String unit,int color,boolean first){
        LinearLayout cell=app.card(parent);cell.setPadding(app.dp(12),app.dp(13),app.dp(10),app.dp(13));
        LinearLayout.LayoutParams params=new LinearLayout.LayoutParams(0,-2,1);params.setMargins(first?0:app.dp(4),app.dp(8),first?app.dp(4):0,app.dp(4));cell.setLayoutParams(params);
        cell.addView(app.style.label(title,12,false,app.style.muted));app.style.space(cell,6);
        LinearLayout numberRow=new LinearLayout(app);numberRow.setGravity(Gravity.BOTTOM);cell.addView(numberRow);
        TextView number=app.style.number(value,26,color);number.setSingleLine(true);number.setAutoSizeTextTypeUniformWithConfiguration(16,26,1,android.util.TypedValue.COMPLEX_UNIT_SP);numberRow.addView(number,new LinearLayout.LayoutParams(0,app.dp(36*Math.max(1,app.getResources().getConfiguration().fontScale)),1));
        TextView suffix=app.style.label(" "+unit,11,false,app.style.muted);suffix.setPadding(0,0,0,app.dp(3));numberRow.addView(suffix);
    }
    private void selection(LinearLayout detail,TrendSeries series,int index){
        detail.removeAllViews();if(index<0||index>=series.points.size())return;
        TrendSeries.Point point=series.points.get(index);
        LinearLayout row=app.row(detail);
        String date=point.key;
        if(series.monthly){YearMonth month=YearMonth.parse(point.key);date=month.getYear()+"年"+month.getMonthValue()+"月";}
        TextView when=app.style.label(date,12,false,app.style.muted);row.addView(when,new LinearLayout.LayoutParams(0,-2,1));
        String value=point.value==null?(point.future?"尚未到来":point.partial?(metric.equals("kcal")?"热量待补":"数值待补"):"暂无记录"):app.n(point.value)+" "+(metric.equals("kg")?"kg":"千卡")+(point.partial?"＊":"");
        TextView amount=app.style.number(value,18,point.value==null?app.style.muted:app.accent);row.addView(amount);
        if(series.monthly&&point.recordedDays>0){TextView days=app.style.label("已记录 "+point.recordedDays+" 天",11,false,app.style.muted);days.setPadding(0,app.dp(5),0,0);detail.addView(days);}
        detail.setContentDescription("选中"+date+"，"+value);detail.setAccessibilityLiveRegion(android.view.View.ACCESSIBILITY_LIVE_REGION_POLITE);
    }
    private void redraw(){if(app.activeScroll!=null)app.activeScroll.scrollTo(0,0);app.show("趋势");}
}
