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
package org.panteleyev.money.app.cells;

import javafx.scene.control.TableCell;
import javafx.scene.image.ImageView;
import org.panteleyev.money.model.Account;
import static org.panteleyev.money.app.Images.getCardTypeIcon;

public class AccountCardCell extends TableCell<Account, Account> {
    @Override
    protected void updateItem(Account account, boolean empty) {
        super.updateItem(account, empty);

        setText("");
        setGraphic(null);

        if (empty || account == null || account.cardNumber().isBlank()) {
            return;
        }

        setText(account.cardNumber());
        setGraphic(new ImageView(getCardTypeIcon(account.cardType())));
    }
}
