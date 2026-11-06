package cn.qingji.diary;

import android.app.Activity;
import android.app.Instrumentation;
import android.os.Bundle;
import java.time.LocalDate;
import java.util.*;
import org.json.JSONObject;

/** In-memory aggregation contracts; never reads or writes user preferences. */
public final class TrendCheck extends Instrumentation {
    @Override public void onCreate(Bundle args){super.onCreate(args);start();}
    @Override public void onStart(){
        Bundle result=new Bundle();
        try{
            leapMonth();partialMeals();zeroAndMissing();weightedAnnual();latestDailyWeight();futureRecords();
            result.putString("result","PASS: leap February, inclusive dates, partial meal sums, unknown-only days, zero versus missing, annual monthly buckets and day-weighted mean, latest daily weight and monthly average, future exclusion and metric isolation");
            finish(Activity.RESULT_OK,result);
        }catch(Throwable error){
            result.putString("result",android.util.Log.getStackTraceString(error));finish(Activity.RESULT_CANCELED,result);
        }
    }
    private static LocalDate date(String value){return LocalDate.parse(value);}
    private static JSONObject row(String kind,String day,String field,Double value,String id)throws Exception{
        JSONObject row=new JSONObject().put("kind",kind).put("date",day).put("id",id);
        if(value!=null)row.put(field,value);return row;
    }
    private static TrendSeries daily(List<JSONObject> rows,String metric,String start,String end,String today){
        return TrendSeries.build(rows,metric,date(start),date(end),false,date(today));
    }
    private static TrendSeries annual(List<JSONObject> rows,String metric,String today){
        return TrendSeries.build(rows,metric,date("2024-01-01"),date("2024-12-31"),true,date(today));
    }
    private static void leapMonth(){
        TrendSeries series=daily(Collections.emptyList(),"kcal","2024-02-01","2024-02-29","2024-02-29");
        check(series.points.size()==29,"leap February must have 29 dates");
        check(series.points.get(0).key.equals("2024-02-01")&&series.points.get(28).key.equals("2024-02-29"),"daily endpoints must be inclusive");
        check(series.points.get(28).value==null&&!series.points.get(28).future,"leap day must remain an available missing day");
        check(series.average==null&&series.latest==null&&series.change==null&&series.recordedDays==0,"empty period must not fabricate statistics");
    }
    private static void partialMeals()throws Exception{
        List<JSONObject> rows=Arrays.asList(
            row("meal","2024-02-29","kcal",500d,"3"),row("meal","2024-02-29","kcal",250d,"2"),
            row("meal","2024-02-29","kcal",null,"1"),row("meal","2024-02-28","kcal",null,"0"),
            row("exercise","2024-02-29","kcal",999d,"4"),row("meal","2024-03-01","kcal",800d,"5"));
        TrendSeries series=daily(rows,"kcal","2024-02-01","2024-02-29","2024-03-02");
        close(series.points.get(28).value,750,"known meals must sum within the date");
        check(series.points.get(28).partial&&series.partial,"unknown meal must mark partial intake");
        check(series.points.get(27).value==null&&series.points.get(27).partial,"unknown-only day must not become zero");
        check(series.recordedDays==1,"unknown-only days must not dilute the mean");close(series.average,750,"partial mean uses the known daily subtotal");
    }
    private static void zeroAndMissing()throws Exception{
        TrendSeries series=daily(Arrays.asList(row("exercise","2024-01-01","burn",0d,"1"),row("exercise","2024-01-03","burn",100d,"2")),"burn","2024-01-01","2024-01-03","2024-01-03");
        close(series.points.get(0).value,0,"recorded zero must be retained");check(series.points.get(1).value==null,"unrecorded day must remain missing");
        check(series.recordedDays==2&&series.latestIndex()==2,"recorded-day count and last selected bucket");close(series.average,50,"zero counts; missing does not");
    }
    private static void weightedAnnual()throws Exception{
        TrendSeries series=annual(Arrays.asList(row("meal","2024-01-01","kcal",1000d,"1"),row("meal","2024-01-02","kcal",2000d,"2"),row("meal","2024-02-01","kcal",3000d,"3")),"kcal","2024-12-31");
        check(series.points.size()==12&&series.points.get(11).key.equals("2024-12"),"annual series must contain all 12 month buckets");
        close(series.points.get(0).value,1500,"January averages its two recorded days");close(series.points.get(1).value,3000,"February averages its one recorded day");
        check(series.points.get(0).recordedDays==2&&series.points.get(1).recordedDays==1,"monthly recorded-day counts");
        check(series.points.get(2).value==null,"missing month must remain missing");
        check(series.recordedDays==3,"annual record count uses days");close(series.average,2000,"annual mean must weight recorded days, not equally average months");
    }
    private static void latestDailyWeight()throws Exception{
        List<JSONObject> rows=Arrays.asList(row("weight","2024-01-01","kg",70d,"30"),row("weight","2024-01-01","kg",72d,"10"),row("weight","2024-01-02","kg",69d,"40"),row("weight","2024-01-01","kg",71d,"20"),row("exercise","2024-01-02","kg",100d,"50"));
        TrendSeries days=daily(rows,"kg","2024-01-01","2024-01-02","2024-01-02");
        close(days.points.get(0).value,70,"daily weight chooses latest id independently of input order");close(days.points.get(1).value,69,"exercise body mass is not a weight entry");
        TrendSeries months=annual(rows,"kg","2024-12-31");close(months.points.get(0).value,69.5,"monthly weight averages last daily measurements");
        close(months.latest,69,"latest weight uses the last recorded date");close(months.change,-1,"period change compares first and last recorded days");check(months.recordedDays==2,"repeated daily weigh-ins count as one day");
    }
    private static void futureRecords()throws Exception{
        List<JSONObject> rows=Arrays.asList(row("meal","2024-02-14","kcal",500d,"1"),row("meal","2024-02-16","kcal",800d,"2"),row("meal","2024-03-01","kcal",900d,"3"),row("meal","invalid-date","kcal",1000d,"4"));
        TrendSeries days=daily(rows,"kcal","2024-02-01","2024-02-29","2024-02-15");
        close(days.points.get(13).value,500,"past value remains visible");check(days.points.get(14).value==null&&!days.points.get(14).future,"today is selectable even without data");
        check(days.points.get(15).value==null&&days.points.get(15).future,"future records must not enter daily totals");
        TrendSeries months=annual(rows,"kcal","2024-02-15");close(months.points.get(1).value,500,"current month ignores future records");
        check(months.points.get(2).value==null&&months.points.get(2).future,"future month is empty and marked future");check(months.recordedDays==1,"invalid and future dates do not enter statistics");
    }
    private static void close(Double actual,double expected,String message){check(actual!=null&&Math.abs(actual-expected)<.00001,message+": "+actual);}
    private static void check(boolean condition,String message){if(!condition)throw new AssertionError(message);}
}
