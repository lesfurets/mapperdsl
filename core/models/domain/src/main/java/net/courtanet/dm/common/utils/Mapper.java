/*
 * Copyright (C) by Courtanet, All Rights Reserved.
 */
package net.courtanet.dm.common.utils;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

public class Mapper<I, O> {

    public static <I, O> MapperBuilder<I, O> builder() {
        return new MapperBuilder<>();
    }

    public static <I, O> MapperBuilder<I, O> map(I in, O out) {
        return new MapperBuilder<I, O>().map(in, out);
    }

    private final Map<I, Function<I, O>> mappings;
    private final Function<I, O> defaultFunction;

    public Mapper(Map<I, Function<I, O>> mappings, Function<I, O> defaultFunction) {
        this.mappings = mappings;
        this.defaultFunction = defaultFunction;
    }

    public O map(I input) {
        if (mappings.containsKey(input)) {
            return mappings.get(input).apply(input);
        } else {
            return Optional.ofNullable(defaultFunction)
                    .orElseThrow(() -> new IllegalArgumentException(input + " value not supported"))
                    .apply(input);
        }
    }

    public static class MapperBuilder<I, O> {

        Map<I, Function<I, O>> mappings = new HashMap<>();
        Function<I, O> defaultFunction = null;

        public Mapper<I, O> build() {
            return new Mapper<>(Collections.unmodifiableMap(mappings), defaultFunction);
        }

        public MapperBuilder<I, O> withDefault(Function<I, O> defaultFunction) {
            this.defaultFunction = defaultFunction;
            return this;
        }

        public MapperBuilder<I, O> withDefault(O defaultValue) {
            return withDefault(in -> defaultValue);
        }

        public Mapping<I, O> map(I in) {
            return new Mapping<>(in, this);
        }

        public MapperBuilder<I, O> map(I in, O out) {
            map(in).to(out);
            return this;
        }

        private void addMapping(Mapping<I, O> mapping) {
            mappings.put(mapping.in, mapping.function);
        }

    }

    public static class Mapping<I, O> {

        private MapperBuilder<I, O> mapperBuilder;

        I in;
        Function<I, O> function;

        public Mapping(I in, MapperBuilder<I, O> mapperBuilder) {
            this.in = in;
            this.mapperBuilder = mapperBuilder;
        }

        public MapperBuilder<I, O> to(O out) {
            this.function = I -> out;
            mapperBuilder.addMapping(this);
            return mapperBuilder;
        }

        public MapperBuilder<I, O> with(Function<I, O> function) {
            this.function = function;
            mapperBuilder.addMapping(this);
            return mapperBuilder;
        }

        public MapperBuilder<I, O> withIllegalArgumentException(Function<I, String> exceptionMessage) {
            return with((e) -> {
                throw new IllegalArgumentException(exceptionMessage.apply(e));
            });
        }

        public MapperBuilder<I, O> withIllegalArgumentException() {
            return withIllegalArgumentException((e) -> e + " value not supported");
        }

    }
}
