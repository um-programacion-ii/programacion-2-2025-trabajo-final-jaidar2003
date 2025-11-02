package um.tf2025.service.mapper;

import org.mapstruct.*;
import um.tf2025.domain.Evento;
import um.tf2025.service.dto.EventoDTO;

/**
 * Mapper for the entity {@link Evento} and its DTO {@link EventoDTO}.
 */
@Mapper(componentModel = "spring")
public interface EventoMapper extends EntityMapper<EventoDTO, Evento> {}
