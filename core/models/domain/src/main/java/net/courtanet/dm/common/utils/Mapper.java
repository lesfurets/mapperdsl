/*
 * Copyright (C) by Courtanet, All Rights Reserved.
 */
package net.courtanet.dm.common.utils;

import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

public class Mapper<I, O> {

    public static <I, O> MapperBuilder<I, O> builder() {
        return new MapperBuilder<>();
    }

    public static <I, O> MapperBuilder<I, O> map(I in, O out) {
        return new MapperBuilder<I, O>().map(in, out);
    }

    private final Map<I, Function<I, O>> mappings;
    private final Function<I, O> defaultFunction;
    private final Supplier<O> nullSupplier;

    public Mapper(Map<I, Function<I, O>> mappings, Function<I, O> defaultFunction, Supplier<O> nullSupplier) {
        this.mappings = mappings;
        this.defaultFunction = defaultFunction;
        this.nullSupplier = nullSupplier;
    }

    public O map(I input) {
        if (nullSupplier != null && input == null) {
            return nullSupplier.get();
        }
        if (mappings.containsKey(input)) {
            return mappings.get(input).apply(input);
        }
        return Optional.ofNullable(defaultFunction)
                .orElseThrow(() -> new IllegalArgumentException(input + " value not supported"))
                .apply(input);
    }

    public Map<I, Function<I, O>> getMappings() {
        return mappings;
    }

    public static class MapperBuilder<I, O> {

        private Map<I, Function<I, O>> mappings;
        private Function<I, O> defaultFunction = null;
        private Supplier<O> nullSupplier = null;

        public Mapper<I, O> build() {
            if (mappings == null) {
                if (defaultFunction == null) {
                    throw new IllegalStateException(
                            "Mapper configuration incomplete. Specify at least one mapping or a default function.");
                } else {
                    mappings = Collections.emptyMap();
                }
            }
            return new Mapper<>(Collections.unmodifiableMap(mappings), defaultFunction, nullSupplier);
        }

        public MapperBuilder<I, O> withDefault(Function<I, O> defaultFunction) {
            this.defaultFunction = defaultFunction;
            return this;
        }

        public MapperBuilder<I, O> withDefault(O defaultValue) {
            return withDefault(in -> defaultValue);
        }

        public Mapping<I, O> map(I in) {
            Objects.requireNonNull(in, "Use mapNull method for mapping null value.");
            return new Mapping<>(in, this);
        }

        public MapperBuilder<I, O> map(I in, O out) {
            map(in).to(out);
            return this;
        }

        public NullMapping<I, O> mapNull() {
            return new NullMapping<>(this);
        }

        private void addMapping(Mapping<I, O> mapping) {
            if (mappings == null) {
                if (mapping.in.getClass().isEnum()) {
                    mappings = new EnumMap(mapping.in.getClass());
                } else {
                    mappings = new HashMap<>();
                }
            }
            mappings.put(mapping.in, mapping.function);
        }
    }

    public static class Mapping<I, O> {

        private MapperBuilder<I, O> mapperBuilder;

        private I in;
        private Function<I, O> function;

        public Mapping(I in, MapperBuilder<I, O> mapperBuilder) {
            this.in = in;
            this.mapperBuilder = mapperBuilder;
        }

        public MapperBuilder<I, O> with(Function<I, O> function) {
            this.function = function;
            mapperBuilder.addMapping(this);
            return mapperBuilder;
        }

        public MapperBuilder<I, O> to(O out) {
            return this.with((in) -> out);
        }

        public MapperBuilder<I, O> withIllegalArgumentException(Function<I, String> exceptionMessage) {
            return with((e) -> {
                throw new IllegalArgumentException(exceptionMessage.apply(e));
            });
        }

        public MapperBuilder<I, O> withIllegalArgumentException() {
            return withIllegalArgumentException((e) -> "Unsupported " + e + " value.");
        }

    }

    public static class NullMapping<I, O> {

        private MapperBuilder<I, O> mapperBuilder;

        public NullMapping(MapperBuilder<I, O> mapperBuilder) {
            this.mapperBuilder = mapperBuilder;
        }

        public MapperBuilder<I, O> with(Supplier<O> supplier) {
            mapperBuilder.nullSupplier = supplier;
            return mapperBuilder;
        }

        public MapperBuilder<I, O> to(O out) {
            return this.with(() -> out);
        }

        public MapperBuilder<I, O> withIllegalArgumentException() {
            return with(() -> {
                throw new IllegalArgumentException("Unsupported null value.");
            });
        }
    }
}
