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

package org.panteleyev.money;

import javafx.application.Platform;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.MapChangeListener;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Control;
import javafx.scene.control.Label;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TextField;
import javafx.scene.control.TitledPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;
import javafx.util.converter.IntegerStringConverter;
import org.controlsfx.control.textfield.TextFields;
import org.controlsfx.validation.ValidationResult;
import org.controlsfx.validation.ValidationSupport;
import org.controlsfx.validation.Validator;
import org.panteleyev.money.model.Account;
import org.panteleyev.money.model.Category;
import org.panteleyev.money.model.CategoryType;
import org.panteleyev.money.model.Contact;
import org.panteleyev.money.model.Named;
import org.panteleyev.money.model.Transaction;
import org.panteleyev.money.model.TransactionType;
import org.panteleyev.money.persistence.DataCache;
import org.panteleyev.money.persistence.MoneyDAO;
import org.panteleyev.money.statements.StatementRecord;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Calendar;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import static org.panteleyev.money.persistence.MoneyDAO.FIELD_SCALE;
import static org.panteleyev.money.persistence.MoneyDAO.getDao;

public final class TransactionEditorPane extends TitledPane {
    private static final ToStringConverter<TransactionType> TRANSACTION_TYPE_TO_STRING =
        new ToStringConverter<>() {
            public String toString(TransactionType obj) {
                return obj.getTypeName();
            }
        };

    private static final ToStringConverter<Contact> CONTACT_TO_STRING = new ToStringConverter<>() {
        public String toString(Contact obj) {
            return obj.getName();
        }
    };

    private static final ToStringConverter<Account> ACCOUNT_TO_STRING = new ToStringConverter<>() {
        public String toString(Account obj) {
            return obj.getName();
        }
    };

    private static class CompletionProvider<T extends Named> extends BaseCompletionProvider<T> {
        CompletionProvider(Set<T> set) {
            super(set, Options::getAutoCompleteLength);
        }

        public String getElementString(T element) {
            return element.getName();
        }
    }

    private static class TransactionTypeCompletionProvider extends BaseCompletionProvider<TransactionType> {
        TransactionTypeCompletionProvider(Set<TransactionType> set) {
            super(set, Options::getAutoCompleteLength);
        }


        public String getElementString(TransactionType element) {
            return element.getTypeName();
        }
    }

    private class StringCompletionProvider extends BaseCompletionProvider<String> {
        StringCompletionProvider(Set<String> set) {
            super(set, Options::getAutoCompleteLength);
        }

        public String getElementString(String element) {
            return element;
        }
    }

    private ResourceBundle rb = ResourceBundle.getBundle("org.panteleyev.money.res.TransactionEditorPane");

    private final DataCache cache;

    private Spinner<Integer> daySpinner = new Spinner<>();

    private final TextField typeEdit = new TextField();
    private final TextField debitedAccountEdit = new TextField();
    private final TextField creditedAccountEdit = new TextField();
    private final TextField contactEdit = new TextField();
    private final TextField sumEdit = new TextField();
    private final CheckBox checkedCheckBox = new CheckBox();
    private final TextField commentEdit = new TextField();
    private final TextField rate1Edit = new TextField();
    private final ComboBox<String> rateDir1Combo = new ComboBox<>();
    private final TextField invoiceNumberEdit = new TextField();
    private final Label rateAmoutLabel = new Label();
    private final Label debitedCategoryLabel = new Label();
    private final Label creditedCategoryLabel = new Label();

    private final MenuButton typeMenuButton = new MenuButton();
    private final MenuButton debitedMenuButton = new MenuButton();
    private final MenuButton creditedMenuButton = new MenuButton();
    private final MenuButton contactMenuButton = new MenuButton();

    private final Button addButton = new Button(rb.getString("addButton"));
    private final Button updateButton = new Button(rb.getString("updateButton"));
    private final Button deleteButton = new Button(rb.getString("deleteButton"));
    private final Button clearButton = new Button(rb.getString("clearButton"));

    private BiConsumer<Transaction.Builder, String> addTransactionConsumer = (x, y) -> { };
    private BiConsumer<Transaction.Builder, String> updateTransactionConsumer = (x, y) -> { };
    private Consumer<UUID> deleteTransactionConsumer = (x) -> { };

