package com.github.janbols.domain;

import org.apache.commons.lang3.builder.ToStringBuilder;

import java.util.Objects;

import static org.apache.commons.lang3.builder.ToStringStyle.SHORT_PREFIX_STYLE;

public class PersonForm {
    public final String firstName;
    public final String lastName;
    public final String email;
    public final String age;

    public PersonForm(String firstName, String lastName, String email, String age) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.age = age;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PersonForm that = (PersonForm) o;
        return Objects.equals(firstName, that.firstName) &&
                Objects.equals(lastName, that.lastName) &&
                Objects.equals(email, that.email) &&
                Objects.equals(age, that.age);
    }

    @Override
    public int hashCode() {
        return Objects.hash(firstName, lastName, email, age);
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this, SHORT_PREFIX_STYLE)
                .append("firstName", firstName)
                .append("lastName", lastName)
                .append("email", email)
                .append("age", age)
                .toString();
    }

    public enum Field {
        FORM("form"),
        FIRSTNAME("first name"),
        LASTNAME("last name"),
        EMAIL("email"),
        AGE("age");
        public final String value;

        Field(String value) {
            this.value = value;
        }
    }
}
