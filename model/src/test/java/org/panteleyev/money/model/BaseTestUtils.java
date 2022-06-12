/*
 Copyright © 2021-2022 Petr Panteleyev <petr@panteleyev.org>
 SPDX-License-Identifier: BSD-2-Clause
 */
package org.panteleyev.money.model;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Random;
import java.util.UUID;

public interface BaseTestUtils {
    Random RANDOM = new Random(System.currentTimeMillis());

    static int randomDay() {
        return 1 + RANDOM.nextInt(28);
    }

    static int randomMonth() {
        return 1 + RANDOM.nextInt(12);
    }

    static int randomYear() {
        return 1 + RANDOM.nextInt(3000);
    }

    static String randomString() {
        return UUID.randomUUID().toString();
    }

    static boolean randomBoolean() {
        return RANDOM.nextBoolean();
    }

    static int randomInt() {
        return RANDOM.nextInt();
    }

    static BigDecimal randomBigDecimal() {
        return BigDecimal.valueOf(RANDOM.nextDouble()).setScale(6, RoundingMode.HALF_UP);
    }

    static CategoryType randomCategoryType() {
        int index = RANDOM.nextInt(CategoryType.values().length);
        return CategoryType.values()[index];
    }

    static CardType randomCardType() {
        int index = RANDOM.nextInt(CardType.values().length);
        return CardType.values()[index];
    }

    static ContactType randomContactType() {
        int index = RANDOM.nextInt(ContactType.values().length);
        return ContactType.values()[index];
    }

    static TransactionType randomTransactionType() {
        while (true) {
            int index = RANDOM.nextInt(TransactionType.values().length);
            var type = TransactionType.values()[index];
            if (!type.isSeparator()) {
                return type;
            }
        }
    }

    static DocumentType randomDocumentType() {
        int index = RANDOM.nextInt(DocumentType.values().length);
        return DocumentType.values()[index];
    }

    static Account newAccount(Category category, Currency currency) {
        return new Account.Builder()
            .name(UUID.randomUUID().toString())
            .comment(UUID.randomUUID().toString())
            .accountNumber(UUID.randomUUID().toString())
            .openingBalance(randomBigDecimal())
            .accountLimit(randomBigDecimal())
            .currencyRate(randomBigDecimal())
            .type(category.type())
            .categoryUuid(category.uuid())
            .currencyUuid(currency.uuid())
            .enabled(RANDOM.nextBoolean())
            .interest(randomBigDecimal())
            .closingDate(LocalDate.now())
            .cardType(randomCardType())
            .cardNumber(randomString())
            .uuid(UUID.randomUUID())
            .modified(System.currentTimeMillis())
            .build();
    }

    static Account newAccount(Category category, Currency currency, Icon icon) {
        return new Account.Builder()
            .name(UUID.randomUUID().toString())
            .comment(UUID.randomUUID().toString())
            .accountNumber(UUID.randomUUID().toString())
            .openingBalance(randomBigDecimal())
            .accountLimit(randomBigDecimal())
            .currencyRate(randomBigDecimal())
            .type(category.type())
            .categoryUuid(category.uuid())
            .currencyUuid(currency.uuid())
            .enabled(RANDOM.nextBoolean())
            .interest(randomBigDecimal())
            .closingDate(LocalDate.now())
            .iconUuid(icon.uuid())
            .cardType(randomCardType())
            .cardNumber(randomString())
            .uuid(UUID.randomUUID())
            .modified(System.currentTimeMillis())
            .build();
    }

    static Category newCategory() {
        return newCategory(UUID.randomUUID(), randomCategoryType());
    }

    static Category newCategory(Icon icon) {
        return newCategory(UUID.randomUUID(), icon.uuid());
    }

    static Category newCategory(UUID uuid) {
        return newCategory(uuid, randomCategoryType());
    }

    static Category newCategory(UUID uuid, UUID iconUuid) {
        return newCategory(uuid, randomCategoryType(), iconUuid);
    }

    static Category newCategory(UUID uuid, CategoryType type) {
        return newCategory(uuid, type, null);
    }

    static Category newCategory(UUID uuid, CategoryType type, UUID iconUuid) {
        return new Category.Builder()
            .name(UUID.randomUUID().toString())
            .comment(UUID.randomUUID().toString())
            .type(type)
            .iconUuid(iconUuid)
            .uuid(uuid)
            .modified(System.currentTimeMillis())
            .build();
    }

    static Currency newCurrency() {
        return newCurrency(UUID.randomUUID());
    }

