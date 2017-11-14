# Using the tycho compiler with external annotations

This directory provides a sample configuration to configure the tycho compiler to use external annations provided via a maven dependency.

## How to use this

After adding the external annotations as a dependency like this:

```
<dependencies>
    <dependency>
      <groupId>org.lastnpe.eea</groupId>
      <artifactId>jdk-eea</artifactId>
      <version>0.0.1</version>
      <scope>provided</scope>
    </dependency>
  </dependencies>
```

one has to put it on the classpath of the compiler via `<extraClasspathElement>` like this:

```
<plugin>
          <groupId>org.eclipse.tycho</groupId>
          <artifactId>tycho-compiler-plugin</artifactId>
          <version>1.0.0</version>
          <configuration>
            <extraClasspathElements>
              <extraClasspathElement>
                <groupId>org.eclipse.jdt</groupId>
                <artifactId>org.eclipse.jdt.annotation</artifactId>
                <version>${jdt-annotations.version}</version>
              </extraClasspathElement>
              <extraClasspathElement>
                <groupId>org.lastnpe.eea</groupId>
                <artifactId>jdk-eea</artifactId>
                <version>0.0.1</version>
              </extraClasspathElement>
            </extraClasspathElements>
            <compilerArgs>
              <!--Adjust your warning/error level setting here-->
              <arg>-err:+nullAnnot(org.eclipse.jdt.annotation.Nullable|org.eclipse.jdt.annotation.NonNull|org.eclipse.jdt.annotation.NonNullByDefault),+inheritNullAnnot</arg>
              <arg>-warn:+null,+inheritNullAnnot,+nullAnnotConflict,+nullUncheckedConversion,+nullAnnotRedundant,+nullDereference</arg>
              <!--Read annotations from dependencies that we added to the classpath-->
              <arg>-annotationpath</arg>
              <arg>CLASSPATH</arg>
            </compilerArgs>
          </configuration>
</plugin>
```

Now the tycho compiler read the external annotations from the `CLASSPATH`.