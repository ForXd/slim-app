package cn.qingji.diary;
import android.content.Context;
import android.graphics.*;
import android.view.View;
import java.util.*;
final class TrendView extends View {
    final List<Double> values;final List<String> dates;final boolean weight;
    TrendView(Context c,List<Double>v,List<String>d,boolean w){super(c);values=v;dates=d;weight=w;setContentDescription("趋势图，逐日数值列于图表下方");}
    @Override protected void onDraw(Canvas c){
        super.onDraw(c);float density=getResources().getDisplayMetrics().density;
        Paint p=new Paint(3);p.setTextSize(11*density);
        double low=Double.POSITIVE_INFINITY,high=Double.NEGATIVE_INFINITY;
        for(Double v:values)if(v!=null){low=Math.min(low,v);high=Math.max(high,v);}
        if(!Double.isFinite(low)){p.setColor(Color.DKGRAY);c.drawText("这段时间还没有记录",20,70,p);return;}
        low=weight?Math.floor(low)-1:0;high=Math.max(low+1,Math.ceil(high));
        float left=44*density,right=getWidth()-14*density,top=24*density,bottom=getHeight()-36*density;
        for(int i=0;i<3;i++){float y=top+(bottom-top)*i/2;p.setColor(0xffd8cbbd);c.drawLine(left,y,right,y,p);p.setColor(0xff6c5a49);c.drawText(String.valueOf(Math.round(high-(high-low)*i/2)),0,y+4*density,p);}
        Float lastX=null,lastY=null;
        for(int i=0;i<values.size();i++){
            Double v=values.get(i);if(v==null){lastX=lastY=null;continue;}
            float x=left+(right-left)*i/(values.size()-1),y=bottom-(float)((v-low)/(high-low))*(bottom-top);p.setColor(0xff245c47);p.setStrokeWidth(2*density);
            if(weight){if(lastX!=null)c.drawLine(lastX,lastY,x,y,p);c.drawCircle(x,y,3*density,p);}
            else {float width=(right-left)/values.size()*.65f;c.drawRoundRect(x-width/2,y,x+width/2,Math.max(y+2,bottom),3,3,p);}
            lastX=x;lastY=y;
        }
        p.setColor(0xff6c5a49);c.drawText(dates.get(0).substring(5),left,getHeight()-8*density,p);c.drawText(dates.get(dates.size()-1).substring(5),right-38*density,getHeight()-8*density,p);
    }
}
