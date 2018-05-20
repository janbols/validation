# Functional validation

This the material for a blog post about validating in a FP style:
  
  
## Validating a person form 

Take the following example:  

We want to validate a web form with of person data. 
More specifically we have fields like `firstName`, `lastName`, `age` and `email`. 
```java
public class PersonForm {
    public final String firstName;
    public final String lastName;
    public final String email;
    public final String age;
}
```

The incoming data arrives in our system as a bunch of strings 
and we wrap them in a *DTO* called `PersonForm`.
 
Below are the validation rules defined by the business:  

1.  The first name should not be blank.
2.  The last name should not be blank.
3.  The email should not be blank.
4.  The email should contain a '@'
5.  The age should be an integer.
6.  The age should be between 0 and 100 *(my apologies for all those centenarians but this web form is not for you)*.
7.  The person should **not** be known in our database based on the first and last name.

Because we only foresee a certain column width in our database, there's some more rules we can define:  

8.  The first name cannot be larger than 250 characters.
9.  The last name cannot be larger than 250 characters.
10.  The email cannot be larger than 100 characters.

Oh and finally one implicit rule:  

11.  We want to find as much validation violations as possible in one form submit. 
If we violate rule 1 and 2, we want to get a notification of both violations. 
When all validation rules succeed, we want to construct a fully validated Person object like the one below:

```java
public class Person {
    public final PersonName name;
    public final Email email;
    public final Integer age;
    ...
}
```
This *Value Object* contains a `PersonName` field wrapping the first and last name. 
  
## Dependencies

Looking at the rules above, you can tell there's some dependencies between them:   
* we should only check rule 4 if rule 3 succeeds.  
* we should only check rule 6 if rule 5 succeeds.  
* we should only check rule 7 once both rule 1 and 2 succeed.  
* we should only check rule 8 if rule 1 succeeds.  
* we should only check rule 9 if rule 2 succeeds.  
* we should only check rule 10 if rule 3 succeeds.  
  
Those dependencies can be visualised like below:  
<pre>
 Rules:  firstname lastname  email age  
             1        2        3    5  
             |        |       /\    |  
             8        9      4  10  6     
              \      /       \  /   |  
               \    /          /   /  
                 7           /   /  
                  \        /   /
                    \    /   /
                      \ |  / 
                        11
</pre>

## An imperative approach

If we would take an imperative approach those dependencies would be modeled 
using `if/else` statements because we wouldn't have a built-in mechanism 
for describing dependencies between validation rules.  
Each `if/else` statement is a potential source of bugs. 
At least for me. I'm still struggling with boolean logic.

