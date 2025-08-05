/*
 * Copyright 2024-2025 NetCracker Technology Corporation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is provided "AS IS", without warranties
 * or conditions of any kind, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.qubership.atp.svp;

import org.qubership.atp.auth.springbootstarter.security.oauth2.client.config.annotation.EnableM2MRestTemplate;
import org.qubership.atp.auth.springbootstarter.security.oauth2.client.config.annotation.EnableOauth2FeignClientInterceptor;
import org.qubership.atp.crypt.config.annotation.AtpCryptoEnable;
import org.qubership.atp.crypt.config.annotation.AtpDecryptorEnable;
import org.qubership.atp.integration.configuration.configuration.MdcInterceptorsHelperConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.web.servlet.DispatcherServletAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.ServletWebServerFactoryAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.WebMvcAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.context.ApplicationPidFileWriter;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.Import;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(scanBasePackages = {
        "org.qubership.atp.svp",
        "org.qubership.atp.common.probes.controllers"
})
@Configuration
@EnableFeignClients(basePackages = {"org.qubership.atp.integration.configuration.feign",
        "org.qubership.atp.svp.repo.feign"})
@EnableScheduling
@EnableAspectJAutoProxy
@EnableDiscoveryClient
@EnableM2MRestTemplate
@EnableOauth2FeignClientInterceptor
@AtpDecryptorEnable
@AtpCryptoEnable
@Import({
        WebMvcAutoConfiguration.class,
        DispatcherServletAutoConfiguration.class,
        ServletWebServerFactoryAutoConfiguration.class,
        MdcInterceptorsHelperConfiguration.class
})
public class Main {

    /**
     * Setup spring application and run it.
     * @param args args
     */
    public static void main(String[] args) {
        SpringApplicationBuilder app = new SpringApplicationBuilder(Main.class);
        app.build().addListeners(
                new ApplicationPidFileWriter("application.pid")
        );
        app.run(args);
    }
}
