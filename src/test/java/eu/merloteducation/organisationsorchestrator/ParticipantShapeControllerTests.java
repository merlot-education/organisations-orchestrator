package eu.merloteducation.organisationsorchestrator;

import eu.merloteducation.authorizationlibrary.authorization.*;
import eu.merloteducation.authorizationlibrary.config.InterceptorConfig;
import eu.merloteducation.authorizationlibrary.config.MerlotSecurityConfig;
import eu.merloteducation.gxfscataloglibrary.service.GxfsCatalogService;
import eu.merloteducation.gxfscataloglibrary.service.GxfsWizardApiService;
import eu.merloteducation.organisationsorchestrator.config.WebSecurityConfig;
import eu.merloteducation.organisationsorchestrator.controller.ParticipantShapeController;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest({ParticipantShapeController.class, WebSecurityConfig.class})
@Import({JwtAuthConverter.class, ActiveRoleHeaderHandlerInterceptor.class, InterceptorConfig.class,
        AuthorityChecker.class, MerlotSecurityConfig.class})
@AutoConfigureMockMvc()
class ParticipantShapeControllerTests {

    @MockBean
    private GxfsWizardApiService gxfsWizardApiService;

    @MockBean
    private UserInfoOpaqueTokenIntrospector userInfoOpaqueTokenIntrospector;

    @MockBean
    private GxfsCatalogService gxfsCatalogService;

    @MockBean
    private JwtAuthConverterProperties jwtAuthConverterProperties;

    @MockBean
    private JwtAuthConverter jwtAuthConverter;

    @Autowired
    private MockMvc mvc;


    @BeforeEach
    public void beforeEach()  {
        lenient().when(gxfsWizardApiService.getShapeByName(any())).thenReturn("shape");
    }

    @Test
    void getParticipantShapeUnauthenticated() throws Exception {
        mvc.perform(MockMvcRequestBuilders
                        .get("/shapes/merlotParticipant")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .with(csrf()))
                .andDo(print())
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getParticipantShapeAuthenticated() throws Exception {
        mvc.perform(MockMvcRequestBuilders
                        .get("/shapes/merlotParticipant")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .with(csrf())
                        .with(jwt().authorities(
                                new SimpleGrantedAuthority("ROLE_OrgLegRep_20")
                        )))
                .andDo(print())
                .andExpect(status().isOk());
    }
}
