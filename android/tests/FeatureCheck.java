package cn.qingji.diary;
import android.app.*;
import android.os.Bundle;
import android.content.SharedPreferences;
import org.json.*;
import java.net.URL;
public final class FeatureCheck extends Instrumentation {
 public void onCreate(Bundle args){super.onCreate(args);start();}
 static void check(boolean ok,String message){if(!ok)throw new AssertionError(message);}
 interface Action {void run()throws Exception;}
 static void rejects(Action action)throws Exception{boolean rejected=false;try{action.run();}catch(Exception e){rejected=true;}check(rejected,"invalid input accepted");}
 public void onStart(){Bundle out=new Bundle();try{
  SharedPreferences p=getTargetContext().getSharedPreferences("feature-tests",0);p.edit().clear().commit();
  ChallengeStore s=new ChallengeStore(p);String id=s.create("散步100次");
  s.setCell(id,0,true,"first");String at=s.get(id).getJSONObject("cells").getJSONObject("0").getString("at");
  s.setCell(id,0,true,"edited");check(s.get(id).getJSONObject("cells").length()==1,"duplicate count");
  check(s.get(id).getJSONObject("cells").getJSONObject("0").getString("at").equals(at),"timestamp changed");
  for(int i=1;i<100;i++)s.setCell(id,i,true,"same-day");
  check(ChallengeStore.next(s.get(id))==-1,"100 cap");rejects(()->s.setCell(id,100,true,""));
  s.setCell(id,42,false,"");check(ChallengeStore.next(s.get(id))==42,"undo");
  s.archive(id,true);rejects(()->s.setCell(id,42,true,""));s.archive(id,false);s.setCell(id,42,true,"");
  s.rename(id,"new name");ChallengeStore reopened=new ChallengeStore(p);
  check(reopened.get(id).getJSONObject("cells").length()==100,"persisted count");check(reopened.get(id).getString("title").equals("new name"),"rename");
  rejects(()->s.create("  "));p.edit().clear().commit();
  JSONObject meta=new JSONObject().put("version","0.5.0").put("versionCode",5).put("bytes",123).put("sha256",new String(new char[64]).replace('\0','a')).put("url",AppUpdater.ROOT+"/download/v0.5.0/qingji-0.5.0.apk");
  check(AppUpdater.parse(meta.toString()).code==5,"valid manifest");
  meta.put("url","https://evil.example/app.apk");rejects(()->AppUpdater.parse(meta.toString()));
  meta.put("url",AppUpdater.ROOT+"/download/v0.5.0/qingji-0.5.0.apk").put("bytes",AppUpdater.MAX_APK+1);rejects(()->AppUpdater.parse(meta.toString()));
  check(!AppUpdater.allowed(new URL("http://github.com/a")),"cleartext");check(!AppUpdater.allowed(new URL("https://github.com.evil.example/a")),"host spoof");check(!AppUpdater.allowed(new URL("https://user@github.com/a")),"userinfo");
  AppUpdater updater=AppUpdater.get(getTargetContext());java.io.File installed=new java.io.File(getTargetContext().getApplicationInfo().sourceDir);
  AppUpdater.Release same=new AppUpdater.Release(updater.currentVersion(),updater.currentCode(),"",installed.length(),AppUpdater.digest(installed),"");
  rejects(()->updater.verify(installed,same));
  AppUpdater.Release tampered=new AppUpdater.Release("9.0.0",999,"",installed.length(),new String(new char[64]).replace('\0','0'),"");
  rejects(()->updater.verify(installed,tampered));
  out.putString("result","PASS: challenge 100 cells, repeated edit, timestamps, undo, archive, reopen, persistence, invalid input; update manifest, URL restrictions, downgrade and hash rejection");finish(Activity.RESULT_OK,out);
 }catch(Throwable e){out.putString("result",android.util.Log.getStackTraceString(e));finish(Activity.RESULT_CANCELED,out);}}
}
