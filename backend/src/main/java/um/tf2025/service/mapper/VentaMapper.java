package um.tf2025.service.mapper;

import org.mapstruct.*;
import um.tf2025.domain.Evento;
import um.tf2025.domain.Venta;
import um.tf2025.service.dto.EventoDTO;
import um.tf2025.service.dto.VentaDTO;

/**
 * Mapper for the entity {@link Venta} and its DTO {@link VentaDTO}.
 */
@Mapper(componentModel = "spring")
public interface VentaMapper extends EntityMapper<VentaDTO, Venta> {
    @Mapping(target = "evento", source = "evento", qualifiedByName = "eventoNombre")
    VentaDTO toDto(Venta s);

    @Named("eventoNombre")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    @Mapping(target = "nombre", source = "nombre")
    EventoDTO toDtoEventoNombre(Evento evento);
}
