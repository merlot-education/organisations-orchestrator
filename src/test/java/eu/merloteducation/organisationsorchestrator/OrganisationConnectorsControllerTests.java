package eu.merloteducation.organisationsorchestrator;

import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import eu.merloteducation.organisationsorchestrator.auth.AuthorityChecker;
import eu.merloteducation.organisationsorchestrator.auth.JwtAuthConverter;
import eu.merloteducation.organisationsorchestrator.auth.JwtAuthConverterProperties;
import eu.merloteducation.organisationsorchestrator.auth.OrganizationRoleGrantedAuthority;
import eu.merloteducation.organisationsorchestrator.config.WebSecurityConfig;
import eu.merloteducation.organisationsorchestrator.controller.OrganisationConnectorsController;
import eu.merloteducation.organisationsorchestrator.models.PatchOrganisationConnectorModel;
import eu.merloteducation.organisationsorchestrator.models.PostOrganisationConnectorModel;
import eu.merloteducation.organisationsorchestrator.models.entities.OrganisationConnectorExtension;
import eu.merloteducation.organisationsorchestrator.service.OrganisationConnectorsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest({OrganisationConnectorsController.class, WebSecurityConfig.class, AuthorityChecker.class})
@AutoConfigureMockMvc()
class OrganisationConnectorsControllerTests {

    @MockBean
    private OrganisationConnectorsService organisationConnectorsService;

    @Autowired
    private JwtAuthConverter jwtAuthConverter;

    @MockBean
    private JwtAuthConverterProperties jwtAuthConverterProperties;

    @Autowired
    private MockMvc mvc;

