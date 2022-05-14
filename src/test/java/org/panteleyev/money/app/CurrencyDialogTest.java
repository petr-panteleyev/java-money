/*
 Copyright (c) 2017-2022, Petr Panteleyev

 This program is free software: you can redistribute it and/or modify it under the
 terms of the GNU General Public License as published by the Free Software
 Foundation, either version 3 of the License, or (at your option) any later version.

 This program is distributed in the hope that it will be useful, but WITHOUT ANY
 WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.

 You should have received a copy of the GNU General Public License along with this
 program. If not, see <https://www.gnu.org/licenses/>.
 */
package org.panteleyev.money.app;

import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.scene.control.ButtonType;
import org.panteleyev.money.model.Currency;
import org.panteleyev.money.test.BaseTest;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import static org.panteleyev.money.test.BaseTestUtils.randomBigDecimal;
import static org.panteleyev.money.test.BaseTestUtils.randomBoolean;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

public class CurrencyDialogTest extends BaseTest {
    private final static Currency CURRENCY = new Currency.Builder()
        .uuid(UUID.randomUUID())
        .symbol(UUID.randomUUID().toString())
        .description(UUID.randomUUID().toString())
        .formatSymbol(UUID.randomUUID().toString())
        .formatSymbolPosition(1)
        .showFormatSymbol(randomBoolean())
        .def(randomBoolean())
        .rate(randomBigDecimal())
        .direction(1)
        .useThousandSeparator(randomBoolean())
        .created(System.currentTimeMillis())
        .modified(System.currentTimeMillis())
        .build();

    @BeforeClass
    public void setupAndSkip() {
        new JFXPanel();
    }

    private void setupDialog(CurrencyDialog dialog) {
        dialog.getNameEdit().setText(CURRENCY.symbol());
        dialog.getDescrEdit().setText(CURRENCY.description());
        dialog.getRateEdit().setText(CURRENCY.rate().toString());
        dialog.getThousandSeparatorCheck().setSelected(CURRENCY.useThousandSeparator());
        dialog.getDefaultCheck().setSelected(CURRENCY.def());
    }

    private void setupDialogUpdate(CurrencyDialog dialog) {
    }

    @Test
    public void testNewCurrency() throws Exception {
        var queue = new ArrayBlockingQueue<Currency>(1);

        Platform.runLater(() -> {
            var dialog = new CurrencyDialog(null, null, null);
            setupDialog(dialog);
            var category = dialog.getResultConverter().call(ButtonType.OK);
            queue.add(category);
        });

        var currency = queue.take();

        assertNotNull(currency.uuid());
        assertCurrency(currency);
        assertEquals(currency.created(), currency.modified());
    }

    @Test
    public void testExistingCurrency() throws Exception {
        var queue = new ArrayBlockingQueue<Currency>(1);

        Platform.runLater(() -> {
            var dialog = new CurrencyDialog(null, null, CURRENCY);
            setupDialogUpdate(dialog);
            var category = dialog.getResultConverter().call(ButtonType.OK);
            queue.add(category);
        });

        var currency = queue.take();

        assertEquals(currency.uuid(), CURRENCY.uuid());
        assertCurrency(currency);
        assertTrue(currency.modified() > CURRENCY.modified());
        assertTrue(currency.modified() > currency.created());
    }

    private static void assertCurrency(Currency currency) {
        assertEquals(currency.symbol(), CURRENCY.symbol());
        assertEquals(currency.description(), CURRENCY.description());
        assertEquals(currency.rate(), CURRENCY.rate());
        assertEquals(currency.useThousandSeparator(), CURRENCY.useThousandSeparator());
        assertEquals(currency.def(), CURRENCY.def());
    }
}
