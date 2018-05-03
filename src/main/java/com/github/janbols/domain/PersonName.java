package com.github.janbols.domain;

import org.apache.commons.lang3.builder.ToStringBuilder;

import java.util.Objects;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Strings.emptyToNull;
import static org.apache.commons.lang3.builder.ToStringStyle.SHORT_PREFIX_STYLE;

public class PersonName {
    public final String first;
    public final String last;

    public PersonName(String first, String last) {
        this.first = checkNotNull(emptyToNull(first));
        this.last = checkNotNull(emptyToNull(last));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PersonName that = (PersonName) o;
        return Objects.equals(first, that.first) &&
                Objects.equals(last, that.last);
    }

    @Override
    public int hashCode() {

        return Objects.hash(first, last);
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this, SHORT_PREFIX_STYLE)
                .append("first", first)
                .append("last", last)
                .toString();
    }
}
