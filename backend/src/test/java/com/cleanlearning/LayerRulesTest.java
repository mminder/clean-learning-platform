package com.cleanlearning;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.Architectures.layeredArchitecture;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import jakarta.persistence.Entity;
import org.springframework.stereotype.Service;

/**
 * The layer rules of _docs/architecture/backend.md. Empty layers are allowed, so the rules hold before the code exists.
 */
@AnalyzeClasses(packages = "com.cleanlearning", importOptions = ImportOption.DoNotIncludeTests.class)
class LayerRulesTest {

    private static final String CONFIG = "com.cleanlearning.config..";
    private static final String API = "com.cleanlearning.api..";
    private static final String APPLICATION = "com.cleanlearning.application..";
    private static final String DOMAIN = "com.cleanlearning.domain..";
    private static final String ADAPTERS_DATABASE = "com.cleanlearning.adapters.database..";

    @ArchTest
    static final ArchRule layerDependencies = layeredArchitecture()
            .consideringOnlyDependenciesInLayers()
            .withOptionalLayers(true)
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
    static final ArchRule domainIsFreeOfFrameworks = noClasses()
            .that().resideInAPackage(DOMAIN)
            .should().dependOnClassesThat().resideInAnyPackage("org.springframework..", "jakarta.persistence..", "org.hibernate..")
            .allowEmptyShould(true);

    @ArchTest
    static final ArchRule applicationIsFreeOfJpa = noClasses()
            .that().resideInAPackage(APPLICATION)
            .should().dependOnClassesThat().resideInAPackage("jakarta.persistence..")
            .allowEmptyShould(true);

    @ArchTest
    static final ArchRule entitiesLiveInAdaptersDatabase = classes()
            .that().areAnnotatedWith(Entity.class)
            .should().resideInAPackage(ADAPTERS_DATABASE)
            .allowEmptyShould(true);

    @ArchTest
    static final ArchRule useCaseServicesEndInService = classes()
            .that().resideInAPackage(APPLICATION).and().areAnnotatedWith(Service.class)
            .should().haveSimpleNameEndingWith("Service")
            .allowEmptyShould(true);

    @ArchTest
    static final ArchRule portsEndInPort = classes()
            .that().resideInAPackage(APPLICATION).and().areInterfaces()
            .and().doNotHaveSimpleName("package-info") // compiled as a synthetic interface
            .should().haveSimpleNameEndingWith("Port")
            .allowEmptyShould(true);
}
