package com.github.janbols.validator;

import com.github.janbols.UserRepo;
import com.github.janbols.domain.Email;
import com.github.janbols.domain.Person;
import com.github.janbols.domain.PersonForm;
import com.github.janbols.domain.PersonName;
import com.github.janbols.validation.Validation;

import java.util.List;
import java.util.function.BiFunction;

import static com.github.janbols.domain.PersonForm.Field.*;
import static com.github.janbols.validation.ValidationRule.*;
import static com.google.common.collect.Lists.newArrayList;


public class RuleBasedPersonValidator {

    private final BiFunction<String, String, String> takeFirst = (s1, s2) -> s1;
    
    private final UserRepo userRepo;

    Validation<List<String>, PersonName> doesNotExistInUserRepo(PersonName value) {
        return Validation.condition(!userRepo.findIdBy(value).isPresent(),
                newArrayList("Person with name " + value.first + " " + value.last + " already exists."),
                value);
    }


    public RuleBasedPersonValidator(UserRepo userRepo) {
        this.userRepo = userRepo;
    }

    public Validation<List<String>, Person> validate(PersonForm value) {
        
            Validation<List<String>, String> firstNameVal = required
                    .chain(maxLength(250))
                    .validate(value.firstName, FIRSTNAME);

            Validation<List<String>, String> lastNameVal = required
                    .chain(maxLength(250))
                    .validate(value.lastName, LASTNAME);

            Validation<List<String>, PersonName> nameVal = Validation.combine(
                    firstNameVal ,
                    lastNameVal,
                    combineErrors, PersonName::new)
                    .chain(this::doesNotExistInUserRepo);

            Validation<List<String>, Email> emailVal = required
                    .chain(
                            combine(
                                    maxLength(100),
                                    containing("@"),
                                    takeFirst)
                    )
                    .map(Email::new)
                    .validate(value.email, EMAIL);

            Validation<List<String>, Integer> ageVal = optionalOr(isInteger.chain(between(0, 100)))
                    .validate(value.age, AGE)
                    .map(oAge -> oAge.orElse(null));

            return Validation.combine(
                    nameVal,
                    emailVal,
                    ageVal,
                    combineErrors,
                    Person::new
            );
    }
}
