package com.cleanlearning;

import static com.tngtech.archunit.core.domain.JavaClass.Predicates.simpleNameEndingWith;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.Architectures.layeredArchitecture;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

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
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

/**
 * The layer rules of _docs/architecture/backend.md. Every layer and every rule must match classes,
 * so a mistyped package fails instead of passing silently.
 */
@AnalyzeClasses(
    packages = "com.cleanlearning",
    importOptions = ImportOption.DoNotIncludeTests.class)
class LayerRulesTest {

  private static final String CONFIG = "com.cleanlearning.config..";
  private static final String API = "com.cleanlearning.api..";
  private static final String API_RESPONSES = "com.cleanlearning.api..response";
  private static final String API_REQUESTS = "com.cleanlearning.api..request";
  private static final String APPLICATION = "com.cleanlearning.application..";
  private static final String USE_CASES = "com.cleanlearning.application.*";
  private static final String PORTS = "com.cleanlearning.application.*.port";
  private static final String DOMAIN = "com.cleanlearning.domain..";
  private static final String DOMAIN_VALUES = "com.cleanlearning.domain..value";
  private static final String DOMAIN_VIEWS = "com.cleanlearning.domain..view..";
  private static final String ADAPTERS_DATABASE = "com.cleanlearning.adapters.database..";
  private static final String ADAPTER_ENTITIES = "com.cleanlearning.adapters.database.*.entity";
  private static final String ADAPTER_ROWS = "com.cleanlearning.adapters.database.*.row";

  @ArchTest
  static final ArchRule layerDependencies =
      layeredArchitecture()
          .consideringOnlyDependenciesInLayers()
          .layer("config")
          .definedBy(CONFIG)
          .layer("api")
          .definedBy(API)
          .layer("use cases")
          .definedBy(USE_CASES)
          .layer("ports")
          .definedBy(PORTS)
          .layer("domain")
          .definedBy(DOMAIN)
          .layer("adapters.database")
          .definedBy(ADAPTERS_DATABASE)
          .whereLayer("config")
          .mayNotBeAccessedByAnyLayer()
          .whereLayer("api")
          .mayOnlyBeAccessedByLayers("config")
          .whereLayer("use cases")
          .mayOnlyBeAccessedByLayers("config", "api")
          .whereLayer("ports")
          .mayOnlyBeAccessedByLayers("use cases", "adapters.database")
          .whereLayer("domain")
          .mayOnlyBeAccessedByLayers("config", "api", "use cases", "ports", "adapters.database")
          .whereLayer("adapters.database")
          .mayOnlyBeAccessedByLayers("config");

  @ArchTest
  static final ArchRule domainFeaturesAreFreeOfCycles =
      slices().matching("com.cleanlearning.domain.(*)..").should().beFreeOfCycles();

  @ArchTest
  static final ArchRule applicationFeaturesAreFreeOfCycles =
      slices().matching("com.cleanlearning.application.(*)..").should().beFreeOfCycles();

  @ArchTest
  static final ArchRule domainDependsOnlyOnJavaAndDomain =
      classes()
          .that()
          .resideInAPackage(DOMAIN)
          .should()
          .onlyDependOnClassesThat()
          .resideInAnyPackage("java..", DOMAIN);

  /**
   * Models use value objects, never the other way round: a class that needs a model is a model. A
   * value object may throw the shared exceptions, such as {@code InvalidValueException}.
   */
  @ArchTest
  static final ArchRule valueObjectsDependOnlyOnJavaAndValueObjects =
      classes()
          .that()
          .resideInAPackage(DOMAIN_VALUES)
          .should()
          .onlyDependOnClassesThat()
          .resideInAnyPackage("java..", DOMAIN_VALUES, "com.cleanlearning.domain.shared");

  /**
   * Views depend on rules, never the other way round: what an actor sees is built from the rules.
   */
  @ArchTest
  static final ArchRule onlyViewsDependOnViews =
      noClasses()
          .that()
          .resideInAPackage(DOMAIN)
          .and()
          .resideOutsideOfPackage(DOMAIN_VIEWS)
          .should()
          .dependOnClassesThat()
          .resideInAPackage(DOMAIN_VIEWS);

  @ArchTest
  static final ArchRule applicationIsFreeOfJpa =
      noClasses()
          .that()
          .resideInAPackage(APPLICATION)
          .should()
          .dependOnClassesThat()
          .resideInAPackage("jakarta.persistence..");

