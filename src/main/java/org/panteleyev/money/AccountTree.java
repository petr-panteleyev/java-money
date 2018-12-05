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

package org.panteleyev.money;

import javafx.application.Platform;
import javafx.collections.MapChangeListener;
import javafx.geometry.Insets;
import javafx.scene.control.CheckMenuItem;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.Separator;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeTableColumn;
import javafx.scene.control.TreeTableView;
import javafx.scene.control.cell.TreeItemPropertyValueFactory;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import org.panteleyev.money.cells.AccountBalanceCell;
import org.panteleyev.money.persistence.TransactionFilter;
import org.panteleyev.money.persistence.model.Account;
import org.panteleyev.money.persistence.model.Category;
import org.panteleyev.money.persistence.model.CategoryType;
import org.panteleyev.money.persistence.model.Transaction;
import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Predicate;
import static org.panteleyev.money.FXFactory.newMenuItem;
import static org.panteleyev.money.MainWindowController.RB;
import static org.panteleyev.money.persistence.MoneyDAO.getDao;
import static org.panteleyev.money.persistence.dto.Dto.dtoClass;

class AccountTree extends BorderPane {
    private final TreeTableView<AccountTreeItem> tableView = new TreeTableView<>();

    // Filters
    private final ChoiceBox<Object> accountFilterBox = new ChoiceBox<>();
    private final ChoiceBox<Object> transactionFilterBox = new ChoiceBox<>();

    private final CheckMenuItem showDeactivatedAccountsMenuItem = new CheckMenuItem("Show deactivated accounts");

    private final TreeItem<AccountTreeItem> root = new TreeItem<>(new AccountTreeItem("root", "root"));
    private TreeItem<AccountTreeItem> balanceRoot;
    private TreeItem<AccountTreeItem> expIncRoot;

    private TreeItem<AccountTreeItem> banksSubTree;
    private TreeItem<AccountTreeItem> debtsSubTree;
    private TreeItem<AccountTreeItem> portfolioSubTree;
    private TreeItem<AccountTreeItem> assetsSubTree;
    private TreeItem<AccountTreeItem> incomeSubTree;
    private TreeItem<AccountTreeItem> expenseSubTree;

    private final Map<CategoryType, TreeItem<AccountTreeItem>> subRoots = new EnumMap<>(CategoryType.class);

    // tree update globals
    private TreeItem<AccountTreeItem> categoryTreeItem = null;

    // Listeners
    private Consumer<Account> accountSelectedConsumer = x -> {
    };

    private Consumer<Predicate<Transaction>> transactionFilterConsumer = x -> {
    };

    @SuppressWarnings("FieldCanBeLocal")
    private final MapChangeListener<Integer, Account> accountListener =
            (MapChangeListener.Change<? extends Integer, ? extends Account> change) ->
                    Platform.runLater(() -> handleAccountMapChange(change));

    @SuppressWarnings("FieldCanBeLocal")
    private final MapChangeListener<Integer, Transaction> transactionListener =
            change -> Platform.runLater(tableView::refresh);

