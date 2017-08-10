package net.courtanet.maven;

import static net.courtanet.maven.MapperTestGenMojo.formatConstant;
import static net.courtanet.maven.MapperTestGenMojo.formatValue;
import static net.courtanet.maven.MapperTestGenMojo.toCamelCase;
import static org.assertj.core.api.Assertions.*;

import org.junit.Test;

public class MapperTestGenMojoTest {

    @Test
    public void should_return_expression_when_format_value() throws Exception {
        assertThat(formatValue(ETemplate.class, ETemplate.AssertTargetCodeValue))
                .isEqualTo("net.courtanet.maven.ETemplate.AssertTargetCodeValue");
        assertThat(formatValue(String.class, "aString"))
                .isEqualTo("\"aString\"");
        assertThat(formatValue(Integer.class, 12345))
                .isEqualTo("12345");
        assertThat(formatValue(Double.class, 12345.123))
                .isEqualTo("12345.123");
        assertThat(formatValue(Boolean.class, Boolean.TRUE))
                .isEqualTo("true");
        assertThat(formatValue(Void.class, null))
                .isEqualTo("null");
    }

    @Test
    public void should_return_constant_when_format_constant() throws Exception {
        assertThat(formatConstant(ETemplate.class, ETemplate.AssertTargetCodeValue)).isEqualTo("AssertTargetCodeValue");
        assertThat(formatConstant(String.class, "aString"))
                .isEqualTo("aString");
        assertThat(formatConstant(Integer.class, 12345))
                .isEqualTo("12345");
        assertThat(formatConstant(Double.class, 12345.123))
                .isEqualTo("12345_123");
        assertThat(formatConstant(Boolean.class, Boolean.TRUE))
                .isEqualTo("true");
        assertThat(formatConstant(String.class, "somé str!ng with sp@ces & sYmb%ls"))
                .isEqualTo("som__str_ng_with_sp_ces___sYmb_ls");
    }

    @Test
    public void should_return_camel_case_name_from_field_name() throws Exception {
        assertThat(toCamelCase("lowerCamelCase")).isEqualTo("lowerCamelCase");
        assertThat(toCamelCase("UpperCamelCase")).isEqualTo("upperCamelCase");
        assertThat(toCamelCase("UPPERCASE")).isEqualTo("uppercase");
        assertThat(toCamelCase("UNDER_SCORE_2")).isEqualTo("underScore2");
        assertThat(toCamelCase("2@UNDER_SCORE_1")).isEqualTo("2UnderScore1");
        assertThat(toCamelCase("KEBAP-CASE")).isEqualTo("kebapCase");
        assertThat(toCamelCase("KEBAP-CASE-")).isEqualTo("kebapCase");
        assertThat(toCamelCase("creEpy@Case-")).isEqualTo("creEpyCase");
        assertThat(toCamelCase("")).isEmpty();
        assertThat(toCamelCase(null)).isNull();
    }

}