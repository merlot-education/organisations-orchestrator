package eu.merloteducation.organisationsorchestrator;

import eu.merloteducation.organisationsorchestrator.auth.JwtAuthConverter;
import eu.merloteducation.organisationsorchestrator.auth.JwtAuthConverterProperties;
import eu.merloteducation.organisationsorchestrator.auth.OrganizationRoleGrantedAuthority;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;


class JwtAuthConverterTests {

    @Test
    void convertJwt() {
        JwtAuthConverterProperties properties = new JwtAuthConverterProperties();
        JwtAuthConverter converter = new JwtAuthConverter(properties);
        Jwt jwt = new Jwt("someValue",
                Instant.now(), Instant.now().plusSeconds(999),
                Map.of("header1", "header1"),
                Map.of("sub", "myUserId",
                        "realm_access", Map.of("roles", Set.of("OrgLegRep_10", "SomeOtherRole"))));
        Authentication auth = converter.convert(jwt);
        List<OrganizationRoleGrantedAuthority> orgaAuths = (List<OrganizationRoleGrantedAuthority>) auth.getAuthorities();
        assertEquals("OrgLegRep", orgaAuths.get(0).getOrganizationRole());
        assertEquals("10", orgaAuths.get(0).getOrganizationId());

    }

    @Test
    void convertJwtEmpty() {
        JwtAuthConverterProperties properties = new JwtAuthConverterProperties();
        JwtAuthConverter converter = new JwtAuthConverter(properties);
        Jwt jwt = new Jwt("someValue",
                Instant.now(), Instant.now().plusSeconds(999),
                Map.of("header1", "header1"),
                Map.of("sub", "myUserId",
                        "realm_access", Collections.emptyMap()));
        Authentication auth = converter.convert(jwt);
        assertTrue(auth.getAuthorities().isEmpty());

        jwt = new Jwt("someValue",
                Instant.now(), Instant.now().plusSeconds(999),
                Map.of("header1", "header1"),
                Map.of("sub", "myUserId",
                        "realm_access", Map.of("roles", Collections.emptyList())));
        auth = converter.convert(jwt);
        assertTrue(auth.getAuthorities().isEmpty());

    }
}
