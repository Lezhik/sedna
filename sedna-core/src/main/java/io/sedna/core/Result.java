package io.sedna.core;

import java.util.Objects;
import java.util.function.Function;

/**
 * Required boundary type — no raw exceptions across modules.
 */
public sealed interface Result<T, E> permits Result.Ok, Result.Err {

    static <T, E> Result<T, E> ok(T value) {
        return new Ok<>(Objects.requireNonNull(value, "value"));
    }

    static <T, E> Result<T, E> err(E error) {
        return new Err<>(Objects.requireNonNull(error, "error"));
    }

    boolean isOk();

    T value();

    E error();

    default <U> Result<U, E> map(Function<T, U> mapper) {
        if (this instanceof Ok<T, E> ok) {
            return Result.ok(mapper.apply(ok.value));
        }
        return Result.err(((Err<T, E>) this).error);
    }

    record Ok<T, E>(T value) implements Result<T, E> {
        @Override
        public boolean isOk() {
            return true;
        }

        @Override
        public E error() {
            throw new IllegalStateException("ok result has no error");
        }
    }

    record Err<T, E>(E error) implements Result<T, E> {
        @Override
        public boolean isOk() {
            return false;
        }

        @Override
        public T value() {
            throw new IllegalStateException("err result has no value");
        }
    }
}
