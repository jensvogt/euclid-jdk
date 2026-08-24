package de.jensvogt.euclid.dto.eqs;

public record GetQueueMetadataResponse(String region, String accountId, String owner, String nameSpace,
                                        String name, String ern, long size, long messages) {

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        /**
         * Creates an empty builder.
         */
        public Builder() {
        }

        private String region;
        private String accountId;
        private String owner;
        private String nameSpace;
        private String name;
        private String ern;
        private long size;
        private long messages;

        public Builder region(String region) {
            this.region = region;
            return this;
        }

        public Builder accountId(String accountId) {
            this.accountId = accountId;
            return this;
        }

        public Builder owner(String owner) {
            this.owner = owner;
            return this;
        }

        public Builder nameSpace(String nameSpace) {
            this.nameSpace = nameSpace;
            return this;
        }

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder ern(String ern) {
            this.ern = ern;
            return this;
        }

        public Builder size(long size) {
            this.size = size;
            return this;
        }

        public Builder messages(long messages) {
            this.messages = messages;
            return this;
        }

        public GetQueueMetadataResponse build() {
            return new GetQueueMetadataResponse(region, accountId, owner, nameSpace, name, ern, size, messages);
        }
    }
}
