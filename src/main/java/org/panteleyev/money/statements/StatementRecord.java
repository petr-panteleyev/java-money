/*
 * Copyright (c) 2017, Petr Panteleyev <petr@panteleyev.org>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */

package org.panteleyev.money.statements;

import org.panteleyev.money.persistence.Currency;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;
import java.util.Optional;
import static org.panteleyev.money.persistence.MoneyDAO.getDao;

public final class StatementRecord {
    private final LocalDate actual;
    private final LocalDate execution;
    private final String description;
    private final String counterParty;
    private final String place;
    private final String country;
    private final String currency;
    private final String amount;
    private final String accountCurrency;
    private final String accountAmount;

    // calculated fields
    private final int currencyId;
    private final int accountCurrencyId;
    private BigDecimal amountDecimal;
    private BigDecimal accountAmountDecimal;

    public StatementRecord(LocalDate actual, LocalDate execution, String description, String counterParty, String place,
                           String country, String currency, String amount, String accountCurrency,
                           String accountAmount) {
        this.actual = actual;
        this.execution = execution;
        this.description = description;
        this.counterParty = counterParty;
        this.place = place;
        this.country = country;
        this.currency = currency;
        this.amount = amount;
        this.accountCurrency = accountCurrency;
        this.accountAmount = accountAmount;

        this.amountDecimal = toBigDecimal(amount);
        this.accountAmountDecimal = toBigDecimal(accountAmount);

        currencyId = getDao().getCurrencies().stream()
                .filter(c -> c.getDescription().equalsIgnoreCase(currency)
                        || c.getSymbol().equalsIgnoreCase(currency))
                .findAny()
                .map(Currency::getId)
                .orElse(0);

        accountCurrencyId = getDao().getCurrencies().stream()
                .filter(c -> c.getDescription().equalsIgnoreCase(accountCurrency)
                        || c.getSymbol().equalsIgnoreCase(accountCurrency))
                .findAny()
                .map(Currency::getId)
                .orElse(0);
    }

    public LocalDate getActual() {
        return actual;
    }

    public LocalDate getExecution() {
        return execution;
    }

    public String getDescription() {
        return description;
    }

    public String getCounterParty() {
        return counterParty;
    }

    public String getPlace() {
        return place;
    }

    public String getCountry() {
        return country;
    }

    public String getCurrency() {
        return currency;
    }

    public String getAmount() {
        return amount;
    }

    public String getAccountCurrency() {
        return accountCurrency;
    }

    public String getAccountAmount() {
        return accountAmount;
    }

    public int getCurrencyId() {
        return currencyId;
    }

    public int getAccountCurrencyId() {
        return accountCurrencyId;
    }

    public Optional<BigDecimal> getAmountDecimal() {
        return Optional.ofNullable(amountDecimal);
    }

    public Optional<BigDecimal> getAccountAmountDecimal() {
        return Optional.ofNullable(accountAmountDecimal);
    }

    private static BigDecimal toBigDecimal(String value) {
        try {
            return new BigDecimal(value);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (!(o instanceof StatementRecord)) {
            return false;
        }

        StatementRecord that = (StatementRecord) o;

        return Objects.equals(actual, that.actual)
                && Objects.equals(execution, that.execution)
                && Objects.equals(description, that.description)
                && Objects.equals(counterParty, that.counterParty)
                && Objects.equals(place, that.place)
                && Objects.equals(country, that.country)
                && Objects.equals(currency, that.currency)
                && Objects.equals(amount, that.amount)
                && Objects.equals(accountCurrency, that.accountCurrency)
                && Objects.equals(accountAmount, that.accountAmount);
    }

    public int hashCode() {
        return Objects.hash(actual, execution, description, counterParty, place, country,
                currency, amount, accountCurrency, accountAmount);
    }
}
