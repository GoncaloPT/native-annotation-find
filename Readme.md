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
The first approach was to try to add the used classes in reflect-config.json so that somehow the could be "seen"
in runtime.

### @RegisterReflectionForBinding

Classes which take part in the example ( SomeAnnotation, SomeInterface ) were added using this annotation.
This can be found in NativeAnnotationFindApplication.

### @ImportRuntimeHints(Application.GraphqlHints.class)

A RuntimeHintsRegistrar was implemented in NativeAnnotationFindApplication, registering the above mentioned classes.
