package com.github.janbols.validator

//import com.github.janbols.validator.Rules.required
//import com.github.janbols.validator.Rules.maxLength
//import com.github.janbols.validator.Rules.andThen
//import com.github.janbols.validator.Rules.map

//object ArrowPersonValidator {
//
//    data class User(val name: Double)
//
//    fun validate(input: PersonForm?): Unit {
//
//
//        val applicative = Validated.applicative(NonEmptyList.semigroup<String>())
//        val just: ValidatedNel<String, Int> = applicative
//                .map(Valid(8), Valid(32), { (a, b) -> a + b }).fix()
//
////        Validated.applicative()
////        Validated.applicative(Nel.semigroup())  tup tupled (Validated.validNel(8), Validated.validNel(32))
//
//
//        val firstNameRule: ValidationRule<PersonForm, String> =
//                required(Field.FIRSTNAME)
//                        .andThen(maxLength(250, Field.FIRSTNAME))
//                        .local { f -> f.firstName }
//
////        val lastNameRule: ValidationRule<PersonForm, String> =
////                required
////                        .andThen(maxLength(250))
////                        .local { f -> f.lastName }
////
////        val FF = Validated.applicative<NonEmptyList<(Field) -> String>>(Nel.semigroup())
////        val applicative1 = ValidationRule.applicative<ErrorOr, PersonForm>(FF)
////        val nameRule: ValidationRule<PersonForm, PersonName> = applicative1.tupled(firstNameRule, lastNameRule).fix().map { (fn, ln) -> PersonName(fn, ln) }
////
////        val mapper: (String) -> (String) -> PersonName = { fn -> { ln -> PersonName(fn, ln) } }
////        val nameRule2: ValidationRule<PersonForm, PersonName> = firstNameRule.ap(FF, lastNameRule.map(mapper))
////
////        // .map(), BiFunction<B, C, RESULT> { first, last -> PersonName(first, last) })
//        // .flatMap(doesNotExistInUserRepo(userRepo))
//
////        val emailRule = required
////                .flatMap<String>(
////                        combine<String, String, String, String>(
////                                maxLength(100),
////                                containing("@"), takeFirst
////                        )
////                )
////                .map(Function<String, Email> { Email(it) })
////                .from({ f -> f.email }, EMAIL)
////
////        val ageRule = optionalOr<Int>(isInteger.flatMap(between(0, 100)))
////                .map({ optionalAge -> optionalAge.orElse(null) })
////                .from({ f -> f.age }, AGE)
////
////        val personRule = combine(
////                nameRule,
////                emailRule,
////                ageRule,
////                Function3<B, C, D, RESULT> { name, email, age -> Person(name, email, age) }
////        )
////
////        return personRule
////                .validate(value, FORM)
//    }
//
//
//    fun <F> Applicative<F>.randomUserStructure(f: (Double) -> User): Kind<F, User> =
//            this.just(f(Math.random()))
//
//    val list = ListK.applicative().randomUserStructure(::User).fix()
//
//
//    class UserFetcher<F>(AP: Applicative<F>) : Applicative<F> by AP {
//
//        fun genUser() = randomUserStructure(::User)
//    }
//
//    val test = UserFetcher(Option.applicative()).genUser().fix()
//
//
//    fun <F> multiplyBy2(FT: Functor<F>, fa: Kind<F, Int>): Kind<F, Int> =
//            FT.run { fa.map { it * 2 } }
//
//
//    fun <F> printAllValues2(S: Show<Kind<F, Int>>, fa: List<Kind<F, Int>>): Unit {
//        with(S) {
//            fa.forEach { println(it.show()) }
//        }
//    }
//
//    fun <F> Show<Kind<F, Int>>.printAllValues(fa: List<Kind<F, Int>>): Unit {
//        fa.forEach { println(it.show()) }
//    }
//
//    val requiredFun: MyValRule<String, String> = { s ->
//        val applicative = Validated.applicative(String.semigroup())
//        applicative.just("hallo")
//    }
//    val ALWAY_VALID: KValidationRuleTest<String, String> =
//            Kleisli { s -> s.valid() }
//
//
////    Validated.applicative(Nel.semigroup<String>(), Unit).run{
////        Kleisli{s -> }
////    }
//
//}


