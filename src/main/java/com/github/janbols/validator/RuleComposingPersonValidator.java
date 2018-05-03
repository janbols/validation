package com.github.janbols.validator;

import com.github.janbols.UserRepo;
import com.github.janbols.domain.Email;
import com.github.janbols.domain.Person;
import com.github.janbols.domain.PersonForm;
import com.github.janbols.domain.PersonName;
import com.github.janbols.validation.Validation;
import com.github.janbols.validation.ValidationRule;

import java.util.List;
import java.util.function.BiFunction;

import static com.github.janbols.domain.PersonForm.Field.*;
import static com.github.janbols.validation.ValidationRule.*;
import static com.google.common.collect.Lists.newArrayList;

public class RuleComposingPersonValidator {

    private final BiFunction<String, String, String> takeFirst = (s1, s2) -> s1;

    static ValidationRule<PersonName, PersonName> doesNotExistInUserRepo(UserRepo userRepo) {
        return (value, target) -> Validation.condition(!userRepo.findIdBy(value).isPresent(),
                newArrayList("Person with name " + value.first + " " + value.last + " already exists."),
                value);
    }


    private UserRepo userRepo;

    public RuleComposingPersonValidator(UserRepo userRepo) {
        this.userRepo = userRepo;
    }

    public Validation<List<String>, Person> validate(PersonForm value) {

        ValidationRule<PersonForm, String> firstNameRule =
                required
                        .chain(maxLength(250))
                        .from(f -> f.firstName, FIRSTNAME);

        ValidationRule<PersonForm, String> lastNameRule =
                required
                        .chain(maxLength(250))
                        .from(f -> f.lastName, LASTNAME);

        ValidationRule<PersonForm, PersonName> nameRule =
                combine(firstNameRule, lastNameRule, PersonName::new)
                        .chain(doesNotExistInUserRepo(userRepo));

        ValidationRule<PersonForm, Email> emailRule =
                required
                        .chain(
                                combine(
                                        maxLength(100),
                                        containing("@"), takeFirst
                                )
                        )
                        .map(Email::new)
                        .from(f -> f.email, EMAIL);

        ValidationRule<PersonForm, Integer> ageRule =
                optionalOr(isInteger.chain(between(0, 100)))
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
}
