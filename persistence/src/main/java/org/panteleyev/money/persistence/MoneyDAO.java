/*
 * Copyright (c) 2017, 2019, Petr Panteleyev <petr@panteleyev.org>
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

package org.panteleyev.money.persistence;

import com.mysql.cj.jdbc.MysqlDataSource;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import org.panteleyev.money.model.Account;
import org.panteleyev.money.model.Category;
import org.panteleyev.money.model.Contact;
import org.panteleyev.money.model.Currency;
import org.panteleyev.money.model.Icon;
import org.panteleyev.money.model.MoneyRecord;
import org.panteleyev.money.model.Transaction;
import org.panteleyev.money.xml.Import;
import org.panteleyev.persistence.DAO;
import org.panteleyev.persistence.Record;
import javax.sql.DataSource;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class MoneyDAO extends DAO {
    private static final MoneyDAO MONEY_DAO = new MoneyDAO();

    private static final DataCache cache = DataCache.cache();

    public static MoneyDAO getDao() {
        return MONEY_DAO;
    }

    public static final Comparator<Category> COMPARE_CATEGORY_BY_NAME = Comparator.comparing(Category::getName);
    public static final Comparator<Category> COMPARE_CATEGORY_BY_TYPE =
        (o1, o2) -> o1.getType().getTypeName().compareToIgnoreCase(o2.getType().getTypeName());

    public final static Comparator<Account> COMPARE_ACCOUNT_BY_NAME = Comparator.comparing(Account::getName);
    public final static Comparator<Account> COMPARE_ACCOUNT_BY_CATEGORY = (a1, a2) -> {
        var c1 = cache.getCategory(a1.getCategoryUuid()).map(Category::getName).orElse("");
        var c2 = cache.getCategory(a2.getCategoryUuid()).map(Category::getName).orElse("");
        return c1.compareTo(c2);
    };

    public static final Comparator<Transaction> COMPARE_TRANSACTION_BY_DATE =
        Comparator.comparing(Transaction::getDate).thenComparingLong(Transaction::getCreated);

    public static final Comparator<Transaction> COMPARE_TRANSACTION_BY_DAY =
        Comparator.comparingInt(Transaction::getDay).thenComparingLong(Transaction::getCreated);

    public static final int FIELD_SCALE = 6;

    private static final int BATCH_SIZE = 1000;

    public static final Consumer<String> IGNORE_PROGRESS = x -> {
    };

    private static final List<Class<? extends Record>> TABLE_CLASSES = List.of(
        Icon.class,
        Category.class,
        Contact.class,
        Currency.class,
        Account.class,
        Transaction.class
    );

    private static final List<Class<? extends Record>> TABLE_CLASSES_REVERSED = List.of(
        Transaction.class,
        Account.class,
        Currency.class,
        Contact.class,
        Category.class,
        Icon.class
    );


    private final BooleanProperty preloadingProperty = new SimpleBooleanProperty(false);

    public BooleanProperty preloadingProperty() {
        return preloadingProperty;
    }

    ////////////////////////////////////////////////////////////////////////////
    // Icons
    ////////////////////////////////////////////////////////////////////////////

    public void insertIcon(Icon icon) {
        insert(icon);
        cache.icons().put(icon.getUuid(), icon);
    }

    public void updateIcon(Icon icon) {
        update(icon);
        cache.icons().put(icon.getUuid(), icon);
    }

    ////////////////////////////////////////////////////////////////////////////
    // Categories
    ////////////////////////////////////////////////////////////////////////////

    public void insertCategory(Category category) {
        insert(category);
        cache.categories().put(category.getUuid(), category);
    }

    public void updateCategory(Category category) {
        update(category);
        cache.categories().put(category.getUuid(), category);
    }

    ////////////////////////////////////////////////////////////////////////////
    // Currency
    ////////////////////////////////////////////////////////////////////////////


    public void insertCurrency(Currency currency) {
        insert(currency);
        cache.currencies().put(currency.getUuid(), currency);
    }

    public void updateCurrency(Currency currency) {
        update(currency);
        cache.currencies().put(currency.getUuid(), currency);
    }


    ////////////////////////////////////////////////////////////////////////////
    // Contacts
    ////////////////////////////////////////////////////////////////////////////


    public void insertContact(Contact contact) {
        insert(contact);
        cache.contacts().put(contact.getUuid(), contact);
    }

    public void updateContact(Contact contact) {
        update(contact);
        cache.contacts().put(contact.getUuid(), contact);
    }

    ////////////////////////////////////////////////////////////////////////////
    // Accounts
    ////////////////////////////////////////////////////////////////////////////

    public void insertAccount(Account account) {
        insert(account);
        cache.accounts().put(account.getUuid(), account);
    }

    public void updateAccount(Account account) {
        update(account);
        cache.accounts().put(account.getUuid(), account);
    }

    public void deleteAccount(Account account) {
        cache.accounts().remove(account.getUuid());
        delete(account);
    }

    ////////////////////////////////////////////////////////////////////////////
    // Transactions
    ////////////////////////////////////////////////////////////////////////////

    public void insertTransaction(Transaction transaction) {
        insert(transaction);
        cache.transactions().put(transaction.getUuid(), transaction);
    }

    public void updateTransaction(Transaction transaction) {
        update(transaction);
        cache.transactions().put(transaction.getUuid(), transaction);
    }

    public void deleteTransaction(UUID uuid) {
        delete(uuid, Transaction.class);
        cache.transactions().remove(uuid);
    }

    public void createTables(Connection conn) {
        super.createTables(conn, TABLE_CLASSES);
    }

    public void createTables() {
        super.createTables(TABLE_CLASSES);
    }

    public void dropTables() {
        super.dropTables(TABLE_CLASSES_REVERSED);
    }

    public void preload() {
        preload(IGNORE_PROGRESS);
    }

    public void preload(Consumer<String> progress) {
        synchronized (preloadingProperty) {
            preloadingProperty.set(true);

            progress.accept("Preloading primary keys... ");
            preload(TABLE_CLASSES);
            progress.accept(" done\n");

            progress.accept("Preloading data...\n");

            progress.accept("    icons... ");
            getAll(Icon.class, cache.iconsMap());
            progress.accept("done\n");

            progress.accept("    categories... ");
            getAll(Category.class, cache.categoriesMap());
            progress.accept("done\n");

            progress.accept("    contacts... ");
            getAll(Contact.class, cache.contactsMap());
            progress.accept("done\n");

            progress.accept("    currencies... ");
            getAll(Currency.class, cache.currencyMap());
            progress.accept("done\n");

            progress.accept("    accounts... ");
            getAll(Account.class, cache.accountsMap());
            progress.accept("done\n");

            progress.accept("    transactions... ");
            getAll(Transaction.class, cache.transactionsMap());
            progress.accept("done\n");

            progress.accept("done\n");
            preloadingProperty.set(false);
        }
    }

    private void deleteAll(Connection conn, List<Class<? extends Record>> tables) {
        truncate(conn, tables);
        tables.forEach(t -> {
//            deleteAll(conn, t);
//            resetPrimaryKey(t);
        });
    }

    public void initialize(DataSource ds) {
        synchronized (preloadingProperty) {
            setDataSource(ds, DatabaseType.MYSQL);

            preloadingProperty.set(true);
            cache.clear();
            preloadingProperty.set(false);
        }
    }

    public boolean isOpen() {
        return getDataSource() != null;
    }

    public void importFullDump(Import imp, Consumer<String> progress) {
        try (var conn = getDataSource().getConnection()) {
            progress.accept("Recreating tables... ");
            createTables(conn);
            progress.accept(" done\n");

            progress.accept("Importing data...\n");

            progress.accept("    icons... ");
            insert(conn, BATCH_SIZE, imp.getIcons());
            progress.accept("done\n");

            progress.accept("    categories... ");
            insert(conn, BATCH_SIZE, imp.getCategories());
            progress.accept("done\n");

            progress.accept("    currencies... ");
            insert(conn, BATCH_SIZE, imp.getCurrencies());
            progress.accept("done\n");

            progress.accept("    accounts... ");
            insert(conn, BATCH_SIZE, imp.getAccounts());
            progress.accept("done\n");

            progress.accept("    contacts... ");
            insert(conn, BATCH_SIZE, imp.getContacts());
            progress.accept("done\n");

            progress.accept("    transactions... ");
            insert(conn, BATCH_SIZE,
                imp.getTransactions().stream().filter(t -> t.getParentUuid().isEmpty()).collect(Collectors.toList()));
            insert(conn, BATCH_SIZE,
                imp.getTransactions().stream().filter(t -> t.getParentUuid().isPresent()).collect(Collectors.toList()));
            progress.accept("done\n");

            progress.accept("done\n");
        } catch (SQLException ex) {
            throw new RuntimeException(ex);
        }
    }


    private void calculateActions(Map<UUID, ImportAction> idMap,
                                  Map<UUID, ? extends MoneyRecord> existing,
                                  List<? extends MoneyRecord> toImport)
    {
        for (MoneyRecord record : toImport) {
            var found = existing.get(record.getUuid());
            if (found == null) {
                idMap.put(record.getUuid(), ImportAction.INSERT);
            } else {
                if (record.getModified() > found.getModified()) {
                    idMap.put(record.getUuid(), ImportAction.UPDATE);
                } else {
                    idMap.put(record.getUuid(), ImportAction.IGNORE);
                }
            }
        }
    }

    private <T extends MoneyRecord> void importTable(Connection conn,
                                                     List<? extends T> toImport,
                                                     Map<UUID, ImportAction> importActions)
    {
        for (T item : toImport) {
            switch (importActions.get(item.getUuid())) {
                case IGNORE:
                    continue;

                case INSERT:
                    insert(conn, item);
                    break;

                case UPDATE:
                    update(conn, item);
                    break;
            }
        }
    }

    public void importRecords(Import imp, Consumer<String> progress) {
        var iconActions = new HashMap<UUID, ImportAction>();
        var categoryActions = new HashMap<UUID, ImportAction>();
        var currencyActions = new HashMap<UUID, ImportAction>();
        var contactActions = new HashMap<UUID, ImportAction>();
        var accountActions = new HashMap<UUID, ImportAction>();
        var transactionActions = new HashMap<UUID, ImportAction>();

        calculateActions(iconActions, cache.iconsMap(), imp.getIcons());
        calculateActions(currencyActions, cache.currencyMap(), imp.getCurrencies());
        calculateActions(categoryActions, cache.categoriesMap(), imp.getCategories());
        calculateActions(contactActions, cache.contactsMap(), imp.getContacts());
        calculateActions(accountActions, cache.accountsMap(), imp.getAccounts());
        calculateActions(transactionActions, cache.transactionsMap(), imp.getTransactions());

        try (var conn = getDataSource().getConnection()) {
            try {
                conn.setAutoCommit(false);

                importTable(conn, imp.getIcons(), iconActions);
                importTable(conn, imp.getCategories(), categoryActions);
                importTable(conn, imp.getCurrencies(), currencyActions);
                importTable(conn, imp.getContacts(), contactActions);
                importTable(conn, imp.getAccounts(), accountActions);
                importTable(conn, imp.getTransactions(), transactionActions);

                conn.commit();
            } catch (Exception ex) {
                conn.rollback();
                throw ex;
            }
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    public static Exception initDatabase(MysqlDataSource dataSource, String schema) {
        dataSource.setDatabaseName(null);

        try (var conn = dataSource.getConnection(); Statement st = conn.createStatement()) {
            st.execute("CREATE DATABASE " + schema + " CHARACTER SET = utf8");

            dataSource.setDatabaseName(schema);

            var dao = new DAO(dataSource, DatabaseType.MYSQL);
            dao.createTables(TABLE_CLASSES);

            return null;
        } catch (SQLException ex) {
            return ex;
        }
    }
}