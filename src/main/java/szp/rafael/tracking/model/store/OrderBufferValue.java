package szp.rafael.tracking.model.store;

import szp.rafael.tracking.model.tracking.EnrichedTrackingEvent;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.NavigableSet;
import java.util.TreeSet;

/**
 * Mantém um buffer ordenado de eventos de rastreamento enriquecidos usando uma estrutura NavigableSet.
 * Os eventos são ordenados pelo timestamp (eventTime) e podem ser acessados ou removidos em lote.
 * Esta classe é utilizada para armazenar temporariamente eventos processados mantendo sua ordem cronológica.
 */

public class OrderBufferValue implements Serializable {

    //veja: https://www.geeksforgeeks.org/java/navigableset-java-examples/
    //Baseado nisso foi tomada a decisão de usar essa forma de "buffering"
    private final NavigableSet<EnrichedTrackingEvent> ordered = new TreeSet<>(Comparator.comparingLong(e -> e.getOriginal().getEventTime()));

    public void add(EnrichedTrackingEvent e) {
        ordered.add(e);
    }

    public List<EnrichedTrackingEvent> drainAll() {
        List<EnrichedTrackingEvent> all = new ArrayList<>(ordered);
        ordered.clear();
        return all;
    }

    public List<EnrichedTrackingEvent> peekAll() {
        return new ArrayList<>(ordered);
    }

    public EnrichedTrackingEvent peekEarliest() {
        return ordered.first();
    }

    /**
     * Remove earliest element
     */
    public void removeEarliest() {
        ordered.pollFirst();
    }

    public boolean isEmpty() {
        return ordered.isEmpty();
    }
}
