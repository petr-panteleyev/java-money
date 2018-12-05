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
import org.panteleyev.money.persistence.model.Account;
import org.panteleyev.persistence.Record;
import org.panteleyev.persistence.annotations.Column;
import org.panteleyev.persistence.annotations.RecordBuilder;
import org.panteleyev.persistence.annotations.Table;
import java.nio.charset.StandardCharsets;
import static org.panteleyev.crypto.AES.aes256;

@Table("account")
public final class AccountDto implements Record, Dto<Account> {
    @Column(value = "id", primaryKey = true)
    private final int id;
    @Column(value = "bytes", length = BINARY_LENGTH)
    private final byte[] bytes;

    @RecordBuilder
    public AccountDto(@Column("id") int id, @Column("bytes") byte[] bytes) {
        this.id = id;
        this.bytes = bytes;
    }

    AccountDto(Account account, String password) {
        id = account.getId();

        var rawBytes = toJson(account);
        bytes = password != null && !password.isEmpty() ?
                aes256().encrypt(rawBytes, password) : rawBytes;
    }

    @Override
    public int getId() {
        return id;
    }

    @Override
    public byte[] toJson(Account account) {
        var json = new JsonObject();
        json.addProperty("id", account.getId());
        json.addProperty("name", account.getName());
        json.addProperty("comment", account.getComment());
        json.addProperty("accountNumber", account.getAccountNumber());
        json.addProperty("openingBalance", account.getOpeningBalance());
        json.addProperty("accountLimit", account.getAccountLimit());
        json.addProperty("currencyRate", account.getCurrencyRate());
        json.addProperty("typeId", account.getTypeId());
        json.addProperty("categoryId", account.getCategoryId());
        json.addProperty("currencyId", account.getCurrencyId());
        json.addProperty("enabled", account.getEnabled());
        json.addProperty("guid", account.getGuid());
        json.addProperty("modified", account.getModified());
        return json.toString().getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public Account decrypt(String password) {
        var rawBytes = password != null && !password.isEmpty() ?
                aes256().decrypt(bytes, password) : bytes;
        var jsonString = new String(rawBytes, StandardCharsets.UTF_8);
        var obj = (JsonObject) new JsonParser().parse(jsonString);

        var accountNumber = obj.get("accountNumber");

        return new Account(obj.get("id").getAsInt(),
                obj.get("name").getAsString(),
                obj.get("comment").getAsString(),
                accountNumber == null || accountNumber.isJsonNull() ? "" : accountNumber.getAsString(),
                obj.get("openingBalance").getAsBigDecimal(),
                obj.get("accountLimit").getAsBigDecimal(),
                obj.get("currencyRate").getAsBigDecimal(),
                obj.get("typeId").getAsInt(),
                obj.get("categoryId").getAsInt(),
                obj.get("currencyId").getAsInt(),
                obj.get("enabled").getAsBoolean(),
                obj.get("guid").getAsString(),
                obj.get("modified").getAsLong());
    }
}
