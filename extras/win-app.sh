#!/bin/sh

rm -rf target/dist

$JPACKAGE_HOME/bin/jpackage \
    --module org.panteleyev.money/org.panteleyev.money.MoneyApplication \
    --runtime-image "$JAVA_HOME" \
    --verbose \
    --dest target/dist \
    -p target/jmods \
    --java-options "--add-exports javafx.base/com.sun.javafx.event=org.controlsfx.controls --add-reads org.panteleyev.persistence=org.panteleyev.money.model" \
    --icon icons/icons.ico \
    --name "Money Manager" \
    --app-version 19.4.0 \
    --vendor panteleyev.org \
    --win-menu \
    --win-dir-chooser \
    --win-upgrade-uuid 38dac4b6-91d2-4ca8-aacb-2e0cfd54127a \
    --win-menu-group "panteleyev.org"