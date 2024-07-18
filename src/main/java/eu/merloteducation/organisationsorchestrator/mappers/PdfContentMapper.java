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

package eu.merloteducation.organisationsorchestrator.mappers;

import eu.merloteducation.organisationsorchestrator.models.RegistrationFormContent;
import org.apache.commons.lang3.StringUtils;
import org.apache.pdfbox.pdmodel.interactive.form.PDAcroForm;
import org.apache.pdfbox.pdmodel.interactive.form.PDChoice;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface PdfContentMapper {
    @Mapping(target = "organizationName", expression = "java(getField(pDAcroForm, DocumentField.ORGANIZATIONNAME))")
    @Mapping(target = "organizationLegalName", expression = "java(getField(pDAcroForm, DocumentField.ORGANIZATIONLEGALNAME))")
    @Mapping(target = "mailAddress", expression = "java(getField(pDAcroForm, DocumentField.MAILADDRESS))")
    @Mapping(target = "providerTncLink", expression = "java(getField(pDAcroForm, DocumentField.TNCLINK))" )
    @Mapping(target = "providerTncHash", expression = "java(getField(pDAcroForm, DocumentField.TNCHASH))" )
    @Mapping(target = "countryCode", expression = "java(getField(pDAcroForm, DocumentField.COUNTRYCODE))")
    @Mapping(target = "countrySubdivisionCode", expression = "java(getField(pDAcroForm, DocumentField.COUNTRYSUBDIVISIONCODE))")
    @Mapping(target = "city", expression = "java(getField(pDAcroForm, DocumentField.CITY))")
    @Mapping(target = "postalCode", expression = "java(getField(pDAcroForm, DocumentField.POSTALCODE))")
    @Mapping(target = "street", expression = "java(getField(pDAcroForm, DocumentField.STREET))")
    @Mapping(target = "legalForm", expression = "java(getSelection(pDAcroForm, DocumentField.LEGALFORM))")
    @Mapping(target = "registrationNumberLeiCode", expression = "java(getCleanedField(pDAcroForm, DocumentField.REGISTRATIONNUMBER_LEICODE))")
    @Mapping(target = "registrationNumberEori", expression = "java(getCleanedField(pDAcroForm, DocumentField.REGISTRATIONNUMBER_EORI))")
    @Mapping(target = "registrationNumberVatID", expression = "java(getCleanedField(pDAcroForm, DocumentField.REGISTRATIONNUMBER_VATID))")
    RegistrationFormContent getRegistrationFormContentFromRegistrationForm(PDAcroForm pDAcroForm);

    default String getField(PDAcroForm pDAcroForm, DocumentField field) {
        return pDAcroForm.getField(field.getValue()).getValueAsString();
    }

    default String getCleanedField(PDAcroForm pDAcroForm, DocumentField field) {
        return StringUtils.deleteWhitespace(getField(pDAcroForm, field));
    }

    default String getSelection(PDAcroForm pDAcroForm, DocumentField field) {
        return ((PDChoice) pDAcroForm.getField(field.getValue())).getValue().get(0);
    }
}