    static Currency newCurrency(UUID uuid) {
        return new Currency.Builder()
            .symbol(UUID.randomUUID().toString())
            .description(UUID.randomUUID().toString())
            .formatSymbol(UUID.randomUUID().toString())
            .formatSymbolPosition(RANDOM.nextInt(2))
            .showFormatSymbol(RANDOM.nextBoolean())
            .def(RANDOM.nextBoolean())
            .rate(randomBigDecimal())
            .direction(RANDOM.nextInt(2))
            .useThousandSeparator(RANDOM.nextBoolean())
            .uuid(uuid)
            .modified(System.currentTimeMillis())
            .build();
    }

    static Contact newContact() {
        return newContact(UUID.randomUUID());
    }

    static Contact newContact(UUID uuid) {
        return newContact(uuid, null);
    }

    static Contact newContact(UUID uuid, UUID iconUuid) {
        return new Contact.Builder()
            .name(UUID.randomUUID().toString())
            .type(randomContactType())
            .phone(UUID.randomUUID().toString())
            .mobile(UUID.randomUUID().toString())
            .email(UUID.randomUUID().toString())
            .web(UUID.randomUUID().toString())
            .comment(UUID.randomUUID().toString())
            .street(UUID.randomUUID().toString())
            .city(UUID.randomUUID().toString())
            .country(UUID.randomUUID().toString())
            .zip(UUID.randomUUID().toString())
            .iconUuid(iconUuid)
            .uuid(uuid)
            .modified(System.currentTimeMillis())
            .build();
    }

    static Contact newContact(String name) {
        return new Contact.Builder()
            .name(name)
            .type(randomContactType())
            .phone(UUID.randomUUID().toString())
            .mobile(UUID.randomUUID().toString())
            .email(UUID.randomUUID().toString())
            .web(UUID.randomUUID().toString())
            .comment(UUID.randomUUID().toString())
            .street(UUID.randomUUID().toString())
            .city(UUID.randomUUID().toString())
            .country(UUID.randomUUID().toString())
            .zip(UUID.randomUUID().toString())
            .uuid(UUID.randomUUID())
            .created(System.currentTimeMillis())
            .modified(System.currentTimeMillis())
            .build();
    }

    static Transaction newTransaction() {
        return newTransaction(UUID.randomUUID());
    }

    static Transaction newTransaction(UUID uuid) {
        return new Transaction.Builder()
            .uuid(uuid)
            .amount(randomBigDecimal())
            .day(randomDay())
            .month(randomMonth())
            .year(randomYear())
            .type(randomTransactionType())
            .comment(UUID.randomUUID().toString())
            .checked(RANDOM.nextBoolean())
            .accountDebitedUuid(UUID.randomUUID())
            .accountCreditedUuid(UUID.randomUUID())
            .accountDebitedType(randomCategoryType())
            .accountCreditedType(randomCategoryType())
            .accountDebitedCategoryUuid(UUID.randomUUID())
            .accountCreditedCategoryUuid(UUID.randomUUID())
            .contactUuid(UUID.randomUUID())
            .rate(randomBigDecimal())
            .rateDirection(RANDOM.nextInt(2))
            .invoiceNumber(UUID.randomUUID().toString())
            .modified(System.currentTimeMillis())
            .build();
    }

    static Transaction newTransaction(Account accountDebited, Account accountCredited, Contact contact) {
        return new Transaction.Builder()
            .amount(randomBigDecimal())
            .day(randomDay())
            .month(randomMonth())
            .year(randomYear())
            .type(randomTransactionType())
            .comment(UUID.randomUUID().toString())
            .checked(RANDOM.nextBoolean())
            .accountDebitedUuid(accountDebited.uuid())
            .accountCreditedUuid(accountCredited.uuid())
            .accountDebitedType(accountDebited.type())
            .accountCreditedType(accountCredited.type())
            .accountDebitedCategoryUuid(accountDebited.categoryUuid())
            .accountCreditedCategoryUuid(accountCredited.categoryUuid())
            .contactUuid(contact == null ? null : contact.uuid())
            .rate(randomBigDecimal())
            .rateDirection(RANDOM.nextInt(2))
            .invoiceNumber(UUID.randomUUID().toString())
            .uuid(UUID.randomUUID())
            .modified(System.currentTimeMillis())
            .build();
    }

    static Transaction newTransaction(Account accountDebited, Account accountCredited) {
        return newTransaction(accountDebited, accountCredited, null);
    }

    static Icon newIcon(String name) {
        return newIcon(UUID.randomUUID(), name);
    }

    static Icon newIcon(UUID uuid, String name) {
        try (var inputStream = BaseTestUtils.class.getResourceAsStream("/org/panteleyev/money/icons/" + name)) {
            var bytes = inputStream.readAllBytes();
            var timestamp = System.currentTimeMillis();
            return new Icon(uuid, name, bytes, timestamp, timestamp);
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }
}
