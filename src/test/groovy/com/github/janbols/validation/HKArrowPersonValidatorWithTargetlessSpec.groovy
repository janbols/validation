package com.github.janbols.validation

import arrow.data.NonEmptyList
import arrow.data.Validated
import com.github.janbols.UserRepo
import com.github.janbols.domain.Email
import com.github.janbols.domain.Person
import com.github.janbols.domain.PersonForm
import com.github.janbols.domain.PersonName
import com.github.janbols.validator.hk.HKArrowPersonValidatorWithTargetless
import spock.lang.Specification
import spock.lang.Unroll

class HKArrowPersonValidatorWithTargetlessSpec extends Specification {

    def validator = new HKArrowPersonValidatorWithTargetless(new UserRepo.InMemory([
            1  : new PersonName("Donaldo", "Trumpo")
            , 2: new PersonName("Mata", "Hari")
    ]))

    def validForm = new PersonForm("Jan", "Bols", "foo@bar.com", "32")


    def "when validating a valid form, no errors are returned"() {
        when:
        def result = validator.validate(validForm)

        then:
        result.valid
        result.asType(Validated.Valid).a == new Person(new PersonName("Jan", "Bols"), new Email("foo@bar.com"), 32)
    }

    @Unroll
    def "when validating an invalid form, errors are returned"() {
        when:
        def result = validator.validate(invalidForm)

        then:
        result.invalid
        expectedErrorParts.size() == result.asType(Validated.Invalid).e.asType(NonEmptyList).size
        expectedErrorParts.each { errorPart ->
            assert result.asType(Validated.Invalid).e.asType(NonEmptyList).all.any { it.toLowerCase() ==~ errorPart }
        }

        where:
        invalidForm                                  | expectedErrorParts
        form("Jan", null, "foo@bar.com", "brol")     | [/last name .* empty.*/, /age .* integer.*/]
        form("Jan", null, "foo@bar.com", "-5")       | [/last name .* empty.*/, /age .* between .*/]
        form("Donaldo", "Trumpo", "foobar.com", "5") | [/.* already exists.*/, /.*email.*/]
    }

    static PersonForm form(String first, String last, String email, String age) {
        return new PersonForm(first, last, email, age)
    }

}



