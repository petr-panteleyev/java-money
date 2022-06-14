/*
 Copyright © 2017-2022 Petr Panteleyev <petr@panteleyev.org>
 SPDX-License-Identifier: BSD-2-Clause
 */
package org.panteleyev.money.persistence;

import com.mysql.cj.jdbc.MysqlDataSource;
import javafx.application.Platform;
import org.panteleyev.money.model.Account;
import org.panteleyev.money.model.Category;
import org.panteleyev.money.model.Contact;
import org.panteleyev.money.model.Currency;
import org.panteleyev.money.model.Icon;
import org.panteleyev.money.model.MoneyDocument;
import org.panteleyev.money.model.Transaction;
import org.panteleyev.money.xml.BlobContent;
import org.panteleyev.money.xml.Import;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.logging.Logger;

public class MoneyDAO {
    private final static Logger LOGGER = Logger.getLogger(MoneyDAO.class.getName());

    private final DataCache cache;
    private final AtomicReference<DataSource> dataSource = new AtomicReference<>();
    // Repositories
    private final CategoryRepository categoryRepository = new CategoryRepository();
    private final AccountRepository accountRepository = new AccountRepository();
    private final ContactRepository contactRepository = new ContactRepository();
    private final CurrencyRepository currencyRepository = new CurrencyRepository();
    private final TransactionRepository transactionRepository = new TransactionRepository();
    private final IconRepository iconRepository = new IconRepository();
    private final DocumentRepository documentRepository = new DocumentRepository();

    public static final int FIELD_SCALE = 6;

    private static final int BATCH_SIZE = 1000;

    public static final Consumer<String> IGNORE_PROGRESS = x -> {};

    public MoneyDAO(DataCache cache) {
        this.cache = cache;
    }

    ////////////////////////////////////////////////////////////////////////////
    // Generic methods
    ////////////////////////////////////////////////////////////////////////////
    public void withNewConnection(Consumer<Connection> consumer) {
        try (var connection = dataSource.get().getConnection()) {
            try {
                connection.setAutoCommit(false);
                consumer.accept(connection);
                connection.commit();
            } catch (Exception ex) {
                connection.rollback();
                throw new RuntimeException(ex);
            }
        } catch (SQLException ex) {
            throw new RuntimeException(ex);
        }
    }

    public <R> R withNewConnection(Function<Connection, R> function) {
        try (var connection = dataSource.get().getConnection()) {
            try {
                connection.setAutoCommit(false);
                R result = function.apply(connection);
                connection.commit();
                return result;
            } catch (Exception ex) {
                connection.rollback();
                throw new RuntimeException(ex);
            }
        } catch (SQLException ex) {
            throw new RuntimeException(ex);
        }
    }

    ////////////////////////////////////////////////////////////////////////////
    // Icons
    ////////////////////////////////////////////////////////////////////////////

    public void insertIcon(Icon icon) {
        withNewConnection(conn -> {
            iconRepository.insert(conn, icon);
            cache.add(icon);
        });
    }

    public void updateIcon(Icon icon) {
        withNewConnection(conn -> {
            iconRepository.update(conn, icon);
            cache.update(icon);
        });
    }

    ////////////////////////////////////////////////////////////////////////////
    // Documents
    ////////////////////////////////////////////////////////////////////////////

    public void insertDocument(MoneyDocument document, byte[] bytes) {
        withNewConnection(conn -> {
            documentRepository.insert(conn, document);
            if (bytes != null) {
                documentRepository.insertBytes(conn, document.uuid(), bytes);
            }
            cache.add(document);
        });
    }

    public void updateDocument(MoneyDocument document) {
        withNewConnection(conn -> {
            documentRepository.update(conn, document);
            cache.update(document);
        });
    }

    public byte[] getDocumentBytes(MoneyDocument document) {
        try (var conn = dataSource.get().getConnection()) {
            return documentRepository.getBytes(conn, document.uuid())
                    .orElseThrow(() -> new IllegalStateException("Document content not found"));
        } catch (SQLException ex) {
            throw new RuntimeException(ex);
        }
    }

    ////////////////////////////////////////////////////////////////////////////
    // Categories
    ////////////////////////////////////////////////////////////////////////////

    public void insertCategory(Category category) {
        withNewConnection(conn -> {
            categoryRepository.insert(conn, category);
            cache.add(category);
        });
    }

    public void updateCategory(Category category) {
        withNewConnection(conn -> {
            categoryRepository.update(conn, category);
            cache.update(category);
        });
    }

    ////////////////////////////////////////////////////////////////////////////
    // Currency
    ////////////////////////////////////////////////////////////////////////////