    AccountTree() {
        for (var type : CategoryType.values()) {
            subRoots.put(type, new TreeItem<>(new AccountTreeItem(type.getTypeName(), type.getComment())));
        }

        banksSubTree = subRoots.get(CategoryType.BANKS_AND_CASH);
        debtsSubTree = subRoots.get(CategoryType.DEBTS);
        portfolioSubTree = subRoots.get(CategoryType.PORTFOLIO);
        assetsSubTree = subRoots.get(CategoryType.ASSETS);
        incomeSubTree = subRoots.get(CategoryType.INCOMES);
        expenseSubTree = subRoots.get(CategoryType.EXPENSES);

        // Table
        var nameColumn = new TreeTableColumn<AccountTreeItem, String>(RB.getString("column.Name"));
        var commentColumn = new TreeTableColumn<AccountTreeItem, String>(RB.getString("column.Comment"));
        var approvedColumn = new TreeTableColumn<AccountTreeItem, Account>(RB.getString("column.Approved"));
        var balanceColumn = new TreeTableColumn<AccountTreeItem, Account>(RB.getString("column.Balance"));
        var waitingColumn = new TreeTableColumn<AccountTreeItem, Account>(RB.getString("column.Waiting"));

        //noinspection unchecked
        tableView.getColumns().setAll(nameColumn, commentColumn, approvedColumn, balanceColumn, waitingColumn);

        // Context menu
        var m1 = newMenuItem(RB, "menu.Edit.newAccount", event -> onNewAccount());
        var m2 = new MenuItem(RB.getString("menu.CopyName"));
        m2.setAccelerator(new KeyCodeCombination(KeyCode.C, KeyCombination.SHORTCUT_DOWN));
        m2.setOnAction(event -> onCopyName());

        tableView.setContextMenu(
                new ContextMenu(
                        m1,
                        new SeparatorMenuItem(),
                        m2,
                        new SeparatorMenuItem(),
                        showDeactivatedAccountsMenuItem
                )
        );

        tableView.setShowRoot(false);

        // Tool box
        var hBox = new HBox(5.0, accountFilterBox, transactionFilterBox);

        setTop(hBox);
        setCenter(tableView);

        BorderPane.setMargin(hBox, new Insets(5.0, 5.0, 5.0, 5.0));

        ////////////////////////////////////////////////////////////////////////////////////////////////////////////////

        initAccountFilterBox();

        balanceRoot = new TreeItem<>(new AccountTreeItem(RB.getString("account.Tree.Balance"), ""));
        expIncRoot = new TreeItem<>(new AccountTreeItem(RB.getString("account.Tree.IncomesExpenses"), "Income - Expenses"));

        nameColumn.setCellValueFactory(new TreeItemPropertyValueFactory<>("name"));
        commentColumn.setCellValueFactory(new TreeItemPropertyValueFactory<>("comment"));
        approvedColumn.setCellValueFactory(new TreeItemPropertyValueFactory<>("account"));
        approvedColumn.setCellFactory((x) -> new AccountBalanceCell(true, Transaction::getChecked));
        balanceColumn.setCellValueFactory(new TreeItemPropertyValueFactory<>("account"));
        balanceColumn.setCellFactory(x -> new AccountBalanceCell(true, t -> true));
        waitingColumn.setCellValueFactory(new TreeItemPropertyValueFactory<>("account"));
        waitingColumn.setCellFactory(x -> new AccountBalanceCell(t -> !t.getChecked()));

        nameColumn.prefWidthProperty().bind(tableView.widthProperty().subtract(20).multiply(0.3));
        commentColumn.prefWidthProperty().bind(tableView.widthProperty().subtract(20).multiply(0.4));
        approvedColumn.prefWidthProperty().bind(tableView.widthProperty().subtract(20).multiply(0.1));
        balanceColumn.prefWidthProperty().bind(tableView.widthProperty().subtract(20).multiply(0.1));
        waitingColumn.prefWidthProperty().bind(tableView.widthProperty().subtract(20).multiply(0.1));

        initTreeSkeleton();

        balanceRoot.setExpanded(true);
        banksSubTree.setExpanded(true);
        debtsSubTree.setExpanded(true);
        expIncRoot.setExpanded(true);

        showDeactivatedAccountsMenuItem.setSelected(Options.getShowDeactivatedAccounts());

        tableView.getSelectionModel().selectedItemProperty()
                .addListener((x, y, newItem) -> onTreeItemSelected(newItem));

        accountFilterBox.getSelectionModel().selectedIndexProperty()
                .addListener((x, y, newValue) -> onAccountFilterSelected((Integer) newValue));

        transactionFilterBox.getSelectionModel().selectedItemProperty()
                .addListener((x, y, newValue) -> onTransactionFilterSelected(newValue));

        getDao().accounts().addListener(accountListener);
        getDao().transactions().addListener(transactionListener);

        getDao().preloadingProperty().addListener((x, y, newValue) -> {
            if (!newValue) {
                Platform.runLater(this::initTransactionFilterBox);
                Platform.runLater(this::initAccountTree);
            }
        });
    }

