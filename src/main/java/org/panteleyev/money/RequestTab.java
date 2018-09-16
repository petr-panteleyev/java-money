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

import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import org.panteleyev.money.persistence.model.Transaction;
import java.util.List;
import java.util.ResourceBundle;
import static org.panteleyev.money.persistence.MoneyDAO.getDao;

class RequestTab extends BorderPane {
    private static final ResourceBundle rb = MainWindowController.RB;

    private final TransactionTableView table = new TransactionTableView(true);

    private final AccountFilterSelectionBox accBox = new AccountFilterSelectionBox();

    RequestTab() {
        Button clearButton = new Button(rb.getString("button.Clear"));
        clearButton.setOnAction(event -> onClearButton());

        Button findButton = new Button(rb.getString("button.Find"));
        findButton.setOnAction(event -> onFindButton());

        HBox row1 = new HBox(5.0, clearButton, findButton);

        VBox vBox = new VBox(5.0, row1, accBox);

        setTop(vBox);
        setCenter(table);

        BorderPane.setMargin(vBox, new Insets(5.0, 5.0, 5.0, 5.0));

        table.setOnCheckTransaction(this::onCheckTransaction);
    }

    private void onFindButton() {
        table.setTransactionFilter(accBox.getTransactionFilter());
    }

    private void onClearButton() {
        table.setTransactionFilter(x -> false);
        accBox.setupCategoryTypesBox();
    }

    private void onCheckTransaction(List<Transaction> transactions, boolean check) {
        for (Transaction t : transactions) {
            getDao().updateTransaction(t.check(check));
        }
    }
}
