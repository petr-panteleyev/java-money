/*
 Copyright © 2020 Petr Panteleyev <petr@panteleyev.org>
 SPDX-License-Identifier: BSD-2-Clause
 */
package org.panteleyev.ofx;

public class OFXParserException extends RuntimeException {
    OFXParserException(String message) {
        super(message);
    }
}
