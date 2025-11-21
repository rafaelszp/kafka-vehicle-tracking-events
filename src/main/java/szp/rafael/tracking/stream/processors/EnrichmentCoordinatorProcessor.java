package szp.rafael.tracking.stream.processors;

import com.github.f4b6a3.ulid.UlidCreator;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import org.apache.kafka.streams.processor.PunctuationType;
import org.apache.kafka.streams.processor.api.Processor;
import org.apache.kafka.streams.processor.api.ProcessorContext;
import org.apache.kafka.streams.processor.api.Record;
import org.apache.kafka.streams.state.KeyValueIterator;
import org.apache.kafka.streams.state.KeyValueStore;
import org.eclipse.microprofile.config.ConfigProvider;
import org.jboss.logging.Logger;
import szp.rafael.tracking.client.api.AlertEnrichmentClient;
import szp.rafael.tracking.client.api.RouteEnrichmentClient;
import szp.rafael.tracking.client.impl.FakeAlertClient;
import szp.rafael.tracking.client.impl.FakeRouteClient;
import szp.rafael.tracking.client.model.ApiResponse;
import szp.rafael.tracking.model.helper.CompletedResult;
import szp.rafael.tracking.model.store.AlertCacheValue;
import szp.rafael.tracking.model.store.PendingEntry;
import szp.rafael.tracking.model.store.RouteCacheValue;
import szp.rafael.tracking.model.tracking.EnrichedTrackingEvent;
import szp.rafael.tracking.model.tracking.TrackingEvent;
import szp.rafael.tracking.stream.serializers.GsonSerde;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;

public class EnrichmentCoordinatorProcessor implements Processor<String, TrackingEvent, String, EnrichedTrackingEvent> {


    private static final Logger log = Logger.getLogger(EnrichmentCoordinatorProcessor.class.getName());

    // CONFIGURÁVEIS
    private final int MAX_ATTEMPTS;
    private final long BASE_BACKOFF_MS;
    private final Duration httpTimeout;
    private final Duration punctuateInterval;
    private final ExecutorService executor;

    // State stores
    private KeyValueStore<String, RouteCacheValue> routeCacheStore;
    private KeyValueStore<String, AlertCacheValue> alertCacheStore;
    private KeyValueStore<String, PendingEntry> pendingStore;
    private KeyValueStore<String, EnrichedTrackingEvent> orderBufferStore;

    // clients
    private final RouteEnrichmentClient routeClient;
    private final AlertEnrichmentClient alertClient;

    // internal queue for completed async responses (drained in stream thread)
    private final ConcurrentLinkedQueue<CompletedResult> completedResponses = new ConcurrentLinkedQueue<>();

    private ProcessorContext<String, EnrichedTrackingEvent> context;

    private final Gson gson = GsonSerde.build();

    // Constructor with sensible defaults; in production inject via DI or params
    public EnrichmentCoordinatorProcessor(ExecutorService executor) {
        this(executor,
                3,                  // MAX_ATTEMPTS
                60_000L,            // BASE_BACKOFF_MS = 1 minute
                Duration.ofMinutes(5), // HTTP timeout
                Duration.ofSeconds(5)  // punctuate every 5s
        );
    }

    public EnrichmentCoordinatorProcessor(ExecutorService executor,
                                          int maxAttempts,
                                          long baseBackoffMs,
                                          Duration httpTimeout,
                                          Duration punctuateInterval) {
        this.executor = executor;
        this.MAX_ATTEMPTS = maxAttempts;
        this.BASE_BACKOFF_MS = baseBackoffMs;
        this.httpTimeout = httpTimeout;
        this.punctuateInterval = punctuateInterval;
        this.routeClient = new FakeRouteClient();   // substitua por implementação real/configurada
        this.alertClient = new FakeAlertClient();   // substitua por implementação real/configurada
    }

    @Override
    public void init(ProcessorContext<String, EnrichedTrackingEvent> context) {
        this.context = context;
        // recuperar stores pelo nome (devem estar adicionados na Topology)
        this.routeCacheStore = context.getStateStore(getStateStoreName("route_cache_store"));
        this.alertCacheStore = context.getStateStore(getStateStoreName("alert_cache_store"));
        this.pendingStore = context.getStateStore(getStateStoreName("pending_requests_store"));
        this.orderBufferStore = context.getStateStore(getStateStoreName("order_buffer_store"));

        // schedule punctuator
        this.context.schedule(punctuateInterval, PunctuationType.WALL_CLOCK_TIME, this::punctuate);

        log.info("EnrichmentCoordinatorProcessor initialized. Punctuate interval: " + punctuateInterval);
    }

