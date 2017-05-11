package default;

import com.google.common.base.MoreObjects;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;

import java.io.Serializable;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.Future;
import java.util.concurrent.RecursiveTask;

/**
 * Created by zhaoyy on 2017/4/27.
 */
public final class Estimation implements Serializable {

    private static final long serialVersionUID = 1L;

    private final double alpha;
    private final double beta;

    public Estimation(double alpha, double beta) {
        this.alpha = alpha;
        this.beta = beta;
    }

    public double estimate(Sample data) {
        return (data.getRareEventsCount() + alpha) / (data.getEventsCount() + alpha + beta);
    }

    public double defaultEstimate() {
        return alpha / (alpha + beta);
    }

    private static double[] initWithMethodOfMoments(List<Sample> list) {
        double x = 0;
        double x2 = 0;
        for (Sample stat : list) {
            double c = stat.getCtr();
            x += c;
            x2 += c * c;
        }
        x /= list.size();
        x2 /= list.size();
        double var = x2 - x * x;

        double temp = (1 - x) * x / var - 1;
        double alpha = temp * x, beta = temp * (1 - x);
        //CommonLogger.info("use method of moments to init,alpha:[{}],beta:[{}]", alpha, beta);
        return new double[]{alpha, beta};
    }

    public static Estimation calcWithFixedPointIteration(List<Sample> dataList, final double epsilon) {
        Preconditions.checkArgument(dataList != null && !dataList.isEmpty(), "empty data list");
        Preconditions.checkArgument(epsilon > 0, "epsilon must be a positive number");
        final ImmutableList<Sample> data = ImmutableList.copyOf(dataList);
        double[] initValue = initWithMethodOfMoments(data);
        double alpha = initValue[0], lastAlpha = 0, beta = initValue[1], lastBeta = 0;
        final ForkJoinPool pool = new ForkJoinPool();
        long t1 = System.currentTimeMillis();
        while (Math.abs(alpha - lastAlpha) > epsilon || Math.abs(beta - lastBeta) > epsilon) {
            final Future<double[]> future = pool.submit(new FixedPointIteration(data, alpha, beta));
            try {
                lastAlpha = alpha;
                lastBeta = beta;
                double[] result = future.get();
                alpha = alpha * result[0] / result[2];
                beta = beta * result[1] / result[3];
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }
        }
        pool.shutdown();
        long t2 = System.currentTimeMillis();
        //CommonLogger.info("fixed point iteration fork join task completed in[{}] seconds.", (t2 - t1) / 1000);
        //CommonLogger.info("point fixed,alpha:[{}],beta:[{}].", alpha, beta);
        return new Estimation(alpha, beta);
    }


    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("alpha", alpha)
                .add("beta", beta)
                .toString();
    }

    private static final class FixedPointIteration extends RecursiveTask<double[]> {

        private static final long serialVersionUID = 1L;
        private static final int CRITICAL = 0x80;

        private final ImmutableList<Sample> data;
        private final double alpha;
        private final double beta;

        FixedPointIteration(ImmutableList<Sample> data, double alpha, double beta) {
            this.data = data;
            this.alpha = alpha;
            this.beta = beta;
        }

        @Override
        protected double[] compute() {
            final int size = data.size();
            if (size > CRITICAL) {
                FixedPointIteration left = new FixedPointIteration(data.subList(0, size / 2 + 1), alpha, beta);
                FixedPointIteration right = new FixedPointIteration(data.subList(size / 2 + 1, size), alpha, beta);
                left.fork();
                right.fork();
                return joinResult(left.join(), right.join());
            }
            double[] result = new double[4];
            for (Sample stat : data) {
                int c = stat.getRareEventsCount();
                int i = stat.getEventsCount();
                if (i == 0)
                    continue;
                result[0] += (Digamma.digamma(c + alpha) - Digamma.digamma(alpha));
                result[1] += (Digamma.digamma(i - c + beta) - Digamma.digamma(beta));
                double tmp = (Digamma.digamma(i + alpha + beta) - Digamma.digamma(alpha + beta));
                result[2] += tmp;
                result[3] += tmp;
            }
            return result;
        }

        private static double[] joinResult(double[] left, double[] right) {
            assert left.length == right.length;
            double[] result = new double[left.length];
            for (int i = 0; i < result.length; i++)
                result[i] = left[i] + right[i];
            return result;
        }
    }

}




