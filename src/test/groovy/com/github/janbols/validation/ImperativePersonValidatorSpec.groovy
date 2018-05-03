package com.github.janbols.validation

import com.github.janbols.UserRepo
import com.github.janbols.domain.PersonForm
import com.github.janbols.domain.PersonName
import com.github.janbols.validator.ImperativePersonValidator
import spock.lang.Specification
import spock.lang.Unroll

class ImperativePersonValidatorSpec extends Specification {

    def validator = new ImperativePersonValidator(new UserRepo.InMemory([
            1  : new PersonName("Donaldo", "Trumpo")
            , 2: new PersonName("Mata", "Hari")
    ]))

    def validForm = new PersonForm("Jan", "Bols", "foo@bar.com", "32")


    def "when validating a valid form, no errors are returned"() {
        when:
        def result = validator.validate(validForm)

        then:
        result.isEmpty()
    }

    @Unroll
    def "when validating an invalid form, errors are returned"() {
        when:
        def result = validator.validate(invalidForm)

        then:
        !result.isEmpty()
        expectedErrorParts.size() == result.size()
        expectedErrorParts.each { errorPart ->
            assert result.any { it.toLowerCase() ==~ errorPart }
        }

        where:
        invalidForm                                  | expectedErrorParts
        form("Jan", null, "foo@bar.com", "brol")     | [/last name .* required/, /age .* integer/]
        form("Jan", null, "foo@bar.com", "-5")       | [/last name .* required/, /age .* between .*/]
        form("Donaldo", "Trumpo", "foobar.com", "5") | [/.* already exists.*/, /.*email.*/]
    }

    static PersonForm form(String first, String last, String email, String age) {
        return new PersonForm(first, last, email, age)
    }

}



