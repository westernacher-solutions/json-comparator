package com.westernacher

import spock.lang.Specification
import spock.lang.Subject

@Subject(JsonComparator)
class JsonComparatorSpec extends Specification {

    def "Comparing equal json objects"() {

        when: "a simple json object is compared with itself"
        def result = JsonComparator.compareJson(expected, actual)

        then: "they are considered equal"
        result == true

        where: "sample data is"
        expected                       | actual
        '{ firstName : "John"}'        | '{ firstName : "John"}'
        '[{first: null, extra: null}]' | '[{first: null, extra: null}]'
    }

    def "Comparing json objects with a property that is empty array/null"() {

        given: "two json objects, one with an empty array property and the other with null for it"
        def expected = '{ firstName : []}'
        def actual = '{ firstName : null}'

        when: "they are compared"
        def result = JsonComparator.compareJson(expected, actual, mode: mode)

        then: "the result is what we expect"
        result == expectedResult

        where: "comparator modes are"
        mode                       | expectedResult
        JsonComparator.Mode.Soft | true
        JsonComparator.Mode.Hard | false
    }

    def "Comparing a json object with a property with an empty value to another object with a missing such"() {

        when: "two json objects, one with an extra property that has no value are compared"
        def result = JsonComparator.compareJson(expected, actual, mode: mode)

        then: "the result is what we expect"
        result == expectedResult

        where: "comparator modes are"
        expected                              | actual                | mode                       | expectedResult
        '{ firstName : null, lastName: null}' | '{ firstName : null}' | JsonComparator.Mode.Soft | true
        '{ firstName : null, lastName: []}'   | '{ firstName : null}' | JsonComparator.Mode.Hard | false
    }

    def "Comparing null value to null or missing is correct"() {

        when: "is being compared with itself"
        def result = JsonComparator.compareJson(expected, actual, asserts: asserts)

        then: "the result is positive"
        result == expectedResult

        where:
        expected                 | actual                               | asserts                   | expectedResult
        '{first: null}'          | '{first: null, extra: null}'         | [extra: { true }]         | true
        '{first: null}'          | '{first: null, extra: null}'         | [extra: true]             | false
        '{first: null}'          | '{first: null, extra: 1}'            | [:]                       | false
        '{first : {name: null}}' | '{first: {name: null, extra: 1}}'    | [:]                       | false
        '{first : {name: null}}' | '{first: {name: null, extra: null}}' | ['first.extra': { true }] | true
        '{e: [{first: null}]}'   | '{e: [{first: null, extra: 1}]}'     | [:]                       | false
        '{e: [{first: null}]}'   | '{e: [{first: null, extra: null}]}'  | [e: { it.size() == 1 }]   | true

    }

    def "Comparing not equal json objects"() {

        when: "two not equal json objects are compared"
        def result = JsonComparator.compareJson(expected, actual)

        then: "the result is negative"
        result == false

        where:
        expected                                                                                    | actual
        '{ firstName: "John" }'                                                                     | '{ firstName: "Bon" }'
        '{address: { streetName: "Dawning street" }}'                                               | '{address: { streetName: "Downing street" }}'
        '{aliases: [ "johnny", "jo" ]}'                                                             | '{aliases: [ "bonny", "bo" ]}'
        '{participants: [ { firstName : "Pol", representatives: [ { lastName: "Mccartney" } ] } ]}' | '{participants: [ { firstName : "John", representatives: [ { lastName: "Lennon" } ] } ]}'
        '{array: [ {} ]}'                                                                           | '{array: []}'
        '{object: { }}'                                                                             | '{object: "x"}'
    }

    def "Comparing malformed json throws an exception"() {

        given: "a rotten json"
        def json = '{ firstName : "John"'

        when: "it is compared with some other json object"
        JsonComparator.compareJson(json, '{ firstName : "John" }')

        then: "the result is obvious"
        thrown(RuntimeException)
    }
}
