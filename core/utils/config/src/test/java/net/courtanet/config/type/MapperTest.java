package net.courtanet.config.type;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import org.junit.Test;

public class MapperTest {

    private enum EA {
        A, B, C, D
    }

    private enum EB {
        A("A"), B("B");

        String code;

        EB(String code) {
            this.code = code;
        }

        String getCode() {
            return this.code;
        }
    }

    @Test
    public void should_throw_illegalargumentexception_when_mapping_does_not_exist() throws Exception {
        Mapper<EA, Integer> mapper = Mapper.builder(EA.class, Integer.class)
                .map(EA.A).to(80)
                .map(EA.B).to(100)
                .map(EA.C).to(126)
                .build();

        assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> mapper.map(EA.D));
        assertThat(mapper.map(EA.A)).isEqualTo(80);
    }

    @Test
    public void should_map_correctly_when_multi_mapping() throws Exception {
        Mapper<EA, Integer> mapper = Mapper.builder(EA.class, Integer.class)
                .map(EA.A).to(80)
                .map(EA.B).to(100)
                .map(EA.C, EA.D).to(126)
                .build();

        assertThat(mapper.map(EA.A)).isEqualTo(80);
        assertThat(mapper.map(EA.C)).isEqualTo(126);
        assertThat(mapper.map(EA.D)).isEqualTo(126);
    }

    @Test
    public void should_map_to_null_correctly() throws Exception {
        Mapper<EA, String> resiliationStringMapper = Mapper.builder(EA.class, String
                .class)
                .map(EA.A).to("")
                .map(EA.B, "loi")
                .map(EA.C, "2 trois ans")
                .mapNull().to("null")
                .build();

        assertThat(resiliationStringMapper.map(EA.B)).isEqualTo("loi");
        assertThat(resiliationStringMapper.map(null)).isEqualTo("null");
    }

    @Test
    public void should_map_with_function_correctly() throws Exception {
        Mapper<EB, String> resiliationStringMapper = Mapper.builder(EB.class, String.class)
                .map(EB.A).with(EB::getCode)
                .mapNull().withIllegalArgumentException()
                .withDefault(e -> null)
                .build();

        assertThat(resiliationStringMapper.map(EB.A)).isEqualTo("A");
        assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> resiliationStringMapper.map(null));
        assertThat(resiliationStringMapper.map(EB.B)).isNull();
    }

    @Test
    public void should_map_to_null_before_applying_default_mapping() throws Exception {
        Mapper<EB, String> resiliationStringMapper = Mapper.builder(EB.class, String.class)
                .mapNull().to(null)
                .withDefault(EB::getCode)
                .build();

        assertThat(resiliationStringMapper.map(EB.A)).isEqualTo("A");
        assertThat(resiliationStringMapper.map(null)).isNull();
        assertThat(resiliationStringMapper.map(EB.B)).isEqualTo("B");
    }

    @Test
    public void should_throw_illegalargumentexception_when_default_mapping_does_not_exist() throws Exception {
        Mapper<EA, EB> mapper = Mapper.builder(EA.class, EB.class)
                .mapNull().to(null)
                .withDefault(ea -> EB.valueOf(ea.name()))
                .build();
        assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> mapper.map(EA.C));
        assertThat(mapper.map(EA.A)).isEqualTo(EB.A);
        assertThat(mapper.map(EA.B)).isEqualTo(EB.B);
    }

    @Test
    public void should_map_with_default_function_when_custom_mapping_is_provided() throws Exception {
        Mapper<EA, EB> mapper = Mapper.builder(EA.class, EB.class)
                .mapNull().to(null)
                .withDefault(ea -> EB.valueOf(ea.name()))
                .map(EA.C).to(null)
                .build();
        assertThat(mapper.map(EA.A)).isEqualTo(EB.A);
        assertThat(mapper.map(EA.B)).isEqualTo(EB.B);
        assertThat(mapper.map(EA.C)).isEqualTo(null);
    }

    @Test(expected = IllegalStateException.class)
    public void should_throw_illegalstateexception_when_no_mapping_provided() throws Exception {
        Mapper.builder(EA.class, EB.class).build();
    }

    @Test(expected = IllegalStateException.class)
    public void should_throw_illegalstateexception_when_mapping_incomplete() throws Exception {
        Mapper.builder(EA.class, EB.class).mapNull().to(EB.A).build();
    }

    @Test(expected = IllegalStateException.class)
    public void should_throw_illegalstateexception_when_default_mapping_throws_nullpointerexception() throws Exception {
        Mapper.builder(EA.class, EB.class).withDefault(ea -> EB.valueOf(ea.name())).build();
    }

    @Test
    public void should_not_throw_classcastexception_at_initialize_mapper_is_already_enum_type() throws Exception {
        Mapper<Object, Integer> M = Mapper.builder(Object.class, Integer.class)
                .map(EB.A).to(1)
                .map("String").to(2)
                .build();
    }

    @Test
    public void should_initialize_mapper_with_correct_types() throws Exception {
        Mapper<EB, Integer> M = Mapper.builder(EB.class, Integer.class)
                .map(EB.A).to(1)
                .build();
    }
}