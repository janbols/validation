package com.github.janbols.validation;


import fj.F3;
import fj.Semigroup;
import org.apache.commons.lang3.builder.ToStringBuilder;

import java.util.function.BiFunction;
import java.util.function.Function;

import static com.google.common.base.Preconditions.checkNotNull;
import static fj.Semigroup.semigroup;
import static org.apache.commons.lang3.builder.ToStringStyle.SHORT_PREFIX_STYLE;

/**
 * Isomorphic to {@link fj.data.Either} but has renamed functions and represents failure on the left and success on the right.
 * This type also has accumulating functions that accept a {@link Semigroup} for binding computation while keeping error
 * values
 *
 * Stolen from functional java and adapted for educational reasons.
 *
 * @version %build.number%
 */
public class Validation<E, T> {
    private final E e;
    private final T t;

    private Validation(E fail, T success) {
        this.e = fail;
        this.t = success;
    }


    /**
     * Returns <code>true</code> if this is a success, <code>false</code> otherwise.
     *
     * @return <code>true</code> if this is a success, <code>false</code> otherwise.
     */
    public boolean isSuccess() {
        return t != null;
    }

    /**
     * Returns <code>true</code> if this is a failure, <code>false</code> otherwise.
     *
     * @return <code>true</code> if this is a failure, <code>false</code> otherwise.
     */
    public boolean isFail() {
        return !isSuccess();
    }

    /**
     * Returns the failing value, or throws an error if there is no failing value.
     *
     * @return the failing value, or throws an error if there is no failing value.
     */
    public E fail() {
        if (isFail())
            return e;
        else
            throw new UnsupportedOperationException("Validation: fail on success value");
    }

    /**
     * Returns the success value, or throws an error if there is no success value.
     *
     * @return the success value, or throws an error if there is no success value.
     */
    public T success() {
        if (isSuccess())
            return t;
        else
            throw new UnsupportedOperationException("Validation: success on fail value");
    }


    /**
     * Returns a succeeding validation containing the given value.
     *
     * @param t The value to use in the succeeding validation.
     * @return A succeeding validation containing the given value.
     */
    public static <E, T> Validation<E, T> success(T t) {
        return new Validation<>(null, checkNotNull(t));
    }

    /**
     * Returns a failing validation containing the given value.
     *
     * @param e The value to use in the failing validation.
     * @return A failing validation containing the given value.
     */
    public static <E, T> Validation<E, T> fail(E e) {
        return new Validation<>(e, null);
    }


    /**
     * Returns a validation based on a boolean condition. If the condition is <code>true</code>, the validation succeeds,
     * otherwise it fails.
     *
     * @param c The condition to base the returned validation on.
     * @param e The failing value to use if the condition is <code>false</code>.
     * @param t The succeeding value to use if the condition is <code>true</code>.
     * @return A validation based on a boolean condition.
     */
    public static <E, T> Validation<E, T> condition(boolean c, E e, T t) {
        return c ? Validation.success(t) : Validation.fail(e);
    }


    /**
     * Maps the given function across the success side of this validation.
     *
     * @param f The function to map.
     * @return A new validation with the function mapped.
     */
    @SuppressWarnings("unchecked")
    public <A> Validation<E, A> map(Function<T, A> f) {
        return isFail() ?
                fail(fail()) :
                success(f.apply(success()));
    }


    public <A> Validation<E, A> chain(Function<T, Validation<E, A>> f) {
        return bind(f);
    }

    /**
     * Binds the given function across this validation's success value if it has one.
     *
     * @param f The function to bind across this validation.
     * @return A new validation value after binding.
     */
    @SuppressWarnings("unchecked")
    public <A> Validation<E, A> bind(Function<T, Validation<E, A>> f) {
        return isSuccess() ? f.apply(success()) : Validation.fail(fail());
    }


    private <A> Validation<E, A> accumapply(Semigroup<E> s,
                                            Validation<E, Function<T, A>> v) {
        return isFail() ?
                Validation.fail(v.isFail() ?
                        s.sum(v.fail(), fail()) :
                        fail()) :
                v.isFail() ?
                        Validation.fail(v.fail()) :
                        Validation.success(v.success().apply(success()));
    }

    public static <E, A, B, RESULT> Validation<E, RESULT> combine(
            Validation<E, A> first,
            Validation<E, B> second,
            BiFunction<E,E,E> combineErrors,
            BiFunction<A, B, RESULT> combineSuccesses
    ) {
       return first.accumulate(semigroup(combineErrors::apply), second, combineSuccesses);
    }

    public static <E, A, B, C, RESULT> Validation<E, RESULT> combine(
            Validation<E, A> first,
            Validation<E, B> second,
            Validation<E, C> third,
            BiFunction<E,E,E> combineErrors,
            F3<A, B, C, RESULT> combineSuccesses
    ) {
        return first.accumulate(semigroup(combineErrors::apply), second, third, combineSuccesses);
    }


    public <B, RESULT> Validation<E, RESULT> combine(Validation<E, B> second,
                                                     BiFunction<E,E,E> combineErrors,
                                                     BiFunction<T, B, RESULT> combineSuccesses) {
        return accumulate(semigroup(combineErrors::apply), second, combineSuccesses);
    }

    /**
     * Accumulates errors on the failing side of this or any given validation if one or more are encountered, or applies
     * the given function if all succeeded and returns that value on the successful side.
     *
     * @param s  The semigroup to accumulate errors with if one or more validations fail.
     * @param va The second validation to accumulate errors with if it failed.
     * @param f  The function to apply if all validations have succeeded.
     * @return A succeeding validation if all validations succeeded, or a failing validation with errors accumulated if
     * one or more failed.
     */
    public <A, B> Validation<E, B> accumulate(Semigroup<E> s,
                                              Validation<E, A> va,
                                              BiFunction<T, A, B> f) {
        return va.accumapply(s, map(t -> a -> f.apply(t, a)));
    }


    /**
     * Accumulates errors on the failing side of this or any given validation if one or more are encountered, or applies
     * the given function if all succeeded and returns that value on the successful side.
     *
     * @param s  The semigroup to accumulate errors with if one or more validations fail.
     * @param va The second validation to accumulate errors with if it failed.
     * @param vb The third validation to accumulate errors with if it failed.
     * @param f  The function to apply if all validations have succeeded.
     * @return A succeeding validation if all validations succeeded, or a failing validation with errors accumulated if
     * one or more failed.
     */
    public <A, B, C> Validation<E, C> accumulate(Semigroup<E> s,
                                                 Validation<E, A> va,
                                                 Validation<E, B> vb,
                                                 F3<T, A, B, C> f) {
        return vb.accumapply(s, accumulate(s, va, (t, a) -> b -> f.f(t, a, b)));
    }


    @Override
    public String toString() {
        return isSuccess() ?
                new ToStringBuilder(this, SHORT_PREFIX_STYLE).append("success", t).toString() :
                new ToStringBuilder(this, SHORT_PREFIX_STYLE).append("fail", e).toString();
    }
}
