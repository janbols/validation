package com.github.janbols.validator;

import com.github.janbols.UserRepo;
import com.github.janbols.domain.Email;
import com.github.janbols.domain.Person;
import com.github.janbols.domain.PersonForm;
import com.github.janbols.domain.PersonName;
import com.google.common.primitives.Ints;
import io.vavr.Function3;
import io.vavr.collection.List;
import io.vavr.collection.Seq;
import io.vavr.control.Validation;

import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;

import static com.github.janbols.domain.PersonForm.Field.*;
import static com.github.janbols.validator.VavrPersonValidator.ValidationRule.*;
import static io.vavr.control.Validation.invalid;
import static io.vavr.control.Validation.valid;
import static org.apache.commons.lang3.StringUtils.contains;
import static org.apache.commons.lang3.StringUtils.isBlank;

public class VavrPersonValidator {

    private final BiFunction<String, String, String> takeFirst = (s1, s2) -> s1;

    static ValidationRule<PersonName, PersonName> doesNotExistInUserRepo(UserRepo userRepo) {
        return (value, target) -> userRepo.findIdBy(value).isPresent() ?
                invalid(List.of("Person with name " + value.first + " " + value.last + " already exists.")) :
                valid(value);
    }


    private UserRepo userRepo;

    public VavrPersonValidator(UserRepo userRepo) {
        this.userRepo = userRepo;
    }

    public Validation<Seq<String>, Person> validate(PersonForm value) {

        ValidationRule<PersonForm, String> firstNameRule =
                required
                        .flatMap(maxLength(250))
                        .from(f -> f.firstName, FIRSTNAME);

        ValidationRule<PersonForm, String> lastNameRule =
                required
                        .flatMap(maxLength(250))
                        .from(f -> f.lastName, LASTNAME);

        ValidationRule<PersonForm, PersonName> nameRule =
                combine(firstNameRule, lastNameRule, PersonName::new)
                        .flatMap(doesNotExistInUserRepo(userRepo));

        ValidationRule<PersonForm, Email> emailRule =
                required
                        .flatMap(
                                combine(
                                        maxLength(100),
                                        containing("@"), takeFirst
                                )
                        )
                        .map(Email::new)
                        .from(f -> f.email, EMAIL);

        ValidationRule<PersonForm, Integer> ageRule =
                optionalOr(isInteger.flatMap(between(0, 100)))
                        .map(optionalAge -> optionalAge.orElse(null))
                        .from(f -> f.age, AGE);

        ValidationRule<PersonForm, Person> personRule =
                combine(
                        nameRule,
                        emailRule,
                        ageRule,
                        Person::new
                );

        return personRule
                .validate(value, FORM);
    }


    /**
     * Defines a validation rule
     *
     * @param <A> The input type to validate
     * @param <B> The resulting validated value
     */
    @FunctionalInterface
    public interface ValidationRule<A, B> {


        /**
         * Validates the given input value of type A for the given target
         *
         * @param value
         * @param target
         * @return Validation of either a list of strings or a value of type B
         */
        Validation<Seq<String>, B> validate(A value, PersonForm.Field target);


        /**
         * Maps the successful output to another output, using the given mapping function
         */
        default <C> ValidationRule<A, C> map(Function<B, C> f) {
            return (value, target) -> this.validate(value, target).map(f);
        }

        /**
         * Maps the input of this rule to another input, using the given mapping function
         */
        default <C> ValidationRule<C, B> contraMap(Function<C, A> f) {
            return (value, target) -> this.validate(f.apply(value), target);
        }


        /**
         * Binds the given other {@link ValidationRule} across this validation's success value of this rule.
         */
        default <C> ValidationRule<A, C> flatMap(ValidationRule<B, C> other) {
            return (value, target) -> this.validate(value, target)
                    .flatMap(firstResult -> other.validate(firstResult, target));
        }

        /**
         * Accumulates errors on the failing side of this or the given other {@link ValidationRule} if one or more are encountered, or applies
         * the given function if all succeeded and returns that value on the successful side.
         */
        default <C, RESULT> ValidationRule<A, RESULT> combine(ValidationRule<A, C> other, BiFunction<B, C, RESULT> composeResult) {
            return (value, target) ->
                    this.validate(value, target)
                            .combine(other.validate(value, target))
                            .ap(composeResult::apply)
                            .mapError(reduceErrors);
        }

        static <A, B, C, RESULT> ValidationRule<A, RESULT> combine(
                ValidationRule<A, B> first,
                ValidationRule<A, C> second,
                BiFunction<B, C, RESULT> composeResult) {
            return first.combine(second, composeResult);
        }

