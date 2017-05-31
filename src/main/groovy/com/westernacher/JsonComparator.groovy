package com.westernacher

import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonPrimitive
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import static com.westernacher.JsonUtils.*

final class JsonComparator {

    private JsonComparator() {}

    static enum Mode {
        Hard, Soft
    }

    static final Logger LOG = LoggerFactory.getLogger(JsonComparator.class)

    public static ignoredKeys = [

    ]

    private static class JsonComparatorHelper {

        private Mode mode
        private List<String> ignoredKeys
        private Map assertionRules

        JsonComparatorHelper(Mode mode, List<String> ignoredKeys, Map assertionRules) {
            this.mode = mode
            this.ignoredKeys = ignoredKeys
            this.assertionRules = assertionRules
        }

        boolean compare(JsonElement expected, JsonElement actual, path = "") {

            if (mode == Mode.Soft && isNullOrEmpty(expected) && isNullOrEmpty(actual)) {
                return true
            }

            if (mode == Mode.Hard && actual == null && expected != null) {
                return false
            }

            if (expected && actual && expected.class != actual.class) {
                LOG.error("Elements $expected and $actual at path $path have different types!")
                return false
            }

            switch (expected.class) {

                case JsonObject:
                    def expectedMatchesActual = expected.asJsonObject.entrySet().collect { it ->
                        def currentPath = path + "." + it.key

                        if (existsRuleFor(currentPath)) {
                            return areAllRulesSatisfied(actual.asJsonObject.get(it.key), currentPath)
                        }

                        if (!shouldBeProcessed(it.key)) {
                            return true
                        }

                        return compare(it.value, actual.asJsonObject.get(it.key), currentPath)
                    }.every { it }

                    def actualMatchesExpected = actual.asJsonObject.entrySet()
                            .findAll({ !expected.asJsonObject.has(it.key) }) // properties that are not in the expected json
                            .collect {
                        def currentPath = path + "." + it.key

                        if (!existsRuleFor(currentPath)) {
                            def shouldBeIgnored = isEmpty(it.value) || !shouldBeProcessed(it.key)
                            if (!shouldBeIgnored) {
                                LOG.error("Property from response at path $currentPath has value ${it.value} but is not expected/asserted!")
                            }
                            return shouldBeIgnored
                        }

                        return areAllRulesSatisfied(it.value, currentPath)
                    }.every { it }

                    return expectedMatchesActual && actualMatchesExpected

                case JsonArray:
                    if (mode == Mode.Soft && isEmpty(actual) && isEmpty(expected)) {
                        return true
                    }

                    if (expected.asJsonArray.size() != actual.asJsonArray.size()) {
                        LOG.error("Arrays at path $path have different sizes! Expected: " + expected.asJsonArray.size() + " | Actual: " + actual.asJsonArray.size())
                        return false
                    }

                    return [expected.asJsonArray.toList(), actual.asJsonArray.toList()].transpose().indexed().collect { idx, elems ->
                        compare(elems[0], elems[1], path + "." + idx)
                    }.every { it }

                case JsonPrimitive:
                    if (expected != actual) {
                        LOG.error("Expected value $expected at path $path but got $actual")
                        return false
                    }
                    return true

                default:
                    return (mode == Mode.Soft && !actual) || (actual == expected)
            }
        }

        private existsRuleFor(String path) {
            assertionRules.any { path =~ ~/$it.key/ }
        }

        private boolean areAllRulesSatisfied(JsonElement actual, String path) {
            assertionRules.findAll({ path =~ ~/$it.key/ }).collect {
                def ruleIsSatisfied = it.value in Closure ? it.value(toPrimitive(actual)) : it.value == toPrimitive(actual)
                if (!ruleIsSatisfied) {
                    LOG.error("Rule for path $path is not satisfied. Actual value is $actual")
                }
                ruleIsSatisfied
            }.every { it }
        }

        private boolean shouldBeProcessed(String key) {
            !ignoredKeys.contains(key)
        }

        private static boolean isNullOrEmpty(JsonElement element) {
            // TODO consider primitive types (e.g. empty string)
            return !element ||
                    element.isJsonNull() ||
                    (element.isJsonArray() && element.asJsonArray.size() == 0)
        }
    }

    static boolean compareJson(params = [:], String expected, String actual) {
        def mode = params['mode'] ?: Mode.Soft
        def ignore = params['ignored'] ?: ignoredKeys
        def ignoreExtra = params['ignoreExtra'] ?: []
        def rules = params['asserts'] ?: [:]

        new JsonComparatorHelper(mode, ignore + ignoreExtra, rules).compare(asJsonElement(expected), asJsonElement(actual))
    }

}
