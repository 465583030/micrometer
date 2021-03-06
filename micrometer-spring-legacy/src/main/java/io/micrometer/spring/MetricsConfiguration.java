/**
 * Copyright 2017 Pivotal Software, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.micrometer.spring;

import org.springframework.boot.autoconfigure.condition.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.TagFormatter;
import io.micrometer.spring.scheduling.MetricsSchedulingAspect;

/**
 * Metrics configuration for Spring 4/Boot 1.x
 *
 * @author Jon Schneider
 */
@Configuration
@Import({
        RestTemplateMetricsConfiguration.class,
        RecommendedMeterBinders.class,
        MeterRegistryConfigurationSupport.class
})
class MetricsConfiguration {
    @Bean
    @ConditionalOnMissingBean(TagFormatter.class)
    public TagFormatter tagFormatter() {
        return new TagFormatter() {};
    }

    @Configuration
    @ConditionalOnWebApplication
    @Import(InstrumentServletRequestConfiguration.class)
    static class WebMvcConfiguration {}

    /**
     * If AOP is not enabled, scheduled interception will not work.
     */
    @Bean
    @ConditionalOnClass(name = "org.aopalliance.intercept.Joinpoint")
    @ConditionalOnProperty(value = "spring.aop.enabled", havingValue = "true", matchIfMissing = true)
    public MetricsSchedulingAspect metricsSchedulingAspect(MeterRegistry registry) {
        return new MetricsSchedulingAspect(registry);
    }

    /**
     * If AOP is not enabled, client request interception will still work, but the URI tag
     * will always be evaluated to "none".
     */
    @Configuration
    @ConditionalOnClass(name = {"org.springframework.web.client.RestTemplate", "org.aopalliance.intercept.Joinpoint"})
    @ConditionalOnProperty(value = "spring.aop.enabled", havingValue = "true", matchIfMissing = true)
    static class MetricsRestTemplateAspectConfiguration {
        @Bean
        RestTemplateUrlTemplateCapturingAspect restTemplateUrlTemplateCapturingAspect() {
            return new RestTemplateUrlTemplateCapturingAspect();
        }
    }
}
