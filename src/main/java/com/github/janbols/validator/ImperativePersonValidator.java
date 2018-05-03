package com.github.janbols.validator;

import com.github.janbols.UserRepo;
import com.github.janbols.domain.PersonForm;
import com.github.janbols.domain.PersonName;
import com.github.janbols.validation.Validation;
import com.google.common.primitives.Ints;

import java.util.List;

import static com.google.common.collect.Lists.newArrayList;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

public class ImperativePersonValidator {
    private final UserRepo userRepo;

    public ImperativePersonValidator(UserRepo userRepo) {
        this.userRepo = userRepo;
    }

    public List<String> validate(PersonForm value) {
        List<String> errors = newArrayList();
        if (isBlank(value.lastName))
            errors.add("Last name is required");
        if (isNotBlank(value.lastName) && value.lastName.length() >= 250)
            errors.add("Last name cannot be more than 250 characters");

        if (isBlank(value.firstName))
            errors.add("First name is required");
        if (isNotBlank(value.firstName) && value.firstName.length() >= 250)
            errors.add("First name cannot be more than 250 characters");

        if (isNotBlank(value.lastName) && isNotBlank(value.firstName)) {
            PersonName pName = new PersonName(value.firstName, value.lastName);

            if (userRepo.findIdBy(pName).isPresent()) {
                errors.add("Person already exists");
            }
        }

        if (isBlank(value.email))
            errors.add("Email is required");
        if (isNotBlank(value.email) && !value.email.contains("@"))
            errors.add("Email should contain an @");
        if (isNotBlank(value.email) && value.email.length() >= 100)
            errors.add("Email cannot be more than 100 characters");

        if (value.age != null && Ints.tryParse(value.age) == null)
            errors.add("Age must be an integer");
        if (value.age != null && Ints.tryParse(value.age) != null &&
                (Ints.tryParse(value.age) < 0 || Ints.tryParse(value.age) > 100))
            errors.add("Age must be between 0 and 100");


        return errors;
    }


    static Validation<List<String>, String> required(PersonForm.Field target, String value) {
        return isBlank(value) ?
                Validation.fail(newArrayList(target.value + " can not be empty.")) :
                Validation.success(value);
    }
}
