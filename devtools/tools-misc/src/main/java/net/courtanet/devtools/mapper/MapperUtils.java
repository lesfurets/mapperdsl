/*
 * Copyright (C) by Courtanet, All Rights Reserved.
 */
package net.courtanet.devtools.mapper;

import java.util.*;
import java.util.function.Supplier;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.jooq.lambda.tuple.Tuple;
import org.jooq.lambda.tuple.Tuple2;

import net.courtanet.config.type.Mapper;

public class MapperUtils {

    private static final Logger LOG = Logger.getLogger(MapperUtils.class);

    public static double AUTO_MAPPING_THRESHOLD = 0.4;

    public static Mapper generate(Class inType, Class outType, double threshold) {
        return generateMapper(inType, outType, threshold);
    }

    public static Mapper generate(Class inType, Class outType) {
        return generateMapper(inType, outType, AUTO_MAPPING_THRESHOLD);
    }

    static Mapper generateMapper(Class inType, Class outType, double threshold) {
        Mapper.MapperBuilder builder = Mapper.builder(inType, outType);
        if (inType.isEnum()) {
            for (Object inC : inType.getEnumConstants()) {
                Mapper.Mapping mapping = builder.map(inC);
                if (outType.isEnum()) {
                    Tuple2<Object, Double> c = Arrays.stream(outType.getEnumConstants())
                            .map(e -> Tuple.tuple(e, getDistance(((Enum) inC).name(), ((Enum) e).name())))
                            .max(Comparator.comparing(t -> t.v2)).get();
                    LOG.info(inC + " ~ " + c.v1 + " : " + c.v2);
                    if (c.v2 > threshold) {
                        mapping.to(c.v1);
                    } else {
                        mapping.to(null);
                    }
                } else {
                    mapping.to(null);
                }
            }
        } else {
            builder.withDefault(e -> null);
        }
        return builder.build();
    }

    static double getDistance(String first, String second) {
        double levenshtein = 1 - ((double) StringUtils.getLevenshteinDistance(first, second) /
                (double) Math.max(first.length(), second.length()));
        double jaro = StringUtils.getJaroWinklerDistance(first, second);
        return (levenshtein + jaro) / 2;
    }

    public static String writeMapper(Mapper mapper) {
        return writeMapper(mapper, null);
    }

    public static String writeMapper(Mapper mapper, String fieldName) {
        StringBuilder builder = new StringBuilder();
        builder.append("public static final Mapper<")
                .append(mapper.getInType().getSimpleName()).append(",")
                .append(" ")
                .append(mapper.getOutType().getSimpleName()).append(">")
                .append(" ")
                .append(fieldName == null ? "MAPPER" : fieldName)
                .append(" =")
                .append("\n\t")
                .append("Mapper.builder")
                .append("(")
                .append(mapper.getInType().getSimpleName()).append(".class").append(",")
                .append(" ")
                .append(mapper.getOutType().getSimpleName()).append(".class").append(")")
                .append("\n\t\t");
        Optional.ofNullable(mapper.getNullMapping())
                .map(Supplier::get)
                .ifPresent(o -> writeMapping(mapper, builder, null));
        mapper.getMappings().keySet().stream().forEach(k -> writeMapping(mapper, builder, k));
        builder.append(".build();");
        return builder.toString();
    }

    private static void writeMapping(Mapper mapper, StringBuilder builder, Object in) {
        if (in == null) {
            builder.append(".mapNull()");
        } else {
            builder.append(".map(");
            if (mapper.getInType().isEnum()) {
                Enum inValue = (Enum) in;
                builder.append(mapper.getInType().getSimpleName()).append(".").append(inValue.name());
            } else {
                builder.append(in.toString());
            }
            builder.append(")");
        }
        try {
            Object out = mapper.map(in);
            builder.append(".to(");
            if (mapper.getOutType().isEnum() && out != null) {
                Enum outValue = (Enum) out;
                builder.append(mapper.getOutType().getSimpleName()).append(".").append(outValue.name());
            } else {
                builder.append(String.valueOf(out));
            }
            builder.append(")");
            builder.append("\n\t\t");
        } catch (IllegalArgumentException e) {
            builder.append(".withIllegalArgumentException()");
            builder.append("\n\t\t");
        }
    }
}