    @Override
    public void process(Record<String, TrackingEvent> record) {
        if (record == null || record.value() == null) return;

        String placa = record.key();
        if (placa == null || placa.isEmpty()) {
            placa = normalizePlate(record.value().getPlaca());
        }

        TrackingEvent event = record.value();
        // Key for pending + caches uses placa + eventTime to allow multiple events by placa
        String pendingKey = pendingKey(placa, event.getEventTime());

        // Se já existe um cache completo para esse evento (route e alert), não criamos pending.
        String cacheKey = cacheKey(placa, event.getEventTime());
        RouteCacheValue rcv = routeCacheStore.get(cacheKey);
        AlertCacheValue acv = alertCacheStore.get(cacheKey);

        if (rcv != null && acv != null) {
            // already enriched => forward downstream (ou colocar no orderBuffer dependendo do fluxo)
            // Para simplicidade: cria final e coloca no order buffer (OutputFormatter emitirá na ordem)
            EnrichedTrackingEvent enriched = EnrichedTrackingEvent.from(event, rcv, acv);
            forwardAfterEnrich(placa, enriched);
            return;
        }

        // caso não haja pending, cria
        PendingEntry pending = pendingStore.get(pendingKey);
        if (pending == null) {
            pending = new PendingEntry();
            pending.setOriginalEvent(serializeEvent(event));
            pending.setStage(isRouteMissing(rcv) ? PendingEntry.Stage.ROUTE_ENRICH : PendingEntry.Stage.ALERT_ENRICH);
            pending.setAttempts(1);
            pending.setNextRetryAtMillis(System.currentTimeMillis()); // pronto para ser executado imediatamente pelo punctuator
            pending.setTraceId(event.getTraceId());
            pending.setCreatedAtMillis(System.currentTimeMillis());
            pendingStore.put(pendingKey, pending);
            // métricas: pending++ (implementar Micrometer)
        } else {
            // update: se event data mudou, substitua originalEvent (opcional)
            pending.setOriginalEvent(serializeEvent(event));
            pendingStore.put(pendingKey, pending);
        }
    }

    private void punctuate(long timestamp) {
        try {
            // 1) processar completed responses enfileiradas pelos executors
            drainCompletedResponses();

            // 2) scan pendingStore e despachar tarefas prontas
            dispatchPendingTasks();
        } catch (Exception ex) {
            log.error("Erro no punctuate: " + ex.getMessage(), ex);
        }
    }

    private void drainCompletedResponses() {
        CompletedResult result;
        while ((result = completedResponses.poll()) != null) {
            handleCompletedResultInStreamThread(result);
        }
    }

    //‘Buffer’ de ordenação por timestamp
    private void forwardAfterEnrich(String placa, EnrichedTrackingEvent enriched) {
        EnrichedTrackingEvent previousEnrichment = orderBufferStore.get(placa);
        if (previousEnrichment != null) return; // ja foi enviado, naos precisa enviar outra vez
        orderBufferStore.put(placa, enriched);
        Record<String, EnrichedTrackingEvent> record = new Record<>(placa, enriched, enriched.getProcessedAtMillis());
        context.forward(record);
        orderBufferStore.delete(placa);
    }


