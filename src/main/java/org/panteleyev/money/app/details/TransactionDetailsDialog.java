/*
 Copyright (c) Petr Panteleyev. All rights reserved.
 Licensed under the BSD license. See LICENSE file in the project root for full license information.
 */
package org.panteleyev.money.app.details;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TableView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import org.panteleyev.fx.BaseDialog;
import org.panteleyev.fx.Controller;
import org.panteleyev.money.app.RecordEditorCallback;
import org.panteleyev.money.app.Styles;
import org.panteleyev.money.app.cells.TransactionDetailSumCell;
import org.panteleyev.money.model.Account;
import org.panteleyev.money.model.Transaction;
import org.panteleyev.money.model.TransactionDetail;
import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import static org.panteleyev.fx.BoxFactory.hBox;
import static org.panteleyev.fx.BoxFactory.vBox;
import static org.panteleyev.fx.FxUtils.COLON;
import static org.panteleyev.fx.FxUtils.fxString;
import static org.panteleyev.fx.LabelFactory.label;
import static org.panteleyev.fx.TableColumnBuilder.tableColumn;
import static org.panteleyev.fx.TableColumnBuilder.tableObjectColumn;
import static org.panteleyev.money.app.GlobalContext.cache;
import static org.panteleyev.money.app.MainWindowController.UI;
import static org.panteleyev.money.bundles.Internationalization.I18N_MISC_CREDITED_ACCOUNT;
import static org.panteleyev.money.bundles.Internationalization.I18N_MISC_TRANSACTION_DETAILS;
import static org.panteleyev.money.bundles.Internationalization.I18N_WORD_COMMENT;
import static org.panteleyev.money.bundles.Internationalization.I18N_WORD_DELTA;
import static org.panteleyev.money.bundles.Internationalization.I18N_WORD_SUM;

public final class TransactionDetailsDialog extends BaseDialog<List<TransactionDetail>> implements RecordEditorCallback<TransactionDetail> {
    private final ObservableList<TransactionDetail> details = FXCollections.observableArrayList();
    private final DetailEditorPane detailEditor;
    private final Label deltaLabel = new Label();

    private final BigDecimal totalAmount;
    private final TableView<TransactionDetail> detailsTable = new TableView<>();

    public TransactionDetailsDialog(Controller owner, List<Transaction> transactions, BigDecimal totalAmount, boolean readOnly) {
        super(owner);

        setTitle(fxString(UI, I18N_MISC_TRANSACTION_DETAILS));

        this.totalAmount = totalAmount;
        detailEditor = new DetailEditorPane(this, cache());

        details.addAll(transactions.stream()
            .map(TransactionDetail::new)
            .toList());

        calculateDelta();

        detailsTable.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);

        var w = detailsTable.widthProperty().subtract(20);
        detailsTable.getColumns().setAll(List.of(
            tableColumn(fxString(UI, I18N_MISC_CREDITED_ACCOUNT), b ->
                b.withPropertyCallback(d -> cache().getAccount(d.accountCreditedUuid()).map(Account::name).orElse(""))
                    .withWidthBinding(w.multiply(0.3))),
            tableColumn(fxString(UI, I18N_WORD_COMMENT), b ->
                b.withPropertyCallback(TransactionDetail::comment).withWidthBinding(w.multiply(0.6))),
            tableObjectColumn(fxString(UI, I18N_WORD_SUM), b ->
                b.withCellFactory(x -> new TransactionDetailSumCell()).withWidthBinding(w.multiply(0.1)))
        ));

        detailsTable.setItems(details);

        var hBox = hBox(Styles.BIG_SPACING, label(fxString(UI, I18N_WORD_DELTA, COLON)), deltaLabel);
        var vBox = vBox(Styles.BIG_SPACING, hBox, detailEditor);
        VBox.setMargin(hBox, new Insets(Styles.BIG_SPACING, 0, 0, 0));

        var content = new BorderPane();
        content.setCenter(detailsTable);
        if (!readOnly) {
            content.setBottom(vBox);
        }
        getDialogPane().setContent(content);
        getDialogPane().getButtonTypes().add(ButtonType.OK);
        getDialogPane().getButtonTypes().add(ButtonType.CANCEL);

        detailsTable.getSelectionModel().selectedItemProperty().addListener((x, oldValue, newValue) -> {
            if (oldValue != newValue) {
                detailEditor.setTransactionDetail(newValue);
            }
        });

        setResultConverter((ButtonType b) -> {
            if (b == ButtonType.OK) {
                return details;
            } else {
                return null;
            }
        });

        setResizable(true);
        getDialogPane().setPrefWidth(800);
        getDialogPane().setPrefHeight(400);
    }

    private void calculateDelta() {
        deltaLabel.setText(
            details.stream()
                .map(TransactionDetail::amount)
                .reduce(totalAmount, BigDecimal::subtract).toString()
        );
    }

    @Override
    public void addRecord(TransactionDetail detail) {
        details.add(detail);
        calculateDelta();
    }

    @Override
    public void updateRecord(TransactionDetail detail) {
        for (int index = 0; index < details.size(); index++) {
            if (Objects.equals(details.get(index).uuid(), detail.uuid())) {
                details.set(index, detail);
                calculateDelta();
                return;
            }
        }
    }

    @Override
    public void deleteRecord(TransactionDetail detail) {
        for (var index = 0; index < details.size(); index++) {
            if (Objects.equals(details.get(index).uuid(), detail.uuid())) {
                details.remove(index);
                detailsTable.getSelectionModel().clearSelection();
                calculateDelta();
                return;
            }
        }
    }
}
