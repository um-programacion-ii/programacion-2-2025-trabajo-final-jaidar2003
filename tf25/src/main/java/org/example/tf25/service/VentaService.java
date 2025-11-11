package org.example.tf25.service;

import org.example.tf25.domain.Evento;
import org.example.tf25.domain.Venta;
import org.example.tf25.repository.EventoRepository;
import org.example.tf25.repository.VentaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;

@Service
@Transactional
public class VentaService {

    private final VentaRepository ventaRepository;
    private final EventoRepository eventoRepository;

    public VentaService(VentaRepository ventaRepository, EventoRepository eventoRepository) {
        this.ventaRepository = ventaRepository;
        this.eventoRepository = eventoRepository;
    }

    public Venta crearVenta(Long eventoId, String compradorEmail, int cantidad) {
        Evento evento = eventoRepository.findById(eventoId)
                .orElseThrow(() -> new NoSuchElementException("Evento no encontrado: " + eventoId));

        if (cantidad <= 0) {
            throw new IllegalArgumentException("La cantidad debe ser positiva");
        }
        if (evento.getCupo() == null || evento.getCupo() < cantidad) {
            throw new IllegalStateException("No hay cupo suficiente para el evento");
        }

        // Calcular total
        BigDecimal precio = evento.getPrecio() == null ? BigDecimal.ZERO : evento.getPrecio();
        BigDecimal total = precio.multiply(BigDecimal.valueOf(cantidad));

        // Disminuir cupo
        evento.setCupo(evento.getCupo() - cantidad);
        eventoRepository.save(evento);

        Venta v = new Venta();
        v.setEvento(evento);
        v.setCompradorEmail(compradorEmail);
        v.setCantidad(cantidad);
        v.setTotal(total);
        v.setFechaHora(LocalDateTime.now());
        return ventaRepository.save(v);
    }

    @Transactional(readOnly = true)
    public List<Venta> listarPorEvento(Long eventoId) {
        return ventaRepository.findByEvento_Id(eventoId);
    }
}
