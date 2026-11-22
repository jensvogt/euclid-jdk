package de.jensvogt.euclid.dto.eap;

/**
 * Request to give one instance slot of an application's pool to a worker node, by hand.
 * <p>
 * Slots are named the way the manager names them rather than by application: the pool is the
 * application's runtime name ({@link de.jensvogt.euclid.dto.eap.model.Application#runtimeName()}),
 * and the slot is the instance ID an endpoint or a node's listing reports. Both are required.
 *
 * @param runtimeName the pool the slot belongs to - the application's runtime name, not its ID
 * @param instanceId  the slot to assign
 * @param node        the registered node to give it to, or empty to take it back to the manager's
 *                    own host
 */
public record AssignInstanceRequest(String runtimeName, String instanceId, String node) {

    /**
     * Creates a new instance of the Builder for constructing an AssignInstanceRequest object.
     *
     * @return a new Builder instance for constructing AssignInstanceRequest.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for constructing {@link AssignInstanceRequest} instances.
     */
    public static final class Builder {

        /**
         * Creates an empty builder.
         */
        public Builder() {
        }

        /**
         * The pool the slot belongs to.
         */
        private String runtimeName;

        /**
         * The slot to assign.
         */
        private String instanceId;

        /**
         * The node to give it to; empty takes it back to the manager.
         */
        private String node = "";

        /**
         * Sets the pool the slot belongs to.
         *
         * @param runtimeName the application's runtime name - what the module list calls the pool,
         *                    which differs from its application ID
         * @return the builder instance
         */
        public Builder runtimeName(String runtimeName) {
            this.runtimeName = runtimeName;
            return this;
        }

        /**
         * Sets the slot to assign.
         *
         * @param instanceId the instance ID, as an endpoint reports it
         * @return the builder instance
         */
        public Builder instanceId(String instanceId) {
            this.instanceId = instanceId;
            return this;
        }

        /**
         * Sets the node the slot is given to.
         * <p>
         * It has to be registered to this account, and not drained: draining refuses exactly this,
         * a new assignment. Left empty, the slot goes back to the manager's own host, which is a
         * legitimate thing to ask for and the way to undo an assignment.
         *
         * @param node the node's name, or empty for the manager's own host
         * @return the builder instance
         */
        public Builder node(String node) {
            this.node = node;
            return this;
        }

        /**
         * Builds and returns a new instance of AssignInstanceRequest using the properties set on the Builder.
         *
         * @return a new AssignInstanceRequest instance.
         */
        public AssignInstanceRequest build() {
            return new AssignInstanceRequest(runtimeName, instanceId, node);
        }
    }
}
