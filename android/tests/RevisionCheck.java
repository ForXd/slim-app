package cn.qingji.diary;
import android.app.*;
import android.os.Bundle;
import java.time.*;

/** Regression at plan, calendar and season boundaries; no stored user data is touched. */
public final class RevisionCheck extends Instrumentation {
 @Override public void onCreate(Bundle args){super.onCreate(args);start();}
 @Override public void onStart(){Bundle out=new Bundle();try{
  PlanMath.Estimate female=PlanMath.estimate(165,60,30,PlanMath.Sex.FEMALE,PlanMath.Activity.SEDENTARY);
  near(female.resting,1320.25);near(female.maintenance,1584);
  PlanMath.Choice mild=PlanMath.choice(female,PlanMath.Mode.MILD,0);near(mild.target,1334);near(mild.deficit,250);near(mild.weekKg,250.0*7/7700);
  PlanMath.Choice standard=PlanMath.choice(female,PlanMath.Mode.STANDARD,0);near(standard.target,1200);near(standard.deficit,384);near(standard.fourWeeksKg,384.0*28/7700);
  PlanMath.Estimate male=PlanMath.estimate(165,60,30,PlanMath.Sex.MALE,PlanMath.Activity.SEDENTARY);
  near(male.maintenance,1784);near(PlanMath.choice(male,PlanMath.Mode.STANDARD,0).target,1500);
  PlanMath.Estimate under=PlanMath.estimate(170,50,30,PlanMath.Sex.FEMALE,PlanMath.Activity.SEDENTARY);
  check(!under.allowsReduction,"low BMI cannot offer automatic deficit");near(PlanMath.choice(under,PlanMath.Mode.MILD,0).deficit,0);
  PlanMath.Estimate low=PlanMath.estimate(150,50,78,PlanMath.Sex.FEMALE,PlanMath.Activity.SEDENTARY);
  check(low.maintenance<1200,"low maintenance fixture");near(PlanMath.choice(low,PlanMath.Mode.STANDARD,0).deficit,0);
  near(PlanMath.choice(female,PlanMath.Mode.CUSTOM,999).target,999);
  check(PlanMath.choice(female,PlanMath.Mode.CUSTOM,1800).weekKg<0,"surplus cannot imply weight loss");
  rejects(()->PlanMath.customTarget(0));rejects(()->PlanMath.customTarget(Double.NaN));rejects(()->PlanMath.manualMaintenance(Double.POSITIVE_INFINITY));
  rejects(()->PlanMath.estimate(165,60,17,PlanMath.Sex.FEMALE,PlanMath.Activity.SEDENTARY));
  rejects(()->PlanMath.estimate(165,60,30,null,PlanMath.Activity.SEDENTARY));
  rejects(()->PlanMath.estimate(250,350,18,PlanMath.Sex.MALE,PlanMath.Activity.ACTIVE));
  YearMonth leap=YearMonth.of(2024,2);check(JournalCalendar.firstColumn(leap)==3,"Monday-first February offset");
  int count=0;for(int i=0;i<42;i++)if(JournalCalendar.gridDate(leap,i)!=null)count++;
  check(count==29,"leap February has 29 days");check(JournalCalendar.gridDate(leap,31).equals(LocalDate.of(2024,2,29)),"leap day slot");
  LocalDate today=LocalDate.of(2026,11,6);check(!JournalCalendar.canSelect(today.plusDays(1),today),"future selection");
  check(JournalCalendar.canSelect(today,today),"today selection");check(!JournalCalendar.canGoForward(YearMonth.from(today),today),"future month");
  check(JournalCalendar.canGoForward(YearMonth.from(today).minusMonths(1),today),"previous month navigation");
  String[] keys={"winter","spring","summer","autumn","winter"};int[] months={1,3,6,9,12};
  for(int i=0;i<months.length;i++)check(SeasonTheme.resolve("auto",LocalDate.of(2026,months[i],1)).key.equals(keys[i]),"season boundary "+months[i]);
  check(SeasonTheme.resolve("winter",today).key.equals("winter"),"manual season wins");check(SeasonTheme.resolve("invalid",today).key.equals("autumn"),"invalid preference falls back");
  out.putString("result","PASS: Mifflin estimate, actual preset deficit and floors, low BMI, custom below 1500, surplus, invalid inputs; leap calendar and future bounds; automatic/manual season boundaries");finish(Activity.RESULT_OK,out);
 }catch(Throwable e){out.putString("result",android.util.Log.getStackTraceString(e));finish(Activity.RESULT_CANCELED,out);}}
 interface Check{void run();}
 static void rejects(Check action){boolean rejected=false;try{action.run();}catch(IllegalArgumentException e){rejected=true;}check(rejected,"invalid input accepted");}
 static void near(double actual,double expected){check(Math.abs(actual-expected)<.00001,"expected "+expected+", got "+actual);}
 static void check(boolean ok,String text){if(!ok)throw new AssertionError(text);}
}
