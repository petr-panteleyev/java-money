/*
 * Copyright (c) 2018, 2019, Petr Panteleyev <petr@panteleyev.org>
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

package org.panteleyev.money.xml;

import org.panteleyev.money.model.Account;
import org.panteleyev.money.model.Category;
import org.panteleyev.money.model.Contact;
import org.panteleyev.money.model.Currency;
import org.panteleyev.money.model.Icon;
import org.panteleyev.money.model.MoneyRecord;
import org.panteleyev.money.model.Transaction;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import static java.lang.Integer.parseInt;

class ImportParser extends DefaultHandler {
    enum Tag {
        Icon(ImportParser::parseIcon),
        Category(ImportParser::parseCategory),
        Account(ImportParser::parseAccount),
        Currency(ImportParser::parseCurrency),
        Contact(ImportParser::parseContact),
        Transaction(ImportParser::parseTransaction);

        Tag(Function<Map<String, String>, MoneyRecord> parseMethod) {
            this.parseMethod = parseMethod;
        }

        private Function<Map<String, String>, MoneyRecord> parseMethod;

        Function<Map<String, String>, MoneyRecord> getParseMethod() {
            return parseMethod;
        }

        static Optional<Tag> getTag(String name) {
            try {
                return Optional.of(Enum.valueOf(Tag.class, name));
            } catch (Exception ex) {
                return Optional.empty();
            }
        }
    }

    private static final List<String> NAMES = Arrays.stream(Tag.values())
        .map(Tag::name)
        .collect(Collectors.toList());

    private final List<Icon> icons = new ArrayList<>();
    private final List<Category> categories = new ArrayList<>();
    private final List<Account> accounts = new ArrayList<>();
    private final List<Contact> contacts = new ArrayList<>();
    private final List<Currency> currencies = new ArrayList<>();
    private final List<Transaction> transactions = new ArrayList<>();

    private final Map<Tag, List<? extends MoneyRecord>> RECORD_LISTS = Map.ofEntries(
        Map.entry(Tag.Icon, icons),
        Map.entry(Tag.Category, categories),
        Map.entry(Tag.Account, accounts),
        Map.entry(Tag.Currency, currencies),
        Map.entry(Tag.Contact, contacts),
        Map.entry(Tag.Transaction, transactions)
    );

    private Map<String, String> tags = null;
    private final StringBuilder currentCharacters = new StringBuilder();

    public List<Icon> getIcons() {
        return icons;
    }

    public List<Category> getCategories() {
        return categories;
    }

    public List<Account> getAccounts() {
        return accounts;
    }

    public List<Contact> getContacts() {
        return contacts;
    }

    public List<Currency> getCurrencies() {
        return currencies;
    }

    public List<Transaction> getTransactions() {
        return transactions;
    }

    @Override
    public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
        super.startElement(uri, localName, qName, attributes);

        if (NAMES.contains(qName)) {
            tags = new HashMap<>();
        }
    }

    @Override
    public void endElement(String uri, String localName, String qName) throws SAXException {
        super.endElement(uri, localName, qName);

        Tag.getTag(qName).ifPresentOrElse(tag -> {
            var list = RECORD_LISTS.get(tag);
            var record = tag.getParseMethod().apply(tags);
            ((List<MoneyRecord>) list).add(record);
            tags = null;
        }, () -> {
            if (tags != null) {
                tags.put(qName, currentCharacters.toString());
                currentCharacters.setLength(0);
            }
        });
    }

    @Override
    public void characters(char[] ch, int start, int length) throws SAXException {
        super.characters(ch, start, length);
        currentCharacters.append(new String(ch, start, length));
    }

    private static Icon parseIcon(Map<String, String> tags) {
        var modified = parseLong(tags.get("modified"), 0L);
        var created = parseLong(tags.get("created"), modified);

        return new Icon(UUID.fromString(tags.get("uuid")),
            tags.get("name"),
            Base64.getDecoder().decode(tags.get("bytes")),
            created,
            modified);
    }

    private static Category parseCategory(Map<String, String> tags) {
        var modified = parseLong(tags.get("modified"), 0L);
        var created = parseLong(tags.get("created"), modified);
        return new Category.Builder()
            .name(tags.get("name"))
            .comment(tags.get("comment"))
            .catTypeId(parseInt(tags.get("catTypeId")))
            .iconUuid(parseUuid(tags.get("iconUuid")))
            .guid(UUID.fromString(tags.get("guid")))
            .created(created)
            .modified(modified)
            .build();
    }

    private static Account parseAccount(Map<String, String> tags) {
        var modified = parseLong(tags.get("modified"), 0L);
        var created = parseLong(tags.get("created"), modified);
        return new Account.Builder()
            .name(tags.get("name"))
            .comment(tags.get("comment"))
            .accountNumber(tags.get("accountNumber"))
            .openingBalance(new BigDecimal(tags.get("openingBalance")))
            .accountLimit(new BigDecimal(tags.get("accountLimit")))
            .currencyRate(new BigDecimal(tags.get("currencyRate")))
            .typeId(parseInt(tags.get("typeId")))
            .categoryUuid(parseUuid(tags.get("categoryUuid")))
            .currencyUuid(parseUuid(tags.get("currencyUuid")))
            .enabled(parseBoolean(tags.get("enabled"), true))
            .interest(parseBigDecimal(tags.get("interest"), BigDecimal.ZERO))
            .closingDate(parseLocalDate(tags.get("closingDate"), null))
            .iconUuid(parseUuid(tags.get("iconUuid")))
            .guid(UUID.fromString(tags.get("guid")))
            .created(created)
            .modified(modified)
            .build();
    }

    private static Currency parseCurrency(Map<String, String> tags) {
        var modified = parseLong(tags.get("modified"), 0L);
        var created = parseLong(tags.get("created"), modified);
        return new Currency.Builder()
            .symbol(tags.get("symbol"))
            .description(tags.get("description"))
            .formatSymbol(tags.get("formatSymbol"))
            .formatSymbolPosition(parseInt(tags.get("formatSymbolPosition")))
            .showFormatSymbol(parseBoolean(tags.get("showFormatSymbol"), false))
            .def(parseBoolean(tags.get("default"), false))
            .rate(new BigDecimal(tags.get("rate")))
            .direction(parseInt(tags.get("direction")))
            .useThousandSeparator(parseBoolean(tags.get("useThousandSeparator"), false))
            .guid(UUID.fromString(tags.get("guid")))
            .created(created)
            .modified(modified)
            .build();
    }

    private static Contact parseContact(Map<String, String> tags) {
        var modified = parseLong(tags.get("modified"), 0L);
        var created = parseLong(tags.get("created"), modified);
        return new Contact.Builder()
            .name(tags.get("name"))
            .typeId(parseInt(tags.get("typeId")))
            .phone(tags.get("phone"))
            .mobile(tags.get("mobile"))
            .email(tags.get("email"))
            .web(tags.get("web"))
            .comment(tags.get("comment"))
            .street(tags.get("street"))
            .city(tags.get("city"))
            .country(tags.get("country"))
            .zip(tags.get("zip"))
            .iconUuid(parseUuid(tags.get("iconUuid")))
            .guid(UUID.fromString(tags.get("guid")))
            .created(created)
            .modified(modified)
            .build();
    }

    private static Transaction parseTransaction(Map<String, String> tags) {
        var modified = parseLong(tags.get("modified"), 0L);
        var created = parseLong(tags.get("created"), modified);
        return new Transaction.Builder()
            .amount(new BigDecimal(tags.get("amount")))
            .day(parseInt(tags.get("day")))
            .month(parseInt(tags.get("month")))
            .year(parseInt(tags.get("year")))
            .transactionTypeId(parseInt(tags.get("transactionTypeId")))
            .comment(tags.get("comment"))
            .checked(parseBoolean(tags.get("checked"), false))
            .accountDebitedUuid(parseUuid(tags.get("accountDebitedUuid")))
            .accountCreditedUuid(parseUuid(tags.get("accountCreditedUuid")))
            .accountDebitedTypeId(parseInt(tags.get("accountDebitedTypeId")))
            .accountCreditedTypeId(parseInt(tags.get("accountCreditedTypeId")))
            .accountDebitedCategoryUuid(parseUuid(tags.get("accountDebitedCategoryUuid")))
            .accountCreditedCategoryUuid(parseUuid(tags.get("accountCreditedCategoryUuid")))
            .contactUuid(parseUuid(tags.get("contactUuid")))
            .rate(new BigDecimal(tags.get("rate")))
            .rateDirection(parseInt(tags.get("rateDirection")))
            .invoiceNumber(tags.get("invoiceNumber"))
            .parentUuid(parseUuid(tags.get("parentUuid")))
            .detailed(parseBoolean(tags.get("detailed"), false))
            .guid(UUID.fromString(tags.get("guid")))
            .created(created)
            .modified(modified)
            .build();
    }

    private static UUID parseUuid(String value) {
        return value == null ? null : UUID.fromString(value);
    }

    private static BigDecimal parseBigDecimal(String rawValue, BigDecimal defaultValue) {
        return rawValue == null ? defaultValue : new BigDecimal(rawValue);
    }

    private static LocalDate parseLocalDate(String rawValue, LocalDate defaultValue) {
        return rawValue == null ? defaultValue : LocalDate.ofEpochDay(Long.parseLong(rawValue));
    }

    private static boolean parseBoolean(String rawValue, boolean defaultValue) {
        return rawValue == null ? defaultValue : Boolean.parseBoolean(rawValue);
    }

    private static long parseLong(String rawValue, long defaultValue) {
        return rawValue == null ? defaultValue : Long.parseLong(rawValue);
    }
}