/*
 * Copyright 2012 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.gradle.api.internal

import spock.lang.Specification
import org.gradle.api.DynamicExtension

public class DynamicExtensionDynamicObjectAdapterTest extends Specification {

    DynamicExtension extension = new DefaultDynamicExtension()
    DynamicExtensionDynamicObjectAdapter adapter =  new DynamicExtensionDynamicObjectAdapter(extension)

    def "can get and set properties"() {
        given:
        extension.add("foo", "bar")

        expect:
        adapter.getProperty("foo") == "bar"

        when:
        adapter.setProperty("foo", "baz")

        then:
        adapter.getProperty("foo") == "baz"
        extension.foo == "baz"

        when:
        extension.foo = "bar"

        then:
        adapter.getProperty("foo") == "bar"
    }

    def "can get properties map"() {
        given:
        extension.add("p1", 1)
        extension.add("p2", 2)
        extension.add("p3", 3)

        expect:
        extension.properties == adapter.properties
    }

    def "has no methods"() {
        given:
        extension.add("foo") { }

        expect:
        !adapter.hasMethod("foo", "anything")

        and:
        !adapter.hasMethod("other")
    }
    
    def "getting or setting missing property throws MPE"() {
        when:
        adapter.getProperty("foo")
        
        then:
        thrown(MissingPropertyException)

        when:
        adapter.setProperty("foo", "bar")

        then:
        thrown(MissingPropertyException)
    }

    def "invoking method throws MME"() {
        when:
        adapter.invokeMethod("foo", "bar")

        then:
        thrown(groovy.lang.MissingMethodException)
    }
}
