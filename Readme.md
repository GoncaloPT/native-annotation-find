# Error reproducer

## Problem description

When running a graalvm generated native image/executable, ClassPathScanningCandidateComponentProvider doesn't
seem to be able to find interfaces/classes with annotation.

## Steps to reproduce

Running the following should not yield any exception:

```shell
mvn spring-boot:run
```

But, the same running the native image, doesn't work:

```shell
mvn -Pnative native:compile
target/native-annotation-find
```

## Already tried fixes

### reflect-config.json

The first approach was to try to add the used classes in reflect-config.json so that somehow they could be "seen"
in runtime.

### @RegisterReflectionForBinding

Classes which take part in the example ( SomeAnnotation, SomeInterface ) were added using this annotation.
This can be found in NativeAnnotationFindApplication.

### @ImportRuntimeHints(Application.GraphqlHints.class)

A RuntimeHintsRegistrar was implemented in NativeAnnotationFindApplication, registering the above mentioned classes.

### Tracing agent

Used what is defined
in https://www.graalvm.org/latest/reference-manual/native-image/metadata/AutomaticMetadataCollection/  
Executed `java -agentlib:native-image-agent=config-output-dir=./output -jar target/native-annotation-find-0.0.1-SNAPSHOT.jar`
then copied the contents of output folder to resources/META-INF/native-image

AT THIS POINT https://github.com/spring-projects/spring-boot/issues/37396 was created
### Adding more entries to resource-config - FIXED!

Native execution logs showed something interesting:

```text
2023-09-15T00:20:52.601+01:00  INFO 14904 --- [           main] .i.s.PathMatchingResourcePatternResolver : Skipping search for files matching pattern [**/*.class]: directory [/pt/goncalo/reproducer/nativeannotationfind/other] does not exist
2023-09-15T00:20:52.601+01:00  INFO 14904 --- [           main] .i.s.PathMatchingResourcePatternResolver : Skipping search for files matching pattern [**/*.class]: directory [/pt/goncalo/reproducer/nativeannotationfind] does not exist

```

Which seemed to highlight that the actual class files couldn't be found...
By adding, to resource-config:

```json
[
  {
    "pattern": "\\Qpt/goncalo/reproducer/nativeannotationfind/\\E.+\\.class$"
  },
  {
    "pattern": "\\Qpt/goncalo/reproducer/nativeannotationfind/other/\\E.+\\.class$"
  }
]
```
The situation was solved. The final version of resource-config.json is:

```json
{
  "resources": {
    "includes": [
      {
        "pattern": "\\Qpt/goncalo/\\E.+\\.class$"
      }
    ]
  }
}


```
