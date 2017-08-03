package com.ifeng.dmp.ctrp.smoothing;

import com.ifeng.dmp.ctrp.immutable.RecordEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.concurrent.Immutable;
import java.util.Collection;

/**
 * Created by zhaoyy on 2017/7/14.
 */
@Immutable
public class BayesSmoothingExecutor {

    private static final Logger logger = LoggerFactory.getLogger(BayesSmoothingExecutor.class);

    private final double epsilon;
    private final double maxAllowedIteration;

    private static final int WARM_UP_ITERATION = 0xff;

    public BayesSmoothingExecutor(double epsilon, double maxAllowedIteration) {
        this.epsilon = epsilon;
        this.maxAllowedIteration = maxAllowedIteration;
    }

    public BayesSmoothingResult executeSmoothing(long groupId, Collection<RecordEntry> data) {

        final RecordEntry[] entries = data.toArray(new RecordEntry[data.size()]);
        double[] value = getInitialValue(entries);
        double[] last = new double[2];
        int i = 0;
        while (delta(value, last) > epsilon && ++i < maxAllowedIteration) {
            last[0] = value[0];
            last[1] = value[1];
            if (i > WARM_UP_ITERATION)
                nextSteffensenValue(value, entries);
            else nextValue(value, entries);
        }

        logger.info("groupId:{},size:{},status:{},iteration:{} ---> alpha:{},beta:{}",
                groupId,
                entries.length,
                i < maxAllowedIteration ? "fixed" : "unfixed",
                i, value[0], value[1]);

        return new BayesSmoothingResult(groupId, value[0], value[1]);
    }


    private static double delta(double[] a1, double[] a2) {
        return Math.max(Math.abs(a1[0] - a2[0]),
                Math.abs(a1[1] - a2[1]));
    }

    private static double[] getInitialValue(RecordEntry[] entries) {
        int len = entries.length;
        double ctrSum = 0, ctr2Sum = 0;
        for (RecordEntry entry : entries) {
            double ctr = entry.getCtr();
            ctrSum += ctr;
            ctr2Sum += ctr * ctr;
        }
        double ctrAvg = ctrSum / len;
        double ctrVar = ctr2Sum / len - ctrAvg * ctrAvg;
        double temp1 = (1 - ctrAvg) * ctrAvg / ctrVar - 1;
        double alpha = temp1 * ctrAvg, beta = temp1 * (1 - ctrAvg);
        return new double[]{alpha, beta};
    }

    private static void nextValue(double[] a, RecordEntry[] entries) {
        double s1 = 0, s2 = 0, s3 = 0, s4 = 0;
        for (RecordEntry entry : entries) {
            long _c = entry.getClickCount();
            long _i = entry.getImpressionCount();
            s1 += (Digamma.digamma(_c + a[0]) - Digamma.digamma(a[0]));
            s2 += (Digamma.digamma(_i - _c + a[1]) - Digamma.digamma(a[1]));
            double temp2 = (Digamma.digamma(_i + a[0] + a[1]) - Digamma.digamma(a[0] + a[1]));
            s3 += temp2;
            s4 += temp2;
        }
        a[0] = a[0] * s1 / s3;
        a[1] = a[1] * s2 / s4;
    }

    /**
     * Steffensen 迭代方法
     */
    private static void nextSteffensenValue(double[] a, RecordEntry[] entries) {
        double alpha = a[0], beta = a[1];
        nextValue(a, entries);
        double alpha1 = a[0], beta1 = a[1];
        nextValue(a, entries);
        double alpha2 = a[0], beta2 = a[1];
        a[0] = steffensen(alpha, alpha1, alpha2);
        a[1] = steffensen(beta, beta1, beta2);
    }

    private static double steffensen(double x, double phi, double phiphi) {
        double a = phi - x;
        double b = phiphi - 2 * phi + x;
        if (b == 0)
            return x;
        //保留计算精度
        return x - a * (a / b);
    }

}
