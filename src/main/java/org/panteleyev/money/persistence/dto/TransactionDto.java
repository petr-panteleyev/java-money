/*
 * Copyright (c) 2018, Petr Panteleyev <petr@panteleyev.org>
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

package org.panteleyev.money.persistence.dto;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.panteleyev.money.persistence.model.Transaction;
import org.panteleyev.persistence.Record;
import org.panteleyev.persistence.annotations.Column;
import org.panteleyev.persistence.annotations.RecordBuilder;
import org.panteleyev.persistence.annotations.Table;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import static org.panteleyev.crypto.AES.aes256;

@Table("transaction")
public final class TransactionDto implements Record, Dto<Transaction> {
    @Column(value = "id", primaryKey = true)
    private final int id;
    @Column("date")
    private final LocalDate date;
    @Column(value = "bytes", length = BINARY_LENGTH)
    private final byte[] bytes;

    @RecordBuilder
    public TransactionDto(@Column("id") int id,
                          @Column("date") LocalDate date,
                          @Column("bytes") byte[] bytes) {
        this.id = id;
        this.date = date;
        this.bytes = bytes;
    }

    TransactionDto(Transaction transaction, String password) {
        id = transaction.getId();

        date = LocalDate.of(transaction.getYear(), transaction.getMonth(), transaction.getDay());

        var rawBytes = toJson(transaction);
        bytes = password != null && !password.isEmpty() ?
                aes256().encrypt(rawBytes, password) : rawBytes;
    }

    @Override
    public int getId() {
        return id;
    }

    @Override
    public byte[] toJson(Transaction transaction) {
        var json = new JsonObject();
        json.addProperty("id", transaction.getId());
        json.addProperty("amount", transaction.getAmount());
        json.addProperty("day", transaction.getDay());
        json.addProperty("month", transaction.getMonth());
        json.addProperty("year", transaction.getYear());
        json.addProperty("transactionTypeId", transaction.getTransactionTypeId());
        json.addProperty("comment", transaction.getComment());
        json.addProperty("checked", transaction.getChecked());
        json.addProperty("accountDebitedId", transaction.getAccountDebitedId());
        json.addProperty("accountCreditedId", transaction.getAccountCreditedId());
        json.addProperty("accountDebitedTypeId", transaction.getAccountDebitedTypeId());
        json.addProperty("accountCreditedTypeId", transaction.getAccountCreditedTypeId());
        json.addProperty("accountDebitedCategoryId", transaction.getAccountDebitedCategoryId());
        json.addProperty("accountCreditedCategoryId", transaction.getAccountCreditedCategoryId());
        json.addProperty("groupId", transaction.getGroupId());
        json.addProperty("contactId", transaction.getContactId());
        json.addProperty("rate", transaction.getRate());
        json.addProperty("rateDirection", transaction.getRateDirection());
        json.addProperty("invoiceNumber", transaction.getInvoiceNumber());
        json.addProperty("guid", transaction.getGuid());
        json.addProperty("modified", transaction.getModified());
        return json.toString().getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public Transaction decrypt(String password) {
        var rawBytes = password != null && !password.isEmpty() ?
                aes256().decrypt(bytes, password) : bytes;
        var jsonString = new String(rawBytes, StandardCharsets.UTF_8);
        var obj = (JsonObject) new JsonParser().parse(jsonString);
        return new Transaction(obj.get("id").getAsInt(),
                obj.get("amount").getAsBigDecimal(),
                obj.get("day").getAsInt(),
                obj.get("month").getAsInt(),
                obj.get("year").getAsInt(),
                obj.get("transactionTypeId").getAsInt(),
                obj.get("comment").getAsString(),
                obj.get("checked").getAsBoolean(),
                obj.get("accountDebitedId").getAsInt(),
                obj.get("accountCreditedId").getAsInt(),
                obj.get("accountDebitedTypeId").getAsInt(),
                obj.get("accountCreditedTypeId").getAsInt(),
                obj.get("accountDebitedCategoryId").getAsInt(),
                obj.get("accountCreditedCategoryId").getAsInt(),
                obj.get("groupId").getAsInt(),
                obj.get("contactId").getAsInt(),
                obj.get("rate").getAsBigDecimal(),
                obj.get("rateDirection").getAsInt(),
                obj.get("invoiceNumber").getAsString(),
                obj.get("guid").getAsString(),
                obj.get("modified").getAsLong());
    }
}