    /**
     * Aqui pegamos os resultados pendentes completados com sucesso ou falha:
     * - Sucesso
     * a) Se o Stage for ROUTE_ENRICH(route), atualiza routeCache e avança para ALERT_ENRICH
     * b) Se o Stage for ALERT_ENRICH (alert), atualiza alertCache e finaliza: cria enriched e coloca no buffer de ordenação
     * - Falha
     * a) Se o Stage for ROUTE_ENRICH (route), atualiza pending e decide se deve ser despachado imediatamente ou aguardar
     * b) Se o Stage for ALERT_ENRICH (alert), atualiza pending e decide se deve ser despachado imediatamente ou aguardar
     * A intenção é fazer o Enrich baseado nos resultados de chamadas de API aos endpoints de route e alert.
     * Os resultados são colocados na fila completedResponses sendo processados na stream thread.
     */
    private void handleCompletedResultInStreamThread(CompletedResult result) {
        String pendingKey = result.getPendingKey();
        PendingEntry pending = pendingStore.get(pendingKey);
        if (pending == null) {
            return;
        }

        TrackingEvent originalEvent = deserializeEvent(pending.getOriginalEvent());

        if (result.getStage() == PendingEntry.Stage.ROUTE_ENRICH) {
            if (result.isSuccess()) {
                // grava route cache
                RouteCacheValue routeValue = result.getRouteValue();
                routeValue.setAttempts(pending.getAttempts());
                routeCacheStore.put(cacheKey(originalEvent.getPlaca(), originalEvent.getEventTime()), routeValue);
                // avançar para ALERT_ENRICH
                pending.setStage(PendingEntry.Stage.ALERT_ENRICH); //proxima etapa do enriquecimento
                pending.setNextRetryAtMillis(System.currentTimeMillis());
                pending.setAttempts(1);
                pendingStore.put(pendingKey, pending);
            } else {
                // falha: incrementar tentativas e decidir
                handleFailureInStreamThread(pending, pendingKey, result.getErrorMessage());
            }
        } else { // ALERT_ENRICH
            if (result.isSuccess()) {
                // grava alert cache
                AlertCacheValue alertValue = result.getAlertValue();
                alertValue.setAttempts(pending.getAttempts());
                alertCacheStore.put(cacheKey(originalEvent.getPlaca(), originalEvent.getEventTime()), alertValue);
                // finaliza: criar enriched e colocar no buffer de ordenação
                EnrichedTrackingEvent enriched = EnrichedTrackingEvent.from(
                        originalEvent,
                        routeCacheStore.get(cacheKey(originalEvent.getPlaca(), originalEvent.getEventTime())),
                        alertValue);
                forwardAfterEnrich(originalEvent.getPlaca(), enriched);
                pendingStore.delete(pendingKey);
            } else {
                handleFailureInStreamThread(pending, pendingKey, result.getErrorMessage());
            }
        }
    }

    /*
     * Aqui está a rotina dos retries: atualiza pending e decide se deve ser despachado imediatamente ou aguardar
     * baseado no tempo de espera (exponencial) e número de tentativas.
     * */
    private void handleFailureInStreamThread(PendingEntry pending, String pendingKey, String error) {
        pending.setLastError(error + " | Attempts: "+ pending.getAttempts());
        pending.setAttempts(pending.getAttempts() + 1);
        if (pending.getAttempts() >= MAX_ATTEMPTS) { //valores padrao caso estoure o limite de tentativas
            // permanent failure
            TrackingEvent originalEvent = deserializeEvent(pending.getOriginalEvent());
            /*Uma possível melhoria seria chamar os 2 serviços ao mesmo tempo, mas para fins de simplicidade vamos
            * manter ordem: rota -> alerta
            *
            * Outro fator aqui é que estamos considerando enviar a rota esperada para que o sistema de alerta possa
            * fazer a averiguação*/
            if (pending.getStage() == PendingEntry.Stage.ALERT_ENRICH) {
                // set default NONE with error flag in alert cache and finish
                AlertCacheValue defaultAlert = AlertCacheValue.defaultNoneWithError(error);
                defaultAlert.setAttempts(pending.getAttempts());
                alertCacheStore.put(cacheKey(originalEvent.getPlaca(), originalEvent.getEventTime()), defaultAlert);

                RouteCacheValue routeVal = routeCacheStore.get(cacheKey(originalEvent.getPlaca(), originalEvent.getEventTime()));
                EnrichedTrackingEvent enriched = EnrichedTrackingEvent.from(originalEvent, routeVal, defaultAlert);
                forwardAfterEnrich(originalEvent.getPlaca(), enriched);
                pendingStore.delete(pendingKey);
            } else {
                // ROUTE_ENRICH permanent fail: write empty route with error and advance to alert stage
                RouteCacheValue absentRoute = RouteCacheValue.defaultEmptyWithError(error);
                routeCacheStore.put(cacheKey(originalEvent.getPlaca(), originalEvent.getEventTime()), absentRoute);

                // advance to alert stage and reset attempts to 0
                pending.setStage(PendingEntry.Stage.ALERT_ENRICH);
                pending.setAttempts(1);
                pending.setNextRetryAtMillis(System.currentTimeMillis());
                pendingStore.put(pendingKey, pending);
            }
        } else {
            // schedule retry with exponential backoff
            int base = pending.getAttempts() - 1;
            long backoff = Math.max(BASE_BACKOFF_MS * (1L <<  Math.max(base, 0)),60_000); // 1,2,4 * base
            pending.setNextRetryAtMillis(System.currentTimeMillis() + backoff);
            pendingStore.put(pendingKey, pending);
        }
    }

