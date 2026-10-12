package org.azdev.barber_book.finance.strategy;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class PercentageCommissionStrategy implements CommissionStrategy {
    private final BigDecimal percentage;

    public PercentageCommissionStrategy(BigDecimal percentage) {
        this.percentage = percentage.divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP); // Convert percentage to decimal
    }

    @Override
    public BigDecimal calculateCommission(BigDecimal servicePrice) {
        return servicePrice.multiply(percentage).setScale(2, RoundingMode.HALF_UP);
    }
}
