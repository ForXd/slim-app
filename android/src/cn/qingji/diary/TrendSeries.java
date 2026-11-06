package cn.qingji.diary;

import org.json.JSONObject;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;

/** Read-only daily series; monthly buckets average recorded days, never fill missing days with zero. */
final class TrendSeries {
    static final class Point {
        final String key,label;
        final Double value;
        final boolean partial,future;
        final int recordedDays;
        Point(String key,String label,Double value,boolean partial,boolean future,int days){
            this.key=key;this.label=label;this.value=value;this.partial=partial;this.future=future;recordedDays=days;
        }
    }
    private static final class Day {
        Double value;boolean partial;String latestId="";
    }
    final List<Point> points;
    final String metric;
    final boolean monthly;
    final int recordedDays;
    final Double average,latest,change;
    final boolean partial;
    private TrendSeries(List<Point> points,String metric,boolean monthly,int days,Double average,Double latest,Double change,boolean partial){
        this.points=Collections.unmodifiableList(points);this.metric=metric;this.monthly=monthly;recordedDays=days;this.average=average;this.latest=latest;this.change=change;this.partial=partial;
    }
    static TrendSeries build(List<JSONObject> records,String metric,LocalDate start,LocalDate end,boolean monthly,LocalDate today){
        if(end.isBefore(start))throw new IllegalArgumentException("Invalid trend date range");
        Map<LocalDate,Day> days=new TreeMap<>();
        String kind=metric.equals("kg")?"weight":metric.equals("burn")?"exercise":"meal";
        for(JSONObject row:records){
            if(row==null||!kind.equals(row.optString("kind")))continue;
            LocalDate date;try{date=LocalDate.parse(row.optString("date"));}catch(Exception ignored){continue;}
            if(date.isBefore(start)||date.isAfter(end)||date.isAfter(today))continue;
            Day day=days.get(date);if(day==null){day=new Day();days.put(date,day);}
            if(!JournalStore.known(row,metric)||row.optDouble(metric)<0){day.partial=true;continue;}
            double value=row.optDouble(metric);
            if(metric.equals("kg")){
                String id=row.optString("id");
                if(day.value==null||id.compareTo(day.latestId)>0){day.value=value;day.latestId=id;}
            }else day.value=(day.value==null?0:day.value)+value;
        }
        int recorded=0;double total=0;Double first=null,last=null;boolean partial=false;
        for(Day day:days.values()){
            partial|=day.partial;
            if(day.value!=null){recorded++;total+=day.value;if(first==null)first=day.value;last=day.value;}
        }
        List<Point> points=new ArrayList<>();
        if(monthly){
            YearMonth finish=YearMonth.from(end);
            for(YearMonth month=YearMonth.from(start);!month.isAfter(finish);month=month.plusMonths(1)){
                int count=0;double sum=0;boolean incomplete=false;
                for(int d=1;d<=month.lengthOfMonth();d++){
                    Day day=days.get(month.atDay(d));if(day==null)continue;
                    incomplete|=day.partial;if(day.value!=null){sum+=day.value;count++;}
                }
                points.add(new Point(month.toString(),month.getMonthValue()+"月",count==0?null:sum/count,incomplete,month.atDay(1).isAfter(today),count));
            }
        }else{
            for(LocalDate date=start;!date.isAfter(end);date=date.plusDays(1)){
                Day day=days.get(date);
                points.add(new Point(date.toString(),date.getMonthValue()+"/"+date.getDayOfMonth(),day==null?null:day.value,day!=null&&day.partial,date.isAfter(today),day!=null&&day.value!=null?1:0));
            }
        }
        return new TrendSeries(points,metric,monthly,recorded,recorded==0?null:total/recorded,last,first==null||recorded<2?null:last-first,partial);
    }
    int latestIndex(){for(int i=points.size()-1;i>=0;i--)if(points.get(i).value!=null)return i;for(int i=points.size()-1;i>=0;i--)if(!points.get(i).future)return i;return points.isEmpty()?-1:0;}
    static TrendSeries legacy(List<Double> values,List<String> dates,boolean weight){
        List<Point> points=new ArrayList<>();int count=0;double total=0;Double first=null,last=null;
        for(int i=0;i<values.size();i++){
            Double value=values.get(i);if(value!=null&&!Double.isFinite(value))value=null;
            String date=dates.get(i);points.add(new Point(date,date.length()>=10?date.substring(5):date,value,false,false,value==null?0:1));
            if(value!=null){count++;total+=value;if(first==null)first=value;last=value;}
        }
        return new TrendSeries(points,weight?"kg":"kcal",false,count,count==0?null:total/count,last,count<2?null:last-first,false);
    }
}
