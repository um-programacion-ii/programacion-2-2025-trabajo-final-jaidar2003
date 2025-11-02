package um.tf2025.service;

import jakarta.persistence.criteria.JoinType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tech.jhipster.service.QueryService;
import um.tf2025.domain.*; // for static metamodels
import um.tf2025.domain.Venta;
import um.tf2025.repository.VentaRepository;
import um.tf2025.service.criteria.VentaCriteria;
import um.tf2025.service.dto.VentaDTO;
import um.tf2025.service.mapper.VentaMapper;

/**
 * Service for executing complex queries for {@link Venta} entities in the database.
 * The main input is a {@link VentaCriteria} which gets converted to {@link Specification},
 * in a way that all the filters must apply.
 * It returns a {@link Page} of {@link VentaDTO} which fulfills the criteria.
 */
@Service
@Transactional(readOnly = true)
public class VentaQueryService extends QueryService<Venta> {

    private static final Logger LOG = LoggerFactory.getLogger(VentaQueryService.class);

    private final VentaRepository ventaRepository;

    private final VentaMapper ventaMapper;

    public VentaQueryService(VentaRepository ventaRepository, VentaMapper ventaMapper) {
        this.ventaRepository = ventaRepository;
        this.ventaMapper = ventaMapper;
    }

    /**
     * Return a {@link Page} of {@link VentaDTO} which matches the criteria from the database.
     * @param criteria The object which holds all the filters, which the entities should match.
     * @param page The page, which should be returned.
     * @return the matching entities.
     */
    @Transactional(readOnly = true)
    public Page<VentaDTO> findByCriteria(VentaCriteria criteria, Pageable page) {
        LOG.debug("find by criteria : {}, page: {}", criteria, page);
        final Specification<Venta> specification = createSpecification(criteria);
        return ventaRepository.findAll(specification, page).map(ventaMapper::toDto);
    }

    /**
     * Return the number of matching entities in the database.
     * @param criteria The object which holds all the filters, which the entities should match.
     * @return the number of matching entities.
     */
    @Transactional(readOnly = true)
    public long countByCriteria(VentaCriteria criteria) {
        LOG.debug("count by criteria : {}", criteria);
        final Specification<Venta> specification = createSpecification(criteria);
        return ventaRepository.count(specification);
    }

    /**
     * Function to convert {@link VentaCriteria} to a {@link Specification}
     * @param criteria The object which holds all the filters, which the entities should match.
     * @return the matching {@link Specification} of the entity.
     */
    protected Specification<Venta> createSpecification(VentaCriteria criteria) {
        Specification<Venta> specification = Specification.where(null);
        if (criteria != null) {
            // This has to be called first, because the distinct method returns null
            specification = Specification.allOf(
                Boolean.TRUE.equals(criteria.getDistinct()) ? distinct(criteria.getDistinct()) : null,
                buildRangeSpecification(criteria.getId(), Venta_.id),
                buildRangeSpecification(criteria.getCantidad(), Venta_.cantidad),
                buildRangeSpecification(criteria.getPrecioUnitario(), Venta_.precioUnitario),
                buildRangeSpecification(criteria.getTotal(), Venta_.total),
                buildRangeSpecification(criteria.getFechaZoned(), Venta_.fechaZoned),
                buildSpecification(criteria.getEventoId(), root -> root.join(Venta_.evento, JoinType.LEFT).get(Evento_.id))
            );
        }
        return specification;
    }
}