    private Transaction.Builder builder = new Transaction.Builder();

    private final TreeSet<TransactionType> typeSuggestions = new TreeSet<>();
    private final TreeSet<Contact> contactSuggestions = new TreeSet<>();
    private final TreeSet<Account> debitedSuggestions = new TreeSet<>();
    private final TreeSet<Account> debitedSuggestionsAll = new TreeSet<>();
    private final TreeSet<Account> creditedSuggestions = new TreeSet<>();
    private final TreeSet<Account> creditedSuggestionsAll = new TreeSet<>();
    private final TreeSet<String> commentSuggestions = new TreeSet<>();

    private final ValidationSupport validation = new ValidationSupport();

    private final SimpleBooleanProperty newTransactionProperty = new SimpleBooleanProperty(true);

    private final Validator<String> DECIMAL_VALIDATOR = (Control control, String value) -> {
        var invalid = false;
        try {
            new BigDecimal(value);
            updateRateAmount();
        } catch (NumberFormatException ex) {
            invalid = true;
        }

        return ValidationResult.fromErrorIf(control, null, invalid && !control.isDisabled());
    };

    private String newContactName = "";

    @SuppressWarnings("FieldCanBeLocal")
    private final MapChangeListener<UUID, Contact> contactListener =
        change -> Platform.runLater(this::setupContactMenu);
    @SuppressWarnings("FieldCanBeLocal")
    private final MapChangeListener<UUID, Account> accountListener =
        change -> Platform.runLater(this::setupAccountMenus);
    @SuppressWarnings("FieldCanBeLocal")
    private final MapChangeListener<UUID, Transaction> transactionListener =
        this::transactionChangeListener;

    TransactionEditorPane(DataCache cache) {
        this.cache = cache;

        var debitedBox = new VBox(Styles.SMALL_SPACING, new Label(rb.getString("debitedAccountLabel")),
            new HBox(debitedAccountEdit, debitedMenuButton),
            debitedCategoryLabel);
        HBox.setHgrow(debitedAccountEdit, Priority.ALWAYS);

        var creditedBox = new VBox(Styles.SMALL_SPACING, new Label(rb.getString("creditedAccountLabel")),
            new HBox(creditedAccountEdit, creditedMenuButton),
            creditedCategoryLabel);
        HBox.setHgrow(creditedAccountEdit, Priority.ALWAYS);

        var contactBox = new VBox(Styles.SMALL_SPACING, new Label(rb.getString("contactLabel")),
            new HBox(contactEdit, contactMenuButton));
        HBox.setHgrow(contactEdit, Priority.ALWAYS);

        var hBox1 = new HBox(Styles.BIG_SPACING, sumEdit, checkedCheckBox);
        hBox1.setAlignment(Pos.CENTER_LEFT);
        var sumBox = new VBox(Styles.SMALL_SPACING, new Label(rb.getString("sumLabel")), hBox1);

        var commentBox = new VBox(Styles.SMALL_SPACING, new Label(rb.getString("commentLabel")), commentEdit);

        var rateBox = new VBox(Styles.SMALL_SPACING, new Label(rb.getString("rateLabel")),
            new HBox(rate1Edit, rateDir1Combo),
            rateAmoutLabel);

        var filler = new Region();

        clearButton.setOnAction(event -> onClearButton());
        clearButton.setCancelButton(true);

        addButton.setOnAction(event -> onAddButton());
        addButton.setDefaultButton(true);

        updateButton.setOnAction(event -> onUpdateButton());

        deleteButton.setOnAction(event -> onDeleteButton());

        var row3 = new HBox(Styles.BIG_SPACING,
            new VBox(2.0, new Label(rb.getString("invoiceLabel")), invoiceNumberEdit),
            filler, clearButton, deleteButton, updateButton, addButton);
        row3.setAlignment(Pos.CENTER_LEFT);

        setContent(new VBox(Styles.BIG_SPACING,
            new HBox(Styles.BIG_SPACING,
                new VBox(Styles.SMALL_SPACING, new Label(rb.getString("dayLabel")), daySpinner),
                new VBox(Styles.SMALL_SPACING, new Label(rb.getString("typeLabel")),
                    new HBox(typeEdit, typeMenuButton)),
                debitedBox, creditedBox, contactBox, sumBox),
            new HBox(Styles.BIG_SPACING, commentBox, rateBox),
            row3));

        HBox.setHgrow(debitedBox, Priority.ALWAYS);
        HBox.setHgrow(creditedBox, Priority.ALWAYS);
        HBox.setHgrow(contactBox, Priority.ALWAYS);
        HBox.setHgrow(commentBox, Priority.ALWAYS);
        HBox.setHgrow(filler, Priority.ALWAYS);

        typeMenuButton.setFocusTraversable(false);
        checkedCheckBox.setFocusTraversable(false);
        debitedMenuButton.setFocusTraversable(false);
        creditedMenuButton.setFocusTraversable(false);
        contactMenuButton.setFocusTraversable(false);

        rate1Edit.setDisable(true);
        rate1Edit.setPrefColumnCount(5);

        rateDir1Combo.setDisable(true);

        rateAmoutLabel.getStyleClass().add(Styles.RATE_LABEL);
        debitedCategoryLabel.getStyleClass().add(Styles.SUB_LABEL);
        creditedCategoryLabel.getStyleClass().add(Styles.SUB_LABEL);

        ////////////////////////////////////////////////////////////////////////////////////////////////////////////////

        clearTitle();

        daySpinner.setEditable(true);
        daySpinner.getEditor().prefColumnCountProperty().set(4);
        var valueFactory =
            new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 31, 1, 1);
        valueFactory.setValue(Calendar.getInstance().get(Calendar.DAY_OF_MONTH));
        valueFactory.setConverter(new IntegerStringConverter() {
            @Override
            public Integer fromString(String s) {
                if (s == null || s.isBlank()) {
                    valueFactory.valueProperty().set(-1);       // enforce internal change listeners
                    return LocalDate.now().getDayOfMonth();
                } else {
                    return super.fromString(s);
                }
            }
        });
        daySpinner.setValueFactory(valueFactory);

