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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.qubership.atp.svp.core.exceptions.pot.PotGenerationZipException;
import org.qubership.atp.svp.model.db.pot.session.PotSessionEntity;
import org.qubership.atp.svp.model.db.pot.session.PotSessionPageEntity;
import org.qubership.atp.svp.model.db.pot.session.PotSessionParameterEntity;
import org.qubership.atp.svp.model.db.pot.session.PotSessionTabEntity;
import org.qubership.atp.svp.model.pot.PotFile;
import org.qubership.atp.svp.model.pot.values.AbstractValueObject;
import org.qubership.atp.svp.repo.jpa.pot.session.PotSessionPageRepository;
import org.qubership.atp.svp.service.PotGenerationEngine;
import org.qubership.atp.svp.service.PotGenerator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class PotGenerationEngineImpl implements PotGenerationEngine {

    private final String potNameFormat = "POT_%s_%s";
    private final SimpleDateFormat potDateFormat = new SimpleDateFormat("yyyyMMddHHmmss");

    private PotGenerator potGenerator;
    private MetricsService metricsService;

    private final PotSessionPageRepository potSessionPageRepository;

    /**
     * The PotSessionPageRepository constructor.
     */
    @Autowired
    public PotGenerationEngineImpl(PotGenerator potGenerator,
                                   PotSessionPageRepository potSessionPageRepository,
                                   MetricsService metricsService) {
        this.potGenerator = potGenerator;
        this.potSessionPageRepository = potSessionPageRepository;
        this.metricsService = metricsService;
    }

    @Override
    public PotFile generatePot(PotSessionEntity session) {
        metricsService.incrementPotRequestCounter(session.getExecutionConfiguration().getProjectId());
        log.info("POT generating started");
        boolean isFullInfoNeededInPot = session.getExecutionConfiguration().getIsFullInfoNeededInPot();
        byte[] potFile = potGenerator.generatePotFile(session, isFullInfoNeededInPot);
        log.info("POT word file was generated");
        String potName = generateName(session);
        return isFullInfoNeededInPot ? getZipPotReport(session, potName, potFile)
                : getDocxPotReport(potName, potFile);
    }

    private PotFile getDocxPotReport(String potName, byte[] potFile) {
        log.info("POT Generating successfully ended");
        return new PotFile(potName + ".docx", potFile);
    }

    private PotFile getZipPotReport(PotSessionEntity session, String potName,
                                    byte[] potFile) {
        try {
            log.info("ZIP generating started");
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ZipOutputStream zos = new ZipOutputStream(baos);
            writeZipEntry(zos, potName + ".docx", potFile);

            List<PotSessionPageEntity> pages =
                    potSessionPageRepository.findByPotSessionEntitySessionId(session.getSessionId());

            for (PotSessionPageEntity page : pages) {
                for (PotSessionTabEntity tab : page.getPotSessionTabs()) {
                    for (PotSessionParameterEntity parameter : tab.getPotSessionParameterEntities()) {
                        for (AbstractValueObject arValue : parameter.getArValues()) {
                            if (arValue.getValueAsFile() != null) {
                                writeZipEntry(zos, arValue.getValueAsFile().getName(),
                                        arValue.getValueAsFile().getBytes());
                            }
                        }
                    }
                }
            }
            zos.close();
            log.info("ZIP POT generating with full uploaded info successfully ended");
            return new PotFile(potName + ".zip", baos.toByteArray());
        } catch (IOException e) {
            log.error("I/O error when trying to create ZIP file with POT", e);
            throw new PotGenerationZipException();
        }
    }

    private void writeZipEntry(ZipOutputStream zos, String entryName, byte[] bytes) throws IOException {
        ZipEntry potEntry = new ZipEntry(entryName);
        zos.putNextEntry(potEntry);
        zos.write(bytes, 0, bytes.length);
        zos.closeEntry();
    }

    private String generateName(PotSessionEntity session) {
        return String.format(potNameFormat, session.getExecutionConfiguration().getEnvironment().getName(),
                        potDateFormat.format(new Date()))
                .replaceAll(" ", "_")
                .replaceAll(":", "_");
    }
}
