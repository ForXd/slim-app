package cn.qingji.diary;
import android.app.*;import android.os.Bundle;import android.content.*;import org.json.*;
public final class WeightCheck extends Instrumentation {
 public void onCreate(Bundle b){super.onCreate(b);start();}
 static int count(android.view.View v,String text){int n=v instanceof android.widget.TextView&&((android.widget.TextView)v).getText().toString().equals(text)?1:0;if(v instanceof android.view.ViewGroup){android.view.ViewGroup g=(android.view.ViewGroup)v;for(int i=0;i<g.getChildCount();i++)n+=count(g.getChildAt(i),text);}return n;}
 public void onStart(){Bundle out=new Bundle();try{
 Context isolated=new ContextWrapper(getTargetContext()) {public SharedPreferences getSharedPreferences(String n,int m){return super.getSharedPreferences("weight-regression",m);}};
 JournalStore s=new JournalStore(isolated);s.prefs.edit().clear().commit();
 s.save(new JSONObject().put("kind","weight").put("date","2026-11-07").put("kg",68.15));
 s.save(new JSONObject().put("kind","weight").put("date","2026-11-07").put("kg",68.15));
 if(s.rows().length()!=1)throw new AssertionError("same-day weight duplicated: "+s.rows().length());
 s.save(new JSONObject().put("kind","weight").put("date","2026-11-07").put("kg",68.2));
 if(s.rows().length()!=1||s.rows().getJSONObject(0).getDouble("kg")!=68.2)throw new AssertionError("same-day update failed");
 s.save(new JSONObject().put("kind","weight").put("date","2026-11-06").put("kg",69));
 for(int i=0;i<2;i++)s.save(new JSONObject().put("kind","meal").put("date","2026-11-07").put("food","米饭"));
 if(s.rows().length()!=4)throw new AssertionError("other dates or meals lost");
 s.save(new JSONObject().put("id","legacy-a").put("kind","weight").put("date","2026-11-07").put("kg",68.15));
 s.save(new JSONObject().put("id","legacy-b").put("kind","weight").put("date","2026-11-07").put("kg",68.15));
 MainActivity a=(MainActivity)startActivitySync(new Intent(getTargetContext(),MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
 for(int i=0;i<100&&a.body==null;i++)Thread.sleep(100);
 final Throwable[] failure={null};runOnMainSync(()->{JournalStore old=a.store;android.widget.LinearLayout body=a.body;String date=a.selectedDate;try{
 a.store=s;a.body=new android.widget.LinearLayout(a);a.selectedDate="2026-11-07";new HomeScreen(a).render();
 if(count(a.body,"体重 68.15 kg")!=1||count(a.body,"查看当日全部体重（3条）")!=1)throw new AssertionError("legacy duplicate cards not collapsed");
 if(s.rows().length()!=6)throw new AssertionError("legacy rows deleted");
 }catch(Throwable e){failure[0]=e;}finally{a.store=old;a.body=body;a.selectedDate=date;}});
 if(failure[0]!=null)throw new AssertionError(failure[0]);
 s.prefs.edit().clear().commit();out.putString("result","PASS: same-day weight upsert, other dates and multiple meals preserved");finish(Activity.RESULT_OK,out);
 }catch(Throwable e){out.putString("result",android.util.Log.getStackTraceString(e));finish(Activity.RESULT_CANCELED,out);}}
}
