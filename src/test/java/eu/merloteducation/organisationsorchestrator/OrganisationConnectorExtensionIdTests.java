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

package eu.merloteducation.organisationsorchestrator;

import eu.merloteducation.organisationsorchestrator.models.entities.OrganisationConnectorExtensionId;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class OrganisationConnectorExtensionIdTests {

    @Test
    void testEqualsAndHashCode() {
        OrganisationConnectorExtensionId id1 = new OrganisationConnectorExtensionId("org1", "connector1");
        OrganisationConnectorExtensionId id2 = new OrganisationConnectorExtensionId("org1", "connector1");
        OrganisationConnectorExtensionId id3 = new OrganisationConnectorExtensionId("org2", "connector2");

        // Testing equals method
        assertEquals(id1, id2); // id1 should be equal to id2
        assertNotEquals(id1, id3); // id1 should not be equal to id3

        // Testing hashCode method
        assertEquals(id1.hashCode(), id2.hashCode()); // Hash codes should be equal
        assertNotEquals(id1.hashCode(), id3.hashCode()); // Hash codes should not be equal
    }
}
