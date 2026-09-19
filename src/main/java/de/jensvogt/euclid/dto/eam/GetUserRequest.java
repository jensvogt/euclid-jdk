package de.jensvogt.euclid.dto.eam;

/**
 * Request for one user, by the id they are known by.
 * <p>
 * The user id rather than the ERN, because that is what everything else names a user with: a
 * grant's principal, an application's technical identity, the audit trail's userId column.
 *
 * @param userId the user's id
 */
public record GetUserRequest(String userId) {

    /**
     * Creates a new instance of the Builder for constructing a GetUserRequest object.
     *
     * @return a new Builder instance for constructing GetUserRequest.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for constructing {@link GetUserRequest} instances.
     */
    public static final class Builder {

        /**
         * Creates an empty builder.
         */
        public Builder() {
        }

        /**
         * The user's id.
         */
        private String userId;

        /**
         * Sets the id of the user.
         *
         * @param userId the user's id
         * @return the builder instance
         */
        public Builder userId(String userId) {
            this.userId = userId;
            return this;
        }

        /**
         * Builds and returns a new instance of GetUserRequest using the properties set on the Builder.
         *
         * @return a new GetUserRequest instance.
         */
        public GetUserRequest build() {
            return new GetUserRequest(userId);
        }
    }
}
