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
    final Bitmap paper,branch;
    final int muted=0xff826955,line=0xffe4d4bf;
    JournalStyle(MainActivity app){
        this.app=app;serif=Typeface.createFromAsset(app.getAssets(),"ui/journal-serif.otf");
        paper=asset("paper.png",2);branch=asset("tomato-branch.png",2);
    }
    Bitmap asset(String name,int sample){try(InputStream in=app.getAssets().open("ui/"+name)){BitmapFactory.Options o=new BitmapFactory.Options();o.inSampleSize=sample;return BitmapFactory.decodeStream(in,null,o);}catch(IOException e){return null;}}
    Drawable paper(){
        if(paper==null)return new ColorDrawable(app.paper);
        BitmapDrawable texture=new BitmapDrawable(app.getResources(),paper);texture.setTileModeXY(Shader.TileMode.REPEAT,Shader.TileMode.REPEAT);texture.setAlpha(145);
        return new LayerDrawable(new Drawable[]{new ColorDrawable(app.paper),texture});
    }
    GradientDrawable surface(int color,int radius,boolean border){GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(app.dp(radius));if(border)d.setStroke(1,line);return d;}
    Drawable ripple(int color,int radius,boolean border){return new RippleDrawable(ColorStateList.valueOf(0x22b23924),surface(color,radius,border),null);}
    Drawable icon(String name,int color,int dp){
        int id=app.getResources().getIdentifier("ic_"+name,"drawable",app.getPackageName());
        Drawable d=app.getDrawable(id).mutate();d.setTint(color);d.setBounds(0,0,app.dp(dp),app.dp(dp));return d;
    }
    ImageView image(String name,int color,int size){ImageView i=new ImageView(app);i.setImageDrawable(icon(name,color,size));i.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);return i;}
    TextView label(String value,int size,boolean bold,int color){TextView t=new TextView(app);t.setText(value);t.setTextSize(size);t.setTextColor(color);t.setTypeface(serif,bold?Typeface.BOLD:Typeface.NORMAL);t.setIncludeFontPadding(false);t.setLineSpacing(app.dp(3),1);return t;}
    Button action(LinearLayout parent,String label,String icon,Runnable run){
        Button b=app.button(parent,label,run);b.setTextColor(0xfffff9ef);b.setTypeface(serif,Typeface.BOLD);b.setTextSize(21);b.setBackground(ripple(app.accent,13,false));b.setCompoundDrawables(icon(icon,0xfffff9ef,30),null,null,null);b.setCompoundDrawablePadding(app.dp(9));
        b.setPadding(app.dp(18),app.dp(10),app.dp(14),app.dp(10));return b;
    }
    void space(LinearLayout p,int height){p.addView(new View(app),new LinearLayout.LayoutParams(1,app.dp(height)));}
    void line(LinearLayout p){View v=new View(app);v.setBackgroundColor(line);p.addView(v,new LinearLayout.LayoutParams(-1,app.dp(1)));}
}
