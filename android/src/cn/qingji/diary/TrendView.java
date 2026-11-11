package cn.qingji.diary;

import android.content.Context;
import android.graphics.*;
import android.os.Bundle;
import android.view.*;
import android.view.accessibility.AccessibilityNodeInfo;
import java.text.DecimalFormat;
import java.util.*;

/** Native chart: missing values are gaps; tapping or scrubbing selects an actual date bucket. */
final class TrendView extends View {
    interface Selection {void changed(int index);}
    final TrendSeries series;
    private final Selection listener;
    private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
    private final float density,textScale;
    private final int slop;
    private final DecimalFormat number=new DecimalFormat("0.#");
    private int selected;
    private float left,right,top,bottom,downX,downY;
    private boolean scrubbing,vertical;
    private double low,high;
    TrendView(Context context,TrendSeries series,Selection listener){
        super(context);this.series=series;this.listener=listener;selected=series.latestIndex();
        density=getResources().getDisplayMetrics().density;textScale=getResources().getDisplayMetrics().scaledDensity;
        slop=ViewConfiguration.get(context).getScaledTouchSlop();setFocusable(true);setClickable(true);setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_YES);describe();
    }
    TrendView(Context c,List<Double> values,List<String> dates,boolean weight){this(c,TrendSeries.legacy(values,dates,weight),null);}
    private float dp(float value){return density*value;}
    void select(int index){
        if(index<0||index>=series.points.size()||index==selected)return;
        selected=index;describe();invalidate();if(listener!=null)listener.changed(index);
    }
    private void describe(){
        String value="趋势图";
        if(selected>=0&&selected<series.points.size()){
            TrendSeries.Point p=series.points.get(selected);
            value+="，"+p.key+"，"+(p.value==null?(p.future?"尚未到来":p.partial?"数值待补":"暂无记录"):number.format(p.value)+(series.metric.equals("kg")?"千克":"千卡"))+(p.partial?"，含待补记录":"");
        }
        setContentDescription(value+"；左右探索日期");
    }
    private static double niceStep(double value){
        if(value<=0||!Double.isFinite(value))return 1;
        double unit=Math.pow(10,Math.floor(Math.log10(value))),scaled=value/unit;
        return (scaled<=1?1:scaled<=2?2:scaled<=5?5:10)*unit;
    }
    @Override protected void onDraw(Canvas canvas){
        super.onDraw(canvas);boolean weight=series.metric.equals("kg");
        double min=Double.POSITIVE_INFINITY,max=Double.NEGATIVE_INFINITY;
        for(TrendSeries.Point p:series.points)if(p.value!=null){min=Math.min(min,p.value);max=Math.max(max,p.value);}
        boolean hasData=Double.isFinite(min);double axisStep;
        if(!hasData){low=0;high=weight?1:1000;axisStep=high/2;}
        else if(weight){double step=niceStep(Math.max(1,max-min)/3);low=Math.max(0,Math.floor((min-step*.5)/step)*step);high=Math.ceil((max+step*.5)/step)*step;if(high<=low)high=low+step;axisStep=step;}
        else {low=0;double step=niceStep(Math.max(1,max)/3);high=Math.max(step,Math.ceil(max/step)*step);axisStep=step;}
        paint.setTypeface(Typeface.create("sans-serif",Typeface.NORMAL));paint.setTextSize(10*textScale);paint.setStyle(Paint.Style.FILL);
        left=paint.measureText(number.format(high))+dp(10);right=getWidth()-dp(8);top=dp(20);bottom=getHeight()-dp(30);
        if(right<=left||bottom<=top)return;
        paint.setStrokeWidth(dp(.6f));
        int ticks=Math.max(1,(int)Math.round((high-low)/axisStep));
        for(int i=0;i<=ticks;i++){
            float y=top+(bottom-top)*i/ticks;paint.setColor(0xffe4d4bf);canvas.drawLine(left,y,right,y,paint);
            paint.setColor(0xff826955);paint.setTextAlign(Paint.Align.RIGHT);canvas.drawText(number.format(high-axisStep*i),left-dp(8),y+dp(3),paint);
        }
        int count=series.points.size();if(count==0)return;
        float step=(right-left)/count;
        if(selected>=0){
            float x=x(selected,step);paint.setColor(0x10b23924);canvas.drawRoundRect(x-step*.47f,top-dp(7),x+step*.47f,bottom,dp(5),dp(5),paint);
        }
        if(weight){
            Float previousX=null,previousY=null;
            for(int i=0;i<count;i++){
                Double value=series.points.get(i).value;if(value==null){previousX=previousY=null;continue;}
                float x=x(i,step),y=y(value);paint.setColor(((MainActivity)getContext()).accent);paint.setStrokeWidth(dp(2.2f));
                if(previousX!=null)canvas.drawLine(previousX,previousY,x,y,paint);
                paint.setColor(i==selected?((MainActivity)getContext()).accent:((MainActivity)getContext()).accent);canvas.drawCircle(x,y,dp(i==selected?4.5f:3),paint);
                if(i==selected){paint.setColor(0xfffff8ee);canvas.drawCircle(x,y,dp(1.6f),paint);}
                previousX=x;previousY=y;
            }
        }else{
            float bar=Math.min(dp(22),Math.max(dp(2),step*.58f));
            for(int i=0;i<count;i++){
                TrendSeries.Point p=series.points.get(i);if(p.value==null)continue;
                float x=x(i,step),y=Math.min(bottom-dp(2),y(p.value));paint.setColor(i==selected?((MainActivity)getContext()).accent:(0x88000000|(((MainActivity)getContext()).accent&0xffffff)));
                canvas.drawRoundRect(x-bar/2,y,x+bar/2,bottom,dp(3),dp(3),paint);
                if(p.partial){paint.setColor(0xff826955);paint.setTextAlign(Paint.Align.CENTER);canvas.drawText("*",x,Math.max(top-dp(5),y-dp(3)),paint);}
            }
        }
        paint.setTextAlign(Paint.Align.CENTER);paint.setTextSize(10*textScale);paint.setColor(0xff826955);
        Set<Integer> labels=new LinkedHashSet<>();
        if(count<=7){for(int i=0;i<count;i++)labels.add(i);}else{labels.add(0);labels.add((count-1)/3);labels.add((count-1)*2/3);labels.add(count-1);}
        for(int i:labels){
            String label=series.points.get(i).label;float half=paint.measureText(label)/2;
            float center=Math.max(half+dp(2),Math.min(getWidth()-half-dp(2),x(i,step)));
            canvas.drawText(label,center,getHeight()-dp(8),paint);
        }
        if(!hasData){
            paint.setColor(0xff826955);paint.setTextSize(14*textScale);canvas.drawText(series.partial?"记录数值待补":"这段时间还没有记录",(left+right)/2,(top+bottom)/2,paint);
        }
    }
    private float x(int index,float step){return left+step*(index+.5f);}
    private float y(double value){return bottom-(float)((value-low)/(high-low))*(bottom-top);}
    private int hit(float x){if(series.points.isEmpty()||right<=left)return -1;return Math.max(0,Math.min(series.points.size()-1,(int)((x-left)/(right-left)*series.points.size())));}
    private void parent(boolean locked){if(getParent()!=null)getParent().requestDisallowInterceptTouchEvent(locked);}
    @Override public boolean onTouchEvent(MotionEvent event){
        switch(event.getActionMasked()){
            case MotionEvent.ACTION_DOWN:downX=event.getX();downY=event.getY();scrubbing=false;vertical=false;parent(true);return true;
            case MotionEvent.ACTION_MOVE:
                float dx=event.getX()-downX,dy=event.getY()-downY;
                if(!scrubbing&&Math.abs(dy)>slop&&Math.abs(dy)>Math.abs(dx)){vertical=true;parent(false);}
                if(!vertical&&Math.abs(dx)>slop&&Math.abs(dx)>Math.abs(dy)*1.2f){scrubbing=true;select(hit(event.getX()));}
                return true;
            case MotionEvent.ACTION_UP:
                parent(false);if(!vertical){select(hit(event.getX()));performClick();}scrubbing=false;return true;
            case MotionEvent.ACTION_CANCEL:parent(false);scrubbing=false;return true;
            default:return true;
        }
    }
    @Override public boolean performClick(){super.performClick();performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK);return true;}
    @Override public void onInitializeAccessibilityNodeInfo(AccessibilityNodeInfo info){
        super.onInitializeAccessibilityNodeInfo(info);info.setScrollable(series.points.size()>1);
        if(selected>0)info.addAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_SCROLL_BACKWARD);
        if(selected<series.points.size()-1)info.addAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_SCROLL_FORWARD);
    }
    @Override public boolean performAccessibilityAction(int action,Bundle args){
        if(action==AccessibilityNodeInfo.ACTION_SCROLL_FORWARD&&selected<series.points.size()-1){select(selected+1);announceForAccessibility(getContentDescription());return true;}
        if(action==AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD&&selected>0){select(selected-1);announceForAccessibility(getContentDescription());return true;}
        return super.performAccessibilityAction(action,args);
    }
}
