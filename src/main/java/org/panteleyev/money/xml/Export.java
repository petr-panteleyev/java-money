/*
 * Copyright (c) 2017, 2018, Petr Panteleyev <petr@panteleyev.org>
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

import org.panteleyev.money.persistence.RecordSource;
import org.panteleyev.money.persistence.model.Account;
import org.panteleyev.money.persistence.model.Category;
import org.panteleyev.money.persistence.model.Contact;
import org.panteleyev.money.persistence.model.Currency;
import org.panteleyev.money.persistence.model.Transaction;
import org.panteleyev.money.persistence.model.TransactionGroup;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Optional;
import java.util.stream.Collectors;
import static org.panteleyev.money.XMLUtils.appendElement;
import static org.panteleyev.money.XMLUtils.appendTextNode;
import static org.panteleyev.money.XMLUtils.createDocument;
import static org.panteleyev.money.XMLUtils.writeDocument;
import static org.panteleyev.money.persistence.MoneyDAO.getDao;

public class Export {
    private final RecordSource source;

    private Collection<Category> categories = new ArrayList<>();
    private Collection<Account> accounts = new ArrayList<>();
    private Collection<Contact> contacts = new ArrayList<>();
    private Collection<Currency> currencies = new ArrayList<>();
    private Collection<TransactionGroup> transactionGroups = new ArrayList<>();
    private Collection<Transaction> transactions = new ArrayList<>();

    public Export() {
        this(getDao());
    }

    public Export(RecordSource source) {
        this.source = source;
    }

    public RecordSource getSource() {
        return source;
    }

    public Export withCategories(Collection<Category> categories) {
        this.categories = categories;
        return this;
    }

    public Export withAccounts(Collection<Account> accounts, boolean withDeps) {
        this.accounts = accounts;

        if (withDeps) {
            categories = accounts.stream()
                    .map(Account::getCategoryId)
                    .distinct()
                    .map(source::getCategory)
                    .flatMap(Optional::stream)
                    .collect(Collectors.toList());

            currencies = accounts.stream()
                    .map(Account::getCurrencyId)
                    .distinct()
                    .map(source::getCurrency)
                    .flatMap(Optional::stream)
                    .collect(Collectors.toList());
        }

        return this;
    }

    public Export withContacts(Collection<Contact> contacts) {
        this.contacts = contacts;
        return this;
    }

    public Export withCurrencies(Collection<Currency> currencies) {
        this.currencies = currencies;
        return this;
    }

    public Export withTransactionGroups(Collection<TransactionGroup> transactionGroups) {
        this.transactionGroups = transactionGroups;
        return this;
    }

    public Export withTransactions(Collection<Transaction> transactions, boolean withDeps) {
        this.transactions = transactions;

        if (withDeps) {
            transactionGroups = transactions.stream()
                    .filter(t -> t.getGroupId() != 0)
                    .map(Transaction::getGroupId)
                    .distinct()
                    .map(source::getTransactionGroup)
                    .flatMap(Optional::stream)
                    .collect(Collectors.toList());

            contacts = transactions.stream()
                    .filter(t -> t.getContactId() != 0)
                    .map(Transaction::getContactId)
                    .distinct()
                    .map(source::getContact)
                    .flatMap(Optional::stream)
                    .collect(Collectors.toList());

            var accIdList = new HashSet<Integer>();
            for (var t : transactions) {
                accIdList.add(t.getAccountDebitedId());
                accIdList.add(t.getAccountCreditedId());
            }
            withAccounts(accIdList.stream()
                    .map(source::getAccount)
                    .flatMap(Optional::stream)
                    .collect(Collectors.toList()), true);
        }

        return this;
    }

    public void doExport(OutputStream out) {
        try {
            var rootElement = createDocument("Money");
            var doc = rootElement.getOwnerDocument();

            var accountRoot = appendElement(rootElement, "Accounts");
            for (var account : accounts) {
                accountRoot.appendChild(exportAccount(doc, account));
            }

            var categoryRoot = appendElement(rootElement, "Categories");
            for (var category : categories) {
                categoryRoot.appendChild(exportCategory(doc, category));
            }

            var contactRoot = appendElement(rootElement, "Contacts");
            for (var contact : contacts) {
                contactRoot.appendChild(exportContact(doc, contact));
            }

            var currencyRoot = appendElement(rootElement, "Currencies");
            for (var currency : currencies) {
                currencyRoot.appendChild(exportCurrency(doc, currency));
            }

            var transactionGroupRoot = appendElement(rootElement, "TransactionGroups");
            for (var transactionGroup : transactionGroups) {
                transactionGroupRoot.appendChild(exportTransactionGroup(doc, transactionGroup));
            }

            var transactionRoot = appendElement(rootElement, "Transactions");
            for (var transaction : transactions) {
                transactionRoot.appendChild(exportTransaction(doc, transaction));
            }

            writeDocument(doc, out);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    private static Element exportCategory(Document doc, Category category) {
        var e = doc.createElement("Category");
        e.setAttribute("id", Integer.toString(category.getId()));

        appendTextNode(e, "name", category.getName());
        appendTextNode(e, "comment", category.getComment());
        appendTextNode(e, "catTypeId", category.getCatTypeId());
        appendTextNode(e, "expanded", category.getExpanded());
        appendTextNode(e, "guid", category.getGuid());
        appendTextNode(e, "modified", category.getModified());

        return e;
    }

    private static Element exportAccount(Document doc, Account account) {
        var e = doc.createElement("Account");
        e.setAttribute("id", Integer.toString(account.getId()));

        appendTextNode(e, "name", account.getName());
        appendTextNode(e, "comment", account.getComment());
        appendTextNode(e, "openingBalance", account.getOpeningBalance());
        appendTextNode(e, "accountLimit", account.getAccountLimit());
        appendTextNode(e, "currencyRate", account.getCurrencyRate());
        appendTextNode(e, "typeId", account.getTypeId());
        appendTextNode(e, "categoryId", account.getCategoryId());
        appendTextNode(e, "currencyId", account.getCurrencyId());
        appendTextNode(e, "enabled", account.getEnabled());
        appendTextNode(e, "guid", account.getGuid());
        appendTextNode(e, "modified", account.getModified());

        return e;
    }

    private static Element exportContact(Document doc, Contact contact) {
        var e = doc.createElement("Contact");
        e.setAttribute("id", Integer.toString(contact.getId()));

        appendTextNode(e, "name", contact.getName());
        appendTextNode(e, "typeId", contact.getTypeId());
        appendTextNode(e, "phone", contact.getPhone());
        appendTextNode(e, "mobile", contact.getMobile());
        appendTextNode(e, "email", contact.getEmail());
        appendTextNode(e, "web", contact.getWeb());
        appendTextNode(e, "comment", contact.getComment());
        appendTextNode(e, "street", contact.getStreet());
        appendTextNode(e, "city", contact.getCity());
        appendTextNode(e, "country", contact.getCountry());
        appendTextNode(e, "zip", contact.getZip());
        appendTextNode(e, "guid", contact.getGuid());
        appendTextNode(e, "modified", contact.getModified());

        return e;
    }

    private static Element exportCurrency(Document doc, Currency currency) {
        var e = doc.createElement("Currency");
        e.setAttribute("id", Integer.toString(currency.getId()));

        appendTextNode(e, "symbol", currency.getSymbol());
        appendTextNode(e, "description", currency.getDescription());
        appendTextNode(e, "formatSymbol", currency.getFormatSymbol());
        appendTextNode(e, "formatSymbolPosition", currency.getFormatSymbolPosition());
        appendTextNode(e, "showFormatSymbol", currency.getShowFormatSymbol());
        appendTextNode(e, "default", currency.getDef());
        appendTextNode(e, "rate", currency.getRate().toString());
        appendTextNode(e, "direction", currency.getDirection());
        appendTextNode(e, "useThousandSeparator", currency.getUseThousandSeparator());
        appendTextNode(e, "guid", currency.getGuid());
        appendTextNode(e, "modified", currency.getModified());

        return e;
    }

    private static Element exportTransactionGroup(Document doc, TransactionGroup tg) {
        var e = doc.createElement("TransactionGroup");
        e.setAttribute("id", Integer.toString(tg.getId()));

        appendTextNode(e, "day", tg.getDay());
        appendTextNode(e, "month", tg.getMonth());
        appendTextNode(e, "year", tg.getYear());
        appendTextNode(e, "expanded", tg.getExpanded());
        appendTextNode(e, "guid", tg.getGuid());
        appendTextNode(e, "modified", tg.getModified());

        return e;
    }

    private static Element exportTransaction(Document doc, Transaction t) {
        var e = doc.createElement("Transaction");
        e.setAttribute("id", Integer.toString(t.getId()));

        appendTextNode(e, "amount", t.getAmount());
        appendTextNode(e, "day", t.getDay());
        appendTextNode(e, "month", t.getMonth());
        appendTextNode(e, "year", t.getYear());
        appendTextNode(e, "transactionTypeId", t.getTransactionTypeId());
        appendTextNode(e, "comment", t.getComment());
        appendTextNode(e, "checked", t.getChecked());
        appendTextNode(e, "accountDebitedId", t.getAccountDebitedId());
        appendTextNode(e, "accountCreditedId", t.getAccountCreditedId());
        appendTextNode(e, "accountDebitedTypeId", t.getAccountDebitedTypeId());
        appendTextNode(e, "accountCreditedTypeId", t.getAccountCreditedTypeId());
        appendTextNode(e, "accountDebitedCategoryId", t.getAccountDebitedCategoryId());
        appendTextNode(e, "accountCreditedCategoryId", t.getAccountCreditedCategoryId());
        appendTextNode(e, "groupId", t.getGroupId());
        appendTextNode(e, "contactId", t.getContactId());
        appendTextNode(e, "rate", t.getRate());
        appendTextNode(e, "rateDirection", t.getRateDirection());
        appendTextNode(e, "invoiceNumber", t.getInvoiceNumber());
        appendTextNode(e, "guid", t.getGuid());
        appendTextNode(e, "modified", t.getModified());

        return e;
    }
}