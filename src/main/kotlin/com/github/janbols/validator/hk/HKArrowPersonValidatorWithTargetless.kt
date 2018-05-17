package com.github.janbols.validator.hk

import arrow.Kind
import arrow.core.*
import arrow.data.*
import arrow.higherkind
import arrow.instance
import arrow.instances.KleisliApplicativeInstance
import arrow.syntax.function.andThen
import arrow.syntax.function.curried
import arrow.syntax.function.reverse
import arrow.typeclasses.Applicative
import arrow.typeclasses.Functor
import com.github.janbols.UserRepo
import com.github.janbols.domain.Email
import com.github.janbols.domain.Person
import com.github.janbols.domain.PersonForm
import com.github.janbols.domain.PersonForm.Field
import com.github.janbols.domain.PersonName
import com.github.janbols.validator.hk.ExtraRules.between
import com.github.janbols.validator.hk.ExtraRules.containing
import com.github.janbols.validator.hk.ExtraRules.isInteger
import com.github.janbols.validator.hk.ExtraRules.maxLength
import com.github.janbols.validator.hk.ExtraRules.optionalOr
import com.github.janbols.validator.hk.ExtraRules.required


class HKArrowPersonValidatorWithTargetless(private val userRepo: UserRepo) {

    fun <A> targetlessApplic(): KleisliApplicativeInstance<PartialInvalid, A> =
            Kleisli.applicative(Validated.applicative(Nel.semigroup<String>()))


    private fun doesNotExistInUserRepo(userRepo: UserRepo): ValidationRule<PersonName, PersonName> =
            ValidationRule { value, _ ->
                if (userRepo.findIdBy(value).isPresent)
                    "Person with name ${value.first} ${value.last} already exists.".invalidNel()
                else
                    value.valid()
            }

    fun validate(value: PersonForm): InvalidOr<Person> {

        val firstNameRule: TargetlessValRule<PersonForm, String> =
                required
                        .andThen(maxLength(250))
                        .local { pf: PersonForm -> pf.firstName }
                        .target(Field.FIRSTNAME)

        val lastNameRule: TargetlessValRule<PersonForm, String> =
                required
                        .andThen(maxLength(250))
                        .local { pf: PersonForm -> pf.lastName }
                        .target(Field.LASTNAME)

        val nameRule: TargetlessValRule<PersonForm, PersonName> =
                targetlessApplic<PersonForm>().run {
                    tupled(firstNameRule, lastNameRule).fix()
                            .map { PersonName(it.a, it.b) }
                            .andThen(doesNotExistInUserRepo(userRepo).targetless())
                }

        val emailRule: TargetlessValRule<PersonForm, Email> =

                required
                        .andThen(
                                ValidationRule.applicative<String>().run {
                                    tupled(
                                            maxLength(100),
                                            containing("@")
                                    ).map { it.a }
                                }
                        )
                        .map { Email(it) }
                        .local { pf: PersonForm -> pf.email }
                        .target(Field.EMAIL)


        val ageRule: TargetlessValRule<PersonForm, Int?> =
                optionalOr(
                        isInteger.andThen(between(0, 100))
                )
                        .map { it.orNull() }
                        .local { pf: PersonForm -> pf.age }
                        .target(Field.AGE)


        val personRule: TargetlessValRule<PersonForm, Person> =
                targetlessApplic<PersonForm>().run {
                    tupled(
                            nameRule,
                            emailRule,
                            ageRule
                    ).map { Person(it.a, it.b, it.c) }
                }

        return personRule
                .run(value).fix()
    }
}

class HKArrowPersonValidatorWithUntargeted(private val userRepo: UserRepo) {


    private fun doesNotExistInUserRepo(userRepo: UserRepo): ValidationRule<PersonName, PersonName> =
            ValidationRule { value, _ ->
                if (userRepo.findIdBy(value).isPresent)
                    "Person with name ${value.first} ${value.last} already exists.".invalidNel()
                else
                    value.valid()
            }

