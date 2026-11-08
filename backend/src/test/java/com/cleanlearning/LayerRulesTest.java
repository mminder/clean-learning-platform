package com.cleanlearning;

import static com.tngtech.archunit.core.domain.JavaClass.Predicates.simpleNameEndingWith;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.Architectures.layeredArchitecture;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaMethod;
import com.tngtech.archunit.core.domain.JavaModifier;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import jakarta.persistence.Entity;
import org.springframework.stereotype.Service;

/**
 * The layer rules of _docs/architecture/backend.md. Every layer and every rule must match classes, so a mistyped
 * package fails instead of passing silently.
 */
@AnalyzeClasses(packages = "com.cleanlearning", importOptions = ImportOption.DoNotIncludeTests.class)
class LayerRulesTest {

    private static final String CONFIG = "com.cleanlearning.config..";
    private static final String API = "com.cleanlearning.api..";
    private static final String API_RESPONSES = "com.cleanlearning.api..response";
    private static final String APPLICATION = "com.cleanlearning.application..";
    private static final String DOMAIN = "com.cleanlearning.domain..";
    private static final String ADAPTERS_DATABASE = "com.cleanlearning.adapters.database..";
    private static final String ADAPTER_ENTITIES = "com.cleanlearning.adapters.database.*.entity";
    private static final String ADAPTER_ROWS = "com.cleanlearning.adapters.database.*.row";

    @ArchTest
    static final ArchRule layerDependencies = layeredArchitecture()
            .consideringOnlyDependenciesInLayers()
            .layer("config").definedBy(CONFIG)
            .layer("api").definedBy(API)
            .layer("application").definedBy(APPLICATION)
            .layer("domain").definedBy(DOMAIN)
            .layer("adapters.database").definedBy(ADAPTERS_DATABASE)
            .whereLayer("config").mayNotBeAccessedByAnyLayer()
            .whereLayer("api").mayOnlyBeAccessedByLayers("config")
            .whereLayer("application").mayOnlyBeAccessedByLayers("config", "api", "adapters.database")
            .whereLayer("domain").mayOnlyBeAccessedByLayers("config", "api", "application", "adapters.database")
            .whereLayer("adapters.database").mayOnlyBeAccessedByLayers("config");

    @ArchTest
    static final ArchRule domainDependsOnlyOnJavaAndDomain = classes()
            .that().resideInAPackage(DOMAIN)
            .should().onlyDependOnClassesThat().resideInAnyPackage("java..", DOMAIN);

    @ArchTest
    static final ArchRule applicationIsFreeOfJpa = noClasses()
            .that().resideInAPackage(APPLICATION)
            .should().dependOnClassesThat().resideInAPackage("jakarta.persistence..");

    @ArchTest
    static final ArchRule entitiesLiveInAnAdapterEntityPackage = classes()
            .that().areAnnotatedWith(Entity.class)
            .should().resideInAPackage(ADAPTER_ENTITIES);

    @ArchTest
    static final ArchRule rowsLiveInAnAdapterRowPackage = classes()
            .that().resideInAPackage(ADAPTERS_DATABASE).and().haveSimpleNameEndingWith("Row")
            .should().resideInAPackage(ADAPTER_ROWS);

    @ArchTest
    static final ArchRule useCasesEndInUseCase = classes()
            .that().resideInAPackage(APPLICATION).and().areAnnotatedWith(Service.class)
            .should().haveSimpleNameEndingWith("UseCase");

    @ArchTest
    static final ArchRule useCasesHaveOnlyInvokeAsPublicMethod = classes()
            .that().resideInAPackage(APPLICATION).and().areAnnotatedWith(Service.class)
            .should(haveExactlyOnePublicMethodNamedInvoke());

    @ArchTest
    static final ArchRule portsEndInPort = classes()
            .that().resideInAPackage(APPLICATION).and().areInterfaces()
            .and().doNotHaveSimpleName("package-info") // compiled as a synthetic interface
            .should().haveSimpleNameEndingWith("Port");

    /** A request rule follows with the first request record: every rule must match a class. */
    @ArchTest
    static final ArchRule responsesLiveInAResponsePackage = classes()
            .that().resideInAPackage(API).and().haveSimpleNameEndingWith("Response")
            .should().resideInAPackage(API_RESPONSES);

    /** springdoc names an OpenAPI schema after the simple class name, so two records with one name share a schema. */
    @ArchTest
    static final ArchRule requestAndResponseNamesAreUnique = classes()
            .that().resideInAPackage(API)
            .and(simpleNameEndingWith("Response").or(simpleNameEndingWith("Request")))
            .should(haveASimpleNameUniqueAmongThem());

    private static ArchCondition<JavaClass> haveExactlyOnePublicMethodNamedInvoke() {
        return new ArchCondition<>("have exactly one public method, named invoke") {

            @Override
            public void check(JavaClass javaClass, ConditionEvents events) {
                List<String> publicMethods = javaClass.getMethods().stream()
                        .filter(method -> method.getModifiers().contains(JavaModifier.PUBLIC))
                        .map(JavaMethod::getName)
                        .sorted()
                        .toList();
                if (!publicMethods.equals(List.of("invoke"))) {
                    events.add(SimpleConditionEvent.violated(javaClass,
                            javaClass.getName() + " has the public methods " + publicMethods + " instead of one invoke"));
                }
            }
        };
    }

    private static ArchCondition<JavaClass> haveASimpleNameUniqueAmongThem() {
        return new ArchCondition<>("have a simple name that no other of them has (it's the OpenAPI schema name)") {

            private Map<String, Long> countBySimpleName = Map.of();

            @Override
            public void init(Collection<JavaClass> allObjectsToTest) {
                countBySimpleName = allObjectsToTest.stream()
                        .collect(Collectors.groupingBy(JavaClass::getSimpleName, Collectors.counting()));
            }

            @Override
            public void check(JavaClass javaClass, ConditionEvents events) {
                if (countBySimpleName.get(javaClass.getSimpleName()) > 1) {
                    events.add(SimpleConditionEvent.violated(javaClass,
                            javaClass.getName() + " shares its simple name with another class in api"));
                }
            }
        };
    }
}
