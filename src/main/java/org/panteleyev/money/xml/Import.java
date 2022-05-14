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
package org.panteleyev.money.xml;

import org.panteleyev.money.model.Account;
import org.panteleyev.money.model.Category;
import org.panteleyev.money.model.Contact;
import org.panteleyev.money.model.Currency;
import org.panteleyev.money.model.Icon;
import org.panteleyev.money.model.MoneyDocument;
import org.panteleyev.money.model.Transaction;

import javax.xml.XMLConstants;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static org.panteleyev.money.xml.Export.DOCUMENTS_ZIP_DIRECTORY;

public class Import {
    private final static Logger LOGGER = Logger.getLogger(Import.class.getName());
    private static final String SCHEMA = "/org/panteleyev/money/xsd/money.xsd";

    private final List<Icon> icons;
    private final List<Category> categories;
    private final List<Account> accounts;
    private final List<Contact> contacts;
    private final List<Currency> currencies;
    private final List<Transaction> transactions;
    private final List<MoneyDocument> documents;

    private final ZipInputStream zipInputStream;

    private static Schema moneySchema = null;

    private Import(ImportParser importParser, ZipInputStream zipInputStream) {
        icons = importParser.getIcons();
        categories = importParser.getCategories();
        accounts = importParser.getAccounts();
        contacts = importParser.getContacts();
        currencies = importParser.getCurrencies();
        transactions = importParser.getTransactions();
        documents = importParser.getDocuments();

        this.zipInputStream = zipInputStream;
    }

    public List<Icon> getIcons() {
        return icons;
    }

    public List<Category> getCategories() {
        return categories;
    }

    public List<Account> getAccounts() {
        return accounts;
    }

    public List<Contact> getContacts() {
        return contacts;
    }

    public List<Currency> getCurrencies() {
        return currencies;
    }

    public List<Transaction> getTransactions() {
        return transactions;
    }

    public List<MoneyDocument> getDocuments() {
        return documents;
    }

    public static Import doImport(ZipInputStream inStream) {
        try {
            var importParser = new ImportParser();

            ZipEntry zipEntry = inStream.getNextEntry();
            if (zipEntry == null || zipEntry.isDirectory() || !zipEntry.getName().endsWith(".xml")) {
                throw new IllegalStateException("Invalid import file, XML entry is missing");
            }

            var bytes = readEntryBytes(inStream);
            try (var xmlInputStream = new ByteArrayInputStream(bytes)) {
                if (moneySchema == null) {
                    var factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
                    moneySchema = factory.newSchema(Import.class.getResource(SCHEMA));
                }

                var factory = SAXParserFactory.newInstance();
                factory.setSchema(moneySchema);
                factory.setValidating(false);
                var parser = factory.newSAXParser();

                parser.parse(xmlInputStream, importParser);
            }

            return new Import(importParser, inStream);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    public BlobContent getNextBlobContent() {
        if (zipInputStream == null) {
            return null;
        }

        try {
            ZipEntry zipEntry;
            do {
                zipEntry = zipInputStream.getNextEntry();
                if (zipEntry == null) {
                    return null;
                }
            } while (zipEntry.isDirectory());

            BlobContent.BlobType type = null;
            String entryName = null;

            if (zipEntry.getName().startsWith(DOCUMENTS_ZIP_DIRECTORY)) {
                type = BlobContent.BlobType.DOCUMENT;
                entryName = zipEntry.getName().substring(DOCUMENTS_ZIP_DIRECTORY.length());
            }

            if (type == null) {
                return null;
            }

            try {
                var uuid = UUID.fromString(entryName);
                var bytes = readEntryBytes(zipInputStream);
                return new BlobContent(uuid, type, bytes);
            } catch (IllegalArgumentException ex) {
                LOGGER.log(Level.SEVERE, "Incorrect document entry " + entryName + " in import file");
                return null;
            }
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }

    private static byte[] readEntryBytes(ZipInputStream inputStream) throws IOException {
        var buffer = new byte[4096 * 4096];     // 16K buffer

        try (var tempBuffer = new ByteArrayOutputStream()) {
            int len;
            while ((len = inputStream.read(buffer)) > 0) {
                tempBuffer.write(buffer, 0, len);
            }
            return tempBuffer.toByteArray();
        }
    }
}
