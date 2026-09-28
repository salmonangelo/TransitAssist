package com.transitassist;

import com.transitassist.dto.AssistanceRequestResponse;
import com.transitassist.dto.CreateAssistanceRequest;
import com.transitassist.dto.CreateHelperRequest;
import com.transitassist.dto.CreateUserRequest;
import com.transitassist.dto.HelperResponse;
import com.transitassist.dto.UserResponse;
import com.transitassist.enums.AssistanceType;
import com.transitassist.enums.Role;
import com.transitassist.service.AssistanceRequestService;
import com.transitassist.service.HelperService;
import com.transitassist.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Set;

import static org.hamcrest.Matchers.hasKey;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Checks the HTTP layer: status codes and JSON error bodies. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class ApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private UserService userService;
    @Autowired
    private HelperService helperService;
    @Autowired
    private AssistanceRequestService requestService;

    @Test
    void createRequest_withInvalidInputReturns400WithFieldErrors() throws Exception {
        String body = """
                {"userId": 1, "pickupPoint": "", "destination": "Library",
                 "tripDate": "2020-01-01", "tripTime": "10:00", "assistanceType": "WHEELCHAIR"}
                """;

        mockMvc.perform(post("/api/assistance-requests").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.errors", hasKey("pickupPoint")))
                .andExpect(jsonPath("$.errors", hasKey("tripDate")));
    }

    @Test
    void getRequest_unknownIdReturns404() throws Exception {
        mockMvc.perform(get("/api/assistance-requests/999999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.path").value("/api/assistance-requests/999999"));
    }

    @Test
    void assignOverlappingRequest_returns409Conflict() throws Exception {
        UserResponse user = userService.create(new CreateUserRequest(
                "Api User", TestData.uniqueEmail("api"), null, null, Role.USER));
        HelperResponse helper = helperService.create(new CreateHelperRequest(
                "Api Helper", TestData.uniqueEmail("apihelper"), null, Set.of(AssistanceType.ESCORT), true));
        LocalDate date = LocalDate.now().plusDays(5);
        AssistanceRequestResponse first = requestService.create(new CreateAssistanceRequest(
                user.id(), "A", "B", date, LocalTime.of(9, 0), AssistanceType.ESCORT, null));
        AssistanceRequestResponse second = requestService.create(new CreateAssistanceRequest(
                user.id(), "C", "D", date, LocalTime.of(9, 30), AssistanceType.ESCORT, null));

        mockMvc.perform(post("/api/assistance-requests/" + first.id() + "/assign/" + helper.id()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ASSIGNED"));

        mockMvc.perform(post("/api/assistance-requests/" + second.id() + "/assign/" + helper.id()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message")
                        .value("Helper is already assigned to another request at the requested time."));
    }

    @Test
    void cancelCompletedRequest_returns400() throws Exception {
        UserResponse user = userService.create(new CreateUserRequest(
                "Api User 2", TestData.uniqueEmail("api2"), null, null, Role.USER));
        HelperResponse helper = helperService.create(new CreateHelperRequest(
                "Api Helper 2", TestData.uniqueEmail("apihelper2"), null, Set.of(AssistanceType.ESCORT), true));
        AssistanceRequestResponse request = requestService.create(new CreateAssistanceRequest(
                user.id(), "A", "B", LocalDate.now().plusDays(6), LocalTime.of(9, 0), AssistanceType.ESCORT, null));
        requestService.assign(request.id(), helper.id());
        requestService.complete(request.id());

        mockMvc.perform(post("/api/assistance-requests/" + request.id() + "/cancel"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Completed requests cannot be cancelled."));
    }
}
