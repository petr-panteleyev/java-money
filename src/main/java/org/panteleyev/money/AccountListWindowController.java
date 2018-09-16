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
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.FXCollections;
import javafx.collections.MapChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.WeakMapChangeListener;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Menu;
import javafx.scene.control.Separator;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import org.panteleyev.money.persistence.model.Account;
import org.panteleyev.money.persistence.model.Category;
import org.panteleyev.money.persistence.model.CategoryType;
import org.panteleyev.money.persistence.model.Currency;
import org.panteleyev.money.persistence.ReadOnlyStringConverter;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import static org.panteleyev.money.FXFactory.newMenuBar;
import static org.panteleyev.money.FXFactory.newMenuItem;
import static org.panteleyev.money.MainWindowController.RB;
import static org.panteleyev.money.persistence.MoneyDAO.getDao;
import static org.panteleyev.money.persistence.dto.Dto.dtoClass;

class AccountListWindowController extends BaseController {
    private final ChoiceBox<Object> typeChoiceBox = new ChoiceBox<>();
    private final ChoiceBox<Object> categoryChoiceBox = new ChoiceBox<>();
    private final CheckBox showActiveCheckBox = new CheckBox(RB.getString("account.Window.ShowOnlyActive"));
    private final TableView<Account> accountListTable = new TableView<>();

    @SuppressWarnings("FieldCanBeLocal")
    private final MapChangeListener<Integer, Account> accountsListener =
            change -> Platform.runLater(this::reloadAccounts);

    AccountListWindowController() {
        var disableBinding = accountListTable.getSelectionModel().selectedItemProperty().isNull();

        var menuBar = newMenuBar(
                new Menu(RB.getString("menu.File"), null,
                        newMenuItem(RB, "menu.File.Close", event -> onClose())),
                new Menu(RB.getString("menu.Edit"), null,
                        newMenuItem(RB, "menu.Edit.Add", event -> onAddAccount()),
                        newMenuItem(RB, "menu.Edit.Edit", event -> onEditAccount(), disableBinding),
                        new SeparatorMenuItem(),
                        newMenuItem(RB, "menu.Edit.Delete", event -> onDeleteAccount(), disableBinding)),
                createHelpMenu(RB));

        // Context menu
        accountListTable.setContextMenu(new ContextMenu(
                newMenuItem(RB, "menu.Edit.Add", event -> onAddAccount()),
                newMenuItem(RB, "menu.Edit.Edit", event -> onEditAccount(), disableBinding),
                new SeparatorMenuItem(),
                newMenuItem(RB, "menu.Edit.Delete", event -> onDeleteAccount(), disableBinding))
        );

        // Table
        var idColumn = new TableColumn<Account, Integer>("ID");
        var nameColumn = new TableColumn<Account, String>(RB.getString("column.Name"));
        var typeColumn = new TableColumn<Account, String>(RB.getString("column.Type"));
        var categoryColumn = new TableColumn<Account, String>(RB.getString("column.Category"));
        var currencyColumn = new TableColumn<Account, String>(RB.getString("column.Currency"));
        var balanceColumn = new TableColumn<Account, BigDecimal>(RB.getString("column.InitialBalance"));
        var activeColumn = new TableColumn<Account, CheckBox>("A");

        accountListTable.getColumns().setAll(List.of(
                idColumn, nameColumn, typeColumn, categoryColumn,
                currencyColumn, balanceColumn, activeColumn));

        // Content
        var pane = new BorderPane();

        // Toolbox
        var hBox = new HBox(typeChoiceBox, categoryChoiceBox, showActiveCheckBox);
        hBox.setAlignment(Pos.CENTER_LEFT);
        HBox.setMargin(categoryChoiceBox, new Insets(0.0, 0.0, 0.0, 5.0));
        HBox.setMargin(showActiveCheckBox, new Insets(0.0, 0.0, 0.0, 10.0));

        pane.setTop(hBox);
        pane.setCenter(accountListTable);
        BorderPane.setMargin(hBox, new Insets(5.0, 5.0, 5.0, 5.0));

        var self = new BorderPane();
        self.setPrefSize(800.0, 400.0);
        self.setTop(menuBar);
        self.setCenter(pane);

        ////////////////////////////////////////////////////////////////////////////////////////////////////////////////

        ObservableList<Object> types = FXCollections.observableArrayList(CategoryType.values());
        if (!types.isEmpty()) {
            types.add(0, new Separator());
        }
        types.add(0, RB.getString("account.Window.AllTypes"));

        typeChoiceBox.setItems(types);
        typeChoiceBox.getSelectionModel().select(0);

        typeChoiceBox.setConverter(new ReadOnlyStringConverter<>() {
            @Override
            public String toString(Object object) {
                if (object instanceof CategoryType) {
                    return ((CategoryType) object).getTypeName();
                } else {
                    return object != null ? object.toString() : "-";
                }
            }
        });

        typeChoiceBox.valueProperty().addListener((x, y, newValue) -> onTypeChanged(newValue));

        categoryChoiceBox.setConverter(new ReadOnlyStringConverter<>() {
            @Override
            public String toString(Object object) {
                if (object instanceof Category) {
                    return ((Category) object).getName();
                } else {
                    return object != null ? object.toString() : "-";
                }
            }
        });

        categoryChoiceBox.setItems(FXCollections.observableArrayList(RB.getString("account.Window.AllCategories")));
        categoryChoiceBox.getSelectionModel().select(0);
        categoryChoiceBox.valueProperty().addListener((x, y, z) -> reloadAccounts());

        showActiveCheckBox.setOnAction(event -> reloadAccounts());

        idColumn.setCellValueFactory((TableColumn.CellDataFeatures<Account, Integer> p) ->
                new ReadOnlyObjectWrapper<>(p.getValue().getId()));
        nameColumn.setCellValueFactory((TableColumn.CellDataFeatures<Account, String> p) ->
                new ReadOnlyObjectWrapper<>(p.getValue().getName()));

        typeColumn.setCellValueFactory((TableColumn.CellDataFeatures<Account, String> p) ->
                new ReadOnlyObjectWrapper<>(p.getValue().getType().getTypeName()));
        categoryColumn.setCellValueFactory((TableColumn.CellDataFeatures<Account, String> p) ->
                new ReadOnlyObjectWrapper<>(getDao().getCategory(p.getValue().getCategoryId()).map(Category::getName).orElse("")));
        currencyColumn.setCellValueFactory((TableColumn.CellDataFeatures<Account, String> p) ->
                new ReadOnlyObjectWrapper<>(getDao().getCurrency(p.getValue().getCurrencyId()).map(Currency::getSymbol).orElse("")));
        balanceColumn.setCellValueFactory((TableColumn.CellDataFeatures<Account, BigDecimal> p) ->
                new ReadOnlyObjectWrapper<>(p.getValue().getOpeningBalance().setScale(2, RoundingMode.HALF_UP)));
        activeColumn.setCellValueFactory((TableColumn.CellDataFeatures<Account, CheckBox> p) -> {
            var account = p.getValue();
            var cb = new CheckBox();

            cb.setSelected(account.getEnabled());
            cb.setOnAction(event -> {
                boolean enabled = account.getEnabled();
                getDao().updateAccount(account.enable(!enabled));
            });

            return new ReadOnlyObjectWrapper<>(cb);
        });

        reloadAccounts();

        getDao().accounts().addListener(new WeakMapChangeListener<>(accountsListener));

        setupWindow(self);
    }