    void clear() {
        banksSubTree.getChildren().clear();
        portfolioSubTree.getChildren().clear();
        assetsSubTree.getChildren().clear();
        debtsSubTree.getChildren().clear();
        incomeSubTree.getChildren().clear();
        expenseSubTree.getChildren().clear();
    }

    private void initAccountFilterBox() {
        accountFilterBox.getItems().setAll(
                RB.getString("text.All.Accounts"),
                new Separator(),
                RB.getString("account.Tree.Balance"),
                RB.getString("account.Tree.IncomesExpenses"),
                new Separator()
        );

        for (var t : CategoryType.values()) {
            accountFilterBox.getItems().add(t.getTypeName());
        }

        accountFilterBox.getSelectionModel().select(0);
    }

    private void initTransactionFilterBox() {
        transactionFilterBox.getItems().setAll(
                TransactionFilter.ALL,
                new Separator(),
                TransactionFilter.CURRENT_YEAR,
                TransactionFilter.CURRENT_MONTH,
                TransactionFilter.CURRENT_WEEK,
                new Separator(),
                TransactionFilter.LAST_YEAR,
                TransactionFilter.LAST_QUARTER,
                TransactionFilter.LAST_MONTH,
                new Separator()
        );

        for (int i = TransactionFilter.JANUARY.ordinal(); i <= TransactionFilter.DECEMBER.ordinal(); i++) {
            transactionFilterBox.getItems().add(TransactionFilter.values()[i]);
        }

        transactionFilterBox.getItems().add(new Separator());

        getDao().getTransactions().stream()
                .map(Transaction::getYear)
                .distinct()
                .sorted()
                .forEach(transactionFilterBox.getItems()::add);

        transactionFilterBox.getSelectionModel().select(0);
    }

    private void initSubtree(CategoryType categoryType) {
        var rootItem = subRoots.get(categoryType);
        rootItem.getChildren().clear();

        categoryTreeItem = null;

        getDao().getAccountsByType(categoryType).stream()
                .filter(a -> a.getEnabled() || (!a.getEnabled() && Options.getShowDeactivatedAccounts()))
                .sorted(new Account.AccountCategoryNameComparator())
                .forEach(it -> {
                    if (categoryTreeItem == null || it.getCategoryId() != categoryTreeItem.getValue().getCategoryId()) {
                        getDao().getCategory(it.getCategoryId()).ifPresent(category -> {
                            categoryTreeItem = new TreeItem<>(new AccountTreeItem(category));
                            categoryTreeItem.setExpanded(category.getExpanded());

                            categoryTreeItem.expandedProperty().addListener((x, y, newValue) ->
                                    getDao().updateCategory(category.expand(newValue)));

                            rootItem.getChildren().add(categoryTreeItem);
                        });
                    }

                    categoryTreeItem.getChildren().add(new TreeItem<>(new AccountTreeItem(it)));
                });
    }

    @SuppressWarnings("unchecked")
    private void initTreeSkeleton() {
        root.getChildren().setAll(balanceRoot, expIncRoot);
        balanceRoot.getChildren().setAll(banksSubTree, portfolioSubTree, assetsSubTree, debtsSubTree);
        expIncRoot.getChildren().setAll(incomeSubTree, expenseSubTree);
        tableView.setRoot(root);
    }

    private void initAccountTree() {
        for (var t : CategoryType.values()) {
            initSubtree(t);
        }
    }

    private void onAccountFilterSelected(Integer newValue) {
        switch (newValue) {
            case 0:
                initTreeSkeleton();
                break;

            case 2:
                tableView.setRoot(balanceRoot);
                break;

            case 3:
                tableView.setRoot(expIncRoot);
                break;
            default:
                CategoryType type = CategoryType.values()[newValue - 5];
                tableView.setRoot(subRoots.get(type));
                break;
        }
    }

