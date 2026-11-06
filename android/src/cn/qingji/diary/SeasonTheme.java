package cn.qingji.diary;

import java.time.LocalDate;

/** Calendar-month seasons; this intentionally does not calculate the 24 solar terms. */
final class SeasonTheme {
    final String key,title,asset;

    private static final SeasonTheme SPRING=new SeasonTheme("spring","春日田野","season-spring.png");
    private static final SeasonTheme SUMMER=new SeasonTheme("summer","夏日湖泊","season-summer.png");
    private static final SeasonTheme AUTUMN=new SeasonTheme("autumn","秋日柿子","season-autumn.png");
    private static final SeasonTheme WINTER=new SeasonTheme("winter","冬日雪花","season-winter.png");

    private SeasonTheme(String key,String title,String asset){this.key=key;this.title=title;this.asset=asset;}

    static SeasonTheme resolve(String preference,LocalDate date){
        switch(preference==null?"auto":preference){
            case "spring":return SPRING;
            case "summer":return SUMMER;
            case "autumn":return AUTUMN;
            case "winter":return WINTER;
            default:
                int month=date.getMonthValue();
                if(month>=3&&month<=5)return SPRING;
                if(month>=6&&month<=8)return SUMMER;
                if(month>=9&&month<=11)return AUTUMN;
                return WINTER;
        }
    }
}