    private String objectAsJsonString(final Object obj) {
        try {
            return JsonMapper.builder()
                    .addModule(new JavaTimeModule())
                    .build().writeValueAsString(obj);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @BeforeEach
    public void beforeEach() {
        List<OrganisationConnectorExtension> connectors = List.of(new OrganisationConnectorExtension());

        lenient().when(organisationConnectorsService.getAllConnectors(any()))
                .thenReturn(connectors);
        lenient().when(organisationConnectorsService.getConnector(any(), any()))
                .thenReturn(null);
        lenient().when(organisationConnectorsService.getConnector(eq("10"), eq("1234")))
                .thenReturn(new OrganisationConnectorExtension());
        lenient().when(organisationConnectorsService.postConnector(any(), any()))
                .thenReturn(new OrganisationConnectorExtension());
        lenient().when(organisationConnectorsService.patchConnector(any(), any(), any()))
                .thenReturn(new OrganisationConnectorExtension());
    }

    @Test
    void getAllConnectorsUnauthenticatedTest() throws Exception {
        mvc.perform(MockMvcRequestBuilders
                        .get("/organization/10/connectors/")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .with(csrf()))
                .andDo(print())
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getAllConnectorsForbiddenTest() throws Exception {
        mvc.perform(MockMvcRequestBuilders
                        .get("/organization/10/connectors/")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .with(csrf())
                        .with(jwt().authorities(
                                new OrganizationRoleGrantedAuthority("OrgLegRep_20")
                        )))
                .andDo(print())
                .andExpect(status().isForbidden());
    }

    @Test
    void getAllConnectorsAuthenticatedTest() throws Exception {
        mvc.perform(MockMvcRequestBuilders
                        .get("/organization/10/connectors/")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .with(csrf())
                        .with(jwt().authorities(
                                new OrganizationRoleGrantedAuthority("OrgLegRep_10")
                        )))
                .andDo(print())
                .andExpect(status().isOk());
    }

    @Test
    void getSingleConnectorUnauthenticatedTest() throws Exception {
        mvc.perform(MockMvcRequestBuilders
                        .get("/organization/10/connectors/connector/1234")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .with(csrf()))
                .andDo(print())
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getSingleConnectorForbiddenTest() throws Exception {
        mvc.perform(MockMvcRequestBuilders
                        .get("/organization/10/connectors/connector/1234")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .with(csrf())
                        .with(jwt().authorities(
                                new OrganizationRoleGrantedAuthority("OrgLegRep_20")
                        )))
                .andDo(print())
                .andExpect(status().isForbidden());
    }

    @Test
    void getSingleConnectorAuthenticatedTest() throws Exception {
        MvcResult result = mvc.perform(MockMvcRequestBuilders
                        .get("/organization/10/connectors/connector/1234")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .with(csrf())
                        .with(jwt().authorities(
                                new OrganizationRoleGrantedAuthority("OrgLegRep_10")
                        )))
                .andDo(print())
                .andExpect(status().isOk()).andReturn();

        String content = result.getResponse().getContentAsString();
        assertThat(content).contains("id");
    }

    @Test
    void postConnectorUnauthenticatedTest() throws Exception {
        PostOrganisationConnectorModel model = new PostOrganisationConnectorModel();
        model.setConnectorId("1234");
        model.setConnectorEndpoint("asdf");
        model.setConnectorAccessToken("123123123");
        model.setBucketNames(List.of("bucket1", "bucket2"));
        mvc.perform(MockMvcRequestBuilders
                        .post("/organization/10/connectors/connector")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(objectAsJsonString(model))
                        .with(csrf()))
                .andDo(print())
                .andExpect(status().isUnauthorized());
    }

    @Test
    void postConnectorForbiddenTest() throws Exception {
        PostOrganisationConnectorModel model = new PostOrganisationConnectorModel();
        model.setConnectorId("1234");
        model.setConnectorEndpoint("asdf");
        model.setConnectorAccessToken("123123123");
        model.setBucketNames(List.of("bucket1", "bucket2"));
        mvc.perform(MockMvcRequestBuilders
                        .post("/organization/10/connectors/connector")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(objectAsJsonString(model))
                        .with(csrf())
                        .with(jwt().authorities(
                                new OrganizationRoleGrantedAuthority("OrgLegRep_20")
                        )))
                .andDo(print())
                .andExpect(status().isForbidden());
    }

    @Test
    void postConnectorAuthenticatedTest() throws Exception {
        PostOrganisationConnectorModel model = new PostOrganisationConnectorModel();
        model.setConnectorId("1234");
        model.setConnectorEndpoint("asdf");
        model.setConnectorAccessToken("123123123");
        model.setBucketNames(List.of("bucket1", "bucket2"));
        MvcResult result = mvc.perform(MockMvcRequestBuilders
                        .post("/organization/10/connectors/connector")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(objectAsJsonString(model))
                        .with(csrf())
                        .with(jwt().authorities(
                                new OrganizationRoleGrantedAuthority("OrgLegRep_10")
                        )))
                .andDo(print())
                .andExpect(status().isOk()).andReturn();

        String content = result.getResponse().getContentAsString();
        assertThat(content).contains("id");
    }

    @Test
    void patchConnectorUnauthenticatedTest() throws Exception {
        PatchOrganisationConnectorModel model = new PatchOrganisationConnectorModel();
        model.setConnectorEndpoint("asdf");
        model.setConnectorAccessToken("123123123");
        model.setBucketNames(List.of("bucket1", "bucket2"));
        mvc.perform(MockMvcRequestBuilders
                        .patch("/organization/10/connectors/connector/1234")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(objectAsJsonString(model))
                        .with(csrf()))
                .andDo(print())
                .andExpect(status().isUnauthorized());
    }

    @Test
    void patchConnectorForbiddenTest() throws Exception {
        PatchOrganisationConnectorModel model = new PatchOrganisationConnectorModel();
        model.setConnectorEndpoint("asdf");
        model.setConnectorAccessToken("123123123");
        model.setBucketNames(List.of("bucket1", "bucket2"));
        mvc.perform(MockMvcRequestBuilders
                        .patch("/organization/10/connectors/connector/1234")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(objectAsJsonString(model))
                        .with(csrf())
                        .with(jwt().authorities(
                                new OrganizationRoleGrantedAuthority("OrgLegRep_20")
                        )))
                .andDo(print())
                .andExpect(status().isForbidden());
    }

    @Test
    void patchConnectorAuthenticatedTest() throws Exception {
        PatchOrganisationConnectorModel model = new PatchOrganisationConnectorModel();
        model.setConnectorEndpoint("asdf");
        model.setConnectorAccessToken("123123123");
        model.setBucketNames(List.of("bucket1", "bucket2"));
        MvcResult result = mvc.perform(MockMvcRequestBuilders
                        .patch("/organization/10/connectors/connector/1234")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(objectAsJsonString(model))
                        .with(csrf())
                        .with(jwt().authorities(
                                new OrganizationRoleGrantedAuthority("OrgLegRep_10")
                        )))
                .andDo(print())
                .andExpect(status().isOk()).andReturn();

        String content = result.getResponse().getContentAsString();
        assertThat(content).contains("id");
    }

    @Test
    void deleteConnectorUnauthenticatedTest() throws Exception {
        mvc.perform(MockMvcRequestBuilders
                        .delete("/organization/10/connectors/connector/1234")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .with(csrf()))
                .andDo(print())
                .andExpect(status().isUnauthorized());
    }

    @Test
    void deleteConnectorForbiddenTest() throws Exception {
        mvc.perform(MockMvcRequestBuilders
                        .delete("/organization/10/connectors/connector/1234")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .with(csrf())
                        .with(jwt().authorities(
                                new OrganizationRoleGrantedAuthority("OrgLegRep_20")
                        )))
                .andDo(print())
                .andExpect(status().isForbidden());
    }

    @Test
    void deleteConnectorAuthenticatedTest() throws Exception {
        mvc.perform(MockMvcRequestBuilders
                        .delete("/organization/10/connectors/connector/1234")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .with(csrf())
                        .with(jwt().authorities(
                                new OrganizationRoleGrantedAuthority("OrgLegRep_10")
                        )))
                .andDo(print())
                .andExpect(status().isOk());
    }

}
