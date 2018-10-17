import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.io.Serializable;

/** Created by zhaoyy on 2017/7/14. */
@Getter
@EqualsAndHashCode
public final class RecordEntry implements Serializable {

  private static final long serialVersionUID = 1134118140243728389L;
  private static final RecordEntry ZERO = new RecordEntry();
  private final long clickCount;
  private final long impressionCount;
  private final double ctr;

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
      throw new IllegalArgumentException(
          String.format(
              "illegal parameters:clickCount(%d)/impressionCount(%d)",
              clickCount, impressionCount));
    return new RecordEntry(clickCount, impressionCount);
  }

  public static RecordEntry of() {
    return ZERO;
  }

  @Override
  public String toString() {
    return String.format("%s/%s(%.4f)", clickCount, impressionCount, ctr);
  }

  public RecordEntry merge(RecordEntry r) {
    if (r == null) return this;
    return new RecordEntry(clickCount + r.clickCount, impressionCount + r.impressionCount);
  }
}
