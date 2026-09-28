package com.transitassist;

import com.transitassist.dto.CreateHelperRequest;
import com.transitassist.dto.CreateUserRequest;
import com.transitassist.dto.HelperResponse;
import com.transitassist.dto.UserResponse;
import com.transitassist.enums.AssistanceType;
import com.transitassist.enums.Role;
import com.transitassist.exception.BusinessRuleException;
import com.transitassist.service.HelperService;
import com.transitassist.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class UserAndHelperServiceTest {

    @Autowired
    private UserService userService;

    @Autowired
    private HelperService helperService;

    @Test
    void createUser_savesUserWithDefaultRoleAndActiveFlag() {
        UserResponse user = userService.create(
                new CreateUserRequest("Asha", TestData.uniqueEmail("asha"), "9876543210", "secret123", null));

        assertNotNull(user.id());
        assertEquals(Role.USER, user.role());
        assertTrue(user.active());
        assertNotNull(user.createdAt());
    }

    @Test
    void createUser_duplicateEmailIsRejectedWithConflict() {
        String email = TestData.uniqueEmail("dup");
        userService.create(new CreateUserRequest("First", email, null, null, Role.USER));

        BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                () -> userService.create(new CreateUserRequest("Second", email, null, null, Role.USER)));

        assertEquals(HttpStatus.CONFLICT, ex.getStatus());
    }

    @Test
    void createHelper_savesSkillsAndDefaultsToAvailable() {
        HelperResponse helper = helperService.create(new CreateHelperRequest(
                "Kiran", TestData.uniqueEmail("kiran"), "9123456780",
                Set.of(AssistanceType.WHEELCHAIR, AssistanceType.ESCORT), null));

        assertNotNull(helper.id());
        assertTrue(helper.available());
        assertTrue(helper.active());
        assertEquals(2, helper.assistanceSkills().size());
    }

    @Test
    void deactivateHelper_marksHelperInactiveAndUnavailable() {
        HelperResponse helper = helperService.create(new CreateHelperRequest(
                "Sona", TestData.uniqueEmail("sona"), null, Set.of(AssistanceType.ESCORT), true));

        helperService.deactivate(helper.id());

        HelperResponse reloaded = helperService.getById(helper.id());
        assertFalse(reloaded.active());
        assertFalse(reloaded.available());
    }
}