    private void onTransactionFilterSelected(Object newValue) {
        if (newValue instanceof TransactionFilter) {
            transactionFilterConsumer.accept(((TransactionFilter) newValue).predicate());
        } else if (newValue instanceof Integer) {
            transactionFilterConsumer.accept(TransactionFilter.byYear((int) newValue));
        }
    }

    private void onShowDeactivatedAccounts() {
        Options.setShowDeactivatedAccounts(showDeactivatedAccountsMenuItem.isSelected());
        if (getDao().isOpen()) {
            initAccountTree();
        }
    }

    private void onNewAccount() {
        Category initialCategory = null;

        var selectedItem = tableView.getSelectionModel().getSelectedItem();
        if (selectedItem != null) {
            var account = selectedItem.getValue().accountProperty().getValue();
            initialCategory = account != null ?
                    getDao().getCategory(account.getCategoryId()).orElse(null) :
                    selectedItem.getValue().categoryProperty().getValue();
        }

        new AccountDialog(initialCategory).showAndWait().ifPresent(it -> {
            getDao().insertAccount(it.copy(getDao().generatePrimaryKey(dtoClass(Account.class))));
        });
    }

    void setOnAccountSelected(Consumer<Account> consumer) {
        accountSelectedConsumer = consumer;
    }

    void setOnTransactionFilterSelected(Consumer<Predicate<Transaction>> consumer) {
        transactionFilterConsumer = consumer;
    }

    private void onTreeItemSelected(TreeItem<AccountTreeItem> item) {
        accountSelectedConsumer.accept(item.getValue().accountProperty().getValue());
    }

    private void onCopyName() {
        Optional.ofNullable(tableView.getSelectionModel().getSelectedItem())
                .map(TreeItem::getValue)
                .map(item -> item.nameProperty().get())
                .ifPresent(name -> {
                    Clipboard cb = Clipboard.getSystemClipboard();
                    ClipboardContent ct = new ClipboardContent();
                    ct.putString(name);
                    cb.setContent(ct);
                });
    }

    private TreeItem<AccountTreeItem> findAccountTreeItem(TreeItem<AccountTreeItem> node, int accountId) {
        if (node.getValue().getAccountId() == accountId) {
            return node;
        }

        for (var child : node.getChildren()) {
            var found = findAccountTreeItem(child, accountId);
            if (found != null) {
                return found;
            }
        }

        return null;
    }

    private TreeItem<AccountTreeItem> findCategoryTreeItem(TreeItem<AccountTreeItem> node, int categoryId) {
        if (node.getValue().getCategoryId() == categoryId) {
            return node;
        }

        for (var child : node.getChildren()) {
            var found = findCategoryTreeItem(child, categoryId);
            if (found != null) {
                return found;
            }
        }

        return null;
    }

    // Must be executed in UI thread
    private void handleAccountMapChange(MapChangeListener.Change<? extends Integer, ? extends Account> change) {
        var removedAccount = change.getValueRemoved();
        var addedAccount = change.getValueAdded();
        if (removedAccount == null && addedAccount == null) {
            return;
        }

        if (removedAccount == null) {
            if (Options.getShowDeactivatedAccounts() || addedAccount.getEnabled()) {
                // Add account
                var categoryNode = findCategoryTreeItem(root, addedAccount.getCategoryId());
                if (categoryNode == null) {
                    throw new IllegalStateException("Cannot find node for category id: " + addedAccount.getCategoryId());
                }

                categoryNode.getChildren().add(new TreeItem<>(new AccountTreeItem(addedAccount)));
            }
        } else {
            var node = findAccountTreeItem(root, change.getKey());
            if (node == null) {
                return;
            }

            if (addedAccount == null) {
                // Remove account
                node.getParent().getChildren().remove(node);
            } else {
                // Update account
                node.setValue(new AccountTreeItem(addedAccount));
            }
        }
    }
}
