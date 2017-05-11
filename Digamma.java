package default;

/**
 * Created by zhaoyy on 2017/5/10.
 */
public final class Digamma {

    private Digamma() {

    }

    /**
     * <a href="http://en.wikipedia.org/wiki/Euler-Mascheroni_constant">Euler-Mascheroni constant</a>
     */
    private static final double GAMMA = 0.577215664901532860606512090082;
    // limits for switching algorithm in digamma
    /**
     * C limit.
     */
    private static final double C_LIMIT = 49;
    /**
     * S limit.
     */
    private static final double S_LIMIT = 1e-5;

    /**
     * <p>Computes the digamma function of x.</p>
     * <p>
     * <p>This is an independently written implementation of the algorithm described in
     * Jose Bernardo, Algorithm AS 103: Psi (Digamma) Function, Applied Statistics, 1976.</p>
     * <p>
     * <p>Some of the constants have been changed to increase accuracy at the moderate expense
     * of run-time.  The result should be accurate to within 10^-8 absolute tolerance for
     * x >= 10^-5 and within 10^-8 relative tolerance for x > 0.</p>
     * <p>
     * <p>Performance for large negative values of x will be quite expensive (proportional to
     * |x|).  Accuracy for negative values of x should be about 10^-8 absolute for results
     * less than 10^5 and 10^-8 relative for results larger than that.</p>
     *
     * @param x Argument.
     * @return digamma(x) to within 10-8 relative or absolute error whichever is smaller.
     * @see <a href="http://en.wikipedia.org/wiki/Digamma_function">Digamma</a>
     * @see <a href="http://www.uv.es/~bernardo/1976AppStatist.pdf">Bernardo&apos;s original article </a>
     */

    public static double digamma(double x) {
        if (Double.isNaN(x) || Double.isInfinite(x)) {
            return x;
        }
        double y = 0;
        while (true) {
            if (x > 0 && x <= S_LIMIT) {
                // use method 5 from Bernardo AS103
                // accurate to O(x)
                y += -GAMMA - 1 / x;
                return y;
            }

            if (x >= C_LIMIT) {
                // use method 4 (accurate to O(1/x^8)
                double inv = 1 / (x * x);
                //            1       1        1         1
                // log(x) -  --- - ------ + ------- - -------
                //           2 x   12 x^2   120 x^4   252 x^6
                y += Math.log(x) - 0.5 / x - inv * ((1.0 / 12) + inv * (1.0 / 120 - inv / 252));
                return y;
            }
            y -= 1 / x;
            x += 1;
        }
    }

}
