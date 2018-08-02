import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.io.Serializable;

/** Created by zhaoyy on 2017/7/14. */
@AllArgsConstructor
@Getter
@ToString
@EqualsAndHashCode
public final class BayesSmoothingResult implements Serializable {

  private static final long serialVersionUID = 2353494519143065104L;

  private final long groupId;
  private final double alpha;
  private final double beta;

  public double smoothingCtrOf(RecordEntry entry) {
    return (alpha + entry.getClickCount()) / (alpha + beta + entry.getImpressionCount());
  }
}