You can find an implementation below 
or you can [see it on github](https://github.com/janbols/validation/blob/master/src/main/java/com/github/janbols/validator/ImperativePersonValidator.java)  
  
```java
    public List<String> validate(PersonForm value) {
        List<String> errors = Lists.newArrayList();
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
```  
The implementation is a mine field. You can improve this code and make it prettier 
by introducing [builders](https://en.wikipedia.org/wiki/Builder_pattern) 
and validators. 
You can create `NameValidators`, `EmailValidators` and `AgeValidators` to encapsulate 
and hide the nasty looking parts. 
However, somewhere you will need to use `if/else` statements for each dependency 
to guard against NPE's and other exceptions.  

## Modeling dependencies

If we want to model dependencies between validations, we need 3 things:  

1.  We need to encapsulate the effect of a validation rule failing or succeeding.
2.  We need to be able to chain validation rules. 
When a previous rule succeeds the next rule should fire. 
When the previous fails, we shouldn't bother running the next rule.
3.  We need to be able to combine 2 or more validation rules. 
They should all succeed but when they don't, all validation errors should be 
accumulated from all failing rules.

### 1. Effectful validation

Lets take the first step. 

We want to capture the result of a validation rule into some type. 
We call it `Validation` and it can contain either an error message __OR__ a succeeding value, 
but never both. 
We don't know what the type of the error message or succeeding value will look like 
so we use generics to model them:  
```java
public class Validation<E, T> {
    private final E e;
    private final T t;
    ...
}
```
You can construct a succeeding validation using the following factory method..  
```java
public static <E, T> Validation<E, T> success(final T t) {...}
```
and a failing validation using ...  
```java
public static <E, T> Validation<E, T> fail(final E e) {...}
```  
  
So far so good. 

We can create a method checking for blank strings like below:
```java
static Validation<List<String>, String> required(Field target, String value) {
    return isBlank(value) ?
            Validation.fail(newArrayList(target.value + " can not be empty.")) :
            Validation.success(value);
}
```
To give a more useful error message, we add a `Field` argument that will give us 
the name of the field to validate against. 
`Field` is an enumeration of all possible targets to validated against.
That way we can call `required(Field.FIRST_NAME, value.firstName)` 
and reuse the same method when calling `required(Field.EMAIL, value.email)`. 
When the first call leads to a failing validation, the error message will be: *First name can not be empty.*
When the second call returns a failing validation, the error message will be: *Email can not be empty.*

The result is a `Validation` that is either failed or successful.

We can do the same for all the other validation rules 
like a method for checking the max length, for checking if an input field is an integer,
 for checking if a string contains a '@', etc... 
We'll end up with a number of static methods that all return a `Validation` 
and that can be unit tested in isolation. 
You can see an implementation [here on github](https://github.com/janbols/validation/blob/master/src/main/java/com/github/janbols/validator/CombiningPersonValidator.java).

But we still don't have a way to combine the results.
  
### 2\. Chaining validations  

Chaining validations means we need to specify that a validation only needs to be run
if a previous validation succeeds. We can do this by adding a method on our `Validation` class 
called `chain`. 
```java
public <A> Validation<E, A> chain(Function<T, Validation<E, A>> f) {...}
```
Our validation of `E` or `T` is chained by giving it 
a function from `T` to another validation of `E` or `A`. 
The function takes the success value of the current validation and returns a new validation
and is only called when the current validation is successful. 
The result is a validation of `E` or `A`. 

### 3\. Combining validations  

Not only, do we want to chain validations, we also want to combine them. 
This means we take 2 `Validation`s and return a new `Validation` that
only succeeds when both succeed. 
```java
public static <E, A, B, C> Validation<E, C> combine(
            Validation<E, A> first,
            Validation<E, B> second,
            BiFunction<E,E,E> combineErrors,
            BiFunction<A, B, C> combineSuccesses
    ) {...}
```
That's a lot of generics! Let's take it step by step:

You want to combine a validation of either `E` or `A` 
with a validation of either `E` or `B`. 
The failing side should have the same type but the success side could be a different type.
F.e. we could combine a `Validation<String, String>` with a `Validation<String, Integer>`.

To be able to safely combine these 2 validations, we need to provide two more arguments:
1. we need to tell our method how to combine 2 errors of type `E` into a new `E`. 
2. we need to tell our method how to combine the success values of both validations: `A` and `B` 
into a new type `C`.

## Validating with dependencies
Now that we have a way to model dependencies, let's take a look at how we can use them to validate our person form
using the validation rules we defined before. 

Validating the first name looks like this:
```java
Validation<List<String>, String> firstNameVal =
        required(FIRSTNAME, value.firstName)
                .chain(s ->
                        maxLength(250, FIRSTNAME, s)
                );
```
Here we chain the first rule that says the input is required 
with the rule that says it can't be larger then 250 characters.

The validation for the last name is similar:
```java
Validation<List<String>, String> lastNameVal =
        required(LASTNAME, value.lastName)
                .chain(s ->
                        maxLength(250, LASTNAME, s)
                );
```
We can combine both results into a new validation that yields a `PersonName` 
if both are successful or accumulates the error messages if not.
Finally it also checks against existing users: 
```java
Validation<List<String>, PersonName> nameVal =
        combine(
                firstNameVal,
                lastNameVal, combineErrors, PersonName::new
        ).chain(nm ->
                doesNotExistInUserRepo(userRepo, nm)
        );
```

We can do similar validations for email and age and finally combine all validations 
into 1 validation result that either returns all our validation errors 
or a validated `Person` object:
```java
    public Validation<List<String>, Person> validate(PersonForm value) {

        Validation<List<String>, String> firstNameVal = ...
        Validation<List<String>, String> lastNameVal = ...
        Validation<List<String>, PersonName> nameVal = ...
        Validation<List<String>, Email> emailVal = ...
        Validation<List<String>, Integer> ageVal = ...

        return combine(
                nameVal,
                emailVal,
                ageVal,
                combineErrors, Person::new
        );
    }
``` 

You can find a complete implementation [here on github](https://github.com/janbols/validation/blob/master/src/main/java/com/github/janbols/validator/CombiningPersonValidator.java).

## Abstracting away validation rules
Ok, what we achieved now is that the result of our validation rule 
can be composed with the result of other validation rules. 
Instead of composing the result of the rules, we can also try to compose the rules.

Lets define a validation rule as something that takes some value and returns a `Validation` 
like the following: 
```
(A) -> Validation<List<String>, B>
``` 

Because we also need some more context to construct our error message 
we also want to pass the `Field` together with the input value.
This way we know if the validation rule is targeting the first name, the last name, the email 
or the age. What we end up is the following:
```java
@FunctionalInterface
public interface ValidationRule<A, B> {
    
    Validation<List<String>, B> validate(A value, Field target);
}
```
A `ValidationRule` for `A` and `B` is some function that takes an `A` and a `Field` 
and returns a `Validation` of either a list of errors or a `B`.

As we did with `Validation`, we can also define methods like `chain` and `combine` 
to compose validation rules:
```java
@FunctionalInterface
public interface ValidationRule<A, B> {

    Validation<List<String>, B> validate(A value, Field target);


    default <C> ValidationRule<A, C> map(Function<B, C> f) {...}

    default <C> ValidationRule<A, C> chain(ValidationRule<B, C> other) {...}

    default <C, RESULT> ValidationRule<A, RESULT> combine(
            ValidationRule<A, C> other, 
            BiFunction<B, C, RESULT> composeResult) {...}
    ...
}
```
Because we fixed the error type of the validation result to be a list of strings, 
the signatures of our methods become simpler.

We can now rewrite methods that validate for blank fields, max length, integer range, etc
into `ValidationRule`s like below:
```java
ValidationRule<String, String> required = (value, target) ->
        isBlank(value) ?
            Validation.fail(newArrayList(target.value + " can not be empty.")) :
            Validation.success(value);
```

With these building blocks in place, we can simplify our validation rules a little:
```java
public Validation<List<String>, Person> validate(PersonForm value) {
    
        Validation<List<String>, String> firstNameVal = required
                .chain(maxLength(250))
                .validate(value.firstName, FIRSTNAME);

        Validation<List<String>, String> lastNameVal = ...

        Validation<List<String>, PersonName> nameVal = 
            Validation.combine(
                   firstNameVal ,
                   lastNameVal,
                   combineErrors, PersonName::new)
                   .chain(this::doesNotExistInUserRepo);

        Validation<List<String>, Email> emailVal = ...

        Validation<List<String>, Integer> ageVal = ...
        
        return Validation.combine(
                nameVal,
                emailVal,
                ageVal,
                combineErrors,
                Person::new
        );
}
```
The only remaining problem is that the each piece of our validation resolves into a `Validation` object. 
When we want to combine the validation of first name with the last name f.i., 
we're still juggling with `Validation`s instead of `ValidationRule`s.

It would be easier if each part would resolve into a `ValidationRule` instead.
But to combine 2 validation rules they need to have the same input value
and that is definitely not the case for the first and last name.

So instead of having a `ValidationRule<String, String>`,
we would need a `ValidationRule<PersonForm,String>` for both the first name and last name.

Let's add a final method:
```java
@FunctionalInterface
public interface ValidationRule<A, B> {

    Validation<List<String>, B> validate(A value, Field target);
    
    ...
    default <FROM> ValidationRule<FROM, B> from(Function<FROM, A> extractor, Field target)
}
```
The method `from` is applied to a rule from `A` to `B` 
and returns a new rule from `FROM` to `B`
with the help of a function that tells the method how to go from `FROM` to `A`.

With this extra functionality, our validation code can be simplified like the following:
```java
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
```
You can find the full code [on github](https://github.com/janbols/validation/blob/master/src/main/java/com/github/janbols/validator/RuleComposingPersonValidator.java).


We've come a long way from our initial imperative approach to a more functional style.
The latter focuses on ways to compose validation rules and allows us
to express what rules we want to _chain_ and _combine_. 
Each rule can be broken down into pieces that only do 1 thing 
that allow you to test them in isolation. 
Once you have all the necessary validation rules in place, 
you can express their dependencies by chaining and combining them.

It also shows that you don't need to use a functional language to do this sort of things
though it might certainly help. 

## Alternative validations

I didn't invent the `Validation` object. It's a recurring theme in FP 
and several implementations are available in functional libraries. 
If you want to use this type of validation, 
you should take an implementation from one of those libraries instead of copying this one.
They have more functionality and will be better tested.

Below we will look at some:

### Functional java
The implementation I used was taken and adapted 
from a library called [Functional java](http://www.functionaljava.org/). 
The difference with my code is mainly in the naming
of the methods which are far more correct from a functional point of view.
Instead of `Validation.chain` they use `Validation.bind` 
and instead of `Validation.combine`, they use `Validation.accumulate`.

You can see an implementation [here on github](https://github.com/janbols/validation/blob/master/src/main/java/com/github/janbols/validator/FunctionalJavaPersonValidator.java)
but it's not that different from the previous solution.

### VAVR
You can find another implementation of `Validation` 
in a library called [vavr](http://www.vavr.io/) ([previously named javaslang](http://blog.vavr.io/javaslang-changes-name-to-vavr/) 
before they met with the legal department of Oracle). 

The ideas are the same but the naming is a bit different. 
`Validation.chain` is called `Validation.flatmap` and `Validation.combine` uses a builder pattern 
that you must resolve back to a validation type.

You can find an example [here on github](https://github.com/janbols/validation/blob/master/src/main/java/com/github/janbols/validator/VavrPersonValidator.java).

### Arrow 
[Arrow](https://arrow-kt.io) is a functional library for Kotlin. 
Kotlin isn't java and has certain powers that java doesn't have
like [typealiases](https://kotlinlang.org/docs/reference/type-aliases.html) 
and [extension functions](https://kotlinlang.org/docs/reference/extensions.html) 
that make it easier to do functional programming.

Arrow has a data type called [`Validated`](https://arrow-kt.io/docs/datatypes/validated/) 
that we can use in our validation rules:
```kotlin
class ValidationRule<A, B>(val run: (A, Field) -> Validated<Nel<String>, B>) {
    ...
}    
```
Here we define a class called `ValidationRule` with 2 generic parameters `A` and `B`.
It takes a function as the argument in its constructor
that takes an `A` and a `Field` and returns a `Validated` of `Nel<String>` or `B`.

`Nel<String>` is an alias for `NonEmptyList<String>` which is like `List<String>`
except that you cannot create an empty one. 

Using `Nel` instead of a regular `List` as our error type makes sense
because you can't create a failed validation without any error messages.
It would be a bug to return a failed validation and not have an error message.
There's just no way we can create an empty `Nel` 
and if we would, the compiler would prevent that.

Inside our `ValidationRule` class we can define the same methods as before:

```kotlin
class ValidationRule<A, B>(val run: (A, Field) -> Validated<Nel<String>, B>) {

    fun <C> map(f: (B) -> C): ValidationRule<A, C> = ...

    fun <C> andThen(f: ValidationRule<B, C>): ValidationRule<A, C> = ...

    fun <C> from(newTarget: Field, f: (C) -> A): ValidationRule<C, B> = ...

    companion object {
        fun <A, B, C> tupled(
                first: ValidationRule<A, B>,
                second: ValidationRule<A, C>): ValidationRule<A, Tuple2<B, C>> = ...

        fun <A, B, C, D> tupled(
                first: ValidationRule<A, B>,
                second: ValidationRule<A, C>,
                third: ValidationRule<A, D>): ValidationRule<A, Tuple3<B, C, D>> = ...
    }
}
```
Armed with these methods and a number of basic validation rule building blocks, 
we can implement our validation logic as follows:
```kotlin
    fun validate(value: PersonForm): Validated<Nel<String>, Person> {

        val firstNameRule: ValidationRule<PersonForm, String> =
                required
                        .andThen(maxLength(250))
                        .from(Field.FIRSTNAME) { pf: PersonForm -> pf.firstName }

        val lastNameRule: ValidationRule<PersonForm, String> =
                required
                        .andThen(maxLength(250))
                        .from(Field.LASTNAME) { pf: PersonForm -> pf.lastName }

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
                        .from(Field.EMAIL) { pf: PersonForm -> pf.email }

        val ageRule: ValidationRule<PersonForm, Int?> =
                optionalOr(
                        isInteger.andThen(between(0, 100))
                )
                        .map { it.orNull() }
                        .from(Field.AGE) { pf: PersonForm -> pf.age }

        val personRule: ValidationRule<PersonForm, Person> =
                tupled(
                        nameRule,
                        emailRule,
                        ageRule
                ).map { Person(it.a, it.b, it.c) }

        return personRule
                .run(value, Field.FORM)
    }
```

You can find the full implementation [on github](https://github.com/janbols/validation/blob/master/src/main/kotlin/com/github/janbols/validator/ArrowPersonValidator.kt).

### The higher kinded way
Arrow defines [type classes](https://arrow-kt.io/docs/typeclasses/intro/) like 
[`Functor`](https://arrow-kt.io/docs/typeclasses/functor/), 
[`Applicative`](https://arrow-kt.io/docs/typeclasses/applicative/) and 
[`Monad`](https://arrow-kt.io/docs/typeclasses/monad/)
and [data types](https://arrow-kt.io/docs/datatypes/intro/) like 
[`Validated`](https://arrow-kt.io/docs/datatypes/validated/) and
[`NonEmptyList`](https://arrow-kt.io/docs/datatypes/nonemptylist/)
that instantiate those type classes. 
This allows you to use all functions defined for those type classes in your own data type.

I could have done the same with my `ValidationRule` and define instances 
of `Functor` (mapping over success values), `Applicative` (combining rules) and `Monad` (chaining rules). 
However, in this blog post, I don't want to focus on the implementation too much - it might end up in its own blog post.

Instead I rather wanted to focus on the power those 2 methods - `chain` and `combine` - give us
when combining rules and expressing dependencies between them.



Greetings

Jan