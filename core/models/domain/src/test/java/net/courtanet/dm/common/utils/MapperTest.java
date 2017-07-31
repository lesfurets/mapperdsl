package net.courtanet.dm.common.utils;

import static net.courtanet.dm.common.type.ETauxAlcoolemieSanguin.DE_080_A_100;
import static net.courtanet.dm.common.type.ETauxAlcoolemieSanguin.DE_101_A_125;
import static net.courtanet.dm.common.type.ETauxAlcoolemieSanguin.DE_126_A_150;
import static net.courtanet.dm.common.type.ETauxAlcoolemieSanguin.DE_151_A_200;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import org.junit.Test;

import net.courtanet.config.type.Mapper;
import net.courtanet.dm.common.type.*;

public class MapperTest {

    @Test
    public void should_throw_illegalargumentexception_when_mapping_does_not_exist() throws Exception {
        Mapper<ETauxAlcoolemieSanguin, Integer> mapper = Mapper.builder(ETauxAlcoolemieSanguin.class, Integer.class)
                .map(DE_080_A_100).to(80)
                .map(DE_101_A_125).to(100)
                .map(DE_126_A_150).to(126)
                .build();

        assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> mapper.map(DE_151_A_200));
        assertThat(mapper.map(ETauxAlcoolemieSanguin.DE_080_A_100)).isEqualTo(80);
    }

    @Test
    public void should_map_to_null_correctly() throws Exception {
        Mapper<EMotifResiliation, String> resiliationStringMapper = Mapper.builder(EMotifResiliation.class, String.class)
                .map(EMotifResiliation.AUCUN).to("")
                .map(EMotifResiliation.LOI_CHATEL, "loi")
                .map(EMotifResiliation.PLUS_DE_3ANS, "2 trois ans")
                .mapNull().to("null")
                .build();

        assertThat(resiliationStringMapper.map(EMotifResiliation.LOI_CHATEL)).isEqualTo("loi");
        assertThat(resiliationStringMapper.map(null)).isEqualTo("null");
    }

    @Test
    public void should_map_with_function_correctly() throws Exception {
        Mapper<ETypeGarage, String> resiliationStringMapper = Mapper.builder(ETypeGarage.class, String.class)
                .map(ETypeGarage.BOX_FERME).with(ETypeGarage::getCode)
                .mapNull().withIllegalArgumentException()
                .withDefault(e -> null)
                .build();

        assertThat(resiliationStringMapper.map(ETypeGarage.BOX_FERME)).isEqualTo("box");
        assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> resiliationStringMapper.map(null));
        assertThat(resiliationStringMapper.map(ETypeGarage.GARAGE_FERME)).isNull();
    }

    @Test
    public void should_map_to_null_before_applying_default_mapping() throws Exception {
        Mapper<ETypeGarage, String> resiliationStringMapper = Mapper.builder(ETypeGarage.class, String.class)
                .mapNull().to(null)
                .withDefault(ETypeGarage::getCode)
                .build();

        assertThat(resiliationStringMapper.map(ETypeGarage.BOX_FERME)).isEqualTo("box");
        assertThat(resiliationStringMapper.map(null)).isNull();
        assertThat(resiliationStringMapper.map(ETypeGarage.GARAGE_FERME)).isEqualTo("garage_individuel");
    }

    enum EA {
        A, B, C
    }

    enum EB {
        A, B
    }

    @Test
    public void should_throw_illegalargumentexception_when_default_mapping_does_not_exist() throws Exception {
        Mapper<EA, EB> mapper = Mapper.builder(EA.class, EB.class)
                .withDefault(ea -> EB.valueOf(ea.name()))
                .build();
        assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> mapper.map(EA.C));
        assertThat(mapper.map(EA.A)).isEqualTo(EB.A);
        assertThat(mapper.map(EA.B)).isEqualTo(EB.B);
    }

    @Test
    public void should_map_with_default_function_when_custom_mapping_is_provided() throws Exception {
        Mapper<EA, EB> mapper = Mapper.builder(EA.class, EB.class)
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

    @Test
    public void should_not_throw_classcastexception_at_initialize_mapper_is_already_enum_type() throws Exception {
        Mapper<Object, Integer> M = Mapper.builder(Object.class, Integer.class)
                .map(ETypeGarage.BOX_FERME).to(1)
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