package cn.qingji.diary;

import android.animation.ValueAnimator;
import android.app.Activity;
import android.app.Instrumentation;
import android.content.Intent;
import android.os.Bundle;
import android.os.SystemClock;
import android.provider.Settings;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import java.util.ArrayList;

/** Real attached-view gesture regressions. Cards are transient; no journal or challenge writes. */
public final class DeckCheck extends Instrumentation {
    private MainActivity activity;
    private FrameLayout host;
    private View original;
    private ViewGroup content;
    private ViewGroup.LayoutParams originalLayout;
    private ChallengePager pager;
    private ScrollView scroll;
    private final ArrayList<Integer> commits=new ArrayList<>();
    private float width,y;
    private long settleWait;

    @Override public void onCreate(Bundle args){super.onCreate(args);start();}

    @Override public void onStart(){
        Bundle result=new Bundle();Throwable failure=null;
        try{
            activity=(MainActivity)startActivitySync(new Intent(getTargetContext(),MainActivity.class)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            awaitReady();
            main(()->{
                check(ValueAnimator.areAnimatorsEnabled(),"DeckCheck requires enabled system animations");
                float scale=Settings.Global.getFloat(getTargetContext().getContentResolver(),
                        Settings.Global.ANIMATOR_DURATION_SCALE,1f);
                settleWait=(long)(320*Math.max(1,scale))+180;
                content=activity.findViewById(android.R.id.content);
                original=content.getChildAt(0);originalLayout=original.getLayoutParams();
                content.removeView(original);host=new FrameLayout(activity);
                content.addView(host,new ViewGroup.LayoutParams(-1,-1));
            });

            fresh(1);
            drag(-.12f,MotionEvent.ACTION_UP);
            settled("short drag springs back",1,0);

            fresh(0);
            drag(-.42f,MotionEvent.ACTION_UP);
            settled("threshold drag advances once",1,1);

            fresh(0);
            main(()->{
                long t=SystemClock.uptimeMillis();
                event(MotionEvent.ACTION_DOWN,.45f*width,t,t);
                event(MotionEvent.ACTION_MOVE,.87f*width,t,t+200);
                check(pager.getChildCount()==3,"first-card outward drag loses the backing stack");
                event(MotionEvent.ACTION_UP,.87f*width,t,t+400);
            });
            settled("first-card boundary",0,0);
            main(()->check(pager.getChildCount()==3,"first-card rebound loses the backing stack"));

            fresh(2);
            drag(-.42f,MotionEvent.ACTION_UP);
            settled("last-card boundary",2,0);

            fresh(0);
            drag(-.42f,MotionEvent.ACTION_CANCEL);
            settled("cancel never commits",0,0);

            fresh(0);
            main(()->{
                long t=SystemClock.uptimeMillis();
                event(MotionEvent.ACTION_DOWN,.80f*width,t,t);
                event(MotionEvent.ACTION_MOVE,.38f*width,t,t+200);
                pointers(MotionEvent.ACTION_POINTER_DOWN|(1<<MotionEvent.ACTION_POINTER_INDEX_SHIFT),t,t+220);
                pointers(MotionEvent.ACTION_MOVE,t,t+240);
                pointers(MotionEvent.ACTION_POINTER_UP|(1<<MotionEvent.ACTION_POINTER_INDEX_SHIFT),t,t+260);
                event(MotionEvent.ACTION_UP,.30f*width,t,t+400);
            });
            settled("multiple pointers never commit",0,0);

            fresh(0);
            final long[] secondDown={0};
            main(()->{
                long t=SystemClock.uptimeMillis();
                event(MotionEvent.ACTION_DOWN,.80f*width,t,t);
                event(MotionEvent.ACTION_MOVE,.38f*width,t,t+200);
                event(MotionEvent.ACTION_UP,.38f*width,t,t+400);
                check(pager.selected==0&&commits.isEmpty(),"selection must wait for its animation");
                secondDown[0]=t+420;
                event(MotionEvent.ACTION_DOWN,.80f*width,secondDown[0],secondDown[0]);
            });
            settled("first animation settles during second touch",1,1);
            main(()->{
                long t=secondDown[0];
                event(MotionEvent.ACTION_MOVE,.25f*width,t,t+settleWait+100);
                event(MotionEvent.ACTION_UP,.25f*width,t,t+settleWait+300);
            });
            settled("touch begun while settling cannot advance later",1,1);
            drag(-.42f,MotionEvent.ACTION_UP);
            settled("fresh touch after ignored gesture still works",2,2);

            fresh(0,true);
            main(()->{
                long t=SystemClock.uptimeMillis();float startY=pager.getHeight()*.72f;
                scrollEvent(MotionEvent.ACTION_DOWN,.80f*width,startY,t,t);
                // Both axes exceed touch slop: the ScrollView must not steal a horizontal gesture.
                scrollEvent(MotionEvent.ACTION_MOVE,.60f*width,startY-pager.getHeight()*.09f,t,t+160);
                scrollEvent(MotionEvent.ACTION_MOVE,.38f*width,startY-pager.getHeight()*.19f,t,t+320);
                check(Math.abs(front().getTranslationX())>width*.3f,"ScrollView stole the diagonal deck drag");
                scrollEvent(MotionEvent.ACTION_UP,.38f*width,startY-pager.getHeight()*.19f,t,t+520);
            });
            settled("horizontal diagonal drag inside ScrollView advances",1,1);
            main(()->check(scroll.getScrollY()==0,"horizontal deck gesture scrolled its parent"));

            fresh(0,true);
            main(()->{
                long t=SystemClock.uptimeMillis();float startY=pager.getHeight()*.78f;
                scrollEvent(MotionEvent.ACTION_DOWN,.55f*width,startY,t,t);
                // The first move releases the parent; subsequent moves must actually scroll it.
                scrollEvent(MotionEvent.ACTION_MOVE,.56f*width,startY-pager.getHeight()*.08f,t,t+140);
                scrollEvent(MotionEvent.ACTION_MOVE,.57f*width,startY-pager.getHeight()*.26f,t,t+280);
                scrollEvent(MotionEvent.ACTION_MOVE,.57f*width,startY-pager.getHeight()*.50f,t,t+420);
                check(scroll.getScrollY()>pager.getHeight()*.05f,"vertical deck gesture did not scroll its parent");
                scrollEvent(MotionEvent.ACTION_UP,.57f*width,startY-pager.getHeight()*.50f,t,t+620);
            });
            settled("vertical gesture inside ScrollView does not switch cards",0,0);
            main(()->check(scroll.getScrollY()>0,"vertical scroll position was lost"));

            fresh(0);
            main(()->{
                pager.select(1);
                check(pager.selected==0&&commits.isEmpty(),"detach scenario must start a live animation");
                host.removeView(pager);
                check(!pager.isAttachedToWindow(),"pager still attached");
            });
            SystemClock.sleep(settleWait);waitForIdleSync();
            main(()->check(pager.selected==0&&commits.isEmpty(),"detached animation committed selection"));
            result.putString("result","PASS: attached deck short drag rebound, threshold advance, first/last boundaries, preserved backing stack, ACTION_CANCEL, multiple pointers, settling-touch race, recovery, ScrollView diagonal drag and vertical scroll arbitration, detached animation cancellation");
        }catch(Throwable error){failure=error;}
        finally{
            if(content!=null&&original!=null){
                try{main(()->{content.removeAllViews();content.addView(original,originalLayout);});}
                catch(Throwable restoreError){if(failure==null)failure=restoreError;}
            }
        }
        if(failure!=null)result.putString("result",android.util.Log.getStackTraceString(failure));
        finish(failure==null?Activity.RESULT_OK:Activity.RESULT_CANCELED,result);
    }

    private void awaitReady(){
        long end=SystemClock.uptimeMillis()+15000;
        boolean[] ready={false};
        while(SystemClock.uptimeMillis()<end){
            main(()->ready[0]=activity.body!=null&&activity.migration==null);
            if(ready[0]){waitForIdleSync();return;}
            SystemClock.sleep(50);
        }
        throw new AssertionError("MainActivity did not finish preparing its content");
    }

    private void fresh(int selected){
        fresh(selected,false);
    }

    private void fresh(int selected,boolean insideScroll){
        main(()->{
            host.removeAllViews();commits.clear();scroll=null;pager=new ChallengePager(activity);
            pager.configure(3,selected,index->{
                TextView card=new TextView(activity);card.setText("Test card "+index);
                card.setGravity(Gravity.CENTER);card.setBackgroundColor(0xfffff4e6);return card;
            },commits::add);
            int height=Math.round(280*activity.getResources().getDisplayMetrics().density);
            if(insideScroll){
                scroll=new ScrollView(activity);scroll.setOverScrollMode(View.OVER_SCROLL_NEVER);
                LinearLayout page=new LinearLayout(activity);page.setOrientation(LinearLayout.VERTICAL);
                page.addView(pager,new LinearLayout.LayoutParams(-1,height));
                TextView below=new TextView(activity);below.setText("Scrollable test content");
                page.addView(below,new LinearLayout.LayoutParams(-1,Math.max(host.getHeight()*2,height*3)));
                scroll.addView(page,new ScrollView.LayoutParams(-1,-2));
                host.addView(scroll,new FrameLayout.LayoutParams(-1,-1));
            }else host.addView(pager,new FrameLayout.LayoutParams(-1,height));
        });
        long end=SystemClock.uptimeMillis()+3000;
        boolean[] laidOut={false};
        while(SystemClock.uptimeMillis()<end){
            main(()->{
                laidOut[0]=pager.isAttachedToWindow()&&pager.getWidth()>0&&pager.getHeight()>0;
                if(laidOut[0]){width=pager.getWidth();y=pager.getHeight()*.35f;}
            });
            if(laidOut[0])return;
            SystemClock.sleep(16);
        }
        throw new AssertionError("test deck was not attached and laid out");
    }

    private void drag(float amount,int endAction){
        main(()->{
            long t=SystemClock.uptimeMillis();float start=amount<0?.8f:.2f;
            event(MotionEvent.ACTION_DOWN,start*width,t,t);
            event(MotionEvent.ACTION_MOVE,(start+amount)*width,t,t+200);
            check(Math.abs(front().getTranslationX())>1,"drag did not move the front card");
            event(endAction,(start+amount)*width,t,t+400);
        });
    }

    private void settled(String label,int selected,int notifications){
        SystemClock.sleep(settleWait);waitForIdleSync();
        main(()->{
            check(pager.selected==selected,label+": selected "+pager.selected);
            check(commits.size()==notifications,label+": callback count "+commits.size());
            if(notifications>0)check(commits.get(commits.size()-1)==selected,label+": callback index");
            check(Math.abs(front().getTranslationX())<1,label+": front card did not align");
            check(Math.abs(front().getRotation())<.01f,label+": front card still rotated");
        });
    }

    private View front(){return pager.getChildAt(pager.getChildCount()-1);}

    private void event(int action,float x,long down,long time){
        MotionEvent e=MotionEvent.obtain(down,time,action,x,y,0);
        try{check(pager.dispatchTouchEvent(e),"deck rejected touch action "+action);}
        finally{e.recycle();}
    }

    private void scrollEvent(int action,float x,float eventY,long down,long time){
        MotionEvent e=MotionEvent.obtain(down,time,action,x,eventY,0);
        try{check(scroll.dispatchTouchEvent(e),"ScrollView rejected touch action "+action);}
        finally{e.recycle();}
    }

    private void pointers(int action,long down,long time){
        MotionEvent.PointerProperties[] properties=new MotionEvent.PointerProperties[2];
        MotionEvent.PointerCoords[] coordinates=new MotionEvent.PointerCoords[2];
        for(int i=0;i<2;i++){
            properties[i]=new MotionEvent.PointerProperties();properties[i].id=i;
            properties[i].toolType=MotionEvent.TOOL_TYPE_FINGER;
            coordinates[i]=new MotionEvent.PointerCoords();coordinates[i].x=width*(i==0?.3f:.6f);
            coordinates[i].y=y;coordinates[i].pressure=1;coordinates[i].size=1;
        }
        MotionEvent e=MotionEvent.obtain(down,time,action,2,properties,coordinates,0,0,1,1,0,0,
                android.view.InputDevice.SOURCE_TOUCHSCREEN,0);
        try{check(pager.dispatchTouchEvent(e),"deck rejected multiple-pointer action");}
        finally{e.recycle();}
    }

    private void main(Runnable action){
        Throwable[] failure={null};
        runOnMainSync(()->{try{action.run();}catch(Throwable error){failure[0]=error;}});
        if(failure[0]!=null)throw new AssertionError("Main-thread deck check failed",failure[0]);
    }

    private static void check(boolean condition,String message){if(!condition)throw new AssertionError(message);}
}
