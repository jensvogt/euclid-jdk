package de.jensvogt.euclid.dto.emo.model;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Mirrors EMO's {@code get-dashboard} response: one of the caller's own saved dashboard layouts.
 * <p>
 * EMO does not interpret the layout. It stores whatever JSON object it was given and hands it back
 * as JSON, so the layout's shape belongs to whoever drew it - the RUI, or an application keeping
 * its own views. It is therefore read as a plain map of what Jackson makes of JSON: nested objects
 * as maps, arrays as lists, numbers, strings, booleans and nulls as themselves.
 *
 * @param name      the dashboard's name, unique among the caller's own dashboards
 * @param dashboard the layout exactly as it was saved, in its original key order
 * @param created   when it was first saved, ISO 8601 - kept across re-saves, so a layout edited
 *                  every day still says how old it is
 * @param modified  when it was last saved, ISO 8601
 */
public record Dashboard(String name, Map<String, Object> dashboard, String created, String modified) {

    /**
     * Copies the layout's top level and turns null into an empty map, so the record's map is never
     * null and cannot be changed through it. Not {@link Map#copyOf(Map)}, which refuses the null
     * values a JSON layout may well hold.
     */
    public Dashboard {
        dashboard = dashboard == null ? Map.of() : Collections.unmodifiableMap(new LinkedHashMap<>(dashboard));
    }
}
