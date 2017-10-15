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

package org.panteleyev.money.statements;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.Charset;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

public final class StatementParser {
    private static final DateTimeFormatter RBA_DATE_FORMAT = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");
    private static final char RBA_DELIMITER = ';';
    private static final int RBA_RECORD_SIZE = 10;
    private static final String RBA_ENCODING = "windows-1251";

    public static Statement parse(Statement.StatementType type, InputStream inStream) {
        switch (type) {
            case RAIFFEISEN_CREDIT_CARD_CSV:
                return parseRaiffeisenCSV(inStream);

            default:
                throw new IllegalArgumentException();

        }
    }

    private static Statement parseRaiffeisenCSV(InputStream inStream) {
        try {
            List<StatementRecord> records = new ArrayList<>();
            CSVParser parser = CSVParser.parse(inStream, Charset.forName(RBA_ENCODING),
                    CSVFormat.EXCEL.withDelimiter(RBA_DELIMITER));

            for (CSVRecord r : parser.getRecords()) {
                if (r.getRecordNumber() == 1L || r.size() < RBA_RECORD_SIZE) {
                    continue;
                }

                records.add(new StatementRecord(
                        LocalDate.parse(r.get(0), RBA_DATE_FORMAT),
                        LocalDate.parse(r.get(1), RBA_DATE_FORMAT),
                        r.get(2),
                        r.get(3),
                        r.get(4),
                        r.get(5),
                        r.get(6),
                        r.get(7).replaceAll(" ", ""),
                        r.get(8),
                        r.get(9).replaceAll(" ", "")
                ));
            }

            return new Statement(Statement.StatementType.RAIFFEISEN_CREDIT_CARD_CSV, records);
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        } catch (DateTimeParseException ex) {
            throw new RuntimeException(ex);
        }
    }
}
