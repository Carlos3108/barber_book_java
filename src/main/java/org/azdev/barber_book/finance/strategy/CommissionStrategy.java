package org.azdev.barber_book.finance.strategy;

import java.math.BigDecimal;

public interface CommissionStrategy {
    BigDecimal calculateCommission(BigDecimal servicePrice);
}
