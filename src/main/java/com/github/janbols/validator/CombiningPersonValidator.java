package com.github.janbols.validator;

import com.github.janbols.UserRepo;
import com.github.janbols.domain.Email;
import com.github.janbols.domain.Person;
import com.github.janbols.domain.PersonForm;
import com.github.janbols.domain.PersonForm.Field;
import com.github.janbols.domain.PersonName;
import com.github.janbols.validation.Validation;
import com.google.common.primitives.Ints;

import java.util.List;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;

import static com.github.janbols.domain.PersonForm.Field.*;
import static com.github.janbols.validation.Validation.combine;
import static com.google.common.collect.Iterables.concat;
import static com.google.common.collect.Lists.newArrayList;
import static org.apache.commons.lang3.StringUtils.*;


public class CombiningPersonValidator {


    private final BiFunction<String, String, String> takeFirst = (s1, s2) -> s1;

    private final BiFunction<List<String>, List<String>, List<String>> combineErrors = (e1, e2) -> newArrayList(concat(e1, e2));


    static Validation<List<String>, String> required(Field target, String value) {
        return Validation.condition(isNotBlank(value),
                newArrayList(target.value + " is required."),
                value);
    }

    static Validation<List<String>, String> maxLength(int maxLength, Field target, String value) {
        return Validation.condition(length(value) <= maxLength,
                newArrayList(target.value + " cannot be larger than " + maxLength + " characters."),
                value);
    }

    static Validation<List<String>, String> containing(String searchString, Field target, String value) {
        return Validation.condition(contains(value, searchString),
                newArrayList(target.value + " should contain " + searchString + "."),
                value);
    }

    static Validation<List<String>, Integer> between(int min, int max, Field target, int value) {
        return Validation.condition(value >= min && value <= max,
                newArrayList(target.value + " must be between " + min + " and " + max + "."),
                value);
    }

    static Validation<List<String>, Integer> isInteger(Field target, String value) {
        Integer result = Ints.tryParse(value);
        return Validation.condition(result != null,
                newArrayList(target.value + " must be an integer."),
                result);
    }

    static <B> Validation<List<String>, Optional<B>> optionalOr(String value, Function<String, Validation<List<String>, B>> otherRule) {
        return isBlank(value) ?
                Validation.success(Optional.<B>empty()) :
                otherRule.apply(value).map(Optional::ofNullable);
    }

    static Validation<List<String>, PersonName> doesNotExistInUserRepo(UserRepo userRepo, PersonName value) {
        return Validation.condition(!userRepo.findIdBy(value).isPresent(),
                newArrayList("Person with name " + value.first + " " + value.last + " already exists."),
                value);
    }


    private final UserRepo userRepo;


    public CombiningPersonValidator(UserRepo userRepo) {
        this.userRepo = userRepo;
    }

    public Validation<List<String>, Person> validate(PersonForm value) {

        Validation<List<String>, String> firstNameVal =
                required(FIRSTNAME, value.firstName)
                        .chain(s ->
                                maxLength(250, FIRSTNAME, s)
                        );

        Validation<List<String>, String> lastNameVal =
                required(LASTNAME, value.lastName)
                        .chain(s ->
                                maxLength(250, LASTNAME, s)
                        );

        Validation<List<String>, PersonName> nameVal =
                combine(
                        firstNameVal,
                        lastNameVal, combineErrors, PersonName::new
                ).chain(nm ->
                        doesNotExistInUserRepo(userRepo, nm)
                );

        Validation<List<String>, Email> emailVal =
                required(EMAIL, value.email)
                        .chain(s ->
                                combine(
                                        maxLength(100, EMAIL, s),
                                        containing("@", EMAIL, s), combineErrors, takeFirst
                                )
                        )
                        .map(Email::new);

        Validation<List<String>, Integer> ageVal =
                optionalOr(value.age, s ->
                        isInteger(AGE, s)
                                .chain(a ->
                                        between(0, 100, AGE, a)
                                )
                )
                        .map(optionalAge -> optionalAge.orElse(null));

        return combine(
                nameVal,
                emailVal,
                ageVal,
                combineErrors, Person::new
        );
    }


}
