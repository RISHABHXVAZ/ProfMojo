package com.profmojo.security;

import com.profmojo.integration.BasePostgresContainerTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("Actuator Security & Exposure Integration Tests")
class ActuatorSecurityTest extends BasePostgresContainerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("GET /actuator/health is publicly accessible and returns 200 UP")
    void actuatorHealth_PubliclyAccessible_Returns200() throws Exception {
        mockMvc.perform(get("/actuator/health")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    @DisplayName("GET /actuator/info is publicly accessible and returns 200")
    void actuatorInfo_PubliclyAccessible_Returns200() throws Exception {
        mockMvc.perform(get("/actuator/info")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /actuator/prometheus is publicly accessible and returns Prometheus scrape output")
    void actuatorPrometheus_PubliclyAccessible_Returns200() throws Exception {
        mockMvc.perform(get("/actuator/prometheus"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("text/plain"));
    }

    @Test
    @DisplayName("GET /actuator/env is NOT publicly accessible and is rejected with 403 Forbidden")
    void actuatorEnv_NotPublic_ReturnsForbidden() throws Exception {
        mockMvc.perform(get("/actuator/env"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /actuator/beans is NOT publicly accessible and is rejected with 403 Forbidden")
    void actuatorBeans_NotPublic_ReturnsForbidden() throws Exception {
        mockMvc.perform(get("/actuator/beans"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /actuator/heapdump is NOT publicly accessible and is rejected with 403 Forbidden")
    void actuatorHeapdump_NotPublic_ReturnsForbidden() throws Exception {
        mockMvc.perform(get("/actuator/heapdump"))
                .andExpect(status().isForbidden());
    }
}
