/*
 Copyright © 2017-2025 Petr Panteleyev <petr@panteleyev.org>
 SPDX-License-Identifier: BSD-2-Clause
 */
package org.panteleyev.money.statements;

import org.panteleyev.money.desktop.commons.DataCache;

import java.util.List;
import java.util.Optional;

public final class StatementParser {
    private static final List<Parser> PARSERS = List.of(new RBAParser(), new SberbankParser(), new RbaCsvParser());

    private static Optional<Parser> getParser(RawStatementData data) {
        for (var parser : PARSERS) {
            var type = parser.detectType(data);
            if (type != StatementType.UNKNOWN) {
                return Optional.of(parser);
            }
        }
        return Optional.empty();
    }

    public static Optional<Statement> parse(RawStatementData data, DataCache cache) {
        return getParser(data)
                .map(parser -> parser.parse(data, cache));
    }
}
