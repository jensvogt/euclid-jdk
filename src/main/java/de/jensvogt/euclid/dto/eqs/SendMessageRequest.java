package de.jensvogt.euclid.dto.eqs;

import de.jensvogt.euclid.dto.eqs.model.Variant;

import java.util.LinkedHashMap;
import java.util.Map;

public record SendMessageRequest(String ern, String body, Map<String, Variant> attributes, String priority) {

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private String ern;
        private String body;
        private Map<String, Variant> attributes = new LinkedHashMap<>();
        private String priority = "MIDDLE";

        public Builder ern(String ern) {
            this.ern = ern;
            return this;
        }

        public Builder body(String body) {
            this.body = body;
            return this;
        }

        public Builder attributes(Map<String, Variant> attributes) {
            this.attributes = attributes;
            return this;
        }

        public Builder priority(String priority) {
            this.priority = priority;
            return this;
        }

        public SendMessageRequest build() {
            return new SendMessageRequest(ern, body, attributes, priority);
        }
    }
}
