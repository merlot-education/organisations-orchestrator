/*
 *  Copyright 2024 Dataport. All rights reserved. Developed as part of the MERLOT project.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package eu.merloteducation.organisationsorchestrator.repositories;

import eu.merloteducation.modelslib.api.organization.MembershipClass;
import eu.merloteducation.organisationsorchestrator.models.entities.OrganizationMetadata;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface OrganizationMetadataRepository extends JpaRepository<OrganizationMetadata, String> {
    List<OrganizationMetadata> findByMembershipClass(MembershipClass membershipClass);

    @Query("SELECT orgaId FROM OrganizationMetadata metadata WHERE metadata.membershipClass = :membershipClass")
    List<String> getOrgaIdByMembershipClass(@Param("membershipClass") MembershipClass membershipClass);

    @Query("SELECT orgaId FROM OrganizationMetadata metadata WHERE metadata.active = :active")
    List<String> getOrgaIdByActive(@Param("active") boolean active);

    void deleteByOrgaId(String merlotId);
}
