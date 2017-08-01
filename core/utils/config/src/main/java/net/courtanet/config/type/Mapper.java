/*
 * Copyright (C) by Courtanet, All Rights Reserved.
 */
package net.courtanet.config.type;

import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Mapper is a utility class for defining static mappings between two well defined types.
 *
 * @param <I> input type
 * @param <O> output type
 */
public class Mapper<I, O> {

    /**
     * Create builder for Mapper.
     *
     * @param inType  input type class
     * @param outType input type class
     * @param <I>     input type
     * @param <O>     output type
     * @return MapperBuilder to build
     */
    public static <I, O> MapperBuilder<I, O> builder(Class<I> inType, Class<O> outType) {
        return new MapperBuilder<>(inType, outType);
    }

    private final Class<I> inType;

    private final Class<O> outType;

    private final Map<I, Function<I, O>> mappings;

    private final Function<I, O> defaultFunction;

    private final Supplier<O> nullSupplier;

    private Mapper(Class<I> inType,
            Class<O> outType,
            Map<I, Function<I, O>> mappings,
            Function<I, O> defaultFunction,
            Supplier<O> nullSupplier) {

        this.inType = inType;
        this.outType = outType;
        this.mappings = mappings;
        this.defaultFunction = defaultFunction;
        this.nullSupplier = nullSupplier;
    }

    /**
     * @return input type class
     */
    public Class<I> getInType() {
        return inType;
    }

    /**
     * @return output type class
     */
    public Class<O> getOutType() {
        return outType;
    }

    /**
     * Map input value with I type to output value with O type.
     * May return {@code null} when the mapping defines null.
     *
     * @param input input value
     * @return output value
     * @throws IllegalArgumentException if mapping is not defined.
     */
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

    /**
     * @return immutable map of mappings
     */
    public Map<I, Function<I, O>> getMappings() {
        return mappings;
    }

    @Override
    public String toString() {
        return "Mapper{" +
                "inType=" + inType +
                ", outType=" + outType +
                ", mappings=" + mappings +
                ", defaultFunction=" + defaultFunction +
                ", nullSupplier=" + nullSupplier +
                '}';
    }

    public static class MapperBuilder<I, O> {

        private Class<I> inType;
        private Class<O> outType;
        private Map<I, Function<I, O>> mappings;
        private Function<I, O> defaultFunction = null;
        private Supplier<O> nullSupplier = null;

        public MapperBuilder(Class<I> inType, Class<O> outType) {
            this.inType = inType;
            this.outType = outType;
            if (inType.isEnum()) {
                mappings = new EnumMap(inType);
            } else {
                mappings = new HashMap<>();
            }
        }

        /**
         * Build the immutable Mapper with enough information to define a mapping.
         * Configuration is complete when there is at least one static mapping or a default mapping defined.
         *
         * @return built immutable Mapper
         * @throws IllegalStateException when mapper configuration is incomplete.
         */
        public Mapper<I, O> build() {
            if (mappings.isEmpty() && defaultFunction == null) {
                throw new IllegalStateException(
                        "Mapper configuration incomplete. Specify at least one mapping or a default function.");
            }
            return new Mapper<>(inType, outType, Collections.unmodifiableMap(mappings), defaultFunction, nullSupplier);
        }

        /**
         * Defines the mapping for input values for which there is no static mapping.
         *
         * @param defaultFunction function to apply
         * @return mapper builder to build.
         */
        public MapperBuilder<I, O> withDefault(Function<I, O> defaultFunction) {
            this.defaultFunction = defaultFunction;
            return this;
        }

        /**
         * @param defaultValue default output value
         * @return mapper builder to build.
         */
        public MapperBuilder<I, O> withDefault(O defaultValue) {
            return withDefault(in -> defaultValue);
        }

        /**
         * Start defining a static mapping for the given non {@code null} value.
         *
         * @param in in value
         * @return mapping to complete with in value.
         * @throws NullPointerException when value is {@code null}.
         */
        public Mapping<I, O> map(I in) {
            Objects.requireNonNull(in, "Use mapNull method for mapping null value.");
            return new Mapping<>(in, this);
        }

        /**
         * Define mapping in value with out value.
         *
         * @param in  in value
         * @param out out value
         * @return Mapper builder to build.
         */
        public MapperBuilder<I, O> map(I in, O out) {
            map(in).to(out);
            return this;
        }

        /**
         * Start defining a static mapping for the null value
         *
         * @return mapping to complete
         */
        public NullMapping<I, O> mapNull() {
            return new NullMapping<>(this);
        }

        private void addMapping(Mapping<I, O> mapping) {
            mappings.put(mapping.in, mapping.function);
        }
    }

    /**
     * @param <I> input type
     * @param <O> output type
     */
    public static class Mapping<I, O> {

        private MapperBuilder<I, O> mapperBuilder;

        private I in;
        private Function<I, O> function;

        public Mapping(I in, MapperBuilder<I, O> mapperBuilder) {
            this.in = in;
            this.mapperBuilder = mapperBuilder;
        }

        /**
         * Complete the mapping definition with the given function.
         *
         * @param function function to apply for this mapping.
         * @return mapper builder to build.
         */
        public MapperBuilder<I, O> with(Function<I, O> function) {
            this.function = function;
            mapperBuilder.addMapping(this);
            return mapperBuilder;
        }

        /**
         * Complete the mapping definition with the given value.
         *
         * @param out value to return for this mapping.
         * @return mapper builder to build.
         */
        public MapperBuilder<I, O> to(O out) {
            return this.with((in) -> out);
        }

        /**
         * Complete the mapping definition to throw an {@link IllegalArgumentException} as result of mapping
         *
         * @param exceptionMessage exception
         * @return mapper builder to build.
         */
        public MapperBuilder<I, O> withIllegalArgumentException(Function<I, String> exceptionMessage) {
            Objects.requireNonNull(exceptionMessage);
            return with((e) -> {
                throw new IllegalArgumentException(exceptionMessage.apply(e));
            });
        }

        /**
         * Complete the mapping definition to throw an {@link IllegalArgumentException} as result of mapping
         *
         * @return mapper builder to build.
         */
        public MapperBuilder<I, O> withIllegalArgumentException() {
            return withIllegalArgumentException((e) -> "Unsupported " + e + " value.");
        }

    }

    /**
     * @param <I> input type
     * @param <O> output type
     */
    public static class NullMapping<I, O> {

        private MapperBuilder<I, O> mapperBuilder;

        public NullMapping(MapperBuilder<I, O> mapperBuilder) {
            this.mapperBuilder = mapperBuilder;
        }

        /**
         * Complete null mapping with given supplier method
         *
         * @param supplier supplier
         * @return mapper builder to build.
         */
        public MapperBuilder<I, O> with(Supplier<O> supplier) {
            mapperBuilder.nullSupplier = supplier;
            return mapperBuilder;
        }

        /**
         * Complete null mapping with given out value
         *
         * @param out out value
         * @return mapper builder to build.
         */
        public MapperBuilder<I, O> to(O out) {
            return this.with(() -> out);
        }

        /**
         * Complete the mapping definition to throw an {@link IllegalArgumentException} as result of mapping
         *
         * @return mapper builder to build.
         */
        public MapperBuilder<I, O> withIllegalArgumentException() {
            return with(() -> {
                throw new IllegalArgumentException("Unsupported null value.");
            });
        }
    }
}
