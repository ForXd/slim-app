package cn.qingji.diary;

/** Adult starting estimates only; short-term energy arithmetic is not a weight forecast. */
final class PlanMath {
    enum Sex {FEMALE,MALE}
    enum Activity {
        SEDENTARY(1.2),LIGHT(1.375),ACTIVE(1.55);
        final double factor;
        Activity(double factor){this.factor=factor;}
    }
    enum Mode {MAINTAIN,MILD,STANDARD,CUSTOM}

    static final class Estimate {
        final double resting,maintenance,bmi,minimumTarget;
        final boolean allowsReduction;
        private Estimate(double resting,double maintenance,double bmi,double minimumTarget){
            this.resting=resting;this.maintenance=maintenance;this.bmi=bmi;
            this.minimumTarget=minimumTarget;allowsReduction=bmi>=18.5;
        }
    }
    static final class Choice {
        final double target,deficit,weekKg,fourWeeksKg;
        private Choice(double maintenance,double target){
            this.target=target;deficit=maintenance-target;
            weekKg=deficit*7/7700;fourWeeksKg=deficit*28/7700;
        }
    }

    static Estimate estimate(double heightCm,double weightKg,int age,Sex sex,Activity activity){
        range(heightCm,100,250,"身高需为 100–250 cm");
        range(weightKg,25,350,"体重需为 25–350 kg");
        if(age<18||age>78)throw new IllegalArgumentException("自动估算适用于 18–78 岁，可用直接设置填写已有目标");
        if(sex==null)throw new IllegalArgumentException("请选择公式性别");
        if(activity==null)throw new IllegalArgumentException("请选择日常活动程度");
        double resting=10*weightKg+6.25*heightCm-5*age+(sex==Sex.MALE?5:-161);
        double meters=heightCm/100;
        double maintenance=Math.round(resting*activity.factor);
        range(maintenance,1,6500,"估算超出支持范围，请核对身体信息");
        return new Estimate(resting,maintenance,weightKg/(meters*meters),sex==Sex.MALE?1500:1200);
    }

    static Choice choice(Estimate estimate,Mode mode,double custom){
        if(estimate==null||mode==null)throw new IllegalArgumentException("请先计算身体信息");
        double target=estimate.maintenance;
        if(mode==Mode.CUSTOM)target=customTarget(custom);
        else if(estimate.allowsReduction&&(mode==Mode.MILD||mode==Mode.STANDARD)){
            double deficit=mode==Mode.MILD?250:500;
            // A floor never increases a low maintenance estimate or fabricates a deficit.
            target=Math.max(target-deficit,Math.min(target,estimate.minimumTarget));
        }
        return new Choice(estimate.maintenance,target);
    }

    static double customTarget(double target){return range(target,1,6500,"每日目标需为 1–6500 千卡");}
    static double manualMaintenance(double maintenance){return range(maintenance,1,6500,"维持热量需为 1–6500 千卡");}
    private static double range(double value,double min,double max,String message){
        if(!Double.isFinite(value)||value<min||value>max)throw new IllegalArgumentException(message);
        return value;
    }
}
