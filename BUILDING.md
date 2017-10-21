# Building Money Manager

## Source Code

The following repositories must be cloned to build Money Manager:

1. git clone https://github.com/petr-panteleyev/java-utilities.git utilities
2. git clone https://github.com/petr-panteleyev/java-persistence.git persistence
3. git clone https://github.com/petr-panteleyev/java-money.git money

Temporary Maven repository

In order to use custom jar files with module-info setup additional Maven repository in settings.xml and make it appear first:

```
<repository>
  <id>panteleyev.org</id>
  <url>http://www.panteleyev.org/maven/</url>
</repository>
```

## Building Dependencies

```
cd <utilities>
mvn install

cd <persistence>
mvn install
```

## Building Standalone JAR

```
cd <money>
mvn clean package
```

## Building Native Packages

```
cd <money>
mvn clean package
mvn exec:exec@<native-dist>
```

Where &lt;native-dist> depends on native OS and packaging.

`dist-mac` produces DMG file. Its content can be copied to the Applications folder as is.

`dist-win` produces EXE file with a simple installer. This option requires additional software. Please refer to
[javapackager](https://docs.oracle.com/javase/8/docs/technotes/tools/unix/javapackager.html) documentation for details.

`dist-rpm` produces RPM file. This option was tested on OpenSUSE Leap 42.2.

Resulting package can be found in `<money>/target/dists/bundles`.
