/*
 Copyright © 2020 Petr Panteleyev <petr@panteleyev.org>
 SPDX-License-Identifier: BSD-2-Clause
 */
package org.panteleyev.ofx;

public record CreditCardStatement(String currency,
                                  AccountInfo creditCardAccountFrom,
                                  BankTransactionList bankTransactionList,
                                  PendingTransactionList pendingTransactionList) {
}
