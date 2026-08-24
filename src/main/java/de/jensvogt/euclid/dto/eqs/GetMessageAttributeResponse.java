package de.jensvogt.euclid.dto.eqs;

import de.jensvogt.euclid.dto.eqs.model.Variant;

public record GetMessageAttributeResponse(String messageId, String name, Variant value) {

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private String messageId;
        private String name;
        private Variant value;

        public Builder messageId(String messageId) {
            this.messageId = messageId;
            return this;
        }

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder value(Variant value) {
            this.value = value;
            return this;
        }

        public GetMessageAttributeResponse build() {
            return new GetMessageAttributeResponse(messageId, name, value);
        }
    }
}
