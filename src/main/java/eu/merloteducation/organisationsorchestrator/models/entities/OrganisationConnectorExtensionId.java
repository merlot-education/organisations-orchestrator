/*
 *  Copyright 2023-2024 Dataport AöR
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

package eu.merloteducation.organisationsorchestrator.models.entities;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.util.Objects;

@NoArgsConstructor
@Getter
@Setter
public class OrganisationConnectorExtensionId implements Serializable {
    private String orgaId;
    private String connectorId;

    public OrganisationConnectorExtensionId(String orgaId, String connectorId) {
        this.orgaId = orgaId;
        this.connectorId = connectorId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        OrganisationConnectorExtensionId that = (OrganisationConnectorExtensionId) o;
        return Objects.equals(orgaId, that.orgaId) &&
            Objects.equals(connectorId, that.connectorId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(orgaId, connectorId);
    }
}