    public void insertCurrency(Currency currency) {
        withNewConnection(conn -> {
            currencyRepository.insert(conn, currency);
            cache.add(currency);
        });
    }

    public void updateCurrency(Currency currency) {
        withNewConnection(conn -> {
            currencyRepository.update(conn, currency);
            cache.update(currency);
        });
    }

    ////////////////////////////////////////////////////////////////////////////
    // Contacts
    ////////////////////////////////////////////////////////////////////////////

    public void insertContact(Contact contact) {
        withNewConnection(conn -> {
            contactRepository.insert(conn, contact);
            cache.add(contact);
        });
    }

    public void updateContact(Contact contact) {
        withNewConnection(conn -> {
            contactRepository.update(conn, contact);
            cache.update(contact);
        });
    }

    ////////////////////////////////////////////////////////////////////////////
    // Accounts
    ////////////////////////////////////////////////////////////////////////////

    public void insertAccount(Account account) {
        withNewConnection(conn -> {
            accountRepository.insert(conn, account);
            cache.add(account);
        });
    }

    public void updateAccount(Account account) {
        withNewConnection(conn -> {
            updateAccount(conn, account);
        });
    }

    public void updateAccount(Connection conn, Account account) {
        accountRepository.update(conn, account);
        cache.update(account);
    }

    public void deleteAccount(Account account) {
        withNewConnection(conn -> {
            cache.remove(account);
            accountRepository.delete(conn, account);
        });
    }

    ////////////////////////////////////////////////////////////////////////////
    // Transactions
    ////////////////////////////////////////////////////////////////////////////

    public void insertTransaction(Transaction transaction) {
        withNewConnection(conn -> {
            transactionRepository.insert(conn, transaction);
            cache.add(transaction);
            updateAccounts(conn, List.of(transaction));
        });
    }

    public void updateTransaction(Transaction transaction) {
        updateTransactions(List.of(transaction));
    }

    public void updateTransactions(Collection<Transaction> transactions) {
        withNewConnection(conn -> {
            var oldAndUpdatedTransactions = new ArrayList<Transaction>(transactions.size() * 2);
            for (var t : transactions) {
                oldAndUpdatedTransactions.add(t);
                oldAndUpdatedTransactions.add(cache.getTransaction(t.uuid()).orElseThrow());
                transactionRepository.update(conn, t);
                cache.update(t);
            }
            updateAccounts(conn, oldAndUpdatedTransactions);
        });
    }

    public void deleteTransaction(Transaction transaction) {
        deleteTransactions(List.of(transaction));
    }

    public void deleteTransactions(Collection<Transaction> transactions) {
        withNewConnection(conn -> {
            for (var t : transactions) {
                transactionRepository.delete(conn, t);
                cache.remove(t);
            }
            updateAccounts(conn, transactions);
        });
    }

    public void checkTransactions(Collection<Transaction> transactions, boolean check) {
        updateTransactions(
                transactions.stream()
                        .filter(t -> t.checked() != check)
                        .map(t -> t.check(check))
                        .toList()
        );
    }

    /**
     * This method recalculates total values for all involved accounts.
     *
     * @param transactions transactions that were added, updated or deleted.
     */
    private void updateAccounts(Connection conn, Collection<Transaction> transactions) {
        var uniqueAccountIds = new HashSet<UUID>();
        for (var t : transactions) {
            uniqueAccountIds.add(t.accountDebitedUuid());
            uniqueAccountIds.add(t.accountCreditedUuid());
        }
        for (var uuid : uniqueAccountIds) {
            cache.getAccount(uuid).ifPresent(account -> {
                var total = cache.calculateBalance(account, false, t -> true);
                var waiting = cache.calculateBalance(account, false, t -> !t.checked());
                updateAccount(conn, account.updateBalance(total, waiting));
            });
        }
    }

    public void createTables() {
        withNewConnection(conn -> {
            new LiquibaseUtil(conn).dropAndUpdate();
        });
    }

    public void preload() {
        preload(IGNORE_PROGRESS);
    }

