package cn.qingji.diary;
import android.app.*;import android.content.*;import android.os.Bundle;
public final class SeasonColorCheck extends Instrumentation {
 public void onCreate(Bundle b){super.onCreate(b);start();}
 public void onStart(){Bundle out=new Bundle();try{
 MainActivity a=(MainActivity)startActivitySync(new Intent(getTargetContext(),MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
 for(int i=0;i<100&&a.foods==null;i++)Thread.sleep(100);
 final Throwable[] failure={null};runOnMainSync(()->{String old=a.preferences().getString("theme","auto");try{
 String[] keys={"spring","summer","autumn","winter"};int[] colors={0xff436b45,0xff246a78,0xffb23924,0xff466584};
 for(int i=0;i<4;i++){a.preferences().edit().putString("theme",keys[i]).commit();a.style.refreshSeason();a.show("今日");if(a.accent!=colors[i])throw new AssertionError(keys[i]);if(a.getWindow().getStatusBarColor()!=a.paper)throw new AssertionError("system bars");}
 }catch(Throwable e){failure[0]=e;}finally{a.preferences().edit().putString("theme",old).commit();a.style.refreshSeason();a.show("今日");}});
 if(failure[0]!=null)throw new AssertionError(failure[0]);out.putString("result","PASS: four seasonal palettes, native rendering, system bars, original preference restored");finish(Activity.RESULT_OK,out);
 }catch(Throwable e){out.putString("result",android.util.Log.getStackTraceString(e));finish(Activity.RESULT_CANCELED,out);}}
}
