package com.github.janbols.validator

import arrow.Kind
import arrow.core.*
import arrow.data.*
import arrow.higherkind
import arrow.instance
import arrow.syntax.function.andThen
import arrow.typeclasses.Applicative
import arrow.typeclasses.Functor
import com.github.janbols.UserRepo
import com.github.janbols.domain.Email
import com.github.janbols.domain.Person
import com.github.janbols.domain.PersonForm
import com.github.janbols.domain.PersonForm.Field
import com.github.janbols.domain.PersonName
import com.github.janbols.validator.ExtraRules.between
import com.github.janbols.validator.ExtraRules.containing
import com.github.janbols.validator.ExtraRules.isInteger
import com.github.janbols.validator.ExtraRules.maxLength
import com.github.janbols.validator.ExtraRules.optionalOr
import com.github.janbols.validator.ExtraRules.required
import com.github.janbols.validator.ValidationRule.Companion.tupled


class ArrowValidator(private val userRepo: UserRepo) {

    private fun doesNotExistInUserRepo(userRepo: UserRepo): ValidationRule<PersonName, PersonName> =
            ValidationRule { value, _ ->
                if (userRepo.findIdBy(value).isPresent)
                    "Person with name ${value.first} ${value.last} already exists.".invalidNel()
                else
                    value.valid()
            }


    fun validate(value: PersonForm): Validated<Nel<String>, Person> {

        val firstNameRule: ValidationRule<PersonForm, String> =
                required
                        .andThen(maxLength(250))
                        .reTarget(Field.FIRSTNAME) { pf: PersonForm -> pf.firstName }

        val lastNameRule: ValidationRule<PersonForm, String> =
                required
                        .andThen(maxLength(250))
                        .reTarget(Field.LASTNAME) { pf: PersonForm -> pf.lastName }

        val nameRule: ValidationRule<PersonForm, PersonName> =
                tupled(firstNameRule, lastNameRule)
                        .map { PersonName(it.a, it.b) }
                        .andThen(doesNotExistInUserRepo(userRepo))

        val emailRule: ValidationRule<PersonForm, Email> =
                required
                        .andThen(tupled(
                                maxLength(100),
                                containing("@")
                        ).map { it.a }
                        )
                        .map { Email(it) }
                        .reTarget(Field.EMAIL) { pf: PersonForm -> pf.email }

        val ageRule: ValidationRule<PersonForm, Int?> =
                optionalOr(
                        isInteger.andThen(between(0, 100))
                )
                        .map { it.orNull() }
                        .reTarget(Field.AGE) { pf: PersonForm -> pf.age }

        val personRule: ValidationRule<PersonForm, Person> =
                tupled(
                        nameRule,
                        emailRule,
                        ageRule
                ).map { Person(it.a, it.b, it.c) }

        return personRule
                .run(value, Field.FORM)
    }

}

typealias ValidationRuleFun<A, B> = (A, Field) -> Validated<Nel<String>, B>

@higherkind
class ValidationRule<A, B>(val run: ValidationRuleFun<A, B>) : ValidationRuleOf<A, B> {

    fun <C> map(f: (B) -> C): ValidationRule<A, C> =
            ValidationRule(run.andThen { it.map(f) })

    fun <C> ap(other: ValidationRule<A, (B) -> C>) =
            ValidationRule { a: A, target ->
                val firstVal = run(a, target)
                val secondVal = other.run(a, target)
                firstVal.ap(Nel.semigroup(), secondVal)
            }


    fun <C> andThen(f: ValidationRule<B, C>): ValidationRule<A, C> =
            ValidationRule { a: A, target ->
                run(a, target).withEither { eitherOfB ->
                    eitherOfB.flatMap { b -> f.run(b, target).toEither() }
                }
            }

    fun <C> local(f: (C) -> A): ValidationRule<C, B> =
            ValidationRule { c: C, target ->
                run(f(c), target)
            }

    fun target(newTarget: Field): ValidationRule<A, B> =
            ValidationRule { a: A, _ ->
                run(a, newTarget)
            }

    fun <C> reTarget(newTarget: Field, f: (C) -> A): ValidationRule<C, B> =
            local(f).target(newTarget)


