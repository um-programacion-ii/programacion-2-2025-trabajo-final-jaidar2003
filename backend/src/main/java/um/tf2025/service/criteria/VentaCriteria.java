package um.tf2025.service.criteria;

import java.io.Serializable;
import java.util.Objects;
import java.util.Optional;
import org.springdoc.core.annotations.ParameterObject;
import tech.jhipster.service.Criteria;
import tech.jhipster.service.filter.*;

/**
 * Criteria class for the {@link um.tf2025.domain.Venta} entity. This class is used
 * in {@link um.tf2025.web.rest.VentaResource} to receive all the possible filtering options from
 * the Http GET request parameters.
 * For example the following could be a valid request:
 * {@code /ventas?id.greaterThan=5&attr1.contains=something&attr2.specified=false}
 * As Spring is unable to properly convert the types, unless specific {@link Filter} class are used, we need to use
 * fix type specific filters.
 */
@ParameterObject
@SuppressWarnings("common-java:DuplicatedBlocks")
public class VentaCriteria implements Serializable, Criteria {

    private static final long serialVersionUID = 1L;

    private LongFilter id;

    private IntegerFilter cantidad;

    private BigDecimalFilter precioUnitario;

    private BigDecimalFilter total;

    private ZonedDateTimeFilter fechaZoned;

    private LongFilter eventoId;

    private Boolean distinct;

    public VentaCriteria() {}

    public VentaCriteria(VentaCriteria other) {
        this.id = other.optionalId().map(LongFilter::copy).orElse(null);
        this.cantidad = other.optionalCantidad().map(IntegerFilter::copy).orElse(null);
        this.precioUnitario = other.optionalPrecioUnitario().map(BigDecimalFilter::copy).orElse(null);
        this.total = other.optionalTotal().map(BigDecimalFilter::copy).orElse(null);
        this.fechaZoned = other.optionalFechaZoned().map(ZonedDateTimeFilter::copy).orElse(null);
        this.eventoId = other.optionalEventoId().map(LongFilter::copy).orElse(null);
        this.distinct = other.distinct;
    }

    @Override
    public VentaCriteria copy() {
        return new VentaCriteria(this);
    }

    public LongFilter getId() {
        return id;
    }

    public Optional<LongFilter> optionalId() {
        return Optional.ofNullable(id);
    }

    public LongFilter id() {
        if (id == null) {
            setId(new LongFilter());
        }
        return id;
    }

    public void setId(LongFilter id) {
        this.id = id;
    }

    public IntegerFilter getCantidad() {
        return cantidad;
    }

    public Optional<IntegerFilter> optionalCantidad() {
        return Optional.ofNullable(cantidad);
    }

    public IntegerFilter cantidad() {
        if (cantidad == null) {
            setCantidad(new IntegerFilter());
        }
        return cantidad;
    }

    public void setCantidad(IntegerFilter cantidad) {
        this.cantidad = cantidad;
    }

    public BigDecimalFilter getPrecioUnitario() {
        return precioUnitario;
    }

    public Optional<BigDecimalFilter> optionalPrecioUnitario() {
        return Optional.ofNullable(precioUnitario);
    }

    public BigDecimalFilter precioUnitario() {
        if (precioUnitario == null) {
            setPrecioUnitario(new BigDecimalFilter());
        }
        return precioUnitario;
    }

    public void setPrecioUnitario(BigDecimalFilter precioUnitario) {
        this.precioUnitario = precioUnitario;
    }

    public BigDecimalFilter getTotal() {
        return total;
    }

    public Optional<BigDecimalFilter> optionalTotal() {
        return Optional.ofNullable(total);
    }

    public BigDecimalFilter total() {
        if (total == null) {
            setTotal(new BigDecimalFilter());
        }
        return total;
    }

    public void setTotal(BigDecimalFilter total) {
        this.total = total;
    }

    public ZonedDateTimeFilter getFechaZoned() {
        return fechaZoned;
    }

    public Optional<ZonedDateTimeFilter> optionalFechaZoned() {
        return Optional.ofNullable(fechaZoned);
    }

    public ZonedDateTimeFilter fechaZoned() {
        if (fechaZoned == null) {
            setFechaZoned(new ZonedDateTimeFilter());
        }
        return fechaZoned;
    }

    public void setFechaZoned(ZonedDateTimeFilter fechaZoned) {
        this.fechaZoned = fechaZoned;
    }

    public LongFilter getEventoId() {
        return eventoId;
    }

    public Optional<LongFilter> optionalEventoId() {
        return Optional.ofNullable(eventoId);
    }

    public LongFilter eventoId() {
        if (eventoId == null) {
            setEventoId(new LongFilter());
        }
        return eventoId;
    }

    public void setEventoId(LongFilter eventoId) {
        this.eventoId = eventoId;
    }

    public Boolean getDistinct() {
        return distinct;
    }

    public Optional<Boolean> optionalDistinct() {
        return Optional.ofNullable(distinct);
    }

    public Boolean distinct() {
        if (distinct == null) {
            setDistinct(true);
        }
        return distinct;
    }

    public void setDistinct(Boolean distinct) {
        this.distinct = distinct;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final VentaCriteria that = (VentaCriteria) o;
        return (
            Objects.equals(id, that.id) &&
            Objects.equals(cantidad, that.cantidad) &&
            Objects.equals(precioUnitario, that.precioUnitario) &&
            Objects.equals(total, that.total) &&
            Objects.equals(fechaZoned, that.fechaZoned) &&
            Objects.equals(eventoId, that.eventoId) &&
            Objects.equals(distinct, that.distinct)
        );
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, cantidad, precioUnitario, total, fechaZoned, eventoId, distinct);
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "VentaCriteria{" +
            optionalId().map(f -> "id=" + f + ", ").orElse("") +
            optionalCantidad().map(f -> "cantidad=" + f + ", ").orElse("") +
            optionalPrecioUnitario().map(f -> "precioUnitario=" + f + ", ").orElse("") +
            optionalTotal().map(f -> "total=" + f + ", ").orElse("") +
            optionalFechaZoned().map(f -> "fechaZoned=" + f + ", ").orElse("") +
            optionalEventoId().map(f -> "eventoId=" + f + ", ").orElse("") +
            optionalDistinct().map(f -> "distinct=" + f + ", ").orElse("") +
        "}";
    }
}
