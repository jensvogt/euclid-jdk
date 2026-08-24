package de.jensvogt.euclid.dto.eam;

public record DeleteUserRequest(String userId) {

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private String userId;

        public Builder userId(String userId) {
            this.userId = userId;
            return this;
        }

        public DeleteUserRequest build() {
            return new DeleteUserRequest(userId);
        }
    }
}
