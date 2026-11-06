package cn.qingji.diary;

import android.content.*;
import android.content.pm.*;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import org.json.JSONObject;
import javax.net.ssl.HttpsURLConnection;
import java.io.*;
import java.net.URL;
import java.security.MessageDigest;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

final class AppUpdater {
    static final String ROOT="https://github.com/ForXd/slim-app/releases";
    static final long MAX_APK=128L*1024*1024;
    private static AppUpdater instance;
    static synchronized AppUpdater get(Context context){if(instance==null)instance=new AppUpdater(context.getApplicationContext());return instance;}
    final Context context;
    final android.content.SharedPreferences prefs;
    final ExecutorService worker=Executors.newSingleThreadExecutor();
    final Handler main=new Handler(Looper.getMainLooper());
    volatile String message="打开应用后自动检查更新";
    volatile boolean busy;
    volatile boolean cancelled;
    volatile HttpsURLConnection connection;
    volatile Release release;
    volatile File ready;
    private Runnable listener;
    static final class Release {
        final String version,url,hash,notes;
        final long code,bytes;
        Release(String version,long code,String url,long bytes,String hash,String notes){this.version=version;this.code=code;this.url=url;this.bytes=bytes;this.hash=hash;this.notes=notes;}
    }
    private AppUpdater(Context context){this.context=context;prefs=context.getSharedPreferences("app-updates",0);}
    void observe(Runnable listener){this.listener=listener;if(listener!=null)listener.run();}
    void changed(String text){message=text;main.post(()->{if(listener!=null)listener.run();});}
    boolean automatic(){return prefs.getBoolean("automatic",true);}
    void automatic(boolean enabled){prefs.edit().putBoolean("automatic",enabled).apply();}
    long currentCode()throws Exception{return context.getPackageManager().getPackageInfo(context.getPackageName(),0).getLongVersionCode();}
    String currentVersion(){try{return context.getPackageManager().getPackageInfo(context.getPackageName(),0).versionName;}catch(Exception e){return "?";}}
    static Release parse(String body)throws Exception {
        if(body.length()>64000)throw new IOException("更新清单过大");
        JSONObject j=new JSONObject(body);
        Release r=new Release(j.getString("version"),j.getLong("versionCode"),j.getString("url"),j.getLong("bytes"),j.getString("sha256"),j.optString("notes",""));
        if(!r.version.matches("[0-9]{1,5}\\.[0-9]{1,5}\\.[0-9]{1,5}")||r.code<=0||r.bytes<=0||r.bytes>MAX_APK||!r.hash.matches("[a-f0-9]{64}")||r.notes.length()>4000)throw new IOException("更新清单无效");
        if(!r.url.equals(ROOT+"/download/v"+r.version+"/qingji-"+r.version+".apk"))throw new IOException("更新来源不匹配");
        return r;
    }
    static boolean allowed(URL url){
        String host=url.getHost();
        return url.getProtocol().equals("https")&&url.getUserInfo()==null&&(url.getPort()==-1||url.getPort()==443)
            &&(host.equals("github.com")||host.equals("release-assets.githubusercontent.com")||host.equals("objects.githubusercontent.com"));
    }
    private HttpsURLConnection open(String address)throws Exception{
        URL url=new URL(address);
        for(int redirects=0;redirects<6;redirects++){
            if(cancelled)throw new IOException("已取消");
            if(!allowed(url))throw new IOException("更新地址无效");
            HttpsURLConnection c=(HttpsURLConnection)url.openConnection();connection=c;
            c.setConnectTimeout(15000);c.setReadTimeout(30000);c.setInstanceFollowRedirects(false);c.setRequestProperty("Cache-Control","no-cache");
            int code=c.getResponseCode();
            if(code>=300&&code<400){String location=c.getHeaderField("Location");c.disconnect();if(location==null)throw new IOException("无效跳转");url=new URL(url,location);continue;}
            if(code!=200){c.disconnect();throw new IOException("更新服务暂不可用（"+code+"）");}
            return c;
        }
        throw new IOException("更新跳转次数过多");
    }
    void check(boolean manual){
        if(busy||ready!=null)return;
        long now=System.currentTimeMillis(),elapsed=now-prefs.getLong("checked",0);
        if(!manual&&(!automatic()||(elapsed>=0&&elapsed<86400000)))return;
        busy=true;cancelled=false;prefs.edit().putLong("checked",now).apply();changed("正在检查新版本…");
        worker.execute(()->{
            try{
                HttpsURLConnection c=open(ROOT+"/latest/download/update.json");
                ByteArrayOutputStream buffer=new ByteArrayOutputStream();
                try(InputStream in=c.getInputStream()){
                    byte[] bytes=new byte[4096];int n;
                    while((n=in.read(bytes))!=-1){if(buffer.size()+n>64000)throw new IOException("更新清单过大");buffer.write(bytes,0,n);}
                }finally{c.disconnect();}
                Release found=parse(buffer.toString("UTF-8"));
                release=found.code>currentCode()?found:null;
                changed(release==null?"已是最新版本 "+currentVersion():"发现新版本 "+found.version);
            }catch(Exception e){changed("检查失败，可手动重试");}
            finally{busy=false;connection=null;changed(message);}
        });
    }
    static String hex(byte[] data){StringBuilder s=new StringBuilder();for(byte b:data)s.append(String.format(Locale.US,"%02x",b&255));return s.toString();}
    static String digest(File file)throws Exception{
        MessageDigest digest=MessageDigest.getInstance("SHA-256");
        try(InputStream in=new FileInputStream(file)){byte[] b=new byte[65536];int n;while((n=in.read(b))!=-1)digest.update(b,0,n);}
        return hex(digest.digest());
    }
    static Set<String> signers(PackageInfo info)throws Exception{
        if(info.signingInfo==null)throw new IOException("安装包签名不可读");
        Set<String> result=new HashSet<>();
        for(android.content.pm.Signature signature:info.signingInfo.getApkContentsSigners())result.add(hex(MessageDigest.getInstance("SHA-256").digest(signature.toByteArray())));
        return result;
    }
    void verify(File file,Release r)throws Exception{
        if(file.length()!=r.bytes||!digest(file).equals(r.hash))throw new IOException("安装包校验失败");
        PackageManager pm=context.getPackageManager();
        PackageInfo installed=pm.getPackageInfo(context.getPackageName(),PackageManager.GET_SIGNING_CERTIFICATES);
        PackageInfo apk=pm.getPackageArchiveInfo(file.getPath(),PackageManager.GET_SIGNING_CERTIFICATES);
        if(apk==null||!apk.packageName.equals(installed.packageName)||!r.version.equals(apk.versionName)||apk.getLongVersionCode()!=r.code||r.code<=installed.getLongVersionCode())throw new IOException("包名或版本不匹配");
        Set<String> expected=signers(installed);
        if(expected.isEmpty()||!expected.equals(signers(apk)))throw new IOException("安装包签名不匹配");
    }
    void download(){
        Release r=release;if(r==null||busy)return;
        busy=true;cancelled=false;ready=null;changed("准备下载 "+r.version);
        worker.execute(()->{
            File dir=new File(context.getFilesDir(),"updates");dir.mkdirs();
            File partial=new File(dir,"download.part"),file=new File(dir,"ready.apk");
            try{
                if(dir.getUsableSpace()<r.bytes*3)throw new IOException("存储空间不足");
                if(file.exists()){
                    try{verify(file,r);ready=file;changed("校验完成，可以安装");return;}catch(Exception ignored){file.delete();}
                }
                HttpsURLConnection c=open(r.url);
                try(InputStream in=c.getInputStream();OutputStream out=new FileOutputStream(partial)){
                    byte[] bytes=new byte[65536];long total=0,previous=-1;int n;
                    long started=System.currentTimeMillis();
                    while((n=in.read(bytes))!=-1){
                        if(cancelled)throw new IOException("下载已取消");
                        if(System.currentTimeMillis()-started>240000)throw new IOException("下载超时，请重试");
                        total+=n;if(total>r.bytes)throw new IOException("文件大小超过清单");
                        out.write(bytes,0,n);long percent=total*100/r.bytes;if(percent!=previous){previous=percent;changed("正在下载 "+percent+"%");}
                    }
                    if(total!=r.bytes)throw new IOException("下载不完整，请重试");
                }finally{c.disconnect();}
                verify(partial,r);
                if(cancelled)throw new IOException("下载已取消");
                if(!partial.renameTo(file))throw new IOException("无法保存安装包");
                ready=file;changed("校验完成，可以安装");
            }catch(Exception e){partial.delete();changed(cancelled?"下载已取消，可重试":e.getMessage()==null?"下载失败，请重试":e.getMessage());}
            finally{busy=false;connection=null;changed(message);}
        });
    }
    void cancel(){cancelled=true;HttpsURLConnection active=connection;if(active!=null)active.disconnect();}
    void install(MainActivity activity){
        File file=ready;Release r=release;if(file==null||r==null||busy)return;
        if(!context.getPackageManager().canRequestPackageInstalls()){
            activity.startActivity(new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,Uri.parse("package:"+context.getPackageName())));
            activity.toast("允许安装后，返回点击安装新版");return;
        }
        busy=true;changed("正在复核安装包…");
        worker.execute(()->{
            try{
                verify(file,r);
                main.post(()->{
                    try{
                        Intent intent=new Intent(Intent.ACTION_VIEW).setDataAndType(UpdatePackage.URI,"application/vnd.android.package-archive");
                        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                        activity.startActivity(intent);changed("请在系统界面确认安装");
                    }catch(Exception e){changed("无法启动安装器，请重试");}
                });
            }catch(Exception e){ready=null;changed(e.getMessage());}
            finally{busy=false;main.post(()->{if(listener!=null)listener.run();});}
        });
    }
}
