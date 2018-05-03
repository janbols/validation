package com.github.janbols;

import com.github.janbols.domain.PersonName;
import com.google.common.collect.HashBiMap;

import java.util.Map;
import java.util.Optional;

public interface UserRepo {
    Optional<Long> findIdBy(PersonName name);


    class InMemory implements UserRepo {
        private final Map<PersonName, Long> db;

        InMemory(Map<Long, PersonName> db) {
            this.db = HashBiMap.create(db).inverse();
        }

        public Optional<Long> findIdBy(PersonName name) {
            return Optional.ofNullable(db.get(name));
        }


    }
}
