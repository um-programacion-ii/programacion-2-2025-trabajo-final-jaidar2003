package um.tf2025.web.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static um.tf2025.domain.EventoAsserts.*;
import static um.tf2025.web.rest.TestUtil.createUpdateProxyForBean;
import static um.tf2025.web.rest.TestUtil.sameInstant;
import static um.tf2025.web.rest.TestUtil.sameNumber;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Random;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import um.tf2025.IntegrationTest;
import um.tf2025.domain.Evento;
import um.tf2025.repository.EventoRepository;
import um.tf2025.service.dto.EventoDTO;
import um.tf2025.service.mapper.EventoMapper;

/**
 * Integration tests for the {@link EventoResource} REST controller.
 */
@IntegrationTest
@AutoConfigureMockMvc
@WithMockUser
class EventoResourceIT {

    private static final String DEFAULT_NOMBRE = "AAAAAAAAAA";
    private static final String UPDATED_NOMBRE = "BBBBBBBBBB";

    private static final String DEFAULT_DESCRIPCION = "AAAAAAAAAA";
    private static final String UPDATED_DESCRIPCION = "BBBBBBBBBB";

    private static final ZonedDateTime DEFAULT_FECHA_ZONED = ZonedDateTime.ofInstant(Instant.ofEpochMilli(0L), ZoneOffset.UTC);
    private static final ZonedDateTime UPDATED_FECHA_ZONED = ZonedDateTime.now(ZoneId.systemDefault()).withNano(0);
    private static final ZonedDateTime SMALLER_FECHA_ZONED = ZonedDateTime.ofInstant(Instant.ofEpochMilli(-1L), ZoneOffset.UTC);

    private static final BigDecimal DEFAULT_PRECIO_BASE = new BigDecimal(0);
    private static final BigDecimal UPDATED_PRECIO_BASE = new BigDecimal(1);
    private static final BigDecimal SMALLER_PRECIO_BASE = new BigDecimal(0 - 1);

    private static final Integer DEFAULT_STOCK = 0;
    private static final Integer UPDATED_STOCK = 1;
    private static final Integer SMALLER_STOCK = 0 - 1;

    private static final Boolean DEFAULT_ACTIVO = false;
    private static final Boolean UPDATED_ACTIVO = true;

    private static final String ENTITY_API_URL = "/api/eventos";
    private static final String ENTITY_API_URL_ID = ENTITY_API_URL + "/{id}";

    private static Random random = new Random();
    private static AtomicLong longCount = new AtomicLong(random.nextInt() + (2 * Integer.MAX_VALUE));

    @Autowired
    private ObjectMapper om;

    @Autowired
    private EventoRepository eventoRepository;

    @Autowired
    private EventoMapper eventoMapper;

    @Autowired
    private EntityManager em;

    @Autowired
    private MockMvc restEventoMockMvc;

    private Evento evento;

    private Evento insertedEvento;

