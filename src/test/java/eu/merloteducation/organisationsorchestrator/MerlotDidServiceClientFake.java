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

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.merloteducation.modelslib.api.did.ParticipantDidPrivateKeyCreateRequest;
import eu.merloteducation.modelslib.api.did.ParticipantDidPrivateKeyDto;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public class MerlotDidServiceClientFake {

    private final ParticipantDidPrivateKeyDto defaultResponse;

    public MerlotDidServiceClientFake() throws IOException {
        InputStream responseStream = MerlotDidServiceClientFake.class.getClassLoader()
                .getResourceAsStream("example-did-service-response.json");
        defaultResponse = new ObjectMapper().readValue(
                new String(responseStream.readAllBytes(), StandardCharsets.UTF_8),
                new TypeReference<>() {});
    }

    public ParticipantDidPrivateKeyDto generateDidAndPrivateKey(ParticipantDidPrivateKeyCreateRequest request) {
        return defaultResponse;
    }
}
