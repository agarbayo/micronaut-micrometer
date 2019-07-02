/*
 * Copyright 2017-2019 original authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.micronaut.configuration.metrics.micrometer.graphite

import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.composite.CompositeMeterRegistry
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import io.micrometer.graphite.GraphiteMeterRegistry
import io.micronaut.configuration.metrics.micrometer.MeterRegistryCreationListener
import io.micronaut.context.ApplicationContext
import io.micronaut.context.env.Environment
import spock.lang.Specification
import spock.lang.Unroll

import java.time.Duration

import static io.micronaut.configuration.metrics.micrometer.MeterRegistryFactory.MICRONAUT_METRICS_ENABLED
import static io.micronaut.configuration.metrics.micrometer.graphite.GraphiteMeterRegistryFactory.GRAPHITE_CONFIG
import static io.micronaut.configuration.metrics.micrometer.graphite.GraphiteMeterRegistryFactory.GRAPHITE_ENABLED

class GraphiteMeterRegistryFactorySpec extends Specification {

    def "wireup the bean manually"() {
        given:
        Environment mockEnvironment = Stub()
        mockEnvironment.getProperty(_, _) >> Optional.empty()


        when:
        GraphiteMeterRegistryFactory factory = new GraphiteMeterRegistryFactory(new GraphiteConfigurationProperties(mockEnvironment))

        then:
        factory.graphiteMeterRegistry()
    }

    void "verify GraphiteMeterRegistry is created by default when this configuration used"() {
        when:
        ApplicationContext context = ApplicationContext.run()

        then:
        context.getBeansOfType(MeterRegistry).size() == 2
        context.getBeansOfType(MeterRegistry)*.class*.simpleName.containsAll(['CompositeMeterRegistry', 'GraphiteMeterRegistry'])
    }

    void "verify CompositeMeterRegistry created by default"() {
        when:
        ApplicationContext context = ApplicationContext.run()
        CompositeMeterRegistry compositeRegistry = context.getBean(CompositeMeterRegistry)

        then:
        context.getBean(MeterRegistryCreationListener)
        context.getBean(GraphiteMeterRegistry)
        compositeRegistry
        compositeRegistry.registries.size() == 1
        compositeRegistry.registries*.class.containsAll([GraphiteMeterRegistry])
    }

    @Unroll
    void "verify GraphiteMeterRegistry bean exists = #result when config #cfg = #setting"() {
        when:
        ApplicationContext context = ApplicationContext.run([(cfg): setting])

        then:
        context.findBean(GraphiteMeterRegistry).isPresent() == result

        where:
        cfg                       | setting | result
        MICRONAUT_METRICS_ENABLED | false   | false
        MICRONAUT_METRICS_ENABLED | true    | true
        GRAPHITE_ENABLED          | true    | true
        GRAPHITE_ENABLED          | false   | false
    }

    void "verify GraphiteMeterRegistry bean exists with default config"() {
        when:
        ApplicationContext context = ApplicationContext.run([(GRAPHITE_ENABLED): true])
        Optional<GraphiteMeterRegistry> meterRegistry = context.findBean(GraphiteMeterRegistry)

        then:
        meterRegistry.isPresent()
        meterRegistry.get().config.enabled()
        meterRegistry.get().config.port() == 2004
        meterRegistry.get().config.host() == "localhost"
        meterRegistry.get().config.step() == Duration.ofMinutes(1)
    }

    void "verify GraphiteMeterRegistry bean exists changed port, host and step"() {
        when:
        ApplicationContext context = ApplicationContext.run([
                (GRAPHITE_ENABLED)         : true,
                (GRAPHITE_CONFIG + ".host"): "127.0.0.1",
                (GRAPHITE_CONFIG + ".port"): 2345,
                (GRAPHITE_CONFIG + ".step"): "PT2M",
        ])
        Optional<GraphiteMeterRegistry> meterRegistry = context.findBean(GraphiteMeterRegistry)

        then:
        meterRegistry.isPresent()
        meterRegistry.get().config.enabled()
        meterRegistry.get().config.port() == 2345
        meterRegistry.get().config.host() == "127.0.0.1"
        meterRegistry.get().config.step() == Duration.ofMinutes(2)
    }
}
