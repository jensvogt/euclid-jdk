package de.jensvogt.euclid.dto.emo;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A metric carries as many dimensions as the meter it came from. What has to hold is that the
 * multi-dimensional form survives being built, that the single-label form still says what it always
 * did, and that a metric cannot be changed after the fact by whoever passed the map in.
 */
class MetricTest {

    @Test
    @DisplayName("every dimension is kept")
    void everyDimensionIsKept() {

        Metric metric = Metric.gauge("jvm.memory.used", Map.of("area", "heap", "id", "G1 Eden Space"), 4096);

        assertEquals(2, metric.labels().size());
        assertEquals("heap", metric.labels().get("area"));
        assertEquals("G1 Eden Space", metric.labels().get("id"));
        assertEquals(Metric.Type.GAUGE, metric.type());
    }

    @Test
    @DisplayName("the single-label form is one dimension, and reads back as one")
    void theSingleLabelFormIsOneDimension() {

        Metric metric = Metric.gauge("application-utilisation", "instance", "a0087f3a", 42.0);

        assertEquals(Map.of("instance", "a0087f3a"), metric.labels());
        assertEquals("instance", metric.labelName());
        assertEquals("a0087f3a", metric.labelValue());
    }

    @Test
    @DisplayName("a metric with no dimensions has none, rather than one called \"\"")
    void aMetricWithNoDimensionsHasNone() {

        Metric blank = new Metric("queue-depth", "", "", 3.0, Metric.Type.GAUGE);
        Metric missing = new Metric("queue-depth", null, null, 3.0, Metric.Type.GAUGE);

        assertTrue(blank.labels().isEmpty());
        assertTrue(missing.labels().isEmpty());
        assertEquals("", blank.labelName());
        assertEquals("", blank.labelValue());
    }

    @Test
    @DisplayName("labelName is the first dimension in key order, whatever order they arrived in")
    void labelNameIsStable() {

        Map<String, String> unordered = new LinkedHashMap<>();
        unordered.put("status", "200");
        unordered.put("method", "GET");

        Metric metric = Metric.rate("http.requests", unordered, 7);

        // "method" sorts before "status" - the same answer EMO's own labelName() gives the row, so
        // a reader that only handles one dimension sees the same one on both sides.
        assertEquals("method", metric.labelName());
        assertEquals("GET", metric.labelValue());
    }

    @Test
    @DisplayName("the labels are copied, so the caller's map cannot change a metric afterwards")
    void theLabelsAreCopied() {

        Map<String, String> labels = new LinkedHashMap<>();
        labels.put("area", "heap");

        Metric metric = Metric.gauge("jvm.memory.used", labels, 4096);
        labels.put("area", "nonheap");

        assertEquals("heap", metric.labels().get("area"));
        assertThrows(UnsupportedOperationException.class, () -> metric.labels().put("id", "Metaspace"));
    }

    @Test
    @DisplayName("a null label map is empty rather than a null to trip over later")
    void aNullLabelMapIsEmpty() {
        assertTrue(new Metric("cpu", null, 1.0, Metric.Type.GAUGE).labels().isEmpty());
    }
}