    public String getTitle() {
        return RB.getString("account.Window.Title");
    }

    private void reloadAccounts() {
        Collection<Account> accounts;

        var catObject = categoryChoiceBox.getSelectionModel().getSelectedItem();
        if (catObject instanceof Category) {
            accounts = getDao().getAccountsByCategory(((Category) catObject).getId());
        } else {
            var typeObject = typeChoiceBox.getSelectionModel().getSelectedItem();
            if (typeObject instanceof CategoryType) {
                accounts = getDao().getAccountsByType((CategoryType) typeObject);
            } else {
                accounts = getDao().getAccounts();
            }
        }

        if (showActiveCheckBox.isSelected()) {
            accounts = accounts.stream()
                    .filter(Account::getEnabled)
                    .collect(Collectors.toList());
        }

        accountListTable.setItems(FXCollections.observableArrayList(accounts));
    }

    private void onTypeChanged(Object newValue) {
        ObservableList<Object> items;

        if (newValue instanceof String) {
            items = FXCollections.observableArrayList(RB.getString("account.Window.AllCategories"));
        } else {
            items = FXCollections.observableArrayList(getDao().getCategoriesByType((CategoryType) newValue));

            if (!items.isEmpty()) {
                items.add(0, new Separator());
            }
            items.add(0, RB.getString("account.Window.AllCategories"));
        }

        categoryChoiceBox.setItems(items);
        categoryChoiceBox.getSelectionModel().select(0);
    }

    private void onAddAccount() {
        new AccountDialog(null, null).showAndWait()
                .ifPresent(a -> getDao().insertAccount(a.copy(getDao().generatePrimaryKey(dtoClass(Account.class)))));
    }

    private Optional<Account> getSelectedAccount() {
        return Optional.ofNullable(accountListTable.getSelectionModel().getSelectedItem());
    }

    private void onEditAccount() {
        getSelectedAccount().ifPresent(account ->
                new AccountDialog(account, null).showAndWait()
                        .ifPresent(it -> getDao().updateAccount(it)));
    }

    private void onDeleteAccount() {
        getSelectedAccount().ifPresent(account -> {
            long count = getDao().getTransactionCount(account);
            if (count != 0L) {
                new Alert(Alert.AlertType.ERROR,
                        "Unable to delete account\nwith " + count + " associated transactions",
                        ButtonType.CLOSE).showAndWait();
            } else {
                new Alert(Alert.AlertType.CONFIRMATION, RB.getString("text.AreYouSure"), ButtonType.OK, ButtonType.CANCEL)
                        .showAndWait()
                        .filter(response -> response == ButtonType.OK)
                        .ifPresent(b -> getDao().deleteAccount(account));
            }
        });
    }
}