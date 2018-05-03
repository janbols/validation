package com.github.janbols.domain;

import org.apache.commons.lang3.builder.ToStringBuilder;

import java.util.Objects;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Strings.emptyToNull;
import static org.apache.commons.lang3.builder.ToStringStyle.SIMPLE_STYLE;

public class Email {
    public final String value;

    public Email(String value) {
        this.value = checkNotNull(emptyToNull(value));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Email email = (Email) o;
        return Objects.equals(value, email.value);
    }

    @Override
    public int hashCode() {

        return Objects.hash(value);
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this, SIMPLE_STYLE)
                .append("value", value)
                .toString();
    }
}
