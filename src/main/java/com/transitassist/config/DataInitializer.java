package com.transitassist.config;

import com.transitassist.entity.AssistanceRequest;
import com.transitassist.entity.Helper;
import com.transitassist.entity.User;
import com.transitassist.enums.AssistanceType;
import com.transitassist.enums.RequestStatus;
import com.transitassist.enums.Role;
import com.transitassist.repository.AssistanceRequestRepository;
import com.transitassist.repository.HelperRepository;
import com.transitassist.repository.UserRepository;
import com.transitassist.util.PasswordHasher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Set;

/**
 * Inserts realistic demo data the first time the application starts (only when the users table is empty).
 * The demo data never violates the helper-overlap rule.
 *
 * Demo tip: request tomorrow 10:30 overlaps helper Ravi's request at tomorrow 10:00,
 * so assigning Ravi (helper 1) demonstrates the 409 Conflict rule in the viva presentation.
 */
@Component
@Profile("!test")
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final UserRepository userRepository;
    private final HelperRepository helperRepository;
    private final AssistanceRequestRepository requestRepository;

    public DataInitializer(UserRepository userRepository, HelperRepository helperRepository,
                           AssistanceRequestRepository requestRepository) {
        this.userRepository = userRepository;
        this.helperRepository = helperRepository;
        this.requestRepository = requestRepository;
    }

    @Override
    public void run(String... args) {
        if (userRepository.count() > 0) {
            log.info("Demo data already present - skipping initialisation.");
            return;
        }

        // Realistic Users
        User arun = user("Arun Kumar", "arun.kumar@example.com", "9876543210", Role.USER);
        User priya = user("Priya Sharma", "priya.sharma@example.com", "9876501234", Role.USER);
        User student = user("Demo Student", "demo.student@example.com", "9876505678", Role.USER);
        User staff = user("Campus Staff", "staff@example.com", "9876509999", Role.STAFF);
        User coordinator = user("Accessibility Coordinator", "coordinator@example.com", "9876503344", Role.STAFF);
        User transportAdmin = user("Transport Coordinator", "transport.admin@example.com", "9876507788", Role.ADMIN);
        User volunteer = user("Student Volunteer", "volunteer@example.com", "9876504455", Role.USER);
        User faculty = user("Faculty Member (Dr. Rao)", "faculty.rao@example.com", "9876506677", Role.STAFF);

        // Realistic Helpers with diverse skills
        Helper ravi = helper("Ravi Kumar", "ravi.kumar@example.com", "9123456780", Set.of(AssistanceType.WHEELCHAIR));
        Helper meena = helper("Meena Devi", "meena.devi@example.com", "9123456781", Set.of(AssistanceType.ESCORT));
        Helper demoHelper = helper("Demo Helper", "demo.helper@example.com", "9123456782",
                Set.of(AssistanceType.WHEELCHAIR, AssistanceType.ESCORT));
        Helper karthik = helper("Karthik Raj", "karthik.raj@example.com", "9123456783", Set.of(AssistanceType.MOBILITY_SUPPORT));
        Helper anitha = helper("Anitha Kumar", "anitha.kumar@example.com", "9123456784",
                Set.of(AssistanceType.WHEELCHAIR, AssistanceType.MOBILITY_SUPPORT));
        Helper suresh = helper("Suresh Babu", "suresh.babu@example.com", "9123456785",
                Set.of(AssistanceType.ESCORT, AssistanceType.MOBILITY_SUPPORT));

        LocalDate yesterday = LocalDate.now().minusDays(1);
        LocalDate today = LocalDate.now();
        LocalDate tomorrow = LocalDate.now().plusDays(1);
        LocalDate dayAfter = LocalDate.now().plusDays(2);

        // --- YESTERDAY'S REQUESTS (COMPLETED) ---
        request(arun, ravi, "Main Gate", "Science Block", yesterday, LocalTime.of(9, 0),
                AssistanceType.WHEELCHAIR, "Needs wheelchair ramp access", RequestStatus.COMPLETED);
        request(priya, meena, "Hostel A", "Library", yesterday, LocalTime.of(11, 30),
                AssistanceType.ESCORT, "Visual escort to 2nd floor reading hall", RequestStatus.COMPLETED);
        request(volunteer, karthik, "Cafeteria", "Sports Complex", yesterday, LocalTime.of(14, 0),
                AssistanceType.MOBILITY_SUPPORT, "Mobility crutches support", RequestStatus.COMPLETED);

        // --- TODAY'S REQUESTS ---
        request(arun, demoHelper, "Main Gate", "Auditorium", today, LocalTime.of(8, 30),
                AssistanceType.WHEELCHAIR, "Campus orientation seminar", RequestStatus.COMPLETED);
        request(faculty, ravi, "Admin Block", "Cafeteria", today, LocalTime.of(10, 0),
                AssistanceType.WHEELCHAIR, "Wheelchair support for council meeting", RequestStatus.ASSIGNED);
        request(priya, meena, "Library", "Hostel B", today, LocalTime.of(11, 30),
                AssistanceType.ESCORT, "Escort to hostel after study session", RequestStatus.ASSIGNED);
        request(student, null, "Hostel B", "Engineering Block", today, LocalTime.of(13, 0),
                AssistanceType.MOBILITY_SUPPORT, "Assistance carrying project model to lab", RequestStatus.REQUESTED);
        request(volunteer, null, "Medical Centre", "Main Gate", today, LocalTime.of(14, 30),
                AssistanceType.WHEELCHAIR, "Post-appointment pickup assistance", RequestStatus.REQUESTED);

        // --- TOMORROW'S REQUESTS (Key demo viva scenarios) ---
        // #9 ASSIGNED (Ravi at 10:00)
        request(priya, ravi, "Hostel A", "Library", tomorrow, LocalTime.of(10, 0),
                AssistanceType.WHEELCHAIR, "Manual wheelchair assistance", RequestStatus.ASSIGNED);
        // #10 ASSIGNED (Meena at 10:00 - different helper, no conflict)
        request(student, meena, "Admin Block", "Cafeteria", tomorrow, LocalTime.of(10, 0),
                AssistanceType.ESCORT, "Visually impaired student escort", RequestStatus.ASSIGNED);
        // #11 REQUESTED (tomorrow 10:30 - assigning Ravi demonstrates 409 Conflict with #9)
        request(arun, null, "Main Gate", "Auditorium", tomorrow, LocalTime.of(10, 30),
                AssistanceType.WHEELCHAIR, "Event attendance", RequestStatus.REQUESTED);
        // #12 ASSIGNED (Karthik at 11:30)
        request(volunteer, karthik, "Cafeteria", "Sports Complex", tomorrow, LocalTime.of(11, 30),
                AssistanceType.MOBILITY_SUPPORT, "Inter-college tournament attendance", RequestStatus.ASSIGNED);
        // #13 ASSIGNED (Anitha at 14:00)
        request(faculty, anitha, "Engineering Block", "Admin Block", tomorrow, LocalTime.of(14, 0),
                AssistanceType.WHEELCHAIR, "Dean council session", RequestStatus.ASSIGNED);
        // #14 CANCELLED
        request(student, null, "Cafeteria", "Sports Complex", tomorrow, LocalTime.of(15, 0),
                AssistanceType.MOBILITY_SUPPORT, "Plans changed", RequestStatus.CANCELLED);
        // #15 REQUESTED (Ideal for testing Cancel or Assignment in demo)
        request(student, null, "Hostel B", "Main Gate", tomorrow, LocalTime.of(16, 0),
                AssistanceType.MOBILITY_SUPPORT, "Weekend departure assistance", RequestStatus.REQUESTED);

        // --- DAY AFTER TOMORROW ---
        request(priya, suresh, "Hostel A", "Medical Centre", dayAfter, LocalTime.of(9, 30),
                AssistanceType.ESCORT, "Routine accessibility checkup", RequestStatus.ASSIGNED);
        request(arun, null, "Science Block", "Library", dayAfter, LocalTime.of(11, 0),
                AssistanceType.WHEELCHAIR, "Research project meeting", RequestStatus.REQUESTED);
        request(priya, null, "Library", "Hostel A", dayAfter, LocalTime.of(14, 0),
                AssistanceType.ESCORT, null, RequestStatus.REQUESTED);

        log.info("Demo data loaded: {} users, {} helpers, {} requests.",
                userRepository.count(), helperRepository.count(), requestRepository.count());
    }

    private User user(String name, String email, String phone, Role role) {
        User u = new User();
        u.setName(name);
        u.setEmail(email);
        u.setPhone(phone);
        u.setRole(role);
        u.setPasswordHash(PasswordHasher.hash("password123"));
        return userRepository.save(u);
    }

    private Helper helper(String name, String email, String phone, Set<AssistanceType> skills) {
        Helper h = new Helper();
        h.setName(name);
        h.setEmail(email);
        h.setPhone(phone);
        h.setAssistanceSkills(skills);
        h.setAvailable(true);
        return helperRepository.save(h);
    }

    private void request(User user, Helper helper, String pickup, String destination, LocalDate date,
                         LocalTime time, AssistanceType type, String notes, RequestStatus status) {
        AssistanceRequest r = new AssistanceRequest();
        r.setUser(user);
        r.setHelper(helper);
        r.setPickupPoint(pickup);
        r.setDestination(destination);
        r.setTripDate(date);
        r.setTripTime(time);
        r.setAssistanceType(type);
        r.setAdditionalNotes(notes);
        r.setStatus(status);
        requestRepository.save(r);
    }
}
