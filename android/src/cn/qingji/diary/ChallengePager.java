package cn.qingji.diary;

import android.content.Context;
import android.view.*;
import android.widget.*;
import android.os.*;

/** Native fling/scroll physics, with one selection callback after the strip settles. */
final class ChallengePager extends HorizontalScrollView {
    interface Selection {void changed(int index);}
    final LinearLayout strip;final Handler handler=new Handler(Looper.getMainLooper());
    Selection selection;int selected=-1,cardWidth,count;boolean touching,initial=true;
    final Runnable settle=()->snap();
    ChallengePager(Context c){super(c);setHorizontalScrollBarEnabled(false);setOverScrollMode(OVER_SCROLL_NEVER);setFillViewport(false);setClipToPadding(false);strip=new LinearLayout(c);addView(strip);}
    void configure(int count,int index,Selection callback){this.count=count;selected=index;selection=callback;initial=true;requestLayout();}
    @Override protected void onSizeChanged(int w,int h,int oldw,int oldh){super.onSizeChanged(w,h,oldw,oldh);if(w<=0)return;
        cardWidth=Math.max(1,w-(int)(getResources().getDisplayMetrics().density*26));
        initial=true;post(()->{
            for(int i=0;i<strip.getChildCount();i++){LinearLayout.LayoutParams p=(LinearLayout.LayoutParams)strip.getChildAt(i).getLayoutParams();p.width=cardWidth;strip.getChildAt(i).setLayoutParams(p);}
            strip.setPadding(0,0,w-cardWidth,0);
            post(()->{scrollTo(Math.max(0,selected)*cardWidth,0);initial=false;});
        });
    }
    void select(int index){if(index<0||index>=count)return;handler.removeCallbacks(settle);if(android.animation.ValueAnimator.areAnimatorsEnabled())smoothScrollTo(index*cardWidth,0);else scrollTo(index*cardWidth,0);handler.postDelayed(settle,180);}
    @Override public boolean onInterceptTouchEvent(MotionEvent e){if(e.getActionMasked()==MotionEvent.ACTION_DOWN){touching=true;handler.removeCallbacks(settle);}if(e.getActionMasked()==MotionEvent.ACTION_UP||e.getActionMasked()==MotionEvent.ACTION_CANCEL){touching=false;handler.postDelayed(settle,120);}return super.onInterceptTouchEvent(e);}
    @Override public boolean onTouchEvent(MotionEvent e){
        if(e.getActionMasked()==MotionEvent.ACTION_DOWN){touching=true;handler.removeCallbacks(settle);}
        boolean handled=super.onTouchEvent(e);
        if(e.getActionMasked()==MotionEvent.ACTION_UP||e.getActionMasked()==MotionEvent.ACTION_CANCEL){touching=false;handler.postDelayed(settle,120);}
        return handled;
    }
    @Override protected void onScrollChanged(int x,int y,int ox,int oy){super.onScrollChanged(x,y,ox,oy);if(!initial&&!touching){handler.removeCallbacks(settle);handler.postDelayed(settle,120);}}
    void snap(){if(touching||initial||cardWidth==0||count==0)return;int index=Math.max(0,Math.min(count-1,Math.round(getScrollX()/(float)cardWidth)));
        if(Math.abs(getScrollX()-index*cardWidth)>1){if(android.animation.ValueAnimator.areAnimatorsEnabled())smoothScrollTo(index*cardWidth,0);else scrollTo(index*cardWidth,0);}
        if(index!=selected){selected=index;if(selection!=null)selection.changed(index);}
    }
    @Override protected void onDetachedFromWindow(){handler.removeCallbacksAndMessages(null);super.onDetachedFromWindow();}
}
