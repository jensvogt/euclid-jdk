package de.jensvogt.euclid.module.emo;

import de.jensvogt.euclid.dto.emo.Metric;
import io.micrometer.core.instrument.Clock;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.FunctionCounter;
import io.micrometer.core.instrument.FunctionTimer;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.LongTaskTimer;
import io.micrometer.core.instrument.Measurement;
import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.TimeGauge;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.distribution.DistributionStatisticConfig;
import io.micrometer.core.instrument.step.StepMeterRegistry;
import io.micrometer.core.instrument.util.NamedThreadFactory;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A Micrometer registry that publishes an application's meters to EMO.
 *
 * <p>This is the push half of euclid's monitoring: a meter registered here is measured by the
 * application, accumulated by Micrometer over a step, and handed to
 * {@link EuclidEmo#pushMetrics(String, List)} at the end of it - landing in the same bucket-aligned
 * rows EMO's own collectors write, and therefore in the same rollups, the same retention and the
 * same graphs as CPU, memory and the module gauges.
 *
 * <p><b>Why push rather than scrape.</b> An application euclid runs talks over the Unix socket the
 * manager gave it and need not serve HTTP at all; several of them do not. A scraper would reach
 * only the ones that happen to be web applications, and only while whichever configuration binds
 * {@code EUCLID_HTTP_PORT} is in effect. Pushing works for every application, needs no port, and
 * puts the decision about what is worth publishing in the application - which is the only place
 * that knows.
 *
 * <p><b>Cardinality is the caller's to control.</b> An out-of-the-box Spring Boot registry carries
 * a few hundred series, and {@code http.server.requests} alone is one per URI, method and status.
 * Every one of them becomes a stored row per step, so filter at the registry
 * ({@code registry.config().meterFilter(...)}) rather than discovering the volume afterwards.
 *
 * <p>Typical use:
 * <pre>{@code
 * EuclidMeterRegistry registry = new EuclidMeterRegistry(
 *         EuclidMeterRegistryConfig.of(Duration.ofMinutes(1)), Clock.SYSTEM, emo, "parser-dev");
 * registry.config().meterFilter(MeterFilter.acceptNameStartsWith("jvm."))
 *                  .meterFilter(MeterFilter.deny());
 * Metrics.addRegistry(registry);
 * }</pre>
 */
public final class EuclidMeterRegistry extends StepMeterRegistry {

    /**
     * java.util.logging, as everywhere else in this SDK: a client library has no business choosing
     * a logging framework for the application that embeds it.
     */
    private static final Logger LOG = Logger.getLogger(EuclidMeterRegistry.class.getName());

    /**
     * The EMO client the samples are pushed through.
     */
    private final EuclidEmo emo;

    /**
     * Which process is reporting, recorded with every batch. An application's own id is the useful
     * value - it is how a reader tells one pool's numbers from another's.
     */
    private final String module;

    /**
     * Creates a registry that publishes on the config's step.
     *
     * @param config  step and enablement; see {@link EuclidMeterRegistryConfig}
     * @param clock   the clock Micrometer measures steps against, normally {@link Clock#SYSTEM}
     * @param emo     the EMO client to push through, from an authenticated session
     * @param module  the reporting process, normally the application's own id
     */
    public EuclidMeterRegistry(EuclidMeterRegistryConfig config, Clock clock, EuclidEmo emo, String module) {
        super(config, clock);
        this.emo = Objects.requireNonNull(emo, "emo must not be null");
        this.module = Objects.requireNonNull(module, "module must not be null");
        // Its own thread, named after the registry: publishing is a network call on a step, and
        // running it on a shared pool makes a slow gateway somebody else's latency.
        start(new NamedThreadFactory("euclid-metrics-publisher"));
    }

    /**
     * Publishes one step's worth of meters.
     *
     * <p>Failure is logged by Micrometer and the step is dropped rather than retried: the next one
     * is along shortly, and a queue of stale samples would be published with the wrong timestamps.
     */
    @Override
    protected void publish() {

        List<Metric> batch = new ArrayList<>();
        for (Meter meter : getMeters()) {
            batch.addAll(meter.match(
                    this::fromGauge,
                    this::fromCounter,
                    this::fromTimer,
                    this::fromSummary,
                    this::fromLongTaskTimer,
                    this::fromTimeGauge,
                    this::fromFunctionCounter,
                    this::fromFunctionTimer,
                    this::fromMeter));
        }

        if (batch.isEmpty()) {
            return;
        }

        try {
            emo.pushMetrics(module, batch);
        } catch (InterruptedException e) {
            // Restored rather than swallowed: this runs on the registry's own publishing thread,
            // and a shutdown that interrupts it has to be able to stop it.
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            // Deliberately not rethrown. Publishing runs on a timer for the life of the process,
            // and a gateway that is briefly unreachable must not end the schedule - the next step
            // tries again with fresh values.
            LOG.log(Level.WARNING, e, () -> "Pushing metrics to EMO failed, module: " + module + ", metrics: " + batch.size());
        }
    }

    /**
     * Micrometer measures durations in nanoseconds internally; published in milliseconds, which is
     * what every other duration euclid records is in.
     */
    @Override
    protected TimeUnit getBaseTimeUnit() {
        return TimeUnit.MILLISECONDS;
    }

    /**
     * Histograms and percentiles are computed by Micrometer and published as the meters they
     * produce; this registry adds no client-side percentile machinery of its own, because a
     * percentile cannot be aggregated across buckets afterwards the way a mean or a max can.
     */
    @Override
    protected DistributionStatisticConfig defaultHistogramConfig() {
        return DistributionStatisticConfig.DEFAULT;
    }

    // ── Meter conversions ────────────────────────────────────────────────────
    // A gauge is a level, so it averages across a bucket; a counter is an accumulating quantity, so
    // it totals. Timers and summaries are published as the three figures that survive being rolled
    // up - count, total and max - rather than as a percentile, which does not.

    private List<Metric> fromGauge(Gauge gauge) {
        return finite(gauge.value())
                ? List.of(Metric.gauge(name(gauge), labels(gauge), gauge.value()))
                : List.of();
    }

    private List<Metric> fromTimeGauge(TimeGauge gauge) {
        final double value = gauge.value(getBaseTimeUnit());
        return finite(value) ? List.of(Metric.gauge(name(gauge), labels(gauge), value)) : List.of();
    }

    private List<Metric> fromCounter(Counter counter) {
        return List.of(Metric.rate(name(counter), labels(counter), counter.count()));
    }

    private List<Metric> fromFunctionCounter(FunctionCounter counter) {
        return finite(counter.count())
                ? List.of(Metric.rate(name(counter), labels(counter), counter.count()))
                : List.of();
    }

    private List<Metric> fromTimer(Timer timer) {
        final String name = name(timer);
        final Map<String, String> labels = labels(timer);
        return List.of(
                Metric.rate(name + ".count", labels, timer.count()),
                Metric.rate(name + ".total", labels, timer.totalTime(getBaseTimeUnit())),
                Metric.gauge(name + ".max", labels, timer.max(getBaseTimeUnit())));
    }

    private List<Metric> fromFunctionTimer(FunctionTimer timer) {
        final String name = name(timer);
        final Map<String, String> labels = labels(timer);
        if (!finite(timer.count()) || !finite(timer.totalTime(getBaseTimeUnit()))) {
            return List.of();
        }
        return List.of(
                Metric.rate(name + ".count", labels, timer.count()),
                Metric.rate(name + ".total", labels, timer.totalTime(getBaseTimeUnit())));
    }

    private List<Metric> fromSummary(DistributionSummary summary) {
        final String name = name(summary);
        final Map<String, String> labels = labels(summary);
        return List.of(
                Metric.rate(name + ".count", labels, summary.count()),
                Metric.rate(name + ".total", labels, summary.totalAmount()),
                Metric.gauge(name + ".max", labels, summary.max()));
    }

    private List<Metric> fromLongTaskTimer(LongTaskTimer timer) {
        final String name = name(timer);
        final Map<String, String> labels = labels(timer);
        return List.of(
                Metric.gauge(name + ".active", labels, timer.activeTasks()),
                Metric.gauge(name + ".duration", labels, timer.duration(getBaseTimeUnit())));
    }

    /**
     * A meter of a kind this registry does not know: published as whatever measurements it offers,
     * each suffixed with its statistic, so a custom meter is not silently dropped.
     */
    private List<Metric> fromMeter(Meter meter) {
        List<Metric> metrics = new ArrayList<>();
        for (Measurement measurement : meter.measure()) {
            if (!finite(measurement.getValue())) {
                continue;
            }
            final String name = name(meter) + "." + measurement.getStatistic().getTagValueRepresentation();
            metrics.add(new Metric(name, labels(meter), measurement.getValue(), typeOf(measurement)));
        }
        return metrics;
    }

    private static Metric.Type typeOf(Measurement measurement) {
        return switch (measurement.getStatistic()) {
            case COUNT, TOTAL, TOTAL_TIME -> Metric.Type.RATE;
            default -> Metric.Type.GAUGE;
        };
    }

    // ── Naming ───────────────────────────────────────────────────────────────

    private String name(Meter meter) {
        return meter.getId().getName();
    }

    /**
     * A meter's tags as EMO's dimensions, in the order Micrometer holds them - which is sorted, so
     * the same tag set always produces the same series.
     */
    private Map<String, String> labels(Meter meter) {
        Map<String, String> labels = new LinkedHashMap<>();
        for (Tag tag : meter.getId().getTagsAsIterable()) {
            labels.put(tag.getKey(), tag.getValue());
        }
        return labels;
    }

    /**
     * Whether a value is worth publishing. A gauge whose referent has been collected reads as NaN,
     * and a rate that has never been divided reads as infinity; neither is a measurement, and both
     * would poison the bucket's mean for every other sample in it.
     */
    private static boolean finite(double value) {
        return !Double.isNaN(value) && !Double.isInfinite(value);
    }
}