        /**
         * Accumulates errors on the failing side of this or the second or third {@link ValidationRule}s if one or more are encountered, or applies
         * the given function if all succeeded and returns that value on the successful side.
         */
        default <C, D, RESULT> ValidationRule<A, RESULT> combine(
                ValidationRule<A, C> second,
                ValidationRule<A, D> third,
                Function3<B, C, D, RESULT> composeResult) {
            return (value, target) ->
                    this.validate(value, target)
                            .combine(second.validate(value, target))
                            .combine(third.validate(value, target))
                            .ap(composeResult)
                            .mapError(reduceErrors);
        }

        static <A, B, C, D, RESULT> ValidationRule<A, RESULT> combine(
                ValidationRule<A, B> first,
                ValidationRule<A, C> second,
                ValidationRule<A, D> third,
                Function3<B, C, D, RESULT> composeResult
        ) {
            return first.combine(second, third, composeResult);
        }

        /**
         * Fixes the target of the validation rule to the given value
         */
        default ValidationRule<A, B> withTarget(PersonForm.Field newTarget) {
            return (value, target) -> this.validate(value, newTarget);
        }

        /**
         * Converts a validation rule for a value of type A to a rule for a value of type C,
         * given a mapping function from C to A and a target {@link PersonForm.Field}
         */
        default <FROM> ValidationRule<FROM, B> from(Function<FROM, A> extractor, PersonForm.Field target) {
            return contraMap(extractor).withTarget(target);
        }

        static <A, B, FROM> ValidationRule<FROM, B> from(
                Function<FROM, A> extractor, PersonForm.Field newTarget,
                ValidationRule<A, B> rule) {
            return rule.from(extractor, newTarget);
        }

        /**
         * Checks that the input is not null
         */
        static <A> ValidationRule<A, A> notNull() {
            return (value, target) ->
                    value == null ?
                            invalid(List.of(target.value + " can not be null.")) :
                            valid(value);
        }

        /**
         * Checks that the input String is not blank
         */
        ValidationRule<String, String> required = (value, target) ->
                isBlank(value) ?
                        invalid(List.of(target.value + " can not be empty.")) :
                        valid(value);


        /**
         * Validates the maximum length of a String input
         */
        static ValidationRule<String, String> maxLength(int max) {
            return (value, target) ->
                    value.length() > max ?
                            invalid(List.of(target.value + " has exceed max length of " + max + " characters.")) :
                            valid(value);
        }


        /**
         * Checks that the input string contains the given searchString
         */
        static ValidationRule<String, String> containing(String searchString) {
            return (value, target) ->
                    !contains(value, searchString) ?
                            invalid(List.of(target.value + " should contain " + searchString + ".")) :
                            valid(value);
        }


        /**
         * Turns a blank string input into a successful empty value or continues with the given otherRule rule
         */
        static <B> ValidationRule<String, Optional<B>> optionalOr(ValidationRule<String, B> otherRule) {
            return condition(
                    v -> isBlank(v),
                    ValidationRule.<String>identityRule().map(v -> Optional.empty()),
                    otherRule.map(Optional::ofNullable)
            );
        }


        /**
         * Creates a rule that returns this input value as the successful result
         */
        static <A> ValidationRule<A, A> identityRule() {
            return (value, target) -> Validation.valid(value);
        }


        /**
         * Creates a rule that executes one of the given rules based on the outcome of a test
         */
        static <A, B> ValidationRule<A, B> condition(Predicate<A> tester,
                                                     ValidationRule<A, B> ruleWhenTrue,
                                                     ValidationRule<A, B> ruleWenFalse) {
            return (value, target) -> tester.test(value) ?
                    ruleWhenTrue.validate(value, target) :
                    ruleWenFalse.validate(value, target);
        }


        /**
         * Checks that the string input is an integer
         */
        ValidationRule<String, Integer> isInteger = (value, target) ->
                Ints.tryParse(value) == null ?
                        invalid(List.of(target.value + " must be an integer.")) :
                        valid(Ints.tryParse(value));

        /**
         * Checks that the input integer is between the given min and max value
         */
        static ValidationRule<Integer, Integer> between(int min, int max) {
            return (value, target) ->
                    value < min || value > max ?
                            invalid(List.of(target.value + " must be between " + min + " and " + max + ".")) :
                            valid(value);
        }


        Function<Seq<Seq<String>>, Seq<String>> reduceErrors = es -> es.reduce(Seq::appendAll);


    }

}