    fun validate(value: PersonForm): InvalidOr<Person> {

        val firstNameRule: UntargetedValidationRule<PersonForm, String> =
                required
                        .andThen(maxLength(250))
                        .local { pf: PersonForm -> pf.firstName }
                        .untarget(Field.FIRSTNAME)

        val lastNameRule: UntargetedValidationRule<PersonForm, String> =
                required
                        .andThen(maxLength(250))
                        .local { pf: PersonForm -> pf.lastName }
                        .untarget(Field.LASTNAME)

        val nameRule: UntargetedValidationRule<PersonForm, PersonName> =
                UntargetedValidationRule.applicative<PersonForm>()
                        .tupled(firstNameRule, lastNameRule).fix()
                        .map { PersonName(it.a, it.b) }
                        .andThen(doesNotExistInUserRepo(userRepo).untarget())

        val emailRule: UntargetedValidationRule<PersonForm, Email> =
                required
                        .andThen(
                                ValidationRule.applicative<String>()
                                        .tupled(
                                                maxLength(100),
                                                containing("@")
                                        ).fix()
                                        .map { it.a }
                        )
                        .map { Email(it) }
                        .local { pf: PersonForm -> pf.email }
                        .untarget(Field.EMAIL)


        val ageRule: UntargetedValidationRule<PersonForm, Int?> =
                optionalOr(
                        isInteger.andThen(between(0, 100))
                )
                        .map { it.orNull() }
                        .local { pf: PersonForm -> pf.age }
                        .untarget(Field.AGE)


        val personRule: UntargetedValidationRule<PersonForm, Person> =
                UntargetedValidationRule.applicative<PersonForm>()
                        .tupled(nameRule,
                                emailRule,
                                ageRule).fix()
                        .map { Person(it.a, it.b, it.c) }

        return personRule
                .run(value).fix()
    }
}



typealias InvalidOr<B> = Validated<Nel<String>, B>
typealias PartialInvalid = ValidatedPartialOf<Nel<String>>
typealias ValidationRuleFun<A, B> = (A, Field) -> InvalidOr<B>

typealias TargetlessValRule<A, B> = Kleisli<PartialInvalid, A, B>

fun <A, B, C> TargetlessValRule<A, B>.andThen(other: TargetlessValRule<B, C>) =
        TargetlessValRule { a: A ->
            run(a).fix().withEither { eitherOfB ->
                eitherOfB.flatMap { b -> other.run(b).fix().toEither() }
            }
        }


@higherkind
class UntargetedValidationRule<A, B>(val run: (A) -> InvalidOr<B>) : UntargetedValidationRuleOf<A, B> {
    private val kleisli: Kleisli<PartialInvalid, A, B> = Kleisli(run)

    fun <C> map(f: (B) -> C): UntargetedValidationRule<A, C> =
            kleisli.map(valApplic, f).asUntargeted()

    fun <C> ap(other: UntargetedValidationRule<A, (B) -> C>) =
            kleisli.ap(valApplic, other.kleisli).asUntargeted()

    fun <C> andThen(f: UntargetedValidationRule<B, C>): UntargetedValidationRule<A, C> =
            UntargetedValidationRule { a: A ->
                run(a).withEither { eitherOfB ->
                    eitherOfB.flatMap { b -> f.run(b).toEither() }
                }
            }

    fun <C> local(f: (C) -> A): UntargetedValidationRule<C, B> =
            kleisli.local(f).asUntargeted()


