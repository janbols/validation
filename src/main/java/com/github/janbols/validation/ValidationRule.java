package com.github.janbols.validation;

import com.github.janbols.domain.PersonForm.Field;
import com.google.common.primitives.Ints;
import fj.F3;
import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;

import static com.google.common.collect.Iterables.concat;
import static com.google.common.collect.Lists.newArrayList;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

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
    Validation<List<String>, B> validate(A value, Field target);


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
    default <C> ValidationRule<A, C> chain(ValidationRule<B, C> other) {
        return (value, target) -> this.validate(value, target)
                .chain(firstResult -> other.validate(firstResult, target));
    }

    /**
     * Accumulates errors on the failing side of this or the given other {@link ValidationRule} if one or more are encountered, or applies
     * the given function if all succeeded and returns that value on the successful side.
     */
    default <C, RESULT> ValidationRule<A, RESULT> combine(ValidationRule<A, C> other, BiFunction<B, C, RESULT> composeResult) {
        return (value, target) -> Validation.combine(
                this.validate(value, target),
                other.validate(value, target),
                combineErrors, composeResult);
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
            F3<B, C, D, RESULT> composeResult) {
        return (value, target) -> Validation.combine(
                this.validate(value, target),
                second.validate(value, target),
                third.validate(value, target),
                combineErrors, composeResult);
    }

    static <A, B, C, D, RESULT> ValidationRule<A, RESULT> combine(
            ValidationRule<A, B> first,
            ValidationRule<A, C> second,
            ValidationRule<A, D> third,
            F3<B, C, D, RESULT> composeResult
    ) {
        return first.combine(second, third, composeResult);
    }

    /**
     * Fixes the target of the validation rule to the given value
     */
    default ValidationRule<A, B> withTarget(Field newTarget) {
        return (value, target) -> this.validate(value, newTarget);
    }

    /**
     * Converts a validation rule for a value of type A to a rule for a value of type C,
     * given a mapping function from C to A and a target {@link Field}
     */
    default <FROM> ValidationRule<FROM, B> from(Function<FROM, A> extractor, Field target) {
        return contraMap(extractor).withTarget(target);
    }

    static <A, B, FROM> ValidationRule<FROM, B> from(
            Function<FROM, A> extractor, Field newTarget,
            ValidationRule<A,B> rule) {
        return rule.from(extractor, newTarget);
    }

    /**
     * Checks that the input is not null
     */
    static <A> ValidationRule<A, A> notNull() {
        return (value, target) ->
                Validation.condition(value != null,
                        newArrayList(target.value + " can not be null."),
                        value);
    }

    /**
     * Checks that the input String is not blank
     */
    ValidationRule<String, String> required = (value, target) ->
            Validation.condition(isNotBlank(value),
                    newArrayList(target.value + " can not be empty."),
                    value);


    /**
     * Validates the maximum length of a String input
     */
    static ValidationRule<String, String> maxLength(int max) {
        return (value, target) ->
                Validation.condition(value.length() <= max,
                        newArrayList(target.value + " has exceed max length of " + max + " characters."),
                        value);
    }


    /**
     * Checks that the input string contains the given searchString
     */
    static ValidationRule<String, String> containing(String searchString) {
        return (value, target) ->
                Validation.condition(StringUtils.contains(value, searchString),
                        newArrayList(target.value + " should contain " + searchString + "."),
                        value);
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
        return (value, target) -> Validation.success(value);
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
            Validation.condition(Ints.tryParse(value) != null,
                    newArrayList(target.value + " must be an integer."),
                    Ints.tryParse(value));

    /**
     * Checks that the input integer is between the given min and max value
     */
    static ValidationRule<Integer, Integer> between(int min, int max) {
        return (value, target) ->
                Validation.condition(value >= min && value <= max,
                        newArrayList(target.value + " must be between " + min + " and " + max + "."),
                        value
                );
    }

    BiFunction<List<String>, List<String>, List<String>> combineErrors = (e1, e2) -> newArrayList(concat(e1, e2));

}
