package cn.qingji.diary;

import android.content.Context;
import android.graphics.*;
import android.graphics.drawable.*;
import android.content.res.ColorStateList;
import android.view.*;
import android.widget.*;
import java.io.*;

/** Shared paper, typography and controls for the native journal. Assets decode once per Activity. */
final class JournalStyle {
    final MainActivity app;
    final Typeface serif;
    final Typeface body=Typeface.create("sans-serif",Typeface.NORMAL);
    final Typeface medium=Typeface.create("sans-serif-medium",Typeface.NORMAL);
    final Bitmap paper;
    Bitmap branch;
    private String seasonKey="";
    int muted=0xff826955,line=0xffe4d4bf;
    JournalStyle(MainActivity app){
        this.app=app;serif=Typeface.createFromAsset(app.getAssets(),"ui/journal-serif.otf");
        paper=asset("paper.png",2);refreshSeason();
    }
    boolean refreshSeason(){
        SeasonTheme season=SeasonTheme.resolve(app.preferences().getString("theme","auto"),java.time.LocalDate.now());
        if(season.key.equals(seasonKey))return false;
        Bitmap next=asset(season.asset,2);if(next==null)next=asset("tomato-branch.png",2);
        branch=next;seasonKey=season.key;
        switch(season.key){
            case "spring":app.accent=0xff436b45;app.paper=0xfff3f5e9;app.ink=0xff293b2c;muted=0xff63725b;line=0xffcfd9c6;break;
            case "summer":app.accent=0xff246a78;app.paper=0xffeef6f4;app.ink=0xff263d43;muted=0xff5c7376;line=0xffc8dbdc;break;
            case "winter":app.accent=0xff466584;app.paper=0xfff0f3f7;app.ink=0xff2e3a4a;muted=0xff647386;line=0xffd0d9e4;break;
            default:app.accent=0xffb23924;app.paper=0xfffaf4ea;app.ink=0xff3f2c22;muted=0xff826955;line=0xffe4d4bf;
        }
        app.getWindow().setStatusBarColor(app.paper);app.getWindow().setNavigationBarColor(app.paper);
        if(app.root!=null)app.root.setBackground(paper());return true;
    }
    Bitmap asset(String name,int sample){try(InputStream in=app.getAssets().open("ui/"+name)){BitmapFactory.Options o=new BitmapFactory.Options();o.inSampleSize=sample;return BitmapFactory.decodeStream(in,null,o);}catch(IOException e){return null;}}
    Drawable paper(){
        if(paper==null)return new ColorDrawable(app.paper);
        BitmapDrawable texture=new BitmapDrawable(app.getResources(),paper);texture.setTileModeXY(Shader.TileMode.REPEAT,Shader.TileMode.REPEAT);texture.setAlpha(145);
        return new LayerDrawable(new Drawable[]{new ColorDrawable(app.paper),texture});
    }
    GradientDrawable surface(int color,int radius,boolean border){GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(app.dp(radius));if(border)d.setStroke(1,line);return d;}
    Drawable ripple(int color,int radius,boolean border){return new RippleDrawable(ColorStateList.valueOf(0x22000000|(app.accent&0xffffff)),surface(color,radius,border),null);}
    Drawable icon(String name,int color,int dp){
        int id=app.getResources().getIdentifier("ic_"+name,"drawable",app.getPackageName());
        Drawable d=app.getDrawable(id).mutate();d.setTint(color);d.setBounds(0,0,app.dp(dp),app.dp(dp));return d;
    }
    ImageView image(String name,int color,int size){ImageView i=new ImageView(app);i.setImageDrawable(icon(name,color,size));i.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);return i;}
    TextView label(String value,int size,boolean bold,int color){TextView t=new TextView(app);t.setText(value);t.setTextSize(size);t.setTextColor(color);t.setTypeface(bold?medium:body);t.setLetterSpacing(0);t.setIncludeFontPadding(false);t.setLineSpacing(app.dp(2),1.08f);return t;}
    /** Serif is reserved for editorial headings; the bundled regular font is never faux-bolded. */
    TextView heading(String value,int size,int color){TextView t=label(value,size,false,color);t.setTypeface(serif);t.setLineSpacing(0,1.08f);return t;}
    TextView number(String value,int size,int color){TextView t=label(value,size,true,color);t.setFontFeatureSettings("'tnum' 1, 'lnum' 1");t.setLineSpacing(0,1);return t;}
    Button action(LinearLayout parent,String label,String icon,Runnable run){
        Button b=app.button(parent,label,run);b.setTextColor(0xfffff9ef);b.setTextSize(20);b.setBackground(ripple(app.accent,13,false));b.setCompoundDrawables(icon(icon,0xfffff9ef,30),null,null,null);b.setCompoundDrawablePadding(app.dp(9));
        b.setPadding(app.dp(18),app.dp(10),app.dp(14),app.dp(10));return b;
    }
    void space(LinearLayout p,int height){p.addView(new View(app),new LinearLayout.LayoutParams(1,app.dp(height)));}
    void line(LinearLayout p){View v=new View(app);v.setBackgroundColor(line);p.addView(v,new LinearLayout.LayoutParams(-1,app.dp(1)));}
}
