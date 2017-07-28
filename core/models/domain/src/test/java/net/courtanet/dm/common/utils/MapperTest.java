package net.courtanet.dm.common.utils;

import static net.courtanet.dm.common.type.ETauxAlcoolemieSanguin.DE_080_A_100;
import static net.courtanet.dm.common.type.ETauxAlcoolemieSanguin.DE_101_A_125;
import static net.courtanet.dm.common.type.ETauxAlcoolemieSanguin.DE_126_A_150;
import static net.courtanet.dm.common.type.ETauxAlcoolemieSanguin.DE_151_A_200;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import org.junit.Test;

import net.courtanet.dm.common.type.*;

public class MapperTest {

    @Test
    public void testETauxAlcoolemieSanguin() throws Exception {
        Mapper<ETauxAlcoolemieSanguin, Integer> mapper = Mapper.<ETauxAlcoolemieSanguin, Integer> builder()
                .map(DE_080_A_100).to(80)
                .map(DE_101_A_125).to(100)
                .map(DE_126_A_150).to(126)
                .build();

        assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> mapper.map(DE_151_A_200));
        assertThat(mapper.map(ETauxAlcoolemieSanguin.DE_080_A_100)).isEqualTo(80);
    }

    @Test
    public void test_EMotifResiliation() throws Exception {
        Mapper<EMotifResiliation, String> resiliationStringMapper = Mapper.<EMotifResiliation, String> builder()
                .map(EMotifResiliation.AUCUN).to("")
                .map(EMotifResiliation.LOI_CHATEL, "loi")
                .map(EMotifResiliation.PLUS_DE_3ANS, "2 trois ans")
                .map(null, "null")
                .build();

        assertThat(resiliationStringMapper.map(EMotifResiliation.LOI_CHATEL)).isEqualTo("loi");
        assertThat(resiliationStringMapper.map(null)).isEqualTo("null");
    }

    @Test
    public void test_ETypeGarage() throws Exception {
        Mapper<ETypeGarage, String> resiliationStringMapper = Mapper.<ETypeGarage, String> builder()
                .map(ETypeGarage.BOX_FERME).with(ETypeGarage::getCode)
                .map(null).withIllegalArgumentException()
                .withDefault(e -> null)
                .build();

        assertThat(resiliationStringMapper.map(ETypeGarage.BOX_FERME)).isEqualTo("box");
        assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> resiliationStringMapper.map(null));
        assertThat(resiliationStringMapper.map(ETypeGarage.GARAGE_FERME)).isNull();
    }

    @Test
    public void test_ETypeGarage_default_function() throws Exception {
        Mapper<ETypeGarage, String> resiliationStringMapper = Mapper.<ETypeGarage, String> builder()
                .map(null).to(null)
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
    public void test_enum_default_mapping() throws Exception {
        Mapper<EA, EB> mapper = Mapper.<EA, EB> builder()
                .withDefault(ea -> EB.valueOf(ea.name()))
                .build();
        assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> mapper.map(EA.C));
        assertThat(mapper.map(EA.A)).isEqualTo(EB.A);
        assertThat(mapper.map(EA.B)).isEqualTo(EB.B);
    }

    @Test
    public void test_enum_default_mapping_verify() throws Exception {
        Mapper<EA, EB> mapper = Mapper.<EA, EB> builder()
                .withDefault(ea -> EB.valueOf(ea.name()))
                .map(EA.C).to(null)
                .build();
        assertThat(mapper.map(EA.A)).isEqualTo(EB.A);
        assertThat(mapper.map(EA.B)).isEqualTo(EB.B);
        assertThat(mapper.map(EA.C)).isEqualTo(null);
    }
}