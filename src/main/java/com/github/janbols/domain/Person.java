package com.github.janbols.domain;

import org.apache.commons.lang3.builder.ToStringBuilder;

import java.util.Objects;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.apache.commons.lang3.builder.ToStringStyle.SHORT_PREFIX_STYLE;

public class Person {
    public final PersonName name;
    public final Email email;
    public final Integer age;

    public Person(PersonName name, Email email, Integer age) {
        this.name = checkNotNull(name);
        this.email = checkNotNull(email);
        this.age = age;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Person person = (Person) o;
        return Objects.equals(name, person.name) &&
                Objects.equals(email, person.email) &&
                Objects.equals(age, person.age);
    }

    @Override
    public int hashCode() {

        return Objects.hash(name, email, age);
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this, SHORT_PREFIX_STYLE)
                .append("name", name)
                .append("email", email)
                .append("age", age)
                .toString();
    }
}
