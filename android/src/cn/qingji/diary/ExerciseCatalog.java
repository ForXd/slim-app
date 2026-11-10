package cn.qingji.diary;
/** Selected 2024 Adult Compendium entries; net energy excludes resting expenditure. */
final class ExerciseCatalog {
 static final String[] NAMES={"平地中速步行","轻松骑车","平地慢走","快走（5.6–6.3公里/小时）","慢跑（自选配速）","跑步（约8公里/小时）","爬楼梯","户外徒步","跳绳","椭圆机（中等强度）","划船机（中等强度）","力量训练（多动作）","力量训练（高强度）","自重训练","普拉提","瑜伽","拉伸","低冲击健身操","羽毛球（休闲）","篮球（非比赛）","篮球投篮","拳击沙袋","自定义运动"};
 static final double[] MET={3.8,4,2.8,4.8,7.5,8.5,6.8,6,11,5,5,3.5,6,3,2.8,2.3,2.3,4.8,5.5,6,5,5.8,3};
 static final String[] CODES={"17190","01010","17152","17200","12020","12030","17131","17080","02068","02048","02071","02054","02050","02056","02105","02175","02101","02005","15030","15050","15070","15110","自定义"};
 static int index(String name){for(int i=0;i<NAMES.length;i++)if(NAMES[i].equals(name))return i;return NAMES.length-1;}
 static long estimate(double met,double kg,double minutes){if(!Double.isFinite(met)||met<1||met>25||!Double.isFinite(kg)||kg<1||kg>600||!Double.isFinite(minutes)||minutes<1||minutes>1440)throw new IllegalArgumentException("请检查强度、体重和时长");return Math.round((met-1)*3.5*kg/200*minutes);}
}