        deleteButton.disableProperty().bind(newTransactionProperty);
        updateButton.disableProperty().bind(validation.invalidProperty().or(newTransactionProperty));
        addButton.disableProperty().bind(validation.invalidProperty());

        rateDir1Combo.getItems().setAll("/", "*");

        TextFields.bindAutoCompletion(typeEdit,
            new TransactionTypeCompletionProvider(typeSuggestions), TRANSACTION_TYPE_TO_STRING);
        TextFields.bindAutoCompletion(debitedAccountEdit,
            new CompletionProvider<>(debitedSuggestions), ACCOUNT_TO_STRING);
        TextFields.bindAutoCompletion(creditedAccountEdit,
            new CompletionProvider<>(creditedSuggestions), ACCOUNT_TO_STRING);
        TextFields.bindAutoCompletion(contactEdit, new CompletionProvider<>(contactSuggestions), CONTACT_TO_STRING);
        TextFields.bindAutoCompletion(commentEdit, new StringCompletionProvider(commentSuggestions));

        Platform.runLater(this::createValidationSupport);

        creditedAccountEdit.focusedProperty().addListener((x, oldValue, newValue) -> {
            if (oldValue && !newValue) {
                processAutoFill();
            }
        });

        typeEdit.focusedProperty().addListener((x, oldValue, newValue) -> {
            if (oldValue && !newValue) {
                handleTypeFocusLoss();
            }
        });

        cache.contacts().addListener(contactListener);
        cache.accounts().addListener(accountListener);
        cache.transactions().addListener(transactionListener);

