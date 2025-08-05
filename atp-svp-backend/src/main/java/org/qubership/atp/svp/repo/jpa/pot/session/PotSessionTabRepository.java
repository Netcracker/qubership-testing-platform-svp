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

package org.qubership.atp.svp.repo.jpa.pot.session;

import java.util.Set;
import java.util.UUID;

import org.qubership.atp.svp.core.enums.ValidationStatus;
import org.qubership.atp.svp.model.db.pot.session.PotSessionTabEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface PotSessionTabRepository extends JpaRepository<PotSessionTabEntity, UUID> {

    @Query(value = "select distinct validation_status  from pot_session_tab pst where pot_session_page_id =?1 and "
            + "validation_status != '1' and  validation_status != '3'", nativeQuery = true)
    Set<ValidationStatus> getImpactingValidationStatus(UUID pageId);

}