  @ArchTest
  static final ArchRule entitiesLiveInAnAdapterEntityPackage =
      classes().that().areAnnotatedWith(Entity.class).should().resideInAPackage(ADAPTER_ENTITIES);

  @ArchTest
  static final ArchRule rowsLiveInAnAdapterRowPackage =
      classes()
          .that()
          .resideInAPackage(ADAPTERS_DATABASE)
          .and()
          .haveSimpleNameEndingWith("Row")
          .should()
          .resideInAPackage(ADAPTER_ROWS);

  @ArchTest
  static final ArchRule useCasesEndInUseCase =
      classes()
          .that()
          .resideInAPackage(APPLICATION)
          .and()
          .areAnnotatedWith(Service.class)
          .should()
          .haveSimpleNameEndingWith("UseCase");

  @ArchTest
  static final ArchRule useCasesLiveAtTheTopOfTheirFeature =
      classes()
          .that()
          .resideInAPackage(APPLICATION)
          .and()
          .areAnnotatedWith(Service.class)
          .should()
          .resideInAPackage(USE_CASES);

  @ArchTest
  static final ArchRule useCasesHaveOnlyInvokeAsPublicMethod =
      classes()
          .that()
          .resideInAPackage(APPLICATION)
          .and()
          .areAnnotatedWith(Service.class)
          .should(haveExactlyOnePublicMethodNamedInvoke());

  @ArchTest
  static final ArchRule portsEndInPort =
      classes()
          .that()
          .resideInAPackage(APPLICATION)
          .and()
          .areInterfaces()
          .and()
          .doNotHaveSimpleName("package-info") // compiled as a synthetic interface
          .should()
          .haveSimpleNameEndingWith("Port")
          .andShould()
          .resideInAPackage(PORTS);

  @ArchTest
  static final ArchRule portPackagesHoldOnlyPorts =
      classes()
          .that()
          .resideInAPackage(PORTS)
          .should()
          .beInterfaces()
          .andShould()
          .haveSimpleNameEndingWith("Port");

  @ArchTest
  static final ArchRule responsesLiveInAResponsePackage =
      classes()
          .that()
          .resideInAPackage(API)
          .and()
          .haveSimpleNameEndingWith("Response")
          .should()
          .resideInAPackage(API_RESPONSES);

  @ArchTest
  static final ArchRule requestsLiveInARequestPackage =
      classes()
          .that()
          .resideInAPackage(API)
          .and()
          .haveSimpleNameEndingWith("Request")
          .should()
          .resideInAPackage(API_REQUESTS);

  /**
   * springdoc names an OpenAPI schema after the simple class name, so two records with one name
   * share a schema.
   */
  @ArchTest
  static final ArchRule requestAndResponseNamesAreUnique =
      classes()
          .that()
          .resideInAPackage(API)
          .and(simpleNameEndingWith("Response").or(simpleNameEndingWith("Request")))
          .should(haveASimpleNameUniqueAmongThem());

  private static ArchCondition<JavaClass> haveExactlyOnePublicMethodNamedInvoke() {
    return new ArchCondition<>("have exactly one public method, named invoke") {

      @Override
      public void check(JavaClass javaClass, ConditionEvents events) {
        List<String> publicMethods =
            javaClass.getMethods().stream()
                .filter(method -> method.getModifiers().contains(JavaModifier.PUBLIC))
                .map(JavaMethod::getName)
                .sorted()
                .toList();
        if (!publicMethods.equals(List.of("invoke"))) {
          events.add(
              SimpleConditionEvent.violated(
                  javaClass,
                  javaClass.getName()
                      + " has the public methods "
                      + publicMethods
                      + " instead of one invoke"));
        }
      }
    };
  }

  private static ArchCondition<JavaClass> haveASimpleNameUniqueAmongThem() {
    return new ArchCondition<>(
        "have a simple name that no other of them has (it's the OpenAPI schema name)") {

      private Map<String, Long> countBySimpleName = Map.of();

      @Override
      public void init(Collection<JavaClass> allObjectsToTest) {
        countBySimpleName =
            allObjectsToTest.stream()
                .collect(Collectors.groupingBy(JavaClass::getSimpleName, Collectors.counting()));
      }

      @Override
      public void check(JavaClass javaClass, ConditionEvents events) {
        if (countBySimpleName.get(javaClass.getSimpleName()) > 1) {
          events.add(
              SimpleConditionEvent.violated(
                  javaClass,
                  javaClass.getName() + " shares its simple name with another class in api"));
        }
      }
    };
  }
}