//interface Rule<A,B> extends Kleisli<ValidOrStringError, A, B>{
//
//}

//typealias MyValRule<A, B> = (A) -> Kind2<ForValidated, String, B>
//
//typealias Error = String
//typealias ErrorOr = ValidatedPartialOf<Nel<Error>>
//typealias EitherErrorOr = EitherPartialOf<Nel<Error>>
//typealias KValidationRuleTest<A, B> = Kleisli<ErrorOr, A, B>
//typealias ValidationRuleFun<A, B> = (A, Field) -> Validated<Nel<String>, B>
//typealias RuleInput<A> = Tuple2<A, Field>
//typealias ValidationRule<A, B> = Kleisli<ErrorOr, RuleInput<A>, B>
////typealias ValRule<A, B> = Kleisli<ErrorOr, A, B>
//typealias EitherRule<A, B> = Kleisli<EitherErrorOr, RuleInput<A>, B>
//
//
//class ValRules<A, B>(val target:Field, val run: ValidationRule<A, B> )
//
//
//typealias VR<A,B> = Reader<Field, ValidationRule<A,B>>


//typealias ValRuleFun2<A, B> = (A) -> Kind<ErrorOr, B>
//typealias ValRuleKleisli<A, B> = Kleisli<ErrorOr, A, B>
//typealias ValRuleFun3<A, B> = (Field) -> ((A) -> Kind<ErrorOr, B>)
//typealias ValRuleFun4<A, B> = (Field) -> ValRuleKleisli<A, B>
//typealias ValRuleFunReader<A, B> = ReaderT<ErrorOr, A, B>
//typealias ValRuleFunReader2<A, B> = ReaderT<ValRuleFunReader<A, B>, Field, B>
//
//typealias ValReader<A, B> = Reader<Field, Reader<A, Either<Nel<String>, B>>>
//typealias ValReaderTInner<A, B> = EitherT<ReaderPartialOf<A>, Nel<String>, B>
//
//typealias ValReaderT<A, B> = EitherTOf<ReaderTPartialOf<ReaderPartialOf<A>, Field>, Nel<String>, B>


