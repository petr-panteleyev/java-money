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
package org.panteleyev.money.bundles;

import org.panteleyev.money.model.DocumentType;

import java.util.ListResourceBundle;

public class DocumentTypeBundle extends ListResourceBundle {
    @Override
    protected Object[][] getContents() {
        return new Object[][]{
                {DocumentType.BILL.name(), "Bill"},
                {DocumentType.CONTRACT.name(), "Contract"},
                {DocumentType.RECEIPT.name(), "Receipt"},
                {DocumentType.STATEMENT.name(), "Statement"},
                {DocumentType.OTHER.name(), "Other"},
        };
    }
}
