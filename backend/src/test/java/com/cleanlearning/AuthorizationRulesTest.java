package com.cleanlearning;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.methods;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noMethods;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaAnnotation;
import com.tngtech.archunit.core.domain.JavaMethod;
import com.tngtech.archunit.core.domain.JavaModifier;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import java.lang.reflect.Method;
import java.util.Set;
import java.util.stream.Stream;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * The authorization rules of _docs/architecture/security.md (D-26). Each controller says who may
 * call its routes with {@code @PreAuthorize}; Spring Security checks it when the method is called,
 * through a proxy. These rules keep that from failing open: a route without the annotation, a
 * method the proxy can't intercept, a second mechanism, or an exception handler that swallows the
 * denial would each let a call through silently. {@code RoleMatrixTest} proves what the annotations
 * say.
 */
@AnalyzeClasses(
    packages = "com.cleanlearning",
    importOptions = ImportOption.DoNotIncludeTests.class)
class AuthorizationRulesTest {

  /**
   * Deny by default: a route without {@code @PreAuthorize} fails the build instead of answering
   * everyone.
   */
  @ArchTest
  static final ArchRule handlerMethodsSayWhoMayCallThem =
      methods()
          .that()
          .areMetaAnnotatedWith(RequestMapping.class)
          .and()
          .areDeclaredInClassesThat()
          .areMetaAnnotatedWith(Controller.class)
          .should(beAnnotatedWithPreAuthorizeOrDeclaredInAClassThatIs());

  /**
   * The proxy subclasses the controller, so a final controller can't be proxied and its routes go
   * unchecked.
   */
  @ArchTest
  static final ArchRule controllersAreNotFinal =
      classes()
          .that()
          .areMetaAnnotatedWith(Controller.class)
          .should()
          .notHaveModifier(JavaModifier.FINAL);

  /** The proxy intercepts only methods it can override: not final, private or static ones. */
  @ArchTest
  static final ArchRule handlerMethodsCanBeIntercepted =
      methods()
          .that()
          .areMetaAnnotatedWith(RequestMapping.class)
          .and()
          .areDeclaredInClassesThat()
          .areMetaAnnotatedWith(Controller.class)
          .should()
          .notHaveModifier(JavaModifier.FINAL)
          .andShould()
          .notHaveModifier(JavaModifier.PRIVATE)
          .andShould()
          .notHaveModifier(JavaModifier.STATIC);

  /**
   * One place: controllers. On a use case or an adapter it would be a second, hidden rule, and
   * Spring Security would check it only where a proxy is called.
   */
  @ArchTest
  static final ArchRule onlyControllersArePreAuthorized =
      classes()
          .that()
          .areAnnotatedWith(PreAuthorize.class)
          .should()
          .beMetaAnnotatedWith(Controller.class);

  /** Today every controller says it once, at class level, so this rule may match nothing. */
  @ArchTest
  static final ArchRule onlyHandlerMethodsArePreAuthorized =
      methods()
          .that()
          .areAnnotatedWith(PreAuthorize.class)
          .should()
          .beMetaAnnotatedWith(RequestMapping.class)
          .andShould()
          .beDeclaredInClassesThat()
          .areMetaAnnotatedWith(Controller.class)
          .allowEmptyShould(true);

  /**
   * {@code @Secured} and JSR-250's annotations aren't enabled, so Spring Security would ignore them
   * silently. {@code @PostAuthorize}, {@code @PreFilter} and {@code @PostFilter} load first and
   * check after; ownership is the scoped queries' job (D-26).
   */
  @ArchTest
  static final ArchRule noClassUsesAnotherAuthorizationAnnotation =
      noClasses().should().beAnnotatedWith(anotherAuthorizationAnnotation());

  @ArchTest
  static final ArchRule noMethodUsesAnotherAuthorizationAnnotation =
      noMethods().should().beAnnotatedWith(anotherAuthorizationAnnotation());

  /**
   * A denied {@code @PreAuthorize} throws {@link AuthorizationDeniedException} through Spring MVC
   * to the security filter, which answers 401 or 403. An exception handler that catches it would
   * answer instead, a 500 or worse.
   */
  @ArchTest
  static final ArchRule exceptionHandlersLeaveDenialsToSecurity =
      methods()
          .that()
          .areAnnotatedWith(ExceptionHandler.class)
          .should(notHandle(AuthorizationDeniedException.class));

  private static ArchCondition<JavaMethod> beAnnotatedWithPreAuthorizeOrDeclaredInAClassThatIs() {
    return new ArchCondition<>(
        "be annotated with @PreAuthorize, or be declared in a class that is") {

      @Override
      public void check(JavaMethod method, ConditionEvents events) {
        boolean annotated =
            method.isAnnotatedWith(PreAuthorize.class)
                || method.getOwner().isAnnotatedWith(PreAuthorize.class);
        if (!annotated) {
          events.add(
              SimpleConditionEvent.violated(
                  method,
                  method.getFullName()
                      + " has no @PreAuthorize, on itself or on its class: say who may call the route"));
        }
      }
    };
  }

  private static DescribedPredicate<JavaAnnotation<?>> anotherAuthorizationAnnotation() {
    Set<String> names =
        Set.of(
            "org.springframework.security.access.annotation.Secured",
            "org.springframework.security.access.prepost.PostAuthorize",
            "org.springframework.security.access.prepost.PreFilter",
            "org.springframework.security.access.prepost.PostFilter");
    return DescribedPredicate.describe(
        "@Secured, @PostAuthorize, @PreFilter, @PostFilter or JSR-250's (jakarta.annotation.security)",
        annotation ->
            names.contains(annotation.getRawType().getName())
                || annotation.getRawType().getPackageName().equals("jakarta.annotation.security"));
  }

  /**
   * The handled types are the annotation's, or else the method's exception parameters, as Spring
   * MVC reads them.
   */
  private static ArchCondition<JavaMethod> notHandle(Class<? extends Throwable> exception) {
    return new ArchCondition<>("not handle " + exception.getSimpleName()) {

      @Override
      public void check(JavaMethod method, ConditionEvents events) {
        Method handler = method.reflect();
        Stream.concat(
                Stream.of(handler.getAnnotation(ExceptionHandler.class).value()),
                Stream.of(handler.getParameterTypes()))
            .filter(type -> type.isAssignableFrom(exception))
            .findFirst()
            .ifPresent(type ->
                events.add(
                    SimpleConditionEvent.violated(
                        method,
                        method.getFullName()
                            + " handles "
                            + type.getSimpleName()
                            + ", so it would answer a denied @PreAuthorize "
                            + "instead of security")));
      }
    };
  }
}