    /**
     * Create an entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static Evento createEntity() {
        return new Evento()
            .nombre(DEFAULT_NOMBRE)
            .descripcion(DEFAULT_DESCRIPCION)
            .fechaZoned(DEFAULT_FECHA_ZONED)
            .precioBase(DEFAULT_PRECIO_BASE)
            .stock(DEFAULT_STOCK)
            .activo(DEFAULT_ACTIVO);
    }

    /**
     * Create an updated entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static Evento createUpdatedEntity() {
        return new Evento()
            .nombre(UPDATED_NOMBRE)
            .descripcion(UPDATED_DESCRIPCION)
            .fechaZoned(UPDATED_FECHA_ZONED)
            .precioBase(UPDATED_PRECIO_BASE)
            .stock(UPDATED_STOCK)
            .activo(UPDATED_ACTIVO);
    }

    @BeforeEach
    void initTest() {
        evento = createEntity();
    }

    @AfterEach
    void cleanup() {
        if (insertedEvento != null) {
            eventoRepository.delete(insertedEvento);
            insertedEvento = null;
        }
    }

    @Test
    @Transactional
    void createEvento() throws Exception {
        long databaseSizeBeforeCreate = getRepositoryCount();
        // Create the Evento
        EventoDTO eventoDTO = eventoMapper.toDto(evento);
        var returnedEventoDTO = om.readValue(
            restEventoMockMvc
                .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(eventoDTO)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString(),
            EventoDTO.class
        );

        // Validate the Evento in the database
        assertIncrementedRepositoryCount(databaseSizeBeforeCreate);
        var returnedEvento = eventoMapper.toEntity(returnedEventoDTO);
        assertEventoUpdatableFieldsEquals(returnedEvento, getPersistedEvento(returnedEvento));

        insertedEvento = returnedEvento;
    }

    @Test
    @Transactional
    void createEventoWithExistingId() throws Exception {
        // Create the Evento with an existing ID
        evento.setId(1L);
        EventoDTO eventoDTO = eventoMapper.toDto(evento);

        long databaseSizeBeforeCreate = getRepositoryCount();

        // An entity with an existing ID cannot be created, so this API call must fail
        restEventoMockMvc
            .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(eventoDTO)))
            .andExpect(status().isBadRequest());

        // Validate the Evento in the database
        assertSameRepositoryCount(databaseSizeBeforeCreate);
    }

    @Test
    @Transactional
    void checkNombreIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        evento.setNombre(null);

        // Create the Evento, which fails.
        EventoDTO eventoDTO = eventoMapper.toDto(evento);

        restEventoMockMvc
            .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(eventoDTO)))
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkFechaZonedIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        evento.setFechaZoned(null);

        // Create the Evento, which fails.
        EventoDTO eventoDTO = eventoMapper.toDto(evento);

        restEventoMockMvc
            .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(eventoDTO)))
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkPrecioBaseIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        evento.setPrecioBase(null);

        // Create the Evento, which fails.
        EventoDTO eventoDTO = eventoMapper.toDto(evento);

        restEventoMockMvc
            .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(eventoDTO)))
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkStockIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        evento.setStock(null);

        // Create the Evento, which fails.
        EventoDTO eventoDTO = eventoMapper.toDto(evento);

        restEventoMockMvc
            .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(eventoDTO)))
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkActivoIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        evento.setActivo(null);

        // Create the Evento, which fails.
        EventoDTO eventoDTO = eventoMapper.toDto(evento);

        restEventoMockMvc
            .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(eventoDTO)))
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void getAllEventos() throws Exception {
        // Initialize the database
        insertedEvento = eventoRepository.saveAndFlush(evento);

        // Get all the eventoList
        restEventoMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(evento.getId().intValue())))
            .andExpect(jsonPath("$.[*].nombre").value(hasItem(DEFAULT_NOMBRE)))
            .andExpect(jsonPath("$.[*].descripcion").value(hasItem(DEFAULT_DESCRIPCION)))
            .andExpect(jsonPath("$.[*].fechaZoned").value(hasItem(sameInstant(DEFAULT_FECHA_ZONED))))
            .andExpect(jsonPath("$.[*].precioBase").value(hasItem(sameNumber(DEFAULT_PRECIO_BASE))))
            .andExpect(jsonPath("$.[*].stock").value(hasItem(DEFAULT_STOCK)))
            .andExpect(jsonPath("$.[*].activo").value(hasItem(DEFAULT_ACTIVO)));
    }

    @Test
    @Transactional
    void getEvento() throws Exception {
        // Initialize the database
        insertedEvento = eventoRepository.saveAndFlush(evento);

        // Get the evento
        restEventoMockMvc
            .perform(get(ENTITY_API_URL_ID, evento.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.id").value(evento.getId().intValue()))
            .andExpect(jsonPath("$.nombre").value(DEFAULT_NOMBRE))
            .andExpect(jsonPath("$.descripcion").value(DEFAULT_DESCRIPCION))
            .andExpect(jsonPath("$.fechaZoned").value(sameInstant(DEFAULT_FECHA_ZONED)))
            .andExpect(jsonPath("$.precioBase").value(sameNumber(DEFAULT_PRECIO_BASE)))
            .andExpect(jsonPath("$.stock").value(DEFAULT_STOCK))
            .andExpect(jsonPath("$.activo").value(DEFAULT_ACTIVO));
    }

    @Test
    @Transactional
    void getEventosByIdFiltering() throws Exception {
        // Initialize the database
        insertedEvento = eventoRepository.saveAndFlush(evento);

        Long id = evento.getId();

        defaultEventoFiltering("id.equals=" + id, "id.notEquals=" + id);

        defaultEventoFiltering("id.greaterThanOrEqual=" + id, "id.greaterThan=" + id);

        defaultEventoFiltering("id.lessThanOrEqual=" + id, "id.lessThan=" + id);
    }

    @Test
    @Transactional
    void getAllEventosByNombreIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedEvento = eventoRepository.saveAndFlush(evento);

        // Get all the eventoList where nombre equals to
        defaultEventoFiltering("nombre.equals=" + DEFAULT_NOMBRE, "nombre.equals=" + UPDATED_NOMBRE);
    }

    @Test
    @Transactional
    void getAllEventosByNombreIsInShouldWork() throws Exception {
        // Initialize the database
        insertedEvento = eventoRepository.saveAndFlush(evento);

        // Get all the eventoList where nombre in
        defaultEventoFiltering("nombre.in=" + DEFAULT_NOMBRE + "," + UPDATED_NOMBRE, "nombre.in=" + UPDATED_NOMBRE);
    }

    @Test
    @Transactional
    void getAllEventosByNombreIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedEvento = eventoRepository.saveAndFlush(evento);

        // Get all the eventoList where nombre is not null
        defaultEventoFiltering("nombre.specified=true", "nombre.specified=false");
    }

    @Test
    @Transactional
    void getAllEventosByNombreContainsSomething() throws Exception {
        // Initialize the database
        insertedEvento = eventoRepository.saveAndFlush(evento);

        // Get all the eventoList where nombre contains
        defaultEventoFiltering("nombre.contains=" + DEFAULT_NOMBRE, "nombre.contains=" + UPDATED_NOMBRE);
    }

    @Test
    @Transactional
    void getAllEventosByNombreNotContainsSomething() throws Exception {
        // Initialize the database
        insertedEvento = eventoRepository.saveAndFlush(evento);

        // Get all the eventoList where nombre does not contain
        defaultEventoFiltering("nombre.doesNotContain=" + UPDATED_NOMBRE, "nombre.doesNotContain=" + DEFAULT_NOMBRE);
    }

    @Test
    @Transactional
    void getAllEventosByDescripcionIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedEvento = eventoRepository.saveAndFlush(evento);

        // Get all the eventoList where descripcion equals to
        defaultEventoFiltering("descripcion.equals=" + DEFAULT_DESCRIPCION, "descripcion.equals=" + UPDATED_DESCRIPCION);
    }

    @Test
    @Transactional
    void getAllEventosByDescripcionIsInShouldWork() throws Exception {
        // Initialize the database
        insertedEvento = eventoRepository.saveAndFlush(evento);

        // Get all the eventoList where descripcion in
        defaultEventoFiltering(
            "descripcion.in=" + DEFAULT_DESCRIPCION + "," + UPDATED_DESCRIPCION,
            "descripcion.in=" + UPDATED_DESCRIPCION
        );
    }

    @Test
    @Transactional
    void getAllEventosByDescripcionIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedEvento = eventoRepository.saveAndFlush(evento);

        // Get all the eventoList where descripcion is not null
        defaultEventoFiltering("descripcion.specified=true", "descripcion.specified=false");
    }

    @Test
    @Transactional
    void getAllEventosByDescripcionContainsSomething() throws Exception {
        // Initialize the database
        insertedEvento = eventoRepository.saveAndFlush(evento);

        // Get all the eventoList where descripcion contains
        defaultEventoFiltering("descripcion.contains=" + DEFAULT_DESCRIPCION, "descripcion.contains=" + UPDATED_DESCRIPCION);
    }

    @Test
    @Transactional
    void getAllEventosByDescripcionNotContainsSomething() throws Exception {
        // Initialize the database
        insertedEvento = eventoRepository.saveAndFlush(evento);

        // Get all the eventoList where descripcion does not contain
        defaultEventoFiltering("descripcion.doesNotContain=" + UPDATED_DESCRIPCION, "descripcion.doesNotContain=" + DEFAULT_DESCRIPCION);
    }

    @Test
    @Transactional
    void getAllEventosByFechaZonedIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedEvento = eventoRepository.saveAndFlush(evento);

        // Get all the eventoList where fechaZoned equals to
        defaultEventoFiltering("fechaZoned.equals=" + DEFAULT_FECHA_ZONED, "fechaZoned.equals=" + UPDATED_FECHA_ZONED);
    }

    @Test
    @Transactional
    void getAllEventosByFechaZonedIsInShouldWork() throws Exception {
        // Initialize the database
        insertedEvento = eventoRepository.saveAndFlush(evento);

        // Get all the eventoList where fechaZoned in
        defaultEventoFiltering("fechaZoned.in=" + DEFAULT_FECHA_ZONED + "," + UPDATED_FECHA_ZONED, "fechaZoned.in=" + UPDATED_FECHA_ZONED);
    }

    @Test
    @Transactional
    void getAllEventosByFechaZonedIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedEvento = eventoRepository.saveAndFlush(evento);

        // Get all the eventoList where fechaZoned is not null
        defaultEventoFiltering("fechaZoned.specified=true", "fechaZoned.specified=false");
    }

    @Test
    @Transactional
    void getAllEventosByFechaZonedIsGreaterThanOrEqualToSomething() throws Exception {
        // Initialize the database
        insertedEvento = eventoRepository.saveAndFlush(evento);

        // Get all the eventoList where fechaZoned is greater than or equal to
        defaultEventoFiltering(
            "fechaZoned.greaterThanOrEqual=" + DEFAULT_FECHA_ZONED,
            "fechaZoned.greaterThanOrEqual=" + UPDATED_FECHA_ZONED
        );
    }

    @Test
    @Transactional
    void getAllEventosByFechaZonedIsLessThanOrEqualToSomething() throws Exception {
        // Initialize the database
        insertedEvento = eventoRepository.saveAndFlush(evento);

        // Get all the eventoList where fechaZoned is less than or equal to
        defaultEventoFiltering("fechaZoned.lessThanOrEqual=" + DEFAULT_FECHA_ZONED, "fechaZoned.lessThanOrEqual=" + SMALLER_FECHA_ZONED);
    }

    @Test
    @Transactional
    void getAllEventosByFechaZonedIsLessThanSomething() throws Exception {
        // Initialize the database
        insertedEvento = eventoRepository.saveAndFlush(evento);

        // Get all the eventoList where fechaZoned is less than
        defaultEventoFiltering("fechaZoned.lessThan=" + UPDATED_FECHA_ZONED, "fechaZoned.lessThan=" + DEFAULT_FECHA_ZONED);
    }

    @Test
    @Transactional
    void getAllEventosByFechaZonedIsGreaterThanSomething() throws Exception {
        // Initialize the database
        insertedEvento = eventoRepository.saveAndFlush(evento);

        // Get all the eventoList where fechaZoned is greater than
        defaultEventoFiltering("fechaZoned.greaterThan=" + SMALLER_FECHA_ZONED, "fechaZoned.greaterThan=" + DEFAULT_FECHA_ZONED);
    }

    @Test
    @Transactional
    void getAllEventosByPrecioBaseIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedEvento = eventoRepository.saveAndFlush(evento);

        // Get all the eventoList where precioBase equals to
        defaultEventoFiltering("precioBase.equals=" + DEFAULT_PRECIO_BASE, "precioBase.equals=" + UPDATED_PRECIO_BASE);
    }

    @Test
    @Transactional
    void getAllEventosByPrecioBaseIsInShouldWork() throws Exception {
        // Initialize the database
        insertedEvento = eventoRepository.saveAndFlush(evento);

        // Get all the eventoList where precioBase in
        defaultEventoFiltering("precioBase.in=" + DEFAULT_PRECIO_BASE + "," + UPDATED_PRECIO_BASE, "precioBase.in=" + UPDATED_PRECIO_BASE);
    }

    @Test
    @Transactional
    void getAllEventosByPrecioBaseIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedEvento = eventoRepository.saveAndFlush(evento);

        // Get all the eventoList where precioBase is not null
        defaultEventoFiltering("precioBase.specified=true", "precioBase.specified=false");
    }

    @Test
    @Transactional
    void getAllEventosByPrecioBaseIsGreaterThanOrEqualToSomething() throws Exception {
        // Initialize the database
        insertedEvento = eventoRepository.saveAndFlush(evento);

        // Get all the eventoList where precioBase is greater than or equal to
        defaultEventoFiltering(
            "precioBase.greaterThanOrEqual=" + DEFAULT_PRECIO_BASE,
            "precioBase.greaterThanOrEqual=" + UPDATED_PRECIO_BASE
        );
    }

    @Test
    @Transactional
    void getAllEventosByPrecioBaseIsLessThanOrEqualToSomething() throws Exception {
        // Initialize the database
        insertedEvento = eventoRepository.saveAndFlush(evento);

        // Get all the eventoList where precioBase is less than or equal to
        defaultEventoFiltering("precioBase.lessThanOrEqual=" + DEFAULT_PRECIO_BASE, "precioBase.lessThanOrEqual=" + SMALLER_PRECIO_BASE);
    }

    @Test
    @Transactional
    void getAllEventosByPrecioBaseIsLessThanSomething() throws Exception {
        // Initialize the database
        insertedEvento = eventoRepository.saveAndFlush(evento);

        // Get all the eventoList where precioBase is less than
        defaultEventoFiltering("precioBase.lessThan=" + UPDATED_PRECIO_BASE, "precioBase.lessThan=" + DEFAULT_PRECIO_BASE);
    }

    @Test
    @Transactional
    void getAllEventosByPrecioBaseIsGreaterThanSomething() throws Exception {
        // Initialize the database
        insertedEvento = eventoRepository.saveAndFlush(evento);

        // Get all the eventoList where precioBase is greater than
        defaultEventoFiltering("precioBase.greaterThan=" + SMALLER_PRECIO_BASE, "precioBase.greaterThan=" + DEFAULT_PRECIO_BASE);
    }

    @Test
    @Transactional
    void getAllEventosByStockIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedEvento = eventoRepository.saveAndFlush(evento);

        // Get all the eventoList where stock equals to
        defaultEventoFiltering("stock.equals=" + DEFAULT_STOCK, "stock.equals=" + UPDATED_STOCK);
    }

    @Test
    @Transactional
    void getAllEventosByStockIsInShouldWork() throws Exception {
        // Initialize the database
        insertedEvento = eventoRepository.saveAndFlush(evento);

        // Get all the eventoList where stock in
        defaultEventoFiltering("stock.in=" + DEFAULT_STOCK + "," + UPDATED_STOCK, "stock.in=" + UPDATED_STOCK);
    }

    @Test
    @Transactional
    void getAllEventosByStockIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedEvento = eventoRepository.saveAndFlush(evento);

        // Get all the eventoList where stock is not null
        defaultEventoFiltering("stock.specified=true", "stock.specified=false");
    }

    @Test
    @Transactional
    void getAllEventosByStockIsGreaterThanOrEqualToSomething() throws Exception {
        // Initialize the database
        insertedEvento = eventoRepository.saveAndFlush(evento);

        // Get all the eventoList where stock is greater than or equal to
        defaultEventoFiltering("stock.greaterThanOrEqual=" + DEFAULT_STOCK, "stock.greaterThanOrEqual=" + UPDATED_STOCK);
    }

    @Test
    @Transactional
    void getAllEventosByStockIsLessThanOrEqualToSomething() throws Exception {
        // Initialize the database
        insertedEvento = eventoRepository.saveAndFlush(evento);

        // Get all the eventoList where stock is less than or equal to
        defaultEventoFiltering("stock.lessThanOrEqual=" + DEFAULT_STOCK, "stock.lessThanOrEqual=" + SMALLER_STOCK);
    }

    @Test
    @Transactional
    void getAllEventosByStockIsLessThanSomething() throws Exception {
        // Initialize the database
        insertedEvento = eventoRepository.saveAndFlush(evento);

        // Get all the eventoList where stock is less than
        defaultEventoFiltering("stock.lessThan=" + UPDATED_STOCK, "stock.lessThan=" + DEFAULT_STOCK);
    }

    @Test
    @Transactional
    void getAllEventosByStockIsGreaterThanSomething() throws Exception {
        // Initialize the database
        insertedEvento = eventoRepository.saveAndFlush(evento);

        // Get all the eventoList where stock is greater than
        defaultEventoFiltering("stock.greaterThan=" + SMALLER_STOCK, "stock.greaterThan=" + DEFAULT_STOCK);
    }

    @Test
    @Transactional
    void getAllEventosByActivoIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedEvento = eventoRepository.saveAndFlush(evento);

        // Get all the eventoList where activo equals to
        defaultEventoFiltering("activo.equals=" + DEFAULT_ACTIVO, "activo.equals=" + UPDATED_ACTIVO);
    }

    @Test
    @Transactional
    void getAllEventosByActivoIsInShouldWork() throws Exception {
        // Initialize the database
        insertedEvento = eventoRepository.saveAndFlush(evento);

        // Get all the eventoList where activo in
        defaultEventoFiltering("activo.in=" + DEFAULT_ACTIVO + "," + UPDATED_ACTIVO, "activo.in=" + UPDATED_ACTIVO);
    }

    @Test
    @Transactional
    void getAllEventosByActivoIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedEvento = eventoRepository.saveAndFlush(evento);

        // Get all the eventoList where activo is not null
        defaultEventoFiltering("activo.specified=true", "activo.specified=false");
    }

    private void defaultEventoFiltering(String shouldBeFound, String shouldNotBeFound) throws Exception {
        defaultEventoShouldBeFound(shouldBeFound);
        defaultEventoShouldNotBeFound(shouldNotBeFound);
    }

    /**
     * Executes the search, and checks that the default entity is returned.
     */
    private void defaultEventoShouldBeFound(String filter) throws Exception {
        restEventoMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(evento.getId().intValue())))
            .andExpect(jsonPath("$.[*].nombre").value(hasItem(DEFAULT_NOMBRE)))
            .andExpect(jsonPath("$.[*].descripcion").value(hasItem(DEFAULT_DESCRIPCION)))
            .andExpect(jsonPath("$.[*].fechaZoned").value(hasItem(sameInstant(DEFAULT_FECHA_ZONED))))
            .andExpect(jsonPath("$.[*].precioBase").value(hasItem(sameNumber(DEFAULT_PRECIO_BASE))))
            .andExpect(jsonPath("$.[*].stock").value(hasItem(DEFAULT_STOCK)))
            .andExpect(jsonPath("$.[*].activo").value(hasItem(DEFAULT_ACTIVO)));

        // Check, that the count call also returns 1
        restEventoMockMvc
            .perform(get(ENTITY_API_URL + "/count?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(content().string("1"));
    }

    /**
     * Executes the search, and checks that the default entity is not returned.
     */
    private void defaultEventoShouldNotBeFound(String filter) throws Exception {
        restEventoMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$").isEmpty());

        // Check, that the count call also returns 0
        restEventoMockMvc
            .perform(get(ENTITY_API_URL + "/count?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(content().string("0"));
    }

    @Test
    @Transactional
    void getNonExistingEvento() throws Exception {
        // Get the evento
        restEventoMockMvc.perform(get(ENTITY_API_URL_ID, Long.MAX_VALUE)).andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    void putExistingEvento() throws Exception {
        // Initialize the database
        insertedEvento = eventoRepository.saveAndFlush(evento);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the evento
        Evento updatedEvento = eventoRepository.findById(evento.getId()).orElseThrow();
        // Disconnect from session so that the updates on updatedEvento are not directly saved in db
        em.detach(updatedEvento);
        updatedEvento
            .nombre(UPDATED_NOMBRE)
            .descripcion(UPDATED_DESCRIPCION)
            .fechaZoned(UPDATED_FECHA_ZONED)
            .precioBase(UPDATED_PRECIO_BASE)
            .stock(UPDATED_STOCK)
            .activo(UPDATED_ACTIVO);
        EventoDTO eventoDTO = eventoMapper.toDto(updatedEvento);

        restEventoMockMvc
            .perform(
                put(ENTITY_API_URL_ID, eventoDTO.getId()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(eventoDTO))
            )
            .andExpect(status().isOk());

        // Validate the Evento in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertPersistedEventoToMatchAllProperties(updatedEvento);
    }

    @Test
    @Transactional
    void putNonExistingEvento() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        evento.setId(longCount.incrementAndGet());

        // Create the Evento
        EventoDTO eventoDTO = eventoMapper.toDto(evento);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restEventoMockMvc
            .perform(
                put(ENTITY_API_URL_ID, eventoDTO.getId()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(eventoDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the Evento in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void putWithIdMismatchEvento() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        evento.setId(longCount.incrementAndGet());

        // Create the Evento
        EventoDTO eventoDTO = eventoMapper.toDto(evento);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restEventoMockMvc
            .perform(
                put(ENTITY_API_URL_ID, longCount.incrementAndGet())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(eventoDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the Evento in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void putWithMissingIdPathParamEvento() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        evento.setId(longCount.incrementAndGet());

        // Create the Evento
        EventoDTO eventoDTO = eventoMapper.toDto(evento);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restEventoMockMvc
            .perform(put(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(eventoDTO)))
            .andExpect(status().isMethodNotAllowed());

        // Validate the Evento in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void partialUpdateEventoWithPatch() throws Exception {
        // Initialize the database
        insertedEvento = eventoRepository.saveAndFlush(evento);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the evento using partial update
        Evento partialUpdatedEvento = new Evento();
        partialUpdatedEvento.setId(evento.getId());

        partialUpdatedEvento.nombre(UPDATED_NOMBRE);

        restEventoMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedEvento.getId())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(partialUpdatedEvento))
            )
            .andExpect(status().isOk());

        // Validate the Evento in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertEventoUpdatableFieldsEquals(createUpdateProxyForBean(partialUpdatedEvento, evento), getPersistedEvento(evento));
    }

    @Test
    @Transactional
    void fullUpdateEventoWithPatch() throws Exception {
        // Initialize the database
        insertedEvento = eventoRepository.saveAndFlush(evento);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the evento using partial update
        Evento partialUpdatedEvento = new Evento();
        partialUpdatedEvento.setId(evento.getId());

        partialUpdatedEvento
            .nombre(UPDATED_NOMBRE)
            .descripcion(UPDATED_DESCRIPCION)
            .fechaZoned(UPDATED_FECHA_ZONED)
            .precioBase(UPDATED_PRECIO_BASE)
            .stock(UPDATED_STOCK)
            .activo(UPDATED_ACTIVO);

        restEventoMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedEvento.getId())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(partialUpdatedEvento))
            )
            .andExpect(status().isOk());

        // Validate the Evento in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertEventoUpdatableFieldsEquals(partialUpdatedEvento, getPersistedEvento(partialUpdatedEvento));
    }

    @Test
    @Transactional
    void patchNonExistingEvento() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        evento.setId(longCount.incrementAndGet());

        // Create the Evento
        EventoDTO eventoDTO = eventoMapper.toDto(evento);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restEventoMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, eventoDTO.getId())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(eventoDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the Evento in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void patchWithIdMismatchEvento() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        evento.setId(longCount.incrementAndGet());

        // Create the Evento
        EventoDTO eventoDTO = eventoMapper.toDto(evento);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restEventoMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, longCount.incrementAndGet())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(eventoDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the Evento in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void patchWithMissingIdPathParamEvento() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        evento.setId(longCount.incrementAndGet());

        // Create the Evento
        EventoDTO eventoDTO = eventoMapper.toDto(evento);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restEventoMockMvc
            .perform(patch(ENTITY_API_URL).contentType("application/merge-patch+json").content(om.writeValueAsBytes(eventoDTO)))
            .andExpect(status().isMethodNotAllowed());

        // Validate the Evento in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void deleteEvento() throws Exception {
        // Initialize the database
        insertedEvento = eventoRepository.saveAndFlush(evento);

        long databaseSizeBeforeDelete = getRepositoryCount();

        // Delete the evento
        restEventoMockMvc
            .perform(delete(ENTITY_API_URL_ID, evento.getId()).accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isNoContent());

        // Validate the database contains one less item
        assertDecrementedRepositoryCount(databaseSizeBeforeDelete);
    }

    protected long getRepositoryCount() {
        return eventoRepository.count();
    }

    protected void assertIncrementedRepositoryCount(long countBefore) {
        assertThat(countBefore + 1).isEqualTo(getRepositoryCount());
    }

    protected void assertDecrementedRepositoryCount(long countBefore) {
        assertThat(countBefore - 1).isEqualTo(getRepositoryCount());
    }

    protected void assertSameRepositoryCount(long countBefore) {
        assertThat(countBefore).isEqualTo(getRepositoryCount());
    }

    protected Evento getPersistedEvento(Evento evento) {
        return eventoRepository.findById(evento.getId()).orElseThrow();
    }

    protected void assertPersistedEventoToMatchAllProperties(Evento expectedEvento) {
        assertEventoAllPropertiesEquals(expectedEvento, getPersistedEvento(expectedEvento));
    }

    protected void assertPersistedEventoToMatchUpdatableProperties(Evento expectedEvento) {
        assertEventoAllUpdatablePropertiesEquals(expectedEvento, getPersistedEvento(expectedEvento));
    }
}
