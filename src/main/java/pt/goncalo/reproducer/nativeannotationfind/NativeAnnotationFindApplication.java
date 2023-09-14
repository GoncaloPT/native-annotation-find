package pt.goncalo.reproducer.nativeannotationfind;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;
import org.springframework.aot.hint.annotation.RegisterReflectionForBinding;
import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.context.annotation.ImportRuntimeHints;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import pt.goncalo.reproducer.nativeannotationfind.other.SomeAnnotation;
import pt.goncalo.reproducer.nativeannotationfind.other.SomeInterface;

import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.Collection;
import java.util.Objects;
import java.util.stream.Collectors;

import static org.springframework.aot.hint.MemberCategory.*;

@SpringBootApplication
@RegisterReflectionForBinding({
        SomeAnnotation.class,
        SomeInterface.class
})
@ImportRuntimeHints(NativeAnnotationFindApplication.Hints.class)
public class NativeAnnotationFindApplication {
    private static final Logger log = LoggerFactory.getLogger(NativeAnnotationFindApplication.class);

    public static void main(String[] args) {
        SpringApplication.run(NativeAnnotationFindApplication.class, args);
    }

    @Bean
    CommandLineRunner commandLineRunner() {
        return args -> {
            var classesWithAnnotation = getClassNamesWithAnnotation(SomeAnnotation.class);
            log.info("class(es) found: {}", classesWithAnnotation);
            if (classesWithAnnotation.isEmpty())
                throw new RuntimeException("failed.. Could not find classes with SomeAnnotation.class");
            log.info("ALL GOOD!");

        };
    }

    static class Hints implements RuntimeHintsRegistrar {

        @Override
        public void registerHints(RuntimeHints hints, ClassLoader classLoader) {
            hints
                    .reflection()

                    .registerType(SomeAnnotation.class, allCategories)
                    .registerType(SomeInterface.class, allCategories)
                    .registerType(NativeAnnotationFindApplication.class, allCategories);
        }

    }

    static Collection<String> getClassNamesWithAnnotation(Class<? extends Annotation> annotation,
                                                          String... searchRootPackages) {
        log.debug("getClassNamesWithAnnotation called with {} and searchPackages: {}", annotation, searchRootPackages);
        if (searchRootPackages == null || searchRootPackages.length == 0) {

            var currentPackageNameSplitted = NativeAnnotationFindApplication.class.getCanonicalName().split("\\.");
            final var currentClassPackagePrefix = NativeAnnotationFindApplication.class.getCanonicalName().split("\\.")[0]
                    + (currentPackageNameSplitted.length > 1 ? "." + NativeAnnotationFindApplication.class.getCanonicalName().split("\\.")[1] : "");

            log.info("No searchRootPackages provided. Using all packages that start with {}",
                    currentClassPackagePrefix);
            searchRootPackages = Arrays
                    .stream(Package.getPackages())
                    .map(Package::getName)
                    .filter(name -> name.startsWith(currentClassPackagePrefix))
                    .toArray(String[]::new);
        }

        log.info("Scanning in # packages {} ", searchRootPackages.length);
        ClassPathScanningCandidateComponentProvider scanner =
                new ClassPathScanningCandidateComponentProvider(false) {
                    // Override isCandidateComponent to only scan for interface
                    @Override
                    protected boolean isCandidateComponent(AnnotatedBeanDefinition beanDefinition) {

                        log.debug("isCandidateComponent scanning bean: {} with metadata {}", beanDefinition,
                                beanDefinition.getMetadata());
                        AnnotationMetadata metadata = beanDefinition.getMetadata();
                        return metadata.isIndependent() && metadata.isInterface();
                    }
                };
        scanner.addIncludeFilter(new AnnotationTypeFilter(annotation));


        var candidates = Arrays
                .stream(searchRootPackages)
                .flatMap(pck -> scanner.findCandidateComponents(pck).stream())
                .filter(Objects::nonNull)
                .map(BeanDefinition::getBeanClassName)
                .collect(Collectors.toSet());

        log.debug("found # {} with annotation {}", candidates.size(), annotation);
        return candidates;


    }

    static MemberCategory[] allCategories = {
            PUBLIC_FIELDS,
            DECLARED_FIELDS,
            INTROSPECT_PUBLIC_CONSTRUCTORS,
            INTROSPECT_DECLARED_CONSTRUCTORS,
            INVOKE_PUBLIC_CONSTRUCTORS,
            INVOKE_DECLARED_CONSTRUCTORS,
            INTROSPECT_PUBLIC_METHODS,
            INTROSPECT_DECLARED_METHODS,
            INVOKE_PUBLIC_METHODS,
            INVOKE_DECLARED_METHODS,
            PUBLIC_CLASSES,
            DECLARED_CLASSES
    };

}
