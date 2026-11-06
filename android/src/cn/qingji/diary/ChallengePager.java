package cn.qingji.diary;

import android.animation.*;
import android.content.Context;
import android.os.Bundle;
import android.view.*;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.animation.*;
import android.widget.FrameLayout;

/** Bounded three-card deck. Only transforms run during a drag; selection commits after settling. */
final class ChallengePager extends FrameLayout {
    interface Selection {void changed(int index);}
    interface Cards {View create(int index);}
    Selection selection;Cards cards;int selected=-1,count;
    private final View[] layers=new View[3];
    private final int slop;private final float density;
    private float downX,downY,offset;private int direction=1;
    private boolean dragging,vertical,settling,ignoreGesture;private VelocityTracker velocity;
    private ValueAnimator animator;private int generation;
    ChallengePager(Context c){
        super(c);density=getResources().getDisplayMetrics().density;slop=ViewConfiguration.get(c).getScaledTouchSlop();
        setClipChildren(false);setClipToPadding(false);setFocusable(true);setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_YES);
    }
    void configure(int count,int index,Cards cards,Selection callback){
        cancelAnimation();this.count=count;this.selected=Math.max(0,Math.min(count-1,index));this.cards=cards;selection=callback;
        direction=selected<count-1?1:-1;rebuild();
    }
    private int px(float dp){return Math.round(dp*density);}
    private void rebuild(){
        removeAllViews();for(int i=0;i<3;i++)layers[i]=null;
        if(cards==null||count==0)return;
        for(int depth=2;depth>=0;depth--){
            int index=selected+depth*direction;if(index<0||index>=count)continue;
            View card=cards.create(index);layers[depth]=card;
            LayoutParams p=new LayoutParams(LayoutParams.MATCH_PARENT,LayoutParams.MATCH_PARENT);p.setMargins(px(5),px(7),px(5),px(40));addView(card,p);
            card.setElevation(px(depth==0?4:1));card.setImportantForAccessibility(depth==0?IMPORTANT_FOR_ACCESSIBILITY_YES:IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS);
            card.setClickable(false);card.setFocusable(false);
        }
        setContentDescription("挑战卡片，第"+(selected+1)+"个，共"+count+"个；左右滑动切换");render(0);
    }
    @Override protected void onLayout(boolean changed,int l,int t,int r,int b){super.onLayout(changed,l,t,r,b);render(offset);}
    private void render(float value){
        offset=value;float width=Math.max(1,getWidth()),progress=Math.min(1,Math.abs(value)/(width*.8f));
        for(int depth=0;depth<3;depth++){
            View card=layers[depth];if(card==null)continue;
            card.setPivotX(card.getWidth()/2f);card.setPivotY(0);
            if(depth==0){card.setTranslationX(value);card.setTranslationY(Math.abs(value)*.025f);card.setRotation(Math.max(-7,Math.min(7,value/width*7)));card.setScaleX(1);card.setScaleY(1);}
            else {float level=depth-progress;float scale=1-level*.045f;card.setScaleX(scale);card.setScaleY(scale);card.setTranslationY(px(21)*level);card.setTranslationX(0);card.setRotation(0);}
        }
    }
    static int destination(int selected,int count,float dx,float velocity,float width,float minFling){
        boolean far=Math.abs(dx)>width*.24f;
        boolean flick=Math.abs(dx)>width*.04f&&Math.abs(velocity)>minFling&&Math.signum(dx)==Math.signum(velocity);
        if(!far&&!flick)return selected;
        int target=selected+(dx<0?1:-1);return Math.max(0,Math.min(count-1,target));
    }
    void select(int index){
        if(index<0||index>=count||index==selected||settling)return;
        direction=index>selected?1:-1;rebuild();animateTo(-direction*Math.max(1,getWidth())*1.12f,index);
    }
    private void animateTo(float end,int target){
        boolean commit=target!=selected;
        if(!ValueAnimator.areAnimatorsEnabled()){if(commit)commit(target);else render(0);return;}
        settling=true;final int token=++generation;
        animator=ValueAnimator.ofFloat(offset,end);animator.setDuration(commit?240:280);
        animator.setInterpolator(commit?new PathInterpolator(.2f,.8f,.2f,1f):new OvershootInterpolator(.65f));
        animator.addUpdateListener(v->{if(token==generation)render((float)v.getAnimatedValue());});
        animator.addListener(new AnimatorListenerAdapter(){@Override public void onAnimationEnd(Animator a){
            if(token!=generation)return;settling=false;animator=null;
            if(commit)commit(target);else render(0);
        }});animator.start();
    }
    private void commit(int target){
        selected=target;direction=selected<count-1?1:-1;offset=0;rebuild();
        performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK);
        if(selection!=null)selection.changed(selected);
    }
    private void lockParent(boolean lock){if(getParent()!=null)getParent().requestDisallowInterceptTouchEvent(lock);}
    private void begin(MotionEvent e){
        lockParent(true);
        downX=e.getX();downY=e.getY();dragging=false;vertical=false;ignoreGesture=false;
        if(velocity!=null)velocity.recycle();velocity=VelocityTracker.obtain();velocity.addMovement(e);
    }
    @Override public boolean onInterceptTouchEvent(MotionEvent e){
        if(count<=1)return false;
        if(ignoreGesture)return true;
        if(settling){if(e.getActionMasked()==MotionEvent.ACTION_DOWN)ignoreGesture=true;return true;}
        if(e.getActionMasked()==MotionEvent.ACTION_DOWN)begin(e);
        if(e.getActionMasked()==MotionEvent.ACTION_MOVE){float dx=e.getX()-downX,dy=e.getY()-downY;
            if(Math.abs(dy)>slop&&Math.abs(dy)>Math.abs(dx)){vertical=true;lockParent(false);}
            if(!vertical&&Math.abs(dx)>slop&&Math.abs(dx)>Math.abs(dy)*1.2f){dragging=true;lockParent(true);return true;}
        }
        return dragging;
    }
    @Override public boolean onTouchEvent(MotionEvent e){
        if(count<=1)return false;
        if(ignoreGesture){if(e.getActionMasked()==MotionEvent.ACTION_UP||e.getActionMasked()==MotionEvent.ACTION_CANCEL){ignoreGesture=false;lockParent(false);}return true;}
        if(e.getActionMasked()==MotionEvent.ACTION_POINTER_DOWN){ignoreGesture=true;dragging=false;if(velocity!=null){velocity.recycle();velocity=null;}lockParent(false);if(!settling)animateTo(0,selected);return true;}
        if(settling){if(e.getActionMasked()==MotionEvent.ACTION_DOWN)ignoreGesture=true;return true;}
        if(e.getActionMasked()==MotionEvent.ACTION_DOWN){begin(e);return true;}
        if(velocity!=null)velocity.addMovement(e);
        float dx=e.getX()-downX,dy=e.getY()-downY;
        if(e.getActionMasked()==MotionEvent.ACTION_MOVE){
            if(!dragging){if(Math.abs(dy)>slop&&Math.abs(dy)>Math.abs(dx)){vertical=true;lockParent(false);}
                if(!vertical&&Math.abs(dx)>slop&&Math.abs(dx)>Math.abs(dy)*1.2f){dragging=true;lockParent(true);}}
            if(dragging){int nextDirection=dx<0?1:-1;
                boolean edge=selected+nextDirection<0||selected+nextDirection>=count;
                if(!edge&&nextDirection!=direction){direction=nextDirection;rebuild();}
                render(edge?dx*.16f:dx);}
            return true;
        }
        if(e.getActionMasked()==MotionEvent.ACTION_UP||e.getActionMasked()==MotionEvent.ACTION_CANCEL){
            float speed=0;if(velocity!=null){velocity.computeCurrentVelocity(1000);speed=velocity.getXVelocity();velocity.recycle();velocity=null;}
            lockParent(false);
            int target=dragging&&e.getActionMasked()==MotionEvent.ACTION_UP?destination(selected,count,dx,speed,Math.max(1,getWidth()),density*650):selected;
            if(dragging)animateTo(target==selected?0:(target>selected?-1:1)*getWidth()*1.12f,target);else performClick();dragging=false;return true;
        }
        return true;
    }
    @Override public boolean performClick(){super.performClick();return true;}
    @Override public void onInitializeAccessibilityNodeInfo(AccessibilityNodeInfo info){
        super.onInitializeAccessibilityNodeInfo(info);info.setScrollable(count>1);
        if(selected<count-1)info.addAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_SCROLL_FORWARD);
        if(selected>0)info.addAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_SCROLL_BACKWARD);
    }
    @Override public boolean performAccessibilityAction(int action,Bundle args){
        if(action==AccessibilityNodeInfo.ACTION_SCROLL_FORWARD&&selected<count-1){select(selected+1);return true;}
        if(action==AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD&&selected>0){select(selected-1);return true;}
        return super.performAccessibilityAction(action,args);
    }
    private void cancelAnimation(){generation++;if(animator!=null){animator.cancel();animator=null;}settling=false;}
    @Override protected void onDetachedFromWindow(){cancelAnimation();if(velocity!=null){velocity.recycle();velocity=null;}super.onDetachedFromWindow();}
}