//object Rules {
//
//    /**
//     * Maps the successful output to another output, using the given mapping function
//     */
//    fun <A, B, C> ValidationRule<A, B>.map(f: (B) -> C): ValidationRule<A, C> {
//       return  Validated.functor<Nel<String>>().run{
//            map(f)
//        }
////        return this.map(Validated.functor(), f)
//    }
//
//    fun <A, B, C> ValRules<A, B>.andThen(other: ValRules<B, C>): ValRules<A, C> {
//        TODO()
////        val thisEither: Kleisli<EitherErrorOr, RuleInput<A>, B> = this.run.toEitherRule()
////        val otherEither: Kleisli<EitherErrorOr, RuleInput<B>, C> = other.run.toEitherRule()
////        val t = target
////        val kleisliMonad: Monad<KleisliPartialOf<EitherPartialOf<Nel<String>>, RuleInput<A>>> = kleisliMonad<A>()
////        kleisliMonad.run {
////            val f: Kleisli<EitherErrorOr, B, C> = otherEither.local<B> { b -> RuleInput(b, target) }
////            thisEither.flatMap(f)
////        }
//    }
//
//    /**
//     * Binds the given other [ValidationRule] across this validation's success value of this rule.
//     */
//    fun <A, B, C> ValidationRule<A, B>.andThen(other: ValidationRule<B, C>): ValidationRule<A, C> {
//        TODO()
////        val otherEitherRule: EitherRule<B, C> = other.toEitherRule().local { }
////        val andThen: EitherRule<A, C> = this.toEitherRule().andThen(eitherMonad, otherEitherRule)
////        return andThen.toValidationRule()
////        val thisEither: Kleisli<EitherErrorOr, RuleInput<A>, B> = this.toEitherRule()
////        val otherEither: Kleisli<EitherErrorOr, RuleInput<B>, C> = other.toEitherRule()
////        val kleisliMonad: Monad<KleisliPartialOf<EitherPartialOf<Nel<String>>, RuleInput<A>>> = kleisliMonad<A>()
////        kleisliMonad.run {
////            thisEither.flatMap(otherEither.local{ b -> Tuple2(b, this.target) })
////        }
//    }
//
//    private fun <A, B> ValidationRule<A, B>.toEitherRule(): EitherRule<A, B> {
//        return EitherRule {
//            this.run(it).fix().toEither()
//        }
//    }
//
//    private fun <A, B> EitherRule<A, B>.toValidationRule(): ValidationRule<A, B> {
//        return ValidationRule {
//            Validated.fromEither(this.run(it).fix())
//        }
//    }
//
//    val eitherMonad: EitherMonadInstance<Nel<String>> = Either.monad<Nel<String>>()
//    fun <A> kleisliMonad(): KleisliMonadInstance<EitherPartialOf<Nel<String>>, RuleInput<A>> {
//        return Kleisli.monad(eitherMonad)
//    }
//    val validatedApplicative: ValidatedApplicativeInstance<Nel<String>> = Validated.applicative(Nel.semigroup())
//    fun <A> kleisliApplicative(): KleisliApplicativeInstance<ValidatedPartialOf<Nel<String>>, RuleInput<A>> {
//        return Kleisli.applicative(validatedApplicative)
//    }
//
//    fun <A, B, C> ((A, B) -> C).toTuple(): (Tuple2<A, B>) -> C = { t -> this(t.a, t.b) }
//
//    /**
//     * Accumulates errors on the failing side of this or the given other [ValidationRuleFun] if one or more are encountered, or applies
//     * the given function if all succeeded and returns that value on the successful side.
//     */
//    fun <A, B, C, RESULT> ValidationRule<A, B>.combine(other: ValidationRule<A, C>, composeResult: (B, C) -> RESULT): ValidationRule<A, RESULT> {
//        return kleisliApplicative<A>()
//                .tupled(this, other)
//                .fix()
//                .map(composeResult.toTuple())
////        return this.ap(Validated.applicative(Nel.semigroup()), other.map { c: C -> { b: B -> composeResult(b, c) } })
//    }
//
//    fun <A, B, C, RESULT> combineS(
//            first: ValidationRule<A, B>,
//            second: ValidationRule<A, C>,
//            composeResult: (B, C) ->  RESULT ): ValidationRule<A, RESULT> {
//        return first.combine(second, composeResult)
//    }
//
//    /**
//     * Accumulates errors on the failing side of this or the second or third [ValidationRuleFun]s if one or more are encountered, or applies
//     * the given function if all succeeded and returns that value on the successful side.
//     */
////    fun <C, D, RESULT> combine(
////            second: ValidationRuleFun<A, C>,
////            third: ValidationRuleFun<A, D>,
////            composeResult: Function3<B, C, D, RESULT>): ValidationRuleFun<A, RESULT> {
////        return { value, target ->
////            this.validate(value, target)
////                    .combine<C>(second.validate(value, target))
////                    .combine<D>(third.validate(value, target))
////                    .ap(composeResult)
////                    .mapError<Seq<String>>(reduceErrors)
////        }
////    }
//
////    fun <A, B, C, D, RESULT> combine(
////            first: ValidationRuleFun<A, B>,
////            second: ValidationRuleFun<A, C>,
////            third: ValidationRuleFun<A, D>,
////            composeResult: Function3<B, C, D, RESULT>
////    ): ValidationRuleFun<A, RESULT> {
////        return first.combine(second, third, composeResult)
////    }
//
////    /**
////     * Fixes the target of the validation rule to the given value
////     */
////    fun withTarget(newTarget: Field): ValidationRuleFun<A, B> {
////        return { value, target -> this.validate(value, newTarget) }
////    }
////
////    /**
////     * Converts a validation rule for a value of type A to a rule for a value of type C,
////     * given a mapping function from C to A and a target [PersonForm.Field]
////     */
////    fun <FROM> from(extractor: Function<FROM, A>, target: Field): ValidationRuleFun<FROM, B> {
////        return contraMap(extractor).withTarget(target)
////    }
////
////    fun <A, B, FROM> from(
////            extractor: Function<FROM, A>, newTarget: Field,
////            rule: ValidationRuleFun<A, B>): ValidationRuleFun<FROM, B> {
////        return rule.from(extractor, newTarget)
////    }
//
//
//    /**
//     * Checks that the input is not null
//     */
//    fun <A> notNull(): ValidationRule<A, A> {
//        return ValidationRule { (value, target) ->
//            if (value == null) "${target.value} can not be null.".invalidNel()
//            else value.validNel()
//        }
//    }
//
//    /**
//     * Checks that the input String is not blank
//     */
//    fun required(target: Field): ValidationRule<String, String> {
//        return ValidationRule { (value, target) ->
//            if (value.isBlank()) "${target.value} can not be empty.".invalidNel()
//            else value.validNel()
//        }
//    }
//
//    /**
//     * Validates the maximum length of a String input
//     */
//    fun maxLength(max: Int, target: Field): ValidationRule<String, String> {
//        return ValidationRule { (value, target) ->
//            if (value.length > max) "${target.value} has exceed max length of $max characters.".invalidNel()
//            else value.validNel()
//        }
//    }
//
//    /**
//     * Checks that the input string contains the given searchString
//     */
//    fun containing(searchString: String, target: Field): ValidationRule<String, String> {
//        return ValidationRule { (value, target) ->
//            if (!value.contains(searchString)) "${target.value} should contain $searchString.".invalidNel()
//            else value.validNel()
//        }
//    }
//
//
//    /**
//     * Turns a blank string input into a successful empty value or continues with the given otherRule rule
//     */
//    fun <B> optionalOr(otherRule: ValidationRule<String, B>): ValidationRule<String, Option<B>> {
//        return condition(
//                { it.isBlank() },
//                identityRule<String>().map { Option.empty<B>() },
//                otherRule.map(Validated.functor()) { Option.fromNullable(it) }
//        )
//    }
//
//    /**
//     * Creates a rule that returns this input value as the successful result
//     */
//    fun <A> identityRule(): ValidationRule<A, A> {
//        return ValidationRule { (value, _) -> value.validNel() }
//    }
//
//
//    /**
//     * Creates a rule that executes one of the given rules based on the outcome of a test
//     */
//    fun <A, B> condition(tester: Predicate<A>,
//                         ruleWhenTrue: ValidationRule<A, B>,
//                         ruleWenFalse: ValidationRule<A, B>): ValidationRule<A, B> {
//        return ValidationRule {
//            val value = it.a
//            if (tester(value)) ruleWhenTrue.run(it)
//            else ruleWenFalse.run(it)
//        }
//    }
//
//
//    /**
//     * Checks that the string input is an integer
//     */
//    fun isInteger(target: Field): ValidationRule<String, Int> {
//        return ValidationRule { (value, target) ->
//            if (Ints.tryParse(value) == null) "${target.value} must be an integer.".invalidNel()
//            else Ints.tryParse(value)!!.validNel()
//        }
//    }
//
//    /**
//     * Checks that the input integer is between the given min and max value
//     */
//    fun between(min: Int, max: Int): ValidationRule<Int, Int> {
//        return ValidationRule { (value, target) ->
//            if (value < min || value > max) "${target.value} must be between $min and $max .".invalidNel()
//            else value.validNel()
//        }
//    }
//
//
//}
