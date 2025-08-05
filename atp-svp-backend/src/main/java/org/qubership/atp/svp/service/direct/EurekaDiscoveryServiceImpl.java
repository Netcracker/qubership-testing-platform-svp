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

package org.qubership.atp.svp.service.direct;

import java.util.List;
import java.util.stream.Collectors;

import org.qubership.atp.svp.service.EurekaDiscoveryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.stereotype.Service;

@Service
public class EurekaDiscoveryServiceImpl implements EurekaDiscoveryService {

    @Value("${service.pod-name}" + ":${spring.application.name}" + ":${server.port}")
    private String podName;

    private DiscoveryClient discoveryClient;

    @Autowired
    public EurekaDiscoveryServiceImpl(DiscoveryClient discoveryClient) {
        this.discoveryClient = discoveryClient;
    }

    @Override
    public List<String> getAllPods() {
        return discoveryClient.getInstances("atp-svp").stream()
                .map(instance -> instance.getInstanceId())
                .collect(Collectors.toList());
    }

    @Override
    public boolean isPresentPod(String name) {
        return getAllPods().contains(name);
    }

    @Override
    public String getCurrentPodName() {
        return this.podName;
    }
}
