package com.westernacher

import com.google.gson.JsonElement
import com.google.gson.JsonParser
import com.google.gson.JsonPrimitive
import groovy.json.JsonSlurper
import groovy.transform.PackageScope
import org.slf4j.Logger
import org.slf4j.LoggerFactory

@PackageScope
class JsonUtils {

    private static final Logger LOG = LoggerFactory.getLogger(JsonUtils.class);

    private JsonUtils() {
    }

    public static JsonElement asJsonElement(String json) {
        try {
            return new JsonParser().parse(json);
        } catch (Exception e) {
            LOG.error("Malformed json: $json");
            throw new RuntimeException(e);
        }
    }

    public static boolean isEmpty(JsonElement jsonElement) {
        return jsonElement.isJsonNull() || (jsonElement.isJsonArray() && jsonElement.getAsJsonArray().size() == 0);
    }

    public static Object toPrimitive(JsonElement jsonElement) {
        if (jsonElement.isJsonNull()) {
            return null;
        }

        if (jsonElement.isJsonPrimitive()) {
            JsonPrimitive jsonPrimitive = jsonElement.getAsJsonPrimitive();

            if (jsonPrimitive.isString()) {
                return jsonPrimitive.getAsString();
            }

            if (jsonPrimitive.isBoolean()) {
                return jsonPrimitive.getAsBoolean();
            }

            if (jsonPrimitive.isNumber()) {
                return jsonPrimitive.getAsNumber();
            }
        }

        return jsonElement;
    }
}