    companion object {
        private val valApplic = Validated.applicative(Nel.semigroup<String>())
        private fun <A> klApplic() = Kleisli.applicative<PartialInvalid, A>(valApplic)


        fun <A, B> Kleisli<PartialInvalid, A, B>.asUntargeted(): UntargetedValidationRule<A, B> =
                UntargetedValidationRule(run.andThen { it.fix() })

        fun <IN, A> just(a: A): UntargetedValidationRule<IN, A> =
                Kleisli.just<PartialInvalid, IN, A>(valApplic, a).asUntargeted()


        fun <A, B, C> tupled(
                first: UntargetedValidationRule<A, B>,
                second: UntargetedValidationRule<A, C>): UntargetedValidationRule<A, Tuple2<B, C>> =
                klApplic<A>().run {
                    tupled(first.kleisli, second.kleisli).fix().asUntargeted()
                }

        fun <A, B, C, D> tupled(
                first: UntargetedValidationRule<A, B>,
                second: UntargetedValidationRule<A, C>,
                third: UntargetedValidationRule<A, D>): UntargetedValidationRule<A, Tuple3<B, C, D>> =
                klApplic<A>().run {
                    tupled(first.kleisli, second.kleisli, third.kleisli).fix().asUntargeted()
                }
    }

}


@higherkind
class ValidationRule<A, B>(val run: ValidationRuleFun<A, B>) : ValidationRuleOf<A, B> {

    fun <C> map(f: (B) -> C): ValidationRule<A, C> =
            ValidationRule(run.andThen { it.map(f) })

    fun <C> ap(other: ValidationRule<A, (B) -> C>) =
            ValidationRule { a: A, target: Field ->
                val firstVal = run(a, target)
                val secondVal = other.run(a, target)
                firstVal.ap(Nel.semigroup(), secondVal)
            }

    fun <C> andThen(f: ValidationRule<B, C>): ValidationRule<A, C> =
            ValidationRule { a: A, target: Field ->
                run(a, target).withEither { eitherOfB ->
                    eitherOfB.flatMap { b -> f.run(b, target).toEither() }
                }
            }

    fun <C> local(f: (C) -> A): ValidationRule<C, B> =
            ValidationRule { c: C, target: Field ->
                run(f(c), target)
            }

    fun target(target: Field): TargetlessValRule<A, B> =
            Kleisli(run.reverse().curried()(target))

    fun targetless(): TargetlessValRule<A, B> =
            Kleisli(run.reverse().curried()(Field.FORM))

    fun untarget(target: Field): UntargetedValidationRule<A, B> =
            UntargetedValidationRule(run.reverse().curried()(target))

    fun untarget(): UntargetedValidationRule<A, B> =
            UntargetedValidationRule(run.reverse().curried()(Field.FORM))

    companion object {
        fun <IN, A> just(a: A): ValidationRule<IN, A> = ValidationRule { _, _ -> Valid(a) }


        fun <A, B, C> tupled(
                first: ValidationRule<A, B>,
                second: ValidationRule<A, C>): ValidationRule<A, Tuple2<B, C>> =
                applicative<A>().run {
                    tupled(first, second).fix()
                }

        fun <A, B, C, D> tupled(
                first: ValidationRule<A, B>,
                second: ValidationRule<A, C>,
                third: ValidationRule<A, D>): ValidationRule<A, Tuple3<B, C, D>> =
                applicative<A>().run {
                    tupled(first, second, third).fix()
                }
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

@instance(UntargetedValidationRule::class)
interface UntargetedValidationRuleFunctorInstance<IN> : Functor<UntargetedValidationRulePartialOf<IN>> {

    override fun <A, B> Kind<UntargetedValidationRulePartialOf<IN>, A>.map(f: (A) -> B): UntargetedValidationRule<IN, B> =
            fix().map(f)

}

@instance(UntargetedValidationRule::class)
interface UntargetedValidationRuleApplicativeInstance<IN> : UntargetedValidationRuleFunctorInstance<IN>, Applicative<UntargetedValidationRulePartialOf<IN>> {

    override fun <A, B> Kind<UntargetedValidationRulePartialOf<IN>, A>.map(f: (A) -> B): UntargetedValidationRule<IN, B> = fix().map(f)

    override fun <A, B> Kind<UntargetedValidationRulePartialOf<IN>, A>.ap(ff: Kind<UntargetedValidationRulePartialOf<IN>, (A) -> B>) =
            fix().ap(ff.fix())

    override fun <A> just(a: A) =
            UntargetedValidationRule.just<IN, A>(a)
}
