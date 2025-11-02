package um.tf2025.service.criteria;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;
import org.assertj.core.api.Condition;
import org.junit.jupiter.api.Test;

class VentaCriteriaTest {

    @Test
    void newVentaCriteriaHasAllFiltersNullTest() {
        var ventaCriteria = new VentaCriteria();
        assertThat(ventaCriteria).is(criteriaFiltersAre(Objects::isNull));
    }

    @Test
    void ventaCriteriaFluentMethodsCreatesFiltersTest() {
        var ventaCriteria = new VentaCriteria();

        setAllFilters(ventaCriteria);

        assertThat(ventaCriteria).is(criteriaFiltersAre(Objects::nonNull));
    }

    @Test
    void ventaCriteriaCopyCreatesNullFilterTest() {
        var ventaCriteria = new VentaCriteria();
        var copy = ventaCriteria.copy();

        assertThat(ventaCriteria).satisfies(
            criteria ->
                assertThat(criteria).is(
                    copyFiltersAre(copy, (a, b) -> (a == null || a instanceof Boolean) ? a == b : (a != b && a.equals(b)))
                ),
            criteria -> assertThat(criteria).isEqualTo(copy),
            criteria -> assertThat(criteria).hasSameHashCodeAs(copy)
        );

        assertThat(copy).satisfies(
            criteria -> assertThat(criteria).is(criteriaFiltersAre(Objects::isNull)),
            criteria -> assertThat(criteria).isEqualTo(ventaCriteria)
        );
    }

    @Test
    void ventaCriteriaCopyDuplicatesEveryExistingFilterTest() {
        var ventaCriteria = new VentaCriteria();
        setAllFilters(ventaCriteria);

        var copy = ventaCriteria.copy();

        assertThat(ventaCriteria).satisfies(
            criteria ->
                assertThat(criteria).is(
                    copyFiltersAre(copy, (a, b) -> (a == null || a instanceof Boolean) ? a == b : (a != b && a.equals(b)))
                ),
            criteria -> assertThat(criteria).isEqualTo(copy),
            criteria -> assertThat(criteria).hasSameHashCodeAs(copy)
        );

        assertThat(copy).satisfies(
            criteria -> assertThat(criteria).is(criteriaFiltersAre(Objects::nonNull)),
            criteria -> assertThat(criteria).isEqualTo(ventaCriteria)
        );
    }

    @Test
    void toStringVerifier() {
        var ventaCriteria = new VentaCriteria();

        assertThat(ventaCriteria).hasToString("VentaCriteria{}");
    }

    private static void setAllFilters(VentaCriteria ventaCriteria) {
        ventaCriteria.id();
        ventaCriteria.cantidad();
        ventaCriteria.precioUnitario();
        ventaCriteria.total();
        ventaCriteria.fechaZoned();
        ventaCriteria.eventoId();
        ventaCriteria.distinct();
    }

    private static Condition<VentaCriteria> criteriaFiltersAre(Function<Object, Boolean> condition) {
        return new Condition<>(
            criteria ->
                condition.apply(criteria.getId()) &&
                condition.apply(criteria.getCantidad()) &&
                condition.apply(criteria.getPrecioUnitario()) &&
                condition.apply(criteria.getTotal()) &&
                condition.apply(criteria.getFechaZoned()) &&
                condition.apply(criteria.getEventoId()) &&
                condition.apply(criteria.getDistinct()),
            "every filter matches"
        );
    }

    private static Condition<VentaCriteria> copyFiltersAre(VentaCriteria copy, BiFunction<Object, Object, Boolean> condition) {
        return new Condition<>(
            criteria ->
                condition.apply(criteria.getId(), copy.getId()) &&
                condition.apply(criteria.getCantidad(), copy.getCantidad()) &&
                condition.apply(criteria.getPrecioUnitario(), copy.getPrecioUnitario()) &&
                condition.apply(criteria.getTotal(), copy.getTotal()) &&
                condition.apply(criteria.getFechaZoned(), copy.getFechaZoned()) &&
                condition.apply(criteria.getEventoId(), copy.getEventoId()) &&
                condition.apply(criteria.getDistinct(), copy.getDistinct()),
            "every filter matches"
        );
    }
}
