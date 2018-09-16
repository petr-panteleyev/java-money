/*
 * Copyright (c) 2017, Petr Panteleyev <petr@panteleyev.org>
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
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Control;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import org.controlsfx.validation.ValidationResult;
import org.panteleyev.money.persistence.model.Currency;
import org.panteleyev.utilities.fx.BaseDialog;
import java.math.BigDecimal;
import java.util.UUID;
import java.util.stream.Collectors;
import static org.panteleyev.money.MainWindowController.RB;
import static org.panteleyev.money.persistence.MoneyDAO.getDao;

final class CurrencyDialog extends BaseDialog<Currency> {
    private final TextField nameEdit = new TextField();
    private final TextField descrEdit = new TextField();
    private final TextField rateEdit = new TextField();
    private final ChoiceBox<String> rateDirectionChoice = new ChoiceBox<>();
    private final CheckBox defaultCheck = new CheckBox(RB.getString("currency.Dialog.Default"));
    private final CheckBox showSymbolCheck = new CheckBox();
    private final ComboBox<String> formatSymbolCombo = new ComboBox<>();
    private final ChoiceBox<String> formatSymbolPositionChoice = new ChoiceBox<>();
    private final CheckBox thousandSeparatorCheck = new CheckBox(RB.getString("currency.Dialog.ShowSeparator"));

    CurrencyDialog(Currency currency) {
        super(MainWindowController.CSS_PATH);

        setTitle(RB.getString("currency.Dialog.Title"));

        GridPane gridPane = new GridPane();
        gridPane.getStyleClass().add(Styles.GRID_PANE);

        int index = 0;
        gridPane.addRow(index++, new Label(RB.getString("label.Symbol")), nameEdit);
        gridPane.addRow(index++, new Label(RB.getString("label.Description")), descrEdit);
        gridPane.addRow(index++, new Label(RB.getString("label.Rate")), rateEdit, rateDirectionChoice);

        HBox hBox = new HBox(showSymbolCheck, formatSymbolCombo, formatSymbolPositionChoice);
        hBox.setAlignment(Pos.CENTER_LEFT);
        HBox.setMargin(formatSymbolPositionChoice, new Insets(0.0, 0.0, 0.0, 5.0));
        gridPane.add(hBox, 1, index++);
        gridPane.add(thousandSeparatorCheck, 1, index++);
        gridPane.add(defaultCheck, 1, index);

        getDialogPane().setContent(gridPane);

        nameEdit.setPrefColumnCount(20);
        formatSymbolCombo.setEditable(true);

        rateDirectionChoice.getItems().setAll("/", "*");

        formatSymbolPositionChoice.getItems().setAll(
                RB.getString("currency.Dialog.Before"),
                RB.getString("currency.Dialog.After"));

        formatSymbolCombo.getItems().setAll(getDao().getCurrencies().stream()
                .map(Currency::getFormatSymbol)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toSet()));

        if (currency == null) {
            rateDirectionChoice.getSelectionModel().select(0);
            formatSymbolPositionChoice.getSelectionModel().select(0);
            rateEdit.setText("1");
        } else {
            nameEdit.setText(currency.getSymbol());
            descrEdit.setText(currency.getDescription());
            rateEdit.setText(currency.getRate().toString());
            defaultCheck.setSelected(currency.getDef());
            rateDirectionChoice.getSelectionModel().select(currency.getDirection());
            showSymbolCheck.setSelected(currency.getShowFormatSymbol());
            formatSymbolCombo.getSelectionModel().select(currency.getFormatSymbol());
            formatSymbolPositionChoice.getSelectionModel().select(currency.getFormatSymbolPosition());
            thousandSeparatorCheck.setSelected(currency.getUseThousandSeparator());
        }

        setResultConverter((ButtonType b) -> {
            if (b == ButtonType.OK) {
                return new Currency(currency != null ? currency.getId() : 0,
                        nameEdit.getText(),
                        descrEdit.getText(),
                        formatSymbolCombo.getSelectionModel().getSelectedItem(),
                        formatSymbolPositionChoice.getSelectionModel().getSelectedIndex(),
                        showSymbolCheck.isSelected(),
                        defaultCheck.isSelected(),
                        new BigDecimal(rateEdit.getText()),
                        rateDirectionChoice.getSelectionModel().getSelectedIndex(),
                        thousandSeparatorCheck.isSelected(),
                        currency != null ? currency.getGuid() : UUID.randomUUID().toString(),
                        System.currentTimeMillis()
                );
            } else {
                return null;
            }
        });

        createDefaultButtons(RB);

        Platform.runLater(this::createValidationSupport);
    }

    private void createValidationSupport() {
        validation.registerValidator(nameEdit, (Control control, String value) ->
                ValidationResult.fromErrorIf(control, null, value.isEmpty()));

        validation.registerValidator(rateEdit, MainWindowController.BIG_DECIMAL_VALIDATOR);
        validation.initInitialDecoration();
    }
}