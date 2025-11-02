package um.tf2025.service.criteria;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;
import org.assertj.core.api.Condition;
import org.junit.jupiter.api.Test;

class EventoCriteriaTest {

    @Test
    void newEventoCriteriaHasAllFiltersNullTest() {
        var eventoCriteria = new EventoCriteria();
        assertThat(eventoCriteria).is(criteriaFiltersAre(Objects::isNull));
    }

    @Test
    void eventoCriteriaFluentMethodsCreatesFiltersTest() {
        var eventoCriteria = new EventoCriteria();

        setAllFilters(eventoCriteria);

        assertThat(eventoCriteria).is(criteriaFiltersAre(Objects::nonNull));
    }

    @Test
    void eventoCriteriaCopyCreatesNullFilterTest() {
        var eventoCriteria = new EventoCriteria();
        var copy = eventoCriteria.copy();

        assertThat(eventoCriteria).satisfies(
            criteria ->
                assertThat(criteria).is(
                    copyFiltersAre(copy, (a, b) -> (a == null || a instanceof Boolean) ? a == b : (a != b && a.equals(b)))
                ),
            criteria -> assertThat(criteria).isEqualTo(copy),
            criteria -> assertThat(criteria).hasSameHashCodeAs(copy)
        );

        assertThat(copy).satisfies(
            criteria -> assertThat(criteria).is(criteriaFiltersAre(Objects::isNull)),
            criteria -> assertThat(criteria).isEqualTo(eventoCriteria)
        );
    }

    @Test
    void eventoCriteriaCopyDuplicatesEveryExistingFilterTest() {
        var eventoCriteria = new EventoCriteria();
        setAllFilters(eventoCriteria);

        var copy = eventoCriteria.copy();

        assertThat(eventoCriteria).satisfies(
            criteria ->
                assertThat(criteria).is(
                    copyFiltersAre(copy, (a, b) -> (a == null || a instanceof Boolean) ? a == b : (a != b && a.equals(b)))
                ),
            criteria -> assertThat(criteria).isEqualTo(copy),
            criteria -> assertThat(criteria).hasSameHashCodeAs(copy)
        );

        assertThat(copy).satisfies(
            criteria -> assertThat(criteria).is(criteriaFiltersAre(Objects::nonNull)),
            criteria -> assertThat(criteria).isEqualTo(eventoCriteria)
        );
    }

    @Test
    void toStringVerifier() {
        var eventoCriteria = new EventoCriteria();

        assertThat(eventoCriteria).hasToString("EventoCriteria{}");
    }

    private static void setAllFilters(EventoCriteria eventoCriteria) {
        eventoCriteria.id();
        eventoCriteria.nombre();
        eventoCriteria.descripcion();
        eventoCriteria.fechaZoned();
        eventoCriteria.precioBase();
        eventoCriteria.stock();
        eventoCriteria.activo();
        eventoCriteria.distinct();
    }

    private static Condition<EventoCriteria> criteriaFiltersAre(Function<Object, Boolean> condition) {
        return new Condition<>(
            criteria ->
                condition.apply(criteria.getId()) &&
                condition.apply(criteria.getNombre()) &&
                condition.apply(criteria.getDescripcion()) &&
                condition.apply(criteria.getFechaZoned()) &&
                condition.apply(criteria.getPrecioBase()) &&
                condition.apply(criteria.getStock()) &&
                condition.apply(criteria.getActivo()) &&
                condition.apply(criteria.getDistinct()),
            "every filter matches"
        );
    }

    private static Condition<EventoCriteria> copyFiltersAre(EventoCriteria copy, BiFunction<Object, Object, Boolean> condition) {
        return new Condition<>(
            criteria ->
                condition.apply(criteria.getId(), copy.getId()) &&
                condition.apply(criteria.getNombre(), copy.getNombre()) &&
                condition.apply(criteria.getDescripcion(), copy.getDescripcion()) &&
                condition.apply(criteria.getFechaZoned(), copy.getFechaZoned()) &&
                condition.apply(criteria.getPrecioBase(), copy.getPrecioBase()) &&
                condition.apply(criteria.getStock(), copy.getStock()) &&
                condition.apply(criteria.getActivo(), copy.getActivo()) &&
                condition.apply(criteria.getDistinct(), copy.getDistinct()),
            "every filter matches"
        );
    }
}
