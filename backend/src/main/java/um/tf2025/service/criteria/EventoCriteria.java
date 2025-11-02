package um.tf2025.service.criteria;

import java.io.Serializable;
import java.util.Objects;
import java.util.Optional;
import org.springdoc.core.annotations.ParameterObject;
import tech.jhipster.service.Criteria;
import tech.jhipster.service.filter.*;

/**
 * Criteria class for the {@link um.tf2025.domain.Evento} entity. This class is used
 * in {@link um.tf2025.web.rest.EventoResource} to receive all the possible filtering options from
 * the Http GET request parameters.
 * For example the following could be a valid request:
 * {@code /eventos?id.greaterThan=5&attr1.contains=something&attr2.specified=false}
 * As Spring is unable to properly convert the types, unless specific {@link Filter} class are used, we need to use
 * fix type specific filters.
 */
@ParameterObject
@SuppressWarnings("common-java:DuplicatedBlocks")
public class EventoCriteria implements Serializable, Criteria {

    private static final long serialVersionUID = 1L;

    private LongFilter id;

    private StringFilter nombre;

    private StringFilter descripcion;

    private ZonedDateTimeFilter fechaZoned;

    private BigDecimalFilter precioBase;

    private IntegerFilter stock;

    private BooleanFilter activo;

    private Boolean distinct;

    public EventoCriteria() {}

    public EventoCriteria(EventoCriteria other) {
        this.id = other.optionalId().map(LongFilter::copy).orElse(null);
        this.nombre = other.optionalNombre().map(StringFilter::copy).orElse(null);
        this.descripcion = other.optionalDescripcion().map(StringFilter::copy).orElse(null);
        this.fechaZoned = other.optionalFechaZoned().map(ZonedDateTimeFilter::copy).orElse(null);
        this.precioBase = other.optionalPrecioBase().map(BigDecimalFilter::copy).orElse(null);
        this.stock = other.optionalStock().map(IntegerFilter::copy).orElse(null);
        this.activo = other.optionalActivo().map(BooleanFilter::copy).orElse(null);
        this.distinct = other.distinct;
    }

    @Override
    public EventoCriteria copy() {
        return new EventoCriteria(this);
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

    public StringFilter getNombre() {
        return nombre;
    }

    public Optional<StringFilter> optionalNombre() {
        return Optional.ofNullable(nombre);
    }

    public StringFilter nombre() {
        if (nombre == null) {
            setNombre(new StringFilter());
        }
        return nombre;
    }

    public void setNombre(StringFilter nombre) {
        this.nombre = nombre;
    }

    public StringFilter getDescripcion() {
        return descripcion;
    }

    public Optional<StringFilter> optionalDescripcion() {
        return Optional.ofNullable(descripcion);
    }

    public StringFilter descripcion() {
        if (descripcion == null) {
            setDescripcion(new StringFilter());
        }
        return descripcion;
    }

    public void setDescripcion(StringFilter descripcion) {
        this.descripcion = descripcion;
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

    public BigDecimalFilter getPrecioBase() {
        return precioBase;
    }

    public Optional<BigDecimalFilter> optionalPrecioBase() {
        return Optional.ofNullable(precioBase);
    }

    public BigDecimalFilter precioBase() {
        if (precioBase == null) {
            setPrecioBase(new BigDecimalFilter());
        }
        return precioBase;
    }

    public void setPrecioBase(BigDecimalFilter precioBase) {
        this.precioBase = precioBase;
    }

    public IntegerFilter getStock() {
        return stock;
    }

    public Optional<IntegerFilter> optionalStock() {
        return Optional.ofNullable(stock);
    }

    public IntegerFilter stock() {
        if (stock == null) {
            setStock(new IntegerFilter());
        }
        return stock;
    }

    public void setStock(IntegerFilter stock) {
        this.stock = stock;
    }

    public BooleanFilter getActivo() {
        return activo;
    }

    public Optional<BooleanFilter> optionalActivo() {
        return Optional.ofNullable(activo);
    }

    public BooleanFilter activo() {
        if (activo == null) {
            setActivo(new BooleanFilter());
        }
        return activo;
    }

    public void setActivo(BooleanFilter activo) {
        this.activo = activo;
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
        final EventoCriteria that = (EventoCriteria) o;
        return (
            Objects.equals(id, that.id) &&
            Objects.equals(nombre, that.nombre) &&
            Objects.equals(descripcion, that.descripcion) &&
            Objects.equals(fechaZoned, that.fechaZoned) &&
            Objects.equals(precioBase, that.precioBase) &&
            Objects.equals(stock, that.stock) &&
            Objects.equals(activo, that.activo) &&
            Objects.equals(distinct, that.distinct)
        );
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, nombre, descripcion, fechaZoned, precioBase, stock, activo, distinct);
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "EventoCriteria{" +
            optionalId().map(f -> "id=" + f + ", ").orElse("") +
            optionalNombre().map(f -> "nombre=" + f + ", ").orElse("") +
            optionalDescripcion().map(f -> "descripcion=" + f + ", ").orElse("") +
            optionalFechaZoned().map(f -> "fechaZoned=" + f + ", ").orElse("") +
            optionalPrecioBase().map(f -> "precioBase=" + f + ", ").orElse("") +
            optionalStock().map(f -> "stock=" + f + ", ").orElse("") +
            optionalActivo().map(f -> "activo=" + f + ", ").orElse("") +
            optionalDistinct().map(f -> "distinct=" + f + ", ").orElse("") +
        "}";
    }
}
