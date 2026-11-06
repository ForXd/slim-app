package cn.qingji.diary;
import android.app.*;import android.os.Bundle;import org.json.*;import java.io.*;import java.time.LocalDate;
/** Explicit test-only fixture for the isolated emulator, never invoked by the app. */
public final class UiFixture extends Instrumentation {
 boolean seed;
 public void onCreate(Bundle args){super.onCreate(args);seed="yes".equals(args.getString("seed"));start();}
 public void onStart(){Bundle out=new Bundle();try{
  JournalStore journal=new JournalStore(getTargetContext());ChallengeStore challenges=new ChallengeStore(getTargetContext().getSharedPreferences("challenges-v1",0));
  if(seed){
   if(journal.rows().length()!=0||challenges.all().length()!=0)throw new AssertionError("Refusing to overwrite existing fixture data");
   journal.prefs.edit().putBoolean("migrated",true).putString("plan",new JSONObject().put("name","自定义目标").put("target",1200).put("base",1800).toString()).commit();
   journal.save(new JSONObject().put("kind","weight").put("date",LocalDate.now().minusDays(1).toString()).put("kg",68.6));
   journal.save(new JSONObject().put("kind","weight").put("date",JournalStore.today()).put("kg",68.2));
   String photo="00000000-0000-0000-0000-000000000001.jpg";File dir=new File(getTargetContext().getFilesDir(),"meal-photos");dir.mkdirs();
   try(InputStream in=getContext().getAssets().open("kung-pao.jpg");OutputStream f=new FileOutputStream(new File(dir,photo))){byte[] b=new byte[8192];int n;while((n=in.read(b))!=-1)f.write(b,0,n);}
   journal.save(new JSONObject().put("kind","meal").put("date",JournalStore.today()).put("food","宫保鸡丁配米饭").put("group","午餐").put("portion","一份").put("kcal",518).put("photo",photo));
   String walk=challenges.create("散步100次");for(int i=0;i<12;i++)challenges.setCell(walk,i,true,"晚饭后的散步");
   String cook=challenges.create("自己做饭100次");for(int i=0;i<6;i++)challenges.setCell(cook,i,true,"认真吃饭");
   challenges.create("早睡100次");challenges.create("Walk 100");String archived=challenges.create("喝水100次");challenges.archive(archived,true);
  }
  if(journal.rows().length()<3||journal.plan().getInt("target")!=1200||challenges.all().length()<5)throw new AssertionError("Fixture data lost during upgrade");
  out.putString("result","PASS: persisted journal, 1200 kcal target, photo reference and 5 challenges");finish(Activity.RESULT_OK,out);
 }catch(Throwable e){out.putString("result",android.util.Log.getStackTraceString(e));finish(Activity.RESULT_CANCELED,out);}}
}
