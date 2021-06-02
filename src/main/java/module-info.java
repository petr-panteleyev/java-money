open module org.panteleyev.money {
    requires java.base;
    requires java.prefs;
    requires java.sql;
    requires java.naming;
    requires java.net.http;
    requires java.logging;

    requires jdk.crypto.cryptoki;

    requires org.panteleyev.fx;
    requires org.panteleyev.ofx;
    requires org.panteleyev.mysqlapi;

    requires org.controlsfx.controls;

    requires com.google.gson;
    requires freemarker;
    requires org.jsoup;

    requires mysql.connector.java;
}
