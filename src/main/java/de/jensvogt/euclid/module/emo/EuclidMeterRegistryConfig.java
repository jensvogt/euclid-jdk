package de.jensvogt.euclid.module.emo;

import io.micrometer.core.instrument.step.StepRegistryConfig;

import java.time.Duration;
import java.util.Objects;

/**
 * How often {@link EuclidMeterRegistry} publishes, and whether it publishes at all.
 *
 * <p>The step is the only setting that really matters, and it should be read together with EMO's
 * own averaging period ({@code euclid.modules.emo.average-period}, five minutes by default). EMO
 * accumulates whatever arrives within a period into one row per series, so a step well under that
 * costs rows nowhere and only buys a shorter wait before a value is reflected; a step longer than
 * it leaves periods with no sample at all, which reads as a gap rather than as a flat line.
 *
 * <p>Implemented as an interface with defaults, the way every Micrometer registry config is, so an
 * application that keeps its settings somewhere else can implement {@link #get(String)} and have
 * them honoured.
 */
public interface EuclidMeterRegistryConfig extends StepRegistryConfig {

    /**
     * The prefix its properties are read under, e.g. {@code euclid.step}.
     *
     * @return the configuration prefix
     */
    @Override
    default String prefix() {
        return "euclid";
    }

    /**
     * How long a step is. One minute by default: short enough that a value is never much older
     * than EMO's own averaging period, long enough that publishing is not a per-second network
     * call.
     *
     * @return the step duration
     */
    @Override
    default Duration step() {
        return Duration.ofMinutes(1);
    }

    /**
     * A config that reads nothing and simply uses the given step.
     *
     * @param step how long a step is
     * @return the configuration
     */
    static EuclidMeterRegistryConfig of(Duration step) {
        Objects.requireNonNull(step, "step must not be null");
        return new EuclidMeterRegistryConfig() {

            /**
             * Nothing is read from anywhere: every value is either this config's own or the
             * interface default.
             */
            @Override
            public String get(String key) {
                return null;
            }

            @Override
            public Duration step() {
                return step;
            }
        };
    }

    /**
     * The default configuration: publish every minute.
     */
    EuclidMeterRegistryConfig DEFAULT = key -> null;
}
