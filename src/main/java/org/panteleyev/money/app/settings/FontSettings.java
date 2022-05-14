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
package org.panteleyev.money.app.settings;

import javafx.scene.text.Font;
import javafx.scene.text.FontPosture;
import javafx.scene.text.FontWeight;
import org.w3c.dom.Element;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import static java.util.Objects.requireNonNull;
import static org.panteleyev.money.xml.XMLUtils.appendElement;
import static org.panteleyev.money.xml.XMLUtils.createDocument;
import static org.panteleyev.money.xml.XMLUtils.getAttribute;
import static org.panteleyev.money.xml.XMLUtils.readDocument;
import static org.panteleyev.money.xml.XMLUtils.writeDocument;

final class FontSettings {
    private static final String DEFAULT_FONT_FAMILY = "System";
    private static final String DEFAULT_FONT_STYLE = "Normal Regular";
    private static final double DEFAULT_FONT_SIZE = 12;

    private static final String ROOT_ELEMENT = "fonts";
    private static final String FONT_ELEMENT = "font";
    private static final String FONT_ATTR_NAME = "name";
    private static final String FONT_ATTR_FAMILY = "family";
    private static final String FONT_ATTR_STYLE = "style";
    private static final String FONT_ATTR_SIZE = "size";

    private final Map<FontName, Font> fontMap = new ConcurrentHashMap<>();

    Font getFont(FontName fontName) {
        return fontMap.computeIfAbsent(fontName,
            key -> Font.font(DEFAULT_FONT_FAMILY, FontWeight.NORMAL, FontPosture.REGULAR, DEFAULT_FONT_SIZE));
    }

    void setFont(FontName fontName, Font font) {
        fontMap.put(requireNonNull(fontName), requireNonNull(font));
    }

    void save(OutputStream out) {
        var root = createDocument(ROOT_ELEMENT);

        for (var entry : fontMap.entrySet()) {
            var e = appendElement(root, FONT_ELEMENT);
            var font = entry.getValue();
            e.setAttribute(FONT_ATTR_NAME, entry.getKey().name());
            e.setAttribute(FONT_ATTR_FAMILY, font.getFamily());
            e.setAttribute(FONT_ATTR_STYLE, font.getStyle());
            e.setAttribute(FONT_ATTR_SIZE, Double.toString(font.getSize()));
        }

        writeDocument(root.getOwnerDocument(), out);
    }

    void load(InputStream in) {
        fontMap.clear();
        var root = readDocument(in);

        var fontNodes = root.getElementsByTagName(FONT_ELEMENT);
        for (int i = 0; i < fontNodes.getLength(); i++) {
            var fontElement = (Element) fontNodes.item(i);
            FontName.of(fontElement.getAttribute(FONT_ATTR_NAME).toUpperCase()).ifPresent(option -> {
                var family = getAttribute(fontElement, FONT_ATTR_FAMILY, DEFAULT_FONT_FAMILY);
                var style = getAttribute(fontElement, FONT_ATTR_STYLE, DEFAULT_FONT_STYLE);
                var size = getAttribute(fontElement, FONT_ATTR_SIZE, DEFAULT_FONT_SIZE);

                var font = Font.font(family,
                    style.toLowerCase().contains("bold") ? FontWeight.BOLD : FontWeight.NORMAL,
                    style.toLowerCase().contains("italic") ? FontPosture.ITALIC : FontPosture.REGULAR,
                    size);

                fontMap.put(option, font);
            });
        }
    }
}
