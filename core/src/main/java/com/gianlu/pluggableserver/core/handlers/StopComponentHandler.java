package com.gianlu.pluggableserver.core.handlers;

import com.gianlu.pluggableserver.core.Applications;
import io.undertow.server.HttpServerExchange;
import org.jetbrains.annotations.NotNull;

/**
 * @author Gianlu
 */
public class StopComponentHandler extends AuthenticatedHandlerWithAppAndComponentId {
    private final Applications applications;

    public StopComponentHandler(@NotNull Applications applications) {
        this.applications = applications;
    }

    @Override
    public void handleAuthenticated(@NotNull HttpServerExchange exchange, @NotNull String appId, @NotNull String componentId) {
        applications.stopComponent(appId, componentId);
        exchange.getResponseSender().send("OK");
    }
}
