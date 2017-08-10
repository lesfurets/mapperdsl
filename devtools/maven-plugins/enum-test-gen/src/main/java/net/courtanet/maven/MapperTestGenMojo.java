/*
 * Copyright (C) by Courtanet, All Rights Reserved.
 */
package net.courtanet.maven;

import static net.courtanet.maven.ETemplate.*;
import static org.apache.maven.plugins.annotations.LifecyclePhase.GENERATE_TEST_SOURCES;
import static org.apache.maven.plugins.annotations.ResolutionScope.TEST;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.net.URLClassLoader;
import java.util.*;
import java.util.stream.Collectors;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.text.WordUtils;
import org.apache.maven.plugins.annotations.Mojo;
import org.jooq.lambda.Unchecked;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Strings;

import net.courtanet.config.macro.MacroProcessor;
import net.courtanet.config.macro.PropertyParsingException;
import net.courtanet.config.type.Mapper;

@Mojo(name = "generate-mapper-test", defaultPhase = GENERATE_TEST_SOURCES, threadSafe = true,
        requiresDependencyResolution = TEST)
public final class MapperTestGenMojo extends EnumTestGenMojo {

    public static final String TEMPLATE_KEY = "Mapper";

    @Override
    protected void processTestClasses(List<String> classes, URLClassLoader classLoader) {
        for (String className : classes) {
            try {
                final Class<?> clazz = Class.forName(className, true, classLoader);
                Map<String, Mapper> mappersFromFields = getMappersFromFields(clazz);
                if (!mappersFromFields.isEmpty()) {
                    getLog().info("Mappers found in class " + clazz.getCanonicalName());
                    mappersFromFields.forEach((fieldName, mapper) -> {
                        try {
                            final StringBuilder builder = new StringBuilder();
                            buildMethods(builder, clazz, fieldName, mapper);
                            final File outputFile = new File(outputDirectory, getOutputFile(clazz, fieldName));
                            createParentFolder(outputFile);
                            final String testClass = formatTestClass(clazz, fieldName, builder.toString(), mapper);
                            IOUtils.write(testClass, new FileOutputStream(outputFile), "UTF-8");
                            getLog().info("written : " + outputFile.getAbsolutePath());
                        } catch (Throwable t) {
                            getLog().warn("ignoring test generation for class " + className + " field " + fieldName, t);
                        }
                    });
                }
            } catch (ClassNotFoundException e) {
                getLog().warn("ignoring test generation for class " + className, e);
            } catch (Throwable t) {
                getLog().warn("ignoring test generation for class " + className);
            }
        }
    }

    private void buildMethods(StringBuilder builder, Class definingClass, String fieldName, Mapper mapper)
            throws IOException, PropertyParsingException {
        Class inType = mapper.getInType();
        if (inType.isEnum()) {
            buildTestMethodsForEnum(builder, definingClass, fieldName, mapper);
        } else if (Integer.class.isAssignableFrom(inType)) {
            getLog().info("Integer mapping Skipped");
        } else if (Boolean.class.isAssignableFrom(inType)) {
            getLog().info("Boolean mapping Skipped");
        } else if (String.class.isAssignableFrom(inType)) {
            buildTestMethodsForString(builder, definingClass, fieldName, mapper);
        }
    }

    private void buildTestMethodsForString(StringBuilder builder, Class clazz, String fieldName, Mapper mapper)
            throws IOException, PropertyParsingException {
        Map mappings = mapper.getMappings();
        Set inStrings = mappings.keySet();
        ArrayList values = new ArrayList(inStrings);
        values.add(null);
        for (Object in : values) {
            try {
                builder.append(formatTestMethod(clazz, fieldName,
                        mapper.getOutType(), mapper.getInType(), mapper.map(in), in));
            } catch (Throwable throwable) {
                builder.append(formatTestMethod(clazz, fieldName,
                        mapper.getOutType(), mapper.getInType(), throwable, in));
            }
        }
    }

    private void buildTestMethodsForEnum(StringBuilder builder, Class clazz, String fieldName, Mapper mapper)
            throws IOException, PropertyParsingException {
        Class inType = mapper.getInType();
        ArrayList values = new ArrayList(Arrays.asList(inType.getEnumConstants()));
        values.add(null);
        for (Object in : values) {
            try {
                builder.append(formatTestMethod(clazz, fieldName,
                        mapper.getOutType(), mapper.getInType(), mapper.map(in), in));
            } catch (Throwable throwable) {
                builder.append(formatTestMethod(clazz, fieldName,
                        mapper.getOutType(), mapper.getInType(), throwable, in));
            }
        }
    }

