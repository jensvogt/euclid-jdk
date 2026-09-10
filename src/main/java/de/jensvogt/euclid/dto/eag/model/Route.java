package de.jensvogt.euclid.dto.eag.model;

import java.util.List;

/**
 * Mirrors {@code Euclid::Database::Entity::EAG::Route} from the Euclid server: one resource the API
 * gateway serves - a path, and the application or euclid module behind it.
 *
 * <p>The gateway routes by configuration rather than by convention. A caller asks for
 * {@code /resource/searchById?id=123} and has no idea which application answers it - that is what a
 * route says, and it is why the application's name never appears in the URL. The same arrangement
 * lets an application be renamed, replaced or split across several routes without anything that
 * calls it having to change.
 *
 * <p>The route says nothing about <em>where</em> the application is. Its instances come and go as
 * the autoscaler grows and shrinks the pool, and their ports are assigned when they start, so the
 * gateway reads them at the moment it needs one rather than recording them here.
 *
 * @param routeId       the name this route is managed under, unique across the installation. It
 *                      never appears in a URL
 * @param ern           the route's ERN
 * @param accountId     the account the route belongs to
 * @param region        the region requests carried by this route act in
 * @param namespace     the namespace requests carried by this route act in, and which listener
 *                      carries it. A route naming none is carried by every listener
 * @param path          the path prefix this route answers for. Matched as a prefix on whole
 *                      segments, so {@code /resource} carries everything beneath it but never
 *                      claims {@code /resource-intern}; where two routes match, the longer wins
 * @param applicationId the application that serves this path, as EAP knows it, or empty for a
 *                      module route
 * @param moduleTarget  the euclid module this route reaches instead of an application, e.g.
 *                      {@code "eam"}, or empty for an ordinary route
 * @param moduleAction  the one action {@code moduleTarget} answers for on this route, e.g.
 *                      {@code "login"}. One per route deliberately: a route that read the action
 *                      out of the remaining path would publish every action the module has,
 *                      including the ones that delete users
 * @param methods       the HTTP methods this route answers for, upper case. Empty means all of
 *                      them - and keeps meaning all, so a route written before PATCH existed does
 *                      not quietly refuse it
 * @param authentication what a caller must present before a request is forwarded
 * @param active        whether the gateway serves this route at all. A route can be taken out of
 *                      service without being deleted, and put back knowing it returns exactly as
 *                      it was
 * @param created       creation timestamp
 * @param modified      last-modified timestamp
 */
public record Route(String routeId, String ern, String accountId, String region, String namespace, String path,
                    String applicationId, String moduleTarget, String moduleAction, List<String> methods,
                    RouteAuthentication authentication, boolean active, String created, String modified) {

    /**
     * Whether this route reaches a euclid module rather than an application euclid runs. The two
     * are exclusive - a route names one or the other, never both.
     *
     * @return {@code true} if the route names a module target
     */
    public boolean isModuleRoute() {
        return moduleTarget != null && !moduleTarget.isEmpty();
    }
}