    public void preload(Consumer<String> progress) {
        withNewConnection(conn -> {
            progress.accept("Preloading data...\n");

            progress.accept("    icons... ");
            var iconList = iconRepository.getAll(conn);
            progress.accept("done\n");

            progress.accept("    categories... ");
            var categoryList = categoryRepository.getAll(conn);
            progress.accept("done\n");

            progress.accept("    contacts... ");
            var contactList = contactRepository.getAll(conn);
            progress.accept("done\n");

            progress.accept("    currencies... ");
            var currencyList = currencyRepository.getAll(conn);
            progress.accept("done\n");

            progress.accept("    accounts... ");
            var accountList = accountRepository.getAll(conn);
            progress.accept("done\n");

            progress.accept("    transactions... ");
            var transactionList = transactionRepository.getAll(conn);
            progress.accept("done\n");

            progress.accept("    documents...");
            var documentList = documentRepository.getAll(conn);
            progress.accept("done\n");

            progress.accept("done\n");

            CompletableFuture.supplyAsync(() -> {
                cache.getIcons().setAll(iconList);
                cache.getDocuments().setAll(documentList);
                cache.getCategories().setAll(categoryList);
                cache.getContacts().setAll(contactList);
                cache.getCurrencies().setAll(currencyList);
                cache.getAccounts().setAll(accountList);
                cache.getTransactions().setAll(transactionList);
                return null;
            }, Platform::runLater);
        });
    }

    public void initialize(DataSource ds) {
        dataSource.set(ds);
        cache.clear();
    }

    public void importFullDump(Import imp, Consumer<String> progress) {
        progress.accept("Recreating tables... ");
        createTables();
        progress.accept("done\n");

        withNewConnection(conn -> {
            progress.accept("Importing data...\n");

            progress.accept("    icons... ");
            iconRepository.insert(conn, BATCH_SIZE, imp.getIcons());
            progress.accept("done\n");

            progress.accept("    categories... ");
            categoryRepository.insert(conn, BATCH_SIZE, imp.getCategories());
            progress.accept("done\n");

            progress.accept("    currencies... ");
            currencyRepository.insert(conn, BATCH_SIZE, imp.getCurrencies());
            progress.accept("done\n");

            progress.accept("    accounts... ");
            accountRepository.insert(conn, BATCH_SIZE, imp.getAccounts());
            progress.accept("done\n");

            progress.accept("    contacts... ");
            contactRepository.insert(conn, BATCH_SIZE, imp.getContacts());
            progress.accept("done\n");

            progress.accept("    transactions... ");
            transactionRepository.insert(conn, BATCH_SIZE,
                    imp.getTransactions().stream().filter(t -> t.parentUuid() == null).toList());
            transactionRepository.insert(conn, BATCH_SIZE,
                    imp.getTransactions().stream().filter(t -> t.parentUuid() != null).toList());
            progress.accept("done\n");

            progress.accept("    documents...");
            documentRepository.insert(conn, BATCH_SIZE, imp.getDocuments());
            progress.accept("done\n");

            progress.accept("done\n");

            progress.accept("Importing blobs...");
            BlobContent blobContent;
            while ((blobContent = imp.getNextBlobContent()) != null) {
                if (blobContent.type() == BlobContent.BlobType.DOCUMENT) {
                    documentRepository.insertBytes(conn, blobContent.uuid(), blobContent.bytes());
                }
            }
            progress.accept("done\n");
        });
    }

    public static Exception resetDatabase(MysqlDataSource dataSource, String schema) {
        try {
            dataSource.setDatabaseName(schema);

            try (var liquibaseConn = dataSource.getConnection()) {
                new LiquibaseUtil(liquibaseConn).dropAndUpdate();
            }

            return null;
        } catch (SQLException ex) {
            return ex;
        }
    }

    public Transaction insertTransaction(Transaction.Builder builder) {
        var newContactName = builder.getNewContactName();
        if (newContactName != null && !newContactName.isEmpty()) {
            var newContact = createContact(newContactName);
            builder.contactUuid(newContact.uuid());
        }

        var transaction = builder
                .modified(0)
                .created(0)
                .build();

        insertTransaction(transaction);
        return transaction;
    }

    public Transaction updateTransaction(Transaction.Builder builder) {
        var newContactName = builder.getNewContactName();
        if (newContactName != null && !newContactName.isEmpty()) {
            var newContact = createContact(newContactName);
            builder.contactUuid(newContact.uuid());
        }

        var transaction = builder
                .modified(0)
                .build();

        updateTransaction(transaction);
        return transaction;
    }

    private Contact createContact(String name) {
        var contact = new Contact.Builder()
                .uuid(UUID.randomUUID())
                .name(name)
                .build();

        insertContact(contact);
        return contact;
    }

    public LiquibaseUtil.SchemaStatus checkSchemaUpdateStatus() {
        return withNewConnection(conn -> {
            return new LiquibaseUtil(conn).checkSchemaUpdateStatus();
        });
    }

    public void updateSchema() {
        withNewConnection(conn -> {
            new LiquibaseUtil(conn).update();
        });
    }
}