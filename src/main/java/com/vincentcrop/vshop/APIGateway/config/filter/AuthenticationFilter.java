package com.vincentcrop.vshop.APIGateway.config.filter;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;

import com.vincentcrop.vshop.APIGateway.model.authenticator.Role;
import com.vincentcrop.vshop.APIGateway.model.authenticator.Route;
import com.vincentcrop.vshop.APIGateway.model.authenticator.User;
import com.vincentcrop.vshop.APIGateway.service.AuthenticatorService;
import com.vincentcrop.vshop.APIGateway.util.Http.HttpResponseThrowers;

import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@RefreshScope
@Component
@Slf4j
public class AuthenticationFilter implements GatewayFilter {
    protected final String DEFAULT_ROLE_OWNER = "OWNER";
    protected final String DEFAULT_ROLE_CO_OWNER = "CO-OWNER";

    @Autowired
    protected AuthenticatorService authenticatorService;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
    
        try {
            ServerHttpRequest request = exchange.getRequest();

            String requestMethod = request.getMethod().name();
            String path = request.getURI().getPath();

            final String token = getBearerJwt(request);
            User user = this.authenticatorService.getUser(token);

            if (!isValidRoute(path, requestMethod, user)) {
                if (this.isAuthMissing(request))
                    return this.onError(exchange, "Authorization header is missing in request", HttpStatus.UNAUTHORIZED);

                return this.onError(exchange, "Forbidden", HttpStatus.FORBIDDEN);
            }

            this.populateRequestWithHeaders(exchange, user);
            return chain.filter(exchange);
        } catch (ResponseStatusException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error(ex.getMessage(), ex);
            HttpResponseThrowers.throwServerError("server is experiencing some error");
        }
        
        this.populateRequestWithHeaders(exchange, null);
        return chain.filter(exchange);
    }

    private int routeCompare(Route a, Route b) {
        // int splitLength = Integer.compare(b.getPath().split("/").length,
        // a.getPath().split("/").length);
        int charLength = Integer.compare(b.getPath().length(), a.getPath().length());

        // return splitLength > charLength ? splitLength : charLength;
        return charLength;
    }

    /**
     * this function will check if path is valid with these criteria in order
     * 1. exact path get check first
     * 2. long path get match first
     * 3. wild card path get match last
     * 
     * @param path          to be match
     * @param requestMethod to be match
     * @param user          login user
     * @return true if a path match is found else false
     */
    public boolean isValidRoute(String path, String requestMethod, User user) {
        // return true if this is super user
        if (isSuperPowerUser(user))
            return true;

        // sort the list in length desc
        List<Route> routes = this.authenticatorService.getAllRoute();
        routes = routes.parallelStream().sorted((a, b) -> routeCompare(a, b)).collect(Collectors.toList());

        // exact path match check
        AtomicBoolean matchCriteria = new AtomicBoolean(false);
        boolean anyMatchExact = routes.parallelStream().anyMatch(e -> {
            String ePath = e.getPath();
            boolean anyMatchExactI = requestMethod.equals(e.getMethod()) && ePath.equals(path);

            if (anyMatchExactI && !e.isSecure())
                matchCriteria.set(true);
            else if (anyMatchExactI && !ObjectUtils.isEmpty(user))
                matchCriteria.set(!e.isSecure() || anyMatchRole(e.getRoles(), user.getUserRoles()));

            return anyMatchExactI;
        });

        if (anyMatchExact)
            return matchCriteria.get();

        // match path check
        matchCriteria.set(false);
        boolean anyMatch = routes.stream().anyMatch(e -> {
            String ePath = e.getPath();
            Boolean match = requestMethod.equals(e.getMethod()) && Pattern.matches("^" + ePath, path);

            if (match && !e.isSecure())
                matchCriteria.set(true);
            else if (match && !ObjectUtils.isEmpty(user))
                matchCriteria.set(anyMatchRole(e.getRoles(), user.getUserRoles()));

            return match;
        });

        if (anyMatch)
            return matchCriteria.get();

        // if(ObjectUtils.isEmpty(user) || !user.isEnable())
        // return false;

        return false;
    }

    public boolean isSuperPowerUser(User user) {
        return !ObjectUtils.isEmpty(user) && user.getUserRoles().parallelStream()
                .anyMatch(r -> r.getName().equals(DEFAULT_ROLE_OWNER) || r.getName().equals(DEFAULT_ROLE_CO_OWNER));
    }

    public boolean anyMatchRole(List<Role> originRole, List<Role> targetRole) {
        if (originRole == null || targetRole == null)
            return false;

        return originRole.parallelStream().anyMatch(or -> targetRole.parallelStream().anyMatch(tr -> tr.equals(or)));
    }

    /**
     * check if path is a secure path
     * 
     * @deprecated
     *             this method is no longer use due to inaccurate checking
     *             <p>
     *             use {@link #isValidRoute(String, String, User) isValidRoute}
     *             instead
     * @param path          to be match
     * @param requestMethod to be match
     * @param routes        to be match with
     * @return true if a path match is found else false
     */
    @Deprecated
    public boolean isSecure(String path, String requestMethod, List<Route> routes) {
        return routes.parallelStream().noneMatch(e -> {
            String ePath = e.getPath();
            return e.isSecure() && requestMethod.equals(e.getMethod()) && Pattern.matches("^" + ePath, path);
        });
    }

    public Mono<Void> onError(ServerWebExchange exchange, String err, HttpStatus httpStatus) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(httpStatus);
        return response.setComplete();
    }

    public static String getBearerJwt(ServerHttpRequest request) {
        String auth = Optional.ofNullable(request.getHeaders().getFirst("Authorization"))
                              .or(() -> Optional.ofNullable(request.getHeaders().getFirst("authorization")))
                              .orElse(null);

        String jwt = Optional.ofNullable(request.getQueryParams().getFirst("Authorization"))
                             .or(() -> Optional.ofNullable(request.getQueryParams().getFirst("authorization")))
                             .or(() -> Optional.ofNullable(request.getQueryParams().getFirst("jwt")))
                             .or(() -> Optional.ofNullable(request.getQueryParams().getFirst("Jwt")))
                             .orElse(null);

        String token = Optional.ofNullable(request.getQueryParams().getFirst("Token"))
                               .or(() -> Optional.ofNullable(request.getQueryParams().getFirst("token")))
                               .orElse(null);

        if(auth != null)
            return auth;

        if(jwt != null) {
            if(!jwt.toLowerCase().startsWith("bearer "))
                jwt = "Bearer " + jwt;
            return jwt;
        }

        if(token != null) {
            if(!token.toLowerCase().startsWith("token "))
                token = "Token " + token;
            return token;
        }

        return null;
    }

    public static String extractJwt(String Authorization) {
        String[] tokens = Authorization.split(" ");
        String token = null;
        if (tokens.length > 1)
            token = tokens[tokens.length - 1];

        return token;
    }

    public boolean isAuthMissing(ServerHttpRequest request) {
        return !request.getHeaders().containsKey("Authorization");
    }

    public void populateRequestWithHeaders(ServerWebExchange exchange, User user) {
        String id = "";
        if(!ObjectUtils.isEmpty(user))
            id = user.getId() + "";

        exchange.getRequest().mutate()
                .header("user_id", id)
                .build();
    }
}
