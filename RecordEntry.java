package default1;

import javax.annotation.concurrent.Immutable;
import java.io.Serializable;
import java.math.BigDecimal;

/**
 * Created by zhaoyy on 2017/7/14.
 */
@Immutable
public final class RecordEntry implements Serializable {

    private static final long serialVersionUID = 1134118140243728389L;

    private final long clickCount;
    private final long impressionCount;
    private final double ctr;

    private static final RecordEntry ZERO = new RecordEntry();

    private RecordEntry() {
        this.clickCount = 0;
        this.impressionCount = 0;
        this.ctr = 0;
    }

    private RecordEntry(long clickCount, long impressionCount) {
        this.clickCount = clickCount;
        this.impressionCount = impressionCount;
        this.ctr = impressionCount == 0 ? 0 : (clickCount + 0.0) / impressionCount;
    }

    public static RecordEntry of(long clickCount, long impressionCount) {
        if (clickCount < 0 || impressionCount < 0)
            throw new IllegalArgumentException(FastStringUtils.placeholderFormat("illegal parameters:clickCount({})/impressionCount({})", clickCount, impressionCount));
        return new RecordEntry(clickCount, impressionCount);
    }

    public static RecordEntry of() {
        return ZERO;
    }

    public long getClickCount() {
        return clickCount;
    }

    public long getImpressionCount() {
        return impressionCount;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        RecordEntry that = (RecordEntry) o;
        return clickCount == that.clickCount && impressionCount == that.impressionCount;

    }

    @Override
    public int hashCode() {
        int result = (int) (clickCount ^ (clickCount >>> 32));
        result = 31 * result + (int) (impressionCount ^ (impressionCount >>> 32));
        return result;
    }

    @Override
    public String toString() {
        return String.format("%s/%s(%.4f)", clickCount, impressionCount,ctr);
    }

    public RecordEntry combine(RecordEntry r) {
        if (r == null)
            return this;
        return new RecordEntry(clickCount + r.clickCount, impressionCount + r.impressionCount);
    }

    public double getCtr() {
        return ctr;
    }
}
