package cn.qingji.diary;

import android.app.AlertDialog;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import org.json.JSONArray;
import org.json.JSONObject;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/** Read-only month overview; a date opens its existing journal on the home screen. */
final class JournalCalendar {
    private final MainActivity app;
    private final Set<String> recordedDays=new HashSet<>();
    private LocalDate today,selected;
    private YearMonth month;
    private AlertDialog dialog;
    private LinearLayout grid;
    private TextView monthTitle;
    private Button next;

    JournalCalendar(MainActivity app){this.app=app;}

    void show(){
        today=LocalDate.now();
        try{selected=LocalDate.parse(app.selectedDate);}catch(RuntimeException e){selected=today;}
        if(selected.isAfter(today))selected=today;
        month=YearMonth.from(selected);
        recordedDays.clear();
        try{
            JSONArray rows=app.store.rows();
            for(int i=0;i<rows.length();i++){
                JSONObject row=rows.optJSONObject(i);
                if(row!=null)recordedDays.add(row.optString("date"));
            }
        }catch(RuntimeException e){app.error(e.getMessage());return;}

        LinearLayout content=new LinearLayout(app);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(app.dp(12),app.dp(4),app.dp(12),app.dp(8));
        LinearLayout navigation=new LinearLayout(app);
        navigation.setGravity(Gravity.CENTER_VERTICAL);
        content.addView(navigation,new LinearLayout.LayoutParams(-1,app.dp(48)));
        monthButton(navigation,"‹","上个月",()->{month=month.minusMonths(1);renderMonth();});
        monthTitle=app.style.heading("",20,app.ink);
        monthTitle.setGravity(Gravity.CENTER);
        monthTitle.setSingleLine(true);
        monthTitle.setAccessibilityHeading(true);
        monthTitle.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);
        navigation.addView(monthTitle,new LinearLayout.LayoutParams(0,-2,1));
        next=monthButton(navigation,"›","下个月",()->{
            if(canGoForward(month,today)){month=month.plusMonths(1);renderMonth();}
        });

        LinearLayout week=new LinearLayout(app);
        content.addView(week,new LinearLayout.LayoutParams(-1,app.dp(30)));
        String[] weekdays={"周一","周二","周三","周四","周五","周六","周日"};
        for(String day:weekdays){
            TextView label=app.style.label(day,11,false,app.style.muted);
            label.setGravity(Gravity.CENTER);
            label.setSingleLine(true);
            week.addView(label,new LinearLayout.LayoutParams(0,-1,1));
        }
        grid=new LinearLayout(app);
        grid.setOrientation(LinearLayout.VERTICAL);
        content.addView(grid,new LinearLayout.LayoutParams(-1,-2));
        TextView legend=app.style.label("● 有记录",12,false,app.style.muted);
        legend.setPadding(app.dp(4),app.dp(12),0,app.dp(4));
        content.addView(legend);
        renderMonth();
        dialog=app.dialog("日历回顾",content);
    }

    private Button monthButton(LinearLayout parent,String text,String description,Runnable action){
        Button button=new Button(app);
        button.setText(text);
        button.setTextSize(27);
        button.setTypeface(app.style.body);
        button.setAllCaps(false);
        button.setTextColor(app.accent);
        button.setContentDescription(description);
        button.setMinWidth(0);button.setMinimumWidth(0);
        button.setMinHeight(0);button.setMinimumHeight(0);
        button.setPadding(0,0,0,0);
        button.setIncludeFontPadding(false);
        button.setStateListAnimator(null);
        button.setBackground(app.style.ripple(Color.TRANSPARENT,10,false));
        parent.addView(button,new LinearLayout.LayoutParams(app.dp(44),app.dp(44)));
        button.setOnClickListener(v->action.run());
        return button;
    }

    private void renderMonth(){
        monthTitle.setText(month.format(DateTimeFormatter.ofPattern("yyyy年M月",Locale.CHINA)));
        next.setEnabled(canGoForward(month,today));
        next.setAlpha(next.isEnabled()?1f:.28f);
        grid.removeAllViews();
        // Six fixed rows keep the dialog steady while moving between months.
        for(int row=0;row<6;row++){
            LinearLayout line=new LinearLayout(app);
            grid.addView(line,new LinearLayout.LayoutParams(-1,-2));
            for(int column=0;column<7;column++){
                LocalDate date=gridDate(month,row*7+column);
                View cell=date==null?new View(app):dateButton(date);
                if(date==null)cell.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
                LinearLayout.LayoutParams size=new LinearLayout.LayoutParams(0,app.dp(44),1);
                size.setMargins(app.dp(1),app.dp(2),app.dp(1),0);
                line.addView(cell,size);
            }
        }
    }

    private Button dateButton(LocalDate date){
        boolean chosen=date.equals(selected),current=date.equals(today),recorded=recordedDays.contains(date.toString());
        boolean enabled=canSelect(date,today);
        Button cell=new Button(app);
        cell.setText(String.valueOf(date.getDayOfMonth()));
        cell.setTextSize(14);
        cell.setTypeface(app.style.medium);
        cell.setFontFeatureSettings("'tnum' 1, 'lnum' 1");
        cell.setLetterSpacing(0);
        cell.setAllCaps(false);
        cell.setSingleLine(true);
        cell.setIncludeFontPadding(false);
        cell.setGravity(Gravity.CENTER);
        cell.setPadding(0,app.dp(4),0,app.dp(3));
        cell.setMinWidth(0);cell.setMinimumWidth(0);
        cell.setMinHeight(0);cell.setMinimumHeight(0);
        cell.setStateListAnimator(null);
        cell.setTextColor(chosen?app.paper:app.ink);
        GradientDrawable background=app.style.surface(chosen?app.accent:Color.TRANSPARENT,9,false);
        if(current)background.setStroke(app.dp(1),app.accent);
        cell.setBackground(new RippleDrawable(ColorStateList.valueOf(chosen?0x33ffffff:0x22b23924),background,null));
        GradientDrawable dot=new GradientDrawable();
        dot.setShape(GradientDrawable.OVAL);
        dot.setColor(recorded?(chosen?app.paper:app.accent):Color.TRANSPARENT);
        dot.setBounds(0,0,app.dp(3),app.dp(3));
        cell.setCompoundDrawables(null,null,null,dot);
        cell.setCompoundDrawablePadding(app.dp(3));
        cell.setSelected(chosen);
        cell.setEnabled(enabled);
        cell.setAlpha(enabled?1f:.28f);
        cell.setContentDescription(date+(recorded?"，有记录":"，无记录")+(current?"，今天":"")+(chosen?"，已选中":"")+(enabled?"":"，未来日期"));
        cell.setOnClickListener(v->{
            if(!canSelect(date,LocalDate.now()))return;
            dialog.dismiss();
            app.openDay(date.toString());
        });
        return cell;
    }

    static int firstColumn(YearMonth month){return month.atDay(1).getDayOfWeek().getValue()-1;}
    static LocalDate gridDate(YearMonth month,int position){
        if(position<0||position>=42)return null;
        int day=position-firstColumn(month)+1;
        return day<1||day>month.lengthOfMonth()?null:month.atDay(day);
    }
    static boolean canSelect(LocalDate date,LocalDate today){return date!=null&&!date.isAfter(today);}
    static boolean canGoForward(YearMonth month,LocalDate today){return month.isBefore(YearMonth.from(today));}
}