    private static String formatTestMethod(Class clazz, String fieldName, Class<?> outType, Class<?> inType,
            Object out, Object in) throws PropertyParsingException,
            IOException, SecurityException {
        final Map<String, Object> params = new HashMap<>();
        params.put("prefix", fieldName);
        params.put("in.type", fqcn(inType));
        params.put("in.name", name(inType));
        params.put("in.constant", formatConstant(inType, in));
        params.put("in.value", formatValue(inType, in));
        params.put("deprecated", buildDeprecatedWarning(in, out));
        params.put("out.name", name(outType));
        params.put("out.constant", formatConstant(outType, out));
        params.put("out.value", formatValue(outType, out));
        params.put("instance", fqcn(clazz) + '.' + fieldName);
        final String template;
        if (in != null && out == null) {
            template = AssertNullMethod.template(TEMPLATE_KEY);
        } else if (in != null && out instanceof Throwable) {
            params.put("exception.type", out.getClass().getName());
            template = AssertExceptionMethod.template(TEMPLATE_KEY);
        } else if (in != null) {
            params.put("out.value", formatValue(outType, out));
            template = AssertValueMethod.template(TEMPLATE_KEY);
        } else if (out == null) {
            template = SourceNullAssertNullMethod.template(TEMPLATE_KEY);
        } else if (out instanceof Throwable) {
            params.put("exception.type", out.getClass().getName());
            template = SourceNullAssertExceptionMethod.template(TEMPLATE_KEY);
        } else {
            params.put("out.value", formatValue(outType, out));
            template = SourceNullAssertValueMethod.template(TEMPLATE_KEY);
        }
        return MacroProcessor.replaceProperties(template, params, 5);
    }

    private static String buildDeprecatedWarning(Object sourceConstant, Object targetConstant)
            throws SecurityException {
        boolean deprecated = false;
        if (isDeprecated(sourceConstant) || isDeprecated(targetConstant)) {
            deprecated = true;
        }
        return deprecated ? "@SuppressWarnings(\"deprecation\")\n    " : "";
    }

    private String formatTestClass(Class clazz, String fieldName, String methodsCode, Mapper mapper)
            throws IOException, PropertyParsingException {

        String imports = "import " + fqcn(mapper.getOutType()) + ";";

        final String classTemplate = MapperTest.template();
        final Map<String, Object> params = new HashMap<>();
        params.put("package.name", clazz.getPackage().getName());
        params.put("imported.types", imports);
        params.put("in.class.name", fqcn(clazz));
        params.put("in.fieldName", fieldName);
        params.put("target.class.name", clazz.getSimpleName() + WordUtils.capitalize(toCamelCase(fieldName)) + "MappingTest");
        params.put("methods.code", methodsCode);
        return MacroProcessor.replaceProperties(classTemplate, params, 5);
    }

    private String getOutputFile(Class<?> clazz, String fieldName) {
        return clazz.getName().replace('.', File.separatorChar) +
                WordUtils.capitalize(toCamelCase(fieldName)) + "MappingTest" + extension;
    }

    public static Map<String, Mapper> getMappersFromFields(Class clazz) {
        return Arrays.stream(clazz.getDeclaredFields())
                .filter(f -> Modifier.isStatic(f.getModifiers()))
                .filter(f -> f.getType().equals(Mapper.class))
                .collect(Collectors.toMap(Field::getName, Unchecked.function(f -> (Mapper) f.get(null))));
    }

    @VisibleForTesting
    protected static String formatValue(Class<?> type, Object value) {
        if (type.isEnum()) {
            return fqcn(type) + "." + nameOf(value);
        } else if (String.class.isAssignableFrom(type)) {
            return "\"" + value + "\"";
        } else if (type.isPrimitive()) {
            return String.valueOf(value);
        }
        return String.valueOf(value);
    }

    @VisibleForTesting
    protected static String formatConstant(Class<?> type, Object value) {
        if (type.isEnum()) {
            return nameOf(value);
        } else {
            return String.valueOf(value).replaceAll("[^a-zA-Z\\d\\w]", "_");
        }
    }

    @VisibleForTesting
    protected static String toCamelCase(final String string) {
        if (Strings.isNullOrEmpty(string)) {
            return string;
        }
        final StringBuilder nameBuilder = new StringBuilder();
        boolean capitalizeNextChar = false;
        boolean previousCharUpperCase = true;
        boolean first = true;

        for (int i = 0; i < string.length(); i++) {
            final char c = string.charAt(i);
            if (!Character.isLetterOrDigit(c)) {
                if (!first) {
                    capitalizeNextChar = true;
                }
            } else {
                char nextC = c;
                if (capitalizeNextChar) {
                    nextC = Character.toUpperCase(c);
                    capitalizeNextChar = false;
                } else if (Character.isUpperCase(c) && previousCharUpperCase) {
                    nextC = Character.toLowerCase(c);
                }
                nameBuilder.append(nextC);
                previousCharUpperCase = Character.isUpperCase(c);
                first = false;
            }
        }
        return nameBuilder.toString();
    }

}
