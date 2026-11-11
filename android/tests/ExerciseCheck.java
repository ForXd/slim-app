package cn.qingji.diary;
import android.app.*;import android.os.Bundle;
public final class ExerciseCheck extends Instrumentation {
 public void onCreate(Bundle b){super.onCreate(b);start();}
 public void onStart(){Bundle b=new Bundle();try{
 if(ExerciseCatalog.estimate(4,60,30)!=95)throw new AssertionError("net energy");
 if(ExerciseCatalog.estimate(1,60,30)!=0)throw new AssertionError("rest subtraction");
 if(ExerciseCatalog.NAMES.length!=23||ExerciseCatalog.MET.length!=23||ExerciseCatalog.CODES.length!=23)throw new AssertionError("catalogue mismatch");
 for(double m:ExerciseCatalog.MET)ExerciseCatalog.estimate(m,60,30);
 for(double m:new double[]{Double.NaN,Double.POSITIVE_INFINITY,0,26}){boolean rejected=false;try{ExerciseCatalog.estimate(m,60,30);}catch(IllegalArgumentException e){rejected=true;}if(!rejected)throw new AssertionError("invalid accepted");}
 if(ExerciseCatalog.index("平地中速步行")!=0||ExerciseCatalog.index("旧版自定义")!=22)throw new AssertionError("legacy mapping");
 if(ExerciseCatalog.intensity(1,20,60)!=8)throw new AssertionError("cycling speed");
 if(ExerciseCatalog.intensity(0,5,60)!=3.8)throw new AssertionError("walking speed");
 if(ExerciseCatalog.intensity(5,8,60)!=8.5)throw new AssertionError("running speed");
 b.putString("result","PASS: 22 presets, custom fallback, net calculation and invalid inputs");finish(Activity.RESULT_OK,b);
 }catch(Throwable e){b.putString("result",android.util.Log.getStackTraceString(e));finish(Activity.RESULT_CANCELED,b);}}
}