    /**
     * Aqui é onde os executors despacham as tarefas de API de route e alert.
     * 1. Primeiro varremos a pendingStore, onde guardamos as requisições pendentes.
     * 2. Para cada requisição, verificamos se já passou o tempo de espera para ela ser executada.
     * 3. Se sim, criamos uma task para o executor e a submetemos para execução.
     * 4. A task é responsável por fazer a chamada à API e atualizar o cache correspondente.
     * <p>
     * O executor é o mecanismo da plataforma Java para execução assíncrona de tarefas. Ele irá obedecer o thread pool
     * informado na parametrização do constructor desta classe.
     */
    private void dispatchPendingTasks() {
        long now = System.currentTimeMillis();
        try (KeyValueIterator<String, PendingEntry> it = pendingStore.all()) { //Varrendo os registros pendentes
            List<String> keysToDispatch = new ArrayList<>();
            while (it.hasNext()) {
                var kv = it.next();
                String key = kv.key;
                PendingEntry pe = kv.value;
                if (pe == null) continue;
                if (pe.getNextRetryAtMillis() <= now) { //Verificação baseada no tempo.
                    keysToDispatch.add(key);
                }
            }
            /**
             * Processa entradas pendentes submetendo tarefas assíncronas para enriquecimento
             * de eventos via chamadas de API (rotas ou alertas) e coleta os resultados em uma
             * fila de respostas completadas.
             */
            for (String key : keysToDispatch) {
                PendingEntry pe = pendingStore.get(key);
                if (pe == null) continue;
                // increase attempts pre-emptively? melhor aumentar só na falha; manter as attempts no handler
                // submit to executor a task with the necessary snapshot
                TrackingEvent eventSnapshot = deserializeEvent(pe.getOriginalEvent());
                PendingEntry.Stage stage = pe.getStage();
                String traceId = pe.getTraceId();
                // create task
                executor.submit(() -> {
                    try {
                        if (stage == PendingEntry.Stage.ROUTE_ENRICH) {
                            ApiResponse<RouteCacheValue> resp = routeClient.fetchRoute(eventSnapshot, traceId, httpTimeout);
                            CompletedResult cr = CompletedResult.forRoute(key, resp.isSuccess(), resp.getValue(), resp.getError(),pe.getAttempts());
                            completedResponses.add(cr);
                        } else {
                            ApiResponse<AlertCacheValue> resp = alertClient.fetchAlert(eventSnapshot, traceId, httpTimeout);
                            CompletedResult cr = CompletedResult.forAlert(key, resp.isSuccess(), resp.getValue(), resp.getError(),pe.getAttempts());
                            completedResponses.add(cr);
                        }
                    } catch (Exception ex) {
                        // executor thread: capture exception and enqueue as failure
                        CompletedResult cr = CompletedResult.forFailure(key, stage, ex.getMessage(),pe.getAttempts());
                        completedResponses.add(cr);
                    }
                });
            }
        }
    }


    //HELPERS
    private boolean isRouteMissing(RouteCacheValue rcv) {
        return rcv == null;
    }

    private String normalizePlate(String raw) {
        if (raw == null) return null;
        return raw.trim().toUpperCase();
    }

    private String cacheKey(String placa, long eventTime) {
        return placa + "::" + eventTime;
    }

    private String pendingKey(String placa, long eventTime) {
        return "PENDING::" + placa + "::" + eventTime;
    }

    private byte[] serializeEvent(TrackingEvent e) {
        return gson.toJson(e).getBytes();
    }

    private TrackingEvent deserializeEvent(byte[] bytes) {
        try {
            return gson.fromJson(new String(bytes), TrackingEvent.class);
        } catch (JsonSyntaxException ex) {
            log.warn("Erro ao desserializar TrackingEvent: " + ex.getMessage());
            return null;
        }
    }

    private String getStateStoreName(String storeSuffix) {
        String preffix = "app.config.kafka";
        return ConfigProvider.getConfig().getValue(preffix + "." + storeSuffix,String.class);
    }

}
