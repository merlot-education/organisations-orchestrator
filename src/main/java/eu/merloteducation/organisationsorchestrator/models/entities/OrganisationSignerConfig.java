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

package eu.merloteducation.organisationsorchestrator.models.entities;

import eu.merloteducation.organisationsorchestrator.models.AttributeEncryptor;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@NoArgsConstructor
public class OrganisationSignerConfig {

    @Id
    @GeneratedValue
    private Long id;

    @NotNull
    @Convert(converter = AttributeEncryptor.class)
    @Column(columnDefinition = "TEXT")
    private String privateKey;

    @NotNull
    private String verificationMethod;

    @NotNull
    private String merlotVerificationMethod;

}
