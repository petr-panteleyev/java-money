package org.panteleyev.money.app;

/*
 * Copyright (c) Petr Panteleyev. All rights reserved.
 * Licensed under the BSD license. See LICENSE file in the project root for full license information.
 */

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.chart.PieChart;
import javafx.scene.control.MenuBar;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.util.Pair;
import org.panteleyev.money.app.filters.AccountSelectionBox;
import org.panteleyev.money.app.filters.TransactionFilterBox;
import org.panteleyev.money.model.Account;
import java.math.BigDecimal;
import java.util.stream.Collectors;
import static org.panteleyev.fx.ButtonFactory.newButton;
import static org.panteleyev.fx.MenuFactory.newMenu;
import static org.panteleyev.fx.MenuFactory.newMenuBar;
import static org.panteleyev.fx.MenuFactory.newMenuItem;
import static org.panteleyev.money.app.MainWindowController.RB;
import static org.panteleyev.money.persistence.DataCache.cache;

public class ChartsWindowController extends BaseController {
    private static final int PIE_CHART_SIZE = 10;  // meaningful items

    private final AccountSelectionBox selectionBox = new AccountSelectionBox();
    private final TransactionFilterBox transactionFilterBox = new TransactionFilterBox(true, true);
    private final PieChart pieChart = new PieChart();

    public ChartsWindowController() {
        var topPanel = new HBox(5.0);

        topPanel.getChildren().addAll(selectionBox,
            transactionFilterBox,
            newButton(RB, "Refresh", x -> updateChart()));
        BorderPane.setMargin(topPanel, new Insets(5.0, 5.0, 5.0, 5.0));
        pieChart.legendVisibleProperty().set(false);

        var centerPane = new BorderPane();
        centerPane.setTop(topPanel);
        centerPane.setCenter(pieChart);

        var root = new BorderPane();
        root.setTop(createMainMenu());
        root.setCenter(centerPane);

        transactionFilterBox.setFilterYears();
        selectionBox.setupCategoryTypesBox();

        setupWindow(root);
        Options.loadStageDimensions(getClass(), getStage());
    }

    @Override
    public String getTitle() {
        return RB.getString("tab.Charts");
    }

    private MenuBar createMainMenu() {
        return newMenuBar(
            newMenu(RB, "File",
                newMenuItem(RB, "Close", event -> onClose())),
            createWindowMenu(),
            createHelpMenu());
    }

    private void updateChart() {
        var accountFilter = selectionBox.getAccountFilter()
            .and(Account.FILTER_ENABLED);

        ObservableList<PieChart.Data> data = FXCollections.observableArrayList();

        var transactionFilter = transactionFilterBox.getTransactionFilter();

        var list = cache().getAccounts(accountFilter)
            .map(a -> new Pair<>(a.name(), cache().calculateBalance(a, true, transactionFilter).abs()))
            .filter(p -> BigDecimal.ZERO.compareTo(p.getValue()) != 0)
            .sorted((o1, o2) -> o2.getValue().compareTo(o1.getValue()))
            .collect(Collectors.toList());

        list.stream().limit(PIE_CHART_SIZE)
            .forEach(p -> data.add(new PieChart.Data(p.getKey(), p.getValue().doubleValue())));

        list.stream().skip(PIE_CHART_SIZE)
            .map(Pair::getValue)
            .reduce(BigDecimal::add)
            .ifPresent(t -> data.add(new PieChart.Data(RB.getString("pie.Chart.Other"), t.doubleValue())));

        pieChart.setData(data);
    }
}