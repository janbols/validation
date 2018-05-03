package com.github.janbols.validation

import arrow.Kind
import arrow.Kind2
import arrow.core.Option
import arrow.core.applicative
import arrow.core.fix
import arrow.data.*
import arrow.instances.semigroup
import arrow.typeclasses.Applicative
import arrow.typeclasses.Functor
import arrow.typeclasses.Show
import com.github.janbols.domain.PersonForm

object KPersonValidator {

    data class User(val name: Double)

    fun validate(input: PersonForm?): Unit {
        val applicative = Validated.applicative(NonEmptyList.semigroup<String>())
        val just: ValidatedNel<String, Int> = applicative
                .map(Valid(8), Valid(32), { (a, b) -> a + b }).fix()

//        Validated.applicative()
//        Validated.applicative(Nel.semigroup())  tup tupled (Validated.validNel(8), Validated.validNel(32))
    }


    fun <F> Applicative<F>.randomUserStructure(f: (Double) -> User): Kind<F, User> =
            this.just(f(Math.random()))

    val list = ListK.applicative().randomUserStructure(::User).fix()


    class UserFetcher<F>(AP: Applicative<F>) : Applicative<F> by AP {

        fun genUser() = randomUserStructure(::User)
    }

    val test = UserFetcher(Option.applicative()).genUser().fix()


    fun <F> multiplyBy2(FT: Functor<F>, fa: Kind<F, Int>): Kind<F, Int> =
            FT.run { fa.map { it * 2 } }


    fun <F> printAllValues2(S: Show<Kind<F, Int>>, fa: List<Kind<F, Int>>): Unit {
        with(S) {
            fa.forEach { println(it.show()) }
        }
    }

    fun <F> Show<Kind<F, Int>>.printAllValues(fa: List<Kind<F, Int>>): Unit {
        fa.forEach { println(it.show()) }
    }

    val requiredFun: MyValRule<String, String> = { s ->
        val applicative = Validated.applicative(String.semigroup())
        applicative.just("hallo")
    }
    val alwayValid: KValidationRule<String, String> =
            Kleisli { s -> s.valid() }


//    Validated.applicative(Nel.semigroup<String>(), Unit).run{
//        Kleisli{s -> }
//    }

}


//interface Rule<A,B> extends Kleisli<ValidOrStringError, A, B>{
//
//}

typealias MyValRule<A,B> = (A) -> Kind2<ForValidated, String, B>

typealias KValidationResult<B> = Validated<Nel<String>, B>
typealias KKK = Kind<ForValidated, String>
typealias ValidOrStringError = ValidatedPartialOf<String>
typealias KValidationRule<A, B> = Kleisli<ValidOrStringError, A, B>