        getDao().preloadingProperty().addListener((x, y, newValue) -> {
            if (!newValue) {
                Platform.runLater(() -> {
                    onChangedTransactionTypes();
                    setupAccountMenus();
                    setupContactMenu();
                    setupComments();
                });
            }
        });
    }

    private void clearTitle() {
        setText(rb.getString("title") + ": ###");
    }

    void initControls() {
        contactEdit.setText("");

        typeMenuButton.getItems().clear();
        debitedMenuButton.getItems().clear();
        creditedMenuButton.getItems().clear();
        contactMenuButton.getItems().clear();

        typeSuggestions.clear();
        contactSuggestions.clear();
        debitedSuggestionsAll.clear();
        debitedSuggestions.clear();
        creditedSuggestionsAll.clear();
        creditedSuggestions.clear();
        commentSuggestions.clear();
    }

    private void setupBanksAndCashMenuItems() {
        var banksAndCashAll = cache.getAccountsByType(CategoryType.BANKS_AND_CASH);
        var enabledCount = banksAndCashAll.stream().filter(Account::getEnabled).count();

        banksAndCashAll.stream()
            .sorted((a1, a2) -> a1.getName().compareToIgnoreCase(a2.getName()))
            .forEach(acc -> {
                debitedSuggestionsAll.add(acc);
                creditedSuggestionsAll.add(acc);

                if (acc.getEnabled()) {
                    var title = "[" + acc.getName() + "]";

                    var m1 = new MenuItem(title);
                    m1.setOnAction(event -> onDebitedAccountSelected(acc));

                    var m2 = new MenuItem(title);
                    m2.setOnAction(event -> onCreditedAccountSelected(acc));

                    debitedMenuButton.getItems().add(m1);
                    creditedMenuButton.getItems().add(m2);

                    debitedSuggestions.add(acc);
                    creditedSuggestions.add(acc);
                }
            });

        if (enabledCount != 0) {
            debitedMenuButton.getItems().add(new SeparatorMenuItem());
            creditedMenuButton.getItems().add(new SeparatorMenuItem());
        }
    }

    private void setupDebtMenuItems() {
        setAccountMenuItemsByCategory(CategoryType.DEBTS, "!");
    }

    private void setupAssetsMenuItems() {
        setAccountMenuItemsByCategory(CategoryType.ASSETS, ".");
    }

    private void setAccountMenuItemsByCategory(CategoryType categoryType, String prefix) {
        var categories = cache.getCategoriesByType(categoryType);

        categories.stream()
            .sorted((c1, c2) -> c1.getName().compareToIgnoreCase(c2.getName()))
            .forEach(x -> {
                List<Account> accounts = cache.getAccountsByCategory(x.getUuid());

                if (!accounts.isEmpty()) {
                    debitedMenuButton.getItems().add(new MenuItem(x.getName()));
                    creditedMenuButton.getItems().add(new MenuItem(x.getName()));

                    for (var acc : accounts) {
                        debitedSuggestionsAll.add(acc);
                        creditedSuggestionsAll.add(acc);

                        if (acc.getEnabled()) {
                            var title = "  " + prefix + " " + acc.getName();
                            var m1 = new MenuItem(title);
                            m1.setOnAction(event -> onDebitedAccountSelected(acc));

                            var m2 = new MenuItem(title);
                            m2.setOnAction(event -> onCreditedAccountSelected(acc));

                            debitedMenuButton.getItems().add(m1);
                            creditedMenuButton.getItems().add(m2);

                            debitedSuggestions.add(acc);
                            creditedSuggestions.add(acc);
                        }
                    }
                }
            });

        if (!categories.isEmpty()) {
            debitedMenuButton.getItems().add(new SeparatorMenuItem());
            creditedMenuButton.getItems().add(new SeparatorMenuItem());
        }
    }

    void clear() {
        builder = new Transaction.Builder();

        newTransactionProperty.set(true);

        daySpinner.getValueFactory().setValue(Calendar.getInstance().get(Calendar.DAY_OF_MONTH));
        typeEdit.setText("");
        creditedAccountEdit.setText("");
        debitedAccountEdit.setText("");
        contactEdit.setText("");
        checkedCheckBox.setSelected(false);
        commentEdit.setText("");
        checkedCheckBox.setSelected(false);
        invoiceNumberEdit.setText("");
        sumEdit.setText("");
        rate1Edit.setText("1.0");
        rateDir1Combo.getSelectionModel().clearAndSelect(0);
        rateAmoutLabel.setText("");

        daySpinner.getEditor().setText(Integer.toString(Calendar.getInstance().get(Calendar.DAY_OF_MONTH)));
        daySpinner.getEditor().selectAll();

        clearTitle();
    }

    public void setTransaction(Transaction tr) {
        builder = new Transaction.Builder(tr);

        newTransactionProperty.set(false);

        setText(rb.getString("title") + ": " + tr.getUuid());

        // Type
        typeEdit.setText(tr.getTransactionType().getTypeName());

        // Accounts
        Optional<Account> accCredited = cache.getAccount(tr.getAccountCreditedUuid());
        creditedAccountEdit.setText(accCredited.map(Account::getName).orElse(""));

        Optional<Account> accDebited = cache.getAccount(tr.getAccountDebitedUuid());
        debitedAccountEdit.setText(accDebited.map(Account::getName).orElse(""));

        contactEdit.setText(cache.getContact(tr.getContactUuid().orElse(null)).map(Contact::getName).orElse(""));

        // Other fields
        commentEdit.setText(tr.getComment());
        checkedCheckBox.setSelected(tr.getChecked());
        invoiceNumberEdit.setText(tr.getInvoiceNumber());

        // Rate
        var debitedCurrencyUuid = accDebited.flatMap(Account::getCurrencyUuid).orElse(null);
        var creditedCurrencyUuid = accCredited.flatMap(Account::getCurrencyUuid).orElse(null);

        if (Objects.equals(debitedCurrencyUuid, creditedCurrencyUuid)) {
            rate1Edit.setDisable(true);
            rate1Edit.setText("");
        } else {
            rate1Edit.setDisable(false);

            BigDecimal rate = tr.getRate();
            if (BigDecimal.ZERO.compareTo(rate) == 0) {
                rate = BigDecimal.ONE.setScale(FIELD_SCALE, RoundingMode.HALF_UP);
            }
            if (rate != null) {
                rate1Edit.setText(rate.toString());
                rateDir1Combo.getSelectionModel().select(tr.getRateDirection());
            } else {
                rate1Edit.setText("");
            }
        }

        // Day
        daySpinner.getValueFactory().setValue(tr.getDay());

        // Sum
        sumEdit.setText(tr.getAmount().setScale(2, RoundingMode.HALF_UP).toString());
        updateRateAmount();
    }

    void setTransactionFromStatement(StatementRecord record, Account account) {
        clear();

        daySpinner.getValueFactory().setValue(record.getActual().getDayOfMonth());

        var amount = record.getAmountDecimal().orElse(BigDecimal.ZERO);
        sumEdit.setText(amount.abs().setScale(2, RoundingMode.HALF_UP).toString());

        var accountString = account == null ? "" : account.getName();
        if (amount.signum() <= 0) {
            debitedAccountEdit.setText(accountString);
        } else {
            creditedAccountEdit.setText(accountString);
        }
    }

    private void onClearButton() {
        clear();
        daySpinner.requestFocus();
    }

    private void onDeleteButton() {
        if (builder.getUuid() != null) {
            deleteTransactionConsumer.accept(builder.getUuid());
        }
    }

    private void onUpdateButton() {
        if (buildTransaction()) {
            updateTransactionConsumer.accept(builder, newContactName);
            daySpinner.requestFocus();
        }
    }

    private void onAddButton() {
        if (buildTransaction()) {
            addTransactionConsumer.accept(builder, newContactName);
            daySpinner.requestFocus();
        }
    }

    private void onContactSelected(Contact c) {
        contactEdit.setText(c.getName());
        builder.contactUuid(c.getUuid());
    }

    private void onDebitedAccountSelected(Account acc) {
        debitedAccountEdit.setText(acc.getName());
        enableDisableRate();
    }

    private void onCreditedAccountSelected(Account acc) {
        creditedAccountEdit.setText(acc.getName());
        enableDisableRate();
    }

    private void onTransactionTypeSelected(TransactionType type) {
        typeEdit.setText(type.getTypeName());
    }

    void setOnAddTransaction(BiConsumer<Transaction.Builder, String> c) {
        addTransactionConsumer = c;
    }

    void setOnUpdateTransaction(BiConsumer<Transaction.Builder, String> c) {
        updateTransactionConsumer = c;
    }

    void setOnDeleteTransaction(Consumer<UUID> c) {
        deleteTransactionConsumer = c;
    }

    private boolean buildTransaction() {
        // Check type id
        handleTypeFocusLoss();
        Optional<TransactionType> type = checkTransactionTypeFieldValue(typeEdit, typeSuggestions,
            TRANSACTION_TYPE_TO_STRING);
        type.ifPresent(it -> builder.transactionType(it));

        var debitedAccount = checkTextFieldValue(debitedAccountEdit, debitedSuggestionsAll, ACCOUNT_TO_STRING);
        if (debitedAccount.isPresent()) {
            builder.accountDebitedUuid(debitedAccount.get().getUuid());
            builder.accountDebitedCategoryUuid(debitedAccount.get().getCategoryUuid());
            builder.accountDebitedType(debitedAccount.get().getType());
        } else {
            return false;
        }

        var creditedAccount = checkTextFieldValue(creditedAccountEdit, creditedSuggestionsAll, ACCOUNT_TO_STRING);
        if (creditedAccount.isPresent()) {
            builder.accountCreditedUuid(creditedAccount.get().getUuid());
            builder.accountCreditedCategoryUuid(creditedAccount.get().getCategoryUuid());
            builder.accountCreditedType(creditedAccount.get().getType());
        } else {
            return false;
        }

        // builder.day(daySpinner.getValue());
        builder.comment(commentEdit.getText());
        builder.checked(checkedCheckBox.isSelected());
        builder.invoiceNumber(invoiceNumberEdit.getText());

        try {
            builder.day(Integer.parseInt(daySpinner.getEditor().getText()));

            builder.amount(new BigDecimal(sumEdit.getText()));

            if (!rate1Edit.isDisabled()) {
                builder.rate(new BigDecimal(rate1Edit.getText()));
                builder.rateDirection(rateDir1Combo.getSelectionModel().getSelectedIndex());
            } else {
                builder.rate(BigDecimal.ONE);
                builder.rateDirection(1);
            }
        } catch (NumberFormatException ex) {
            return false;
        }

        newContactName = "";

        var contactName = contactEdit.getText();
        if (contactName == null || contactName.isEmpty()) {
            builder.contactUuid(null);
        } else {
            var contact = checkTextFieldValue(contactName, contactSuggestions, CONTACT_TO_STRING);
            if (contact.isPresent()) {
                builder.contactUuid(contact.get().getUuid());
            } else {
                newContactName = contactName;
            }
        }

        builder.modified(System.currentTimeMillis());
        return true;
    }

    private void enableDisableRate() {
        boolean disable;

        if (builder.getAccountCreditedUuid() == null || builder.getAccountDebitedUuid() == null) {
            disable = true;
        } else {
            var c1 = cache.getAccount(builder.getAccountDebitedUuid())
                .flatMap(Account::getCurrencyUuid)
                .orElse(null);
            var c2 = cache.getAccount(builder.getAccountCreditedUuid())
                .flatMap(Account::getCurrencyUuid)
                .orElse(null);

            disable = Objects.equals(c1, c2);
        }

        rate1Edit.setDisable(disable);
        rateDir1Combo.setDisable(disable);

        if (!disable && rate1Edit.getText().isEmpty()) {
            rate1Edit.setText("1");
            rateDir1Combo.getSelectionModel().select(0);
        }
    }

    private <T extends Named> Optional<T> checkTextFieldValue(String value,
                                                              Collection<T> items,
                                                              StringConverter<T> converter)
    {
        return items.stream().filter(it -> converter.toString(it).equals(value)).findFirst();
    }

    private Optional<TransactionType> checkTransactionTypeFieldValue(String value, Collection<TransactionType> items,
                                                                     StringConverter<TransactionType> converter)
    {
        return items.stream()
            .filter(it -> converter.toString(it).equals(value)).findFirst();
    }

    private <T extends Named> Optional<T> checkTextFieldValue(TextField field,
                                                              Collection<T> items,
                                                              StringConverter<T> converter)
    {
        return checkTextFieldValue(field.getText(), items, converter);
    }

    private Optional<TransactionType> checkTransactionTypeFieldValue(TextField field,
                                                                     Collection<TransactionType> items,
                                                                     StringConverter<TransactionType> converter)
    {
        return checkTransactionTypeFieldValue(field.getText(), items, converter);
    }

    private void handleTypeFocusLoss() {
        var type = checkTransactionTypeFieldValue(typeEdit, typeSuggestions, TRANSACTION_TYPE_TO_STRING);
        if (type.isEmpty()) {
            typeEdit.setText(TransactionType.UNDEFINED.getTypeName());
        }
    }

    private void createValidationSupport() {
        validation.registerValidator(typeEdit, (Control control, String value) -> {
            var type = checkTransactionTypeFieldValue(typeEdit, typeSuggestions, TRANSACTION_TYPE_TO_STRING);
            return ValidationResult.fromErrorIf(control, null, type.isEmpty());
        });

        validation.registerValidator(debitedAccountEdit, (Control control, String value) -> {
            var account = checkTextFieldValue(debitedAccountEdit, debitedSuggestionsAll, ACCOUNT_TO_STRING);
            updateCategoryLabel(debitedCategoryLabel, account.orElse(null));

            builder.accountDebitedUuid(account.map(Account::getUuid).orElse(null));

            enableDisableRate();
            return ValidationResult.fromErrorIf(control, null, account.isEmpty());
        });

        validation.registerValidator(creditedAccountEdit, (Control control, String value) -> {
            var account = checkTextFieldValue(creditedAccountEdit, creditedSuggestionsAll, ACCOUNT_TO_STRING);
            updateCategoryLabel(creditedCategoryLabel, account.orElse(null));

            builder.accountCreditedUuid(account.map(Account::getUuid).orElse(null));

            enableDisableRate();
            return ValidationResult.fromErrorIf(control, null, account.isEmpty());
        });

        validation.registerValidator(sumEdit, DECIMAL_VALIDATOR);
        validation.registerValidator(rate1Edit, false, DECIMAL_VALIDATOR);

        validation.initInitialDecoration();
    }

    private void setupContactMenu() {
        contactMenuButton.getItems().clear();
        contactSuggestions.clear();

        cache.getContacts().stream()
            .sorted((c1, c2) -> c1.getName().compareToIgnoreCase(c2.getName()))
            .forEach(x -> {
                var m = new MenuItem(x.getName());
                m.setOnAction(event -> onContactSelected(x));

                contactMenuButton.getItems().add(m);
                contactSuggestions.add(x);
            });

        contactMenuButton.setDisable(contactMenuButton.getItems().isEmpty());
    }

    private void onChangedTransactionTypes() {
        typeMenuButton.getItems().clear();
        typeSuggestions.clear();

        TransactionType.valuesAsList().forEach(x -> {
            if (x.isSeparator()) {
                typeMenuButton.getItems().add(new SeparatorMenuItem());
            } else {
                var m = new MenuItem(x.getTypeName());
                m.setOnAction(event -> onTransactionTypeSelected(x));
                typeMenuButton.getItems().add(m);
                typeSuggestions.add(x);
            }
        });
    }

    private void setupAccountMenus() {
        debitedMenuButton.getItems().clear();
        creditedMenuButton.getItems().clear();
        debitedSuggestionsAll.clear();
        debitedSuggestions.clear();
        creditedSuggestionsAll.clear();
        creditedSuggestions.clear();

        // Bank and cash accounts first
        setupBanksAndCashMenuItems();

        // Incomes to debitable accounts
        List<Category> incomeCategories = cache.getCategoriesByType(CategoryType.INCOMES);
        incomeCategories.stream()
            .sorted((c1, c2) -> c1.getName().compareToIgnoreCase(c2.getName()))
            .forEach(x -> {
                var accounts = cache.getAccountsByCategory(x.getUuid());

                if (!accounts.isEmpty()) {
                    debitedMenuButton.getItems().add(new MenuItem(x.getName()));

                    accounts.forEach(acc -> {
                        var accMenuItem = new MenuItem("  + " + acc.getName());
                        accMenuItem.setOnAction(event -> onDebitedAccountSelected(acc));
                        debitedMenuButton.getItems().add(accMenuItem);
                        debitedSuggestionsAll.add(acc);
                        debitedSuggestions.add(acc);
                    });
                }
            });

        if (!incomeCategories.isEmpty()) {
            debitedMenuButton.getItems().add(new SeparatorMenuItem());
        }

        // Expenses to creditable accounts
        List<Category> expenseCategories = cache.getCategoriesByType(CategoryType.EXPENSES);
        expenseCategories.stream()
            .sorted((c1, c2) -> c1.getName().compareToIgnoreCase(c2.getName()))
            .forEach(x -> {
                var accounts = cache.getAccountsByCategory(x.getUuid());

                if (!accounts.isEmpty()) {
                    creditedMenuButton.getItems().add(new MenuItem(x.getName()));

                    accounts.forEach(acc -> {
                        creditedSuggestionsAll.add(acc);
                        creditedSuggestions.add(acc);
                        var accMenuItem = new MenuItem("  - " + acc.getName());
                        accMenuItem.setOnAction(event -> onCreditedAccountSelected(acc));
                        creditedMenuButton.getItems().add(accMenuItem);
                    });
                }
            });

        if (!expenseCategories.isEmpty()) {
            creditedMenuButton.getItems().add(new SeparatorMenuItem());
        }

        setupDebtMenuItems();
        setupAssetsMenuItems();
    }

    private void setupComments() {
        commentSuggestions.clear();
        commentSuggestions.addAll(cache.getUniqueTransactionComments());
    }

    private void transactionChangeListener(MapChangeListener.Change<? extends UUID, ? extends Transaction> change) {
        if (change.wasAdded()) {
            var comment = change.getValueAdded().getComment();
            if (!comment.isEmpty()) {
                commentSuggestions.add(comment);
            }
        }
    }

    private void updateRateAmount() {
        var amount = sumEdit.getText();
        if (amount.isEmpty()) {
            amount = "0";
        }

        var amountValue = new BigDecimal(amount).setScale(FIELD_SCALE, RoundingMode.HALF_UP);

        var rate = rate1Edit.getText();
        if (rate.isEmpty()) {
            rate = "1";
        }

        var rateValue = new BigDecimal(rate).setScale(FIELD_SCALE, RoundingMode.HALF_UP);

        BigDecimal total;

        if (rateDir1Combo.getSelectionModel().getSelectedIndex() == 0) {
            total = amountValue.divide(rateValue, RoundingMode.HALF_UP);
        } else {
            total = amountValue.multiply(rateValue);
        }

        Platform.runLater(() ->
            rateAmoutLabel.setText("= " + total.setScale(2, RoundingMode.HALF_UP).toString()));
    }

    private void updateCategoryLabel(Label label, Account account) {
        if (account != null) {
            var catName = cache.getCategory(account.getCategoryUuid()).map(Category::getName).orElse("");
            label.setText(account.getType().getTypeName() + " | " + catName);
        } else {
            label.setText("");
        }
    }

    private void processAutoFill() {
        var accDebitedUuid = builder.getAccountDebitedUuid();
        var accCreditedUuid = builder.getAccountCreditedUuid();

        if (accDebitedUuid != null && accCreditedUuid != null) {
            cache.getTransactions().stream()
                .filter(it -> Objects.equals(it.getAccountCreditedUuid(), accCreditedUuid)
                    && Objects.equals(it.getAccountDebitedUuid(), accDebitedUuid))
                .max(MoneyDAO.COMPARE_TRANSACTION_BY_DATE)
                .ifPresent(it -> {
                    if (commentEdit.getText().isEmpty()) {
                        commentEdit.setText(it.getComment());
                    }
                    if (sumEdit.getText().isEmpty()) {
                        sumEdit.setText(it.getAmount().setScale(2, RoundingMode.HALF_UP).toString());
                    }

                    cache.getContact(it.getContactUuid().orElse(null)).ifPresent(contact -> {
                        if (contactEdit.getText().isEmpty()) {
                            contactEdit.setText(contact.getName());
                        }
                    });
                });
        }
    }

    // Unit test accessors
    Button getAddButton() {
        return addButton;
    }

    Button getUpdateButton() {
        return updateButton;
    }

    Button getDeleteButton() {
        return deleteButton;
    }

    TextField getTypeEdit() {
        return typeEdit;
    }

    TextField getDebitedAccountEdit() {
        return debitedAccountEdit;
    }

    TextField getCreditedAccountEdit() {
        return creditedAccountEdit;
    }

    TextField getCommentEdit() {
        return commentEdit;
    }

    TextField getSumEdit() {
        return sumEdit;
    }

    TextField getContactEdit() {
        return contactEdit;
    }

    TextField getRate1Edit() {
        return rate1Edit;
    }

    CheckBox getCheckedCheckBox() {
        return checkedCheckBox;
    }

    Spinner<Integer> getDaySpinner() {
        return daySpinner;
    }
}