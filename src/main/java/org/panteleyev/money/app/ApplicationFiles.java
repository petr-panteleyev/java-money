/*
 Copyright (C) 2021, 2022 Petr Panteleyev

 This program is free software: you can redistribute it and/or modify it under the
 terms of the GNU General Public License as published by the Free Software
 Foundation, either version 3 of the License, or (at your option) any later version.

 This program is distributed in the hope that it will be useful, but WITHOUT ANY
 WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.

 You should have received a copy of the GNU General Public License along with this
 program. If not, see <https://www.gnu.org/licenses/>.
 */
package org.panteleyev.money.app;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URL;
import java.util.EnumMap;
import java.util.Map;
import java.util.function.Consumer;

public final class ApplicationFiles {
    public enum AppFile {
        MAIN_CSS("main.css"),
        DIALOG_CSS("dialog.css"),
        ABOUT_DIALOG_CSS("about-dialog.css"),
        PROFILES("profiles.xml"),
        SETTINGS("settings.xml"),
        WINDOWS("windows.xml"),
        COLORS("colors.xml"),
        FONTS("fonts.xml");

        private final String fileName;

        AppFile(String fileName) {
            this.fileName = fileName;
        }

        public String getFileName() {
            return fileName;
        }
    }

    private static final String APPLICATION_DIRECTORY = ".money-manager";

    private final File applicationDirectory =
            new File(System.getProperty("user.home") + File.separator + APPLICATION_DIRECTORY);
    private final File logDirectory =
            new File(applicationDirectory, "logs");

    private final Map<AppFile, File> fileMap = new EnumMap<>(AppFile.class);

    public ApplicationFiles() {
        for (var appFile : AppFile.values()) {
            fileMap.put(appFile, new File(applicationDirectory, appFile.getFileName()));
        }
    }

    public void initialize() {
        initDirectory(applicationDirectory, "Application");
        initDirectory(logDirectory, "Log");
    }

    public void write(AppFile appFile, Consumer<FileOutputStream> fileConsumer) {
        try (var out = new FileOutputStream(fileMap.get(appFile))) {
            fileConsumer.accept(out);
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }

    public void read(AppFile appFile, Consumer<FileInputStream> fileConsumer) {
        var file = fileMap.get(appFile);
        if (!file.exists()) {
            return;
        }

        try (var in = new FileInputStream(file)) {
            fileConsumer.accept(in);
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }

    public URL getUrl(AppFile appFile) {
        try {
            return fileMap.get(appFile).toURI().toURL();
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }

    private static void initDirectory(File dir, String name) {
        if (!dir.exists()) {
            if (!dir.mkdir()) {
                throw new RuntimeException(name + " directory cannot be created");
            }
        } else {
            if (!dir.isDirectory()) {
                throw new RuntimeException(name + " directory cannot be opened");
            }
        }
    }
}
