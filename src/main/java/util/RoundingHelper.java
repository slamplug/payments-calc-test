package util;

import static org.springframework.util.StringUtils.isEmpty;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class RoundingHelper {

    public static BigDecimal round2dp(final BigDecimal toRound) {
        return toRound != null ? toRound.setScale(2, RoundingMode.HALF_UP) : null;
    }

    public static BigDecimal round2dp(final Double toRound) {
        return toRound != null ? new BigDecimal(toRound).setScale(2, RoundingMode.HALF_UP) : null;
    }

    public static BigDecimal round2dp(final String toRound) {
        return !isEmpty(toRound) ? new BigDecimal(toRound).setScale(2, RoundingMode.HALF_UP) : null;
    }
}
