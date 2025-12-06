package com.function.common;

public class HttpConstants {
    
     private HttpConstants() {}

    public static final String CONTENT_TYPE = "Content-Type";
    public static final String APPLICATION_JSON = "application/json";

    public static final String ERROR_MISSING_AUTH =
        "{\"error\":\"Missing or malformed Authorization header\"}";

    public static final String ERROR_INVALID_AUTH =
        "{\"error\":\"Invalid service token\"}";

}
