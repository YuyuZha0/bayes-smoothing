import lombok.extern.slf4j.Slf4j;

import java.util.Collection;

/** Created by zhaoyy on 2017/7/14. */
@Slf4j
public class BayesSmoothingExecutor {

  private static final int WARM_UP_ITERATION = 100;
  private final double epsilon;
  private final double maxAllowedIteration;

  public BayesSmoothingExecutor(double epsilon, double maxAllowedIteration) {
    this.epsilon = epsilon;
    this.maxAllowedIteration = maxAllowedIteration;
  }

  private static double getDelta(double[] a1, double[] a2) {
    // 防止分母为0
    if (a1[0] * a1[1] == 0) return 1;
    double d1 = Math.abs((a2[0] - a1[0]) / a1[0]);
    double d2 = Math.abs((a2[1] - a1[1]) / a1[1]);
    return Math.max(d1, d2);
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
    double ctrVar = getVar(ctr2Sum, ctrSum, len);
    double temp1 = (1 - ctrAvg) * ctrAvg / ctrVar - 1;
    double alpha = temp1 * ctrAvg, beta = temp1 * (1 - ctrAvg);
    return new double[] {alpha, beta};
  }

  private static double getVar(double sxx, double sx, int n) {
    return (sxx - sx * sx / n) / (n - 1);
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

  /** Steffensen 迭代方法 */
  private static void nextSteffensenValue(double[] a, RecordEntry[] entries) {
    double alpha = a[0], beta = a[1];
    nextValue(a, entries);
    // System.out.println("1" + Arrays.toString(a));
    double alpha1 = a[0], beta1 = a[1];
    nextValue(a, entries);
    // System.out.println("2" + Arrays.toString(a));
    double alpha2 = a[0], beta2 = a[1];
    a[0] = steffensen(alpha, alpha1, alpha2);
    a[1] = steffensen(beta, beta1, beta2);
    // System.out.println("3" + Arrays.toString(a));
  }

  private static double steffensen(double x, double phi, double phiphi) {
    double a = phi - x;
    double b = phiphi - 2 * phi + x;
    if (b == 0) return x;
    // 保留计算精度
    return x - a * (a / b);
  }

  public BayesSmoothingResult executeSmoothing(long groupId, Collection<RecordEntry> data) {

    if (data == null || data.size() < 1) throw new IllegalArgumentException("illegal data");

    long t1 = System.currentTimeMillis();
    final RecordEntry[] entries = data.toArray(new RecordEntry[0]);
    double[] value = getInitialValue(entries);
    double[] lastValue = new double[2];
    int i = 0;
    while (getDelta(value, lastValue) > epsilon && (++i < maxAllowedIteration)) {
      lastValue[0] = value[0];
      lastValue[1] = value[1];
      if (i > WARM_UP_ITERATION) {
        nextSteffensenValue(value, entries);
        if (value[0] < 0 || value[1] < 0) {
          while (++i < maxAllowedIteration) nextValue(lastValue, entries);
          value[0] = lastValue[0];
          value[1] = lastValue[1];
          break;
        }
      } else {
        nextValue(value, entries);
      }
    }
    long t2 = System.currentTimeMillis();

    log.info(
        "\n\ttotal-time:{}ms\n\tgroupId:{}\n\tsize:{}\n\tstatus:{}\n\titeration:{}\n> alpha:{},beta:{}",
        (t2 - t1),
        groupId,
        entries.length,
        i < maxAllowedIteration ? "fixed" : "unfixed",
        i,
        value[0],
        value[1]);

    return new BayesSmoothingResult(groupId, value[0], value[1]);
  }
}
