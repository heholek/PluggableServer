package com.gianlu.pluggableserver.core;

import com.gianlu.pluggableserver.api.ApiUtils;
import com.gianlu.pluggableserver.core.handlers.*;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import io.undertow.Undertow;
import io.undertow.server.RoutingHandler;
import org.apache.log4j.Logger;
import org.jetbrains.annotations.Nullable;

import java.io.*;

/**
 * @author Gianlu
 */
public class Core implements Components.SaveState {
    private static final Logger LOGGER = Logger.getLogger(Core.class);
    private final static JsonParser PARSER = new JsonParser();
    private final Undertow undertow;
    private final Components components;
    private final int port;
    private final String apiUrl;
    private final File stateFile;

    public Core(@Nullable String apiUrl, @Nullable String stateFile) throws IOException {
        this.apiUrl = CoreUtils.getEnv("API_URL", apiUrl);
        if (this.apiUrl == null)
            throw new IllegalArgumentException("Missing API URL!");

        this.stateFile = new File(CoreUtils.getEnv("STATE_FILE", stateFile));
        if (!this.stateFile.exists() && !this.stateFile.createNewFile())
            throw new IOException("Cannot create state file!");

        this.port = ApiUtils.getEnvPort(80);
        this.components = new Components(this);
        this.undertow = Undertow.builder()
                .addHttpListener(port, "0.0.0.0")
                .setHandler(components.handler())
                .build();

        addBaseApiHandlers();

        resumeFromState();
    }

    private void resumeFromState() {
        try (InputStream in = new FileInputStream(stateFile)) {
            JsonElement element = PARSER.parse(new InputStreamReader(in));
            if (element == null) {
                LOGGER.info("State file is empty!");
                return;
            }

            if (!element.isJsonArray()) {
                LOGGER.info("Corrupted state file: " + element);
                return;
            }

            for (JsonElement elm : element.getAsJsonArray())
                components.loadFromState(elm.getAsJsonObject());
        } catch (IOException ex) {
            LOGGER.fatal("Failed resuming state from file!", ex);
        }
    }

    @Override
    public void saveState() {
        try (OutputStream out = new FileOutputStream(stateFile)) {
            out.write(components.stateJson().toString().getBytes());
            LOGGER.info("State saved successfully!");
        } catch (IOException ex) {
            LOGGER.fatal("Failed saving state to file!", ex);
        }
    }

    private void addBaseApiHandlers() {
        RoutingHandler router = new RoutingHandler();
        router.get("/", new OkHandler())
                .get("/GenerateToken", new GenerateTokenHandler())
                .get("/ListComponents", new ListComponentsHandler(components))
                .get("/{domain}/SetConfig", new SetConfigHandler(components))
                .get("/{domain}/GetConfig", new GetConfigHandler(components))
                .get("/{domain}/StartComponent", new StartComponentHandler(components))
                .get("/{domain}/StopComponent", new StopComponentHandler(components))
                .put("/{domain}/UploadData", new UploadDataHandler(components))
                .put("/{domain}/UploadComponent", new UploadComponentHandler(components));

        components.addHandler(apiUrl, router);
        LOGGER.info(String.format("Loaded control API at %s.", apiUrl));
    }

    public void start() {
        LOGGER.info(String.format("Starting server on port %d!", port));
        undertow.start();
    }
}