    companion object {
        fun <IN, A> just(a: A): ValidationRule<IN, A> = ValidationRule { _, _ -> Valid(a) }

        fun <A, B, C> tupled(
                first: ValidationRule<A, B>,
                second: ValidationRule<A, C>): ValidationRule<A, Tuple2<B, C>> =
                second.ap(first.map { b -> { c: C -> Tuple2(b, c) } })

        fun <A, B, C, D> tupled(
                first: ValidationRule<A, B>,
                second: ValidationRule<A, C>,
                third: ValidationRule<A, D>): ValidationRule<A, Tuple3<B, C, D>> =
                third.ap(tupled(first, second).map { bc -> { d: D -> Tuple3(bc.a, bc.b, d) } })
    }
}

object ExtraRules {

    /**
     * Checks that the input is not null
     */
    fun <A> notNull(): ValidationRule<A?, A> =
            ValidationRule { value, target ->
                if (value == null) "${target.value} can not be null.".invalidNel()
                else value.valid()
            }

    /**
     * Checks that the input String is not blank
     */
    val required: ValidationRule<String?, String> =
            ValidationRule { value, target ->
                if (value.isNullOrBlank()) "${target.value} can not be empty.".invalidNel()
                else value!!.valid()
            }

    /**
     * Validates the maximum length of a String input
     */
    fun maxLength(max: Int): ValidationRule<String, String> =
            ValidationRule { value, target ->
                if (value.length > max) "${target.value} has exceed max length of $max characters.".invalidNel()
                else value.valid()
            }

    /**
     * Checks that the input string contains the given searchString
     */
    fun containing(searchString: String): ValidationRule<String, String> =
            ValidationRule { value, target ->
                if (!value.contains(searchString)) "${target.value} should contain $searchString.".invalidNel()
                else value.valid()
            }

    /**
     * Turns a blank string input into a successful empty value or continues with the given otherRule rule
     */
    fun <B> optionalOr(otherRule: ValidationRule<String, B>): ValidationRule<String, Option<B>> {
        return condition(
                { it.isBlank() },
                identityRule<String>().map { Option.empty<B>() },
                otherRule.map { Option.just(it) }
        )
    }

    /**
     * Creates a rule that returns this input value as the successful result
     */
    fun <A> identityRule(): ValidationRule<A, A> {
        return ValidationRule { value, _ -> Valid(value) }
    }

    /**
     * Creates a rule that executes one of the given rules based on the outcome of a test
     */
    fun <A, B> condition(tester: Predicate<A>,
                         ruleWhenTrue: ValidationRule<A, B>,
                         ruleWenFalse: ValidationRule<A, B>): ValidationRule<A, B> {
        return ValidationRule { value, target ->
            if (tester(value)) ruleWhenTrue.run(value, target)
            else ruleWenFalse.run(value, target)
        }
    }


    /**
     * Checks that the string input is an integer
     */
    val isInteger: ValidationRule<String, Int> =
            ValidationRule { value, target ->
                if (value.toIntOrNull() == null) "${target.value} must be an integer.".invalidNel()
                else value.toInt().valid()
            }

    /**
     * Checks that the input integer is between the given min and max value
     */
    fun between(min: Int, max: Int): ValidationRule<Int, Int> =
            ValidationRule { value, target ->
                if (value < min || value > max) "${target.value} must be between $min and $max .".invalidNel()
                else value.valid()
            }
}


@instance(ValidationRule::class)
interface ValidationRuleFunctorInstance<IN> : Functor<ValidationRulePartialOf<IN>> {

    override fun <A, B> Kind<ValidationRulePartialOf<IN>, A>.map(f: (A) -> B): ValidationRule<IN, B> =
            fix().map(f)

}

@instance(ValidationRule::class)
interface ValidationRuleApplicativeInstance<IN> : ValidationRuleFunctorInstance<IN>, Applicative<ValidationRulePartialOf<IN>> {

    override fun <A, B> Kind<ValidationRulePartialOf<IN>, A>.map(f: (A) -> B): ValidationRule<IN, B> = fix().map(f)

    override fun <A, B> Kind<ValidationRulePartialOf<IN>, A>.ap(ff: Kind<ValidationRulePartialOf<IN>, (A) -> B>) =
            fix().ap(ff.fix())

    override fun <A> just(a: A) =
            ValidationRule.just<IN, A>(a)
}

