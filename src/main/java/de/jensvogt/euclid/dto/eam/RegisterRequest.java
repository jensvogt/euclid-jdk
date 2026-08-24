package de.jensvogt.euclid.dto.eam;

public record RegisterRequest(String userId, String password, String email, String accountId, String region, boolean isAdmin) {

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        /**
         * Creates an empty builder.
         */
        public Builder() {
        }

        private String userId;
        private String password;
        private String email;
        private String accountId;
        private String region;
        private boolean isAdmin;

        public Builder userId(String userId) {
            this.userId = userId;
            return this;
        }

        public Builder password(String password) {
            this.password = password;
            return this;
        }

        public Builder email(String email) {
            this.email = email;
            return this;
        }

        public Builder accountId(String accountId) {
            this.accountId = accountId;
            return this;
        }

        public Builder region(String region) {
            this.region = region;
            return this;
        }

        public Builder isAdmin(boolean isAdmin) {
            this.isAdmin = isAdmin;
            return this;
        }

        public RegisterRequest build() {
            return new RegisterRequest(userId, password, email, accountId, region, isAdmin);
        }
    }
}
