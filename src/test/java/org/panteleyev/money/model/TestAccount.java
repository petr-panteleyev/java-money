package org.panteleyev.money.model;

/*
 * Copyright (c) Petr Panteleyev. All rights reserved.
 * Licensed under the BSD license. See LICENSE file in the project root for full license information.
 */

import org.panteleyev.money.test.BaseTest;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import java.time.LocalDate;
import java.util.UUID;
import static org.panteleyev.money.test.BaseTestUtils.RANDOM;
import static org.panteleyev.money.test.BaseTestUtils.randomBigDecimal;
import static org.panteleyev.money.test.BaseTestUtils.randomCategoryType;
import static org.testng.Assert.assertEquals;

public class TestAccount extends BaseTest {
    @Test
    public void testEquals() {
        var name = UUID.randomUUID().toString();
        var comment = UUID.randomUUID().toString();
        var accountNumber = UUID.randomUUID().toString();
        var opening = randomBigDecimal();
        var limit = randomBigDecimal();
        var rate = randomBigDecimal();
        var type = randomCategoryType();
        var categoryUuid = UUID.randomUUID();
        var currencyUuid = UUID.randomUUID();
        var enabled = RANDOM.nextBoolean();
        var interest = randomBigDecimal();
        var closingDate = LocalDate.now();
        var iconUuid = UUID.randomUUID();
        var cardType = CardType.MASTERCARD;
        var cardNumber = UUID.randomUUID().toString();
        var uuid = UUID.randomUUID();
        var created = System.currentTimeMillis();
        var modified = System.currentTimeMillis();

        var a1 = new Account.Builder()
            .name(name)
            .comment(comment)
            .accountNumber(accountNumber)
            .openingBalance(opening)
            .accountLimit(limit)
            .currencyRate(rate)
            .type(type)
            .categoryUuid(categoryUuid)
            .currencyUuid(currencyUuid)
            .enabled(enabled)
            .interest(interest)
            .closingDate(closingDate)
            .iconUuid(iconUuid)
            .cardType(cardType)
            .cardNumber(cardNumber)
            .guid(uuid)
            .created(created)
            .modified(modified)
            .build();

        var a2 = new Account.Builder()
            .name(name)
            .comment(comment)
            .accountNumber(accountNumber)
            .openingBalance(opening)
            .accountLimit(limit)
            .currencyRate(rate)
            .type(type)
            .categoryUuid(categoryUuid)
            .currencyUuid(currencyUuid)
            .enabled(enabled)
            .interest(interest)
            .closingDate(closingDate)
            .iconUuid(iconUuid)
            .cardType(cardType)
            .cardNumber(cardNumber)
            .guid(uuid)
            .created(created)
            .modified(modified)
            .build();

        assertEquals(a1, a2);
        assertEquals(a1.hashCode(), a2.hashCode());
    }

    @DataProvider(name = "testAccountNumberDataProvider")
    public Object[][] testAccountNumberDataProvider() {
        return new Object[][]{
            {"   1234  5 6   78 ", "12345678"},
            {"12345678", "12345678"},
            {"123456 78    ", "12345678"},
            {" 12345678", "12345678"},
        };
    }

    @Test(dataProvider = "testAccountNumberDataProvider")
    public void testAccountNumber(String accountNumber, String accountNumberNoSpaces) {
        var a = new Account.Builder()
            .accountNumber(accountNumber)
            .type(CategoryType.DEBTS)
            .categoryUuid(UUID.randomUUID())
            .guid(UUID.randomUUID())
            .created(System.currentTimeMillis())
            .modified(System.currentTimeMillis())
            .build();

        assertEquals(a.accountNumber(), accountNumber);
        assertEquals(a.getAccountNumberNoSpaces(), accountNumberNoSpaces);
    }

    @Test
    public void testBuilder() {
        var original = new Account.Builder()
            .name(UUID.randomUUID().toString())
            .comment(UUID.randomUUID().toString())
            .accountNumber(UUID.randomUUID().toString())
            .openingBalance(randomBigDecimal())
            .accountLimit(randomBigDecimal())
            .currencyRate(randomBigDecimal())
            .type(randomCategoryType())
            .categoryUuid(UUID.randomUUID())
            .currencyUuid(UUID.randomUUID())
            .enabled(RANDOM.nextBoolean())
            .interest(randomBigDecimal())
            .closingDate(LocalDate.now())
            .iconUuid(UUID.randomUUID())
            .cardType(CardType.VISA)
            .cardNumber(UUID.randomUUID().toString())
            .guid(UUID.randomUUID())
            .created(System.currentTimeMillis())
            .modified(System.currentTimeMillis())
            .build();

        var copy = new Account.Builder(original).build();
        assertEquals(copy, original);

        var manualCopy = new Account.Builder()
            .name(original.name())
            .comment(original.comment())
            .accountNumber(original.accountNumber())
            .openingBalance(original.openingBalance())
            .accountLimit(original.accountLimit())
            .currencyRate(original.currencyRate())
            .type(original.type())
            .categoryUuid(original.categoryUuid())
            .currencyUuid(original.currencyUuid())
            .enabled(original.enabled())
            .interest(original.interest())
            .closingDate(original.closingDate())
            .iconUuid(original.iconUuid())
            .cardType(original.cardType())
            .cardNumber(original.cardNumber())
            .guid(original.uuid())
            .created(original.created())
            .modified(original.modified())
            .build();
        assertEquals(manualCopy, original);
    }
}