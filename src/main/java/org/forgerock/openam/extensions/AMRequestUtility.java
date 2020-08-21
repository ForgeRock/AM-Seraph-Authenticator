package org.forgerock.openam.extensions;


import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;

import org.apache.log4j.Logger;
import org.h2.util.StringUtils;
import org.jetbrains.annotations.Nullable;

import com.google.gson.Gson;
import com.google.gson.internal.LinkedTreeMap;

import okhttp3.Call;
import okhttp3.ConnectionPool;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * @author Frank Gasparovic
 */
public class AMRequestUtility {

    private static final Logger log = Logger.getLogger(AMRequestUtility.class);
    private static final String ACCEPT_API_VERSION = "Accept-API-Version";
    private static final String ACCEPT_API_VERSION_VALUE = "resource=2.1, protocol=1.0";
    private static final String APPLICATION_JSON = "application/json";
    private static AMRequestUtility requestUtility = null;
    private final Gson gson;
    private final RequestBody body;
    private final String amCookieName;
    private final String amBaseUri;
    private final OkHttpClient httpClient;
    private final Properties properties;


    private AMRequestUtility() {
        this.gson = new Gson();
        this.body = RequestBody.create("", MediaType.parse(APPLICATION_JSON));
        properties = new Properties();
        try {
            InputStream stream = getClass().getClassLoader().getResourceAsStream("/AMConfig.properties");
            properties.load(stream);
            stream.close();
        } catch (Throwable e) {
            String message = "Error while loading property file";
            log.error(message);
            throw new RuntimeException(message, e);
        }
        this.amCookieName = loadProperty("amCookieName");
        this.amBaseUri = loadProperty("amBaseUri");
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(Long.parseLong(loadProperty("connectTimeout")), TimeUnit.MILLISECONDS)
                .readTimeout(Long.parseLong(loadProperty("readTimeout")), TimeUnit.MILLISECONDS)
                .connectionPool(new ConnectionPool(Integer.parseInt(loadProperty("maxIdleConnections")),
                                                   Long.parseLong(loadProperty("keepAliveDuration")),
                                                   TimeUnit.MILLISECONDS))
                .build();
    }

    public static AMRequestUtility getInstance() {
        if (requestUtility == null) {
            requestUtility = new AMRequestUtility();
        }
        return requestUtility;
    }

    public String loadProperty(String key) {
        String property = properties.getProperty(key);
        if (property == null) {
            String
                    message =
                    "No property for key '" + key +
                            "' found. Make sure there is a AMConfig.properties file with this key in the classpath.";
            log.error(message);
            throw new IllegalArgumentException(message);
        }
        return property;
    }


    String getToken(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();

        if (cookies == null) {
            if (log.isDebugEnabled()) {
                log.debug("No cookies for user");
            }
            return null;
        }

        Optional<Cookie> forgeRockCookie = Arrays.stream(cookies).filter(
                cookie -> StringUtils.equals(cookie.getName(), amCookieName))
                                                 .findFirst();

        if (log.isDebugEnabled() && !forgeRockCookie.isPresent()) {
            log.debug("ForgeRock SSO Cookie is not present in request");
        }

        return forgeRockCookie.map(Cookie::getValue).orElse(null);
    }

    void AMAuthenticate(String userName) {
        log.error("found user=" + userName + " and password");
    }


    @Nullable
    String obtainUsername(HttpServletRequest request) {
        String token = getToken(request);
        if (token == null) {
            log.error("Token is null");
            return null;
        }

        return validateResponse(Objects.requireNonNull(parseResponse(sessionValidationRequest(token), token)), token);
    }


    @Nullable
    String validateResponse(LinkedTreeMap mapResponse, String token) {
        if (mapResponse.containsKey("valid") && Boolean.TRUE == mapResponse.get("valid") && mapResponse.containsKey(
                "uid")) {
            return (String) mapResponse.get("uid");
        }
        log.error("Unable to validate the session and get uid of user");
        if (log.isDebugEnabled()) {
            log.debug("Token is: " + token);
        }
        return null;
    }

    @Nullable
    LinkedTreeMap parseResponse(Response response, String token) {
        try {
            return gson.fromJson(Objects.requireNonNull(response.body()).string(), LinkedTreeMap.class);
        } catch (IOException | NullPointerException e) {
            log.error("Error thrown when trying to parse the response from AM session validation", e);
            if (log.isDebugEnabled()) {
                log.debug("For token: " + token);
            }
        }
        return null;
    }

    @Nullable
    Response sessionValidationRequest(String token) {
        Request sessionValidationRequest = new Request.Builder()
                .header(ACCEPT_API_VERSION, ACCEPT_API_VERSION_VALUE)
                .header(amCookieName, token)
                .url(amBaseUri + "/json/realms/root/sessions?_action=validate")
                .post(body)
                .build();

        Call call = this.httpClient.newCall(sessionValidationRequest);
        try {
            return call.execute();
        } catch (IOException e) {
            log.error("Exception thrown when trying to validate the AM session", e);
            if (log.isDebugEnabled()) {
                log.debug("For token: " + token);
            }
            return null;
        }
    }


}
