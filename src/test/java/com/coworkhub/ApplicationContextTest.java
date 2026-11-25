package com.coworkhub;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.coworkhub.persistence.BookingRepository;
import com.coworkhub.persistence.MemberRepository;
import com.coworkhub.persistence.RoomRepository;
import com.coworkhub.persistence.UserAccountRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:coworkhub-test;MODE=MySQL;DB_CLOSE_DELAY=-1",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.jpa.properties.hibernate.hbm2ddl.import_files=empty-import.sql"
})
@AutoConfigureMockMvc
class ApplicationContextTest {
    @Autowired private BookingRepository bookings;
    @Autowired private RoomRepository rooms;
    @Autowired private MemberRepository members;
    @Autowired private UserAccountRepository accounts;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private MockMvc mockMvc;

    @Test
    void importSqlLoadsTheRequiredSeedDataAndBcryptCredentials() throws Exception {
        runSeedData();
        assertNotNull(bookings);
        assertNotNull(rooms);
        assertEquals(15, bookings.count());
        assertEquals(6, rooms.count());
        assertEquals(5, members.count());
        assertEquals(2, jdbc.queryForObject("select count(*) from locations", Integer.class));
        assertEquals(4, jdbc.queryForObject("select count(*) from equipment", Integer.class));
        assertEquals(3, jdbc.queryForObject("select count(*) from additional_services", Integer.class));
        assertEquals(3, jdbc.queryForObject("select count(*) from membership_plans", Integer.class));
        assertEquals(4, jdbc.queryForObject("select count(*) from user_accounts", Integer.class));
        assertEquals(8, jdbc.queryForObject("select count(*) from booking_items", Integer.class));
        var admin = accounts.findByUsernameIgnoreCase("admin").orElseThrow();
        assertTrue(passwordEncoder.matches("coworkhub123", admin.getPassword()));

        mockMvc.perform(get("/login")).andExpect(status().isOk())
            .andExpect(content().string(org.hamcrest.Matchers.containsString("CoWorkHub")))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("/js/app.js")));
        mockMvc.perform(get("/dashboard")).andExpect(status().isOk())
            .andExpect(content().string(org.hamcrest.Matchers.containsString("Reservas recientes")));
        mockMvc.perform(get("/css/app.css")).andExpect(status().isOk())
            .andExpect(content().string(org.hamcrest.Matchers.containsString(".sidebar")));
        mockMvc.perform(get("/js/app.js")).andExpect(status().isOk())
            .andExpect(content().string(org.hamcrest.Matchers.containsString("coworkhub_token")));

        assertEquals(401, mockMvc.perform(get("/api/sedes")).andReturn().getResponse().getStatus());
        MvcResult login = mockMvc.perform(post("/auth/login").contentType("application/json")
                .content("{\"username\":\"ana\",\"password\":\"coworkhub123\"}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.tipo").value("Bearer"))
            .andExpect(jsonPath("$.venceEnMinutos").value(60)).andReturn();
        String body = login.getResponse().getContentAsString();
        String token = body.replaceFirst(".*\\\"token\\\":\\\"([^\\\"]+)\\\".*", "$1");
        assertNotEquals(body, token, "Login must return a JWT token");
        mockMvc.perform(get("/api/miembros/1").header("Authorization", "Bearer " + token))
            .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(1));
        mockMvc.perform(get("/api/miembros/2").header("Authorization", "Bearer " + token))
            .andExpect(status().isForbidden()).andExpect(jsonPath("$.codigo").value("FORBIDDEN"));
        mockMvc.perform(post("/api/equipamientos").header("Authorization", "Bearer " + token)
                .contentType("application/json").content("{\"name\":\"No autorizado\"}"))
            .andExpect(status().isForbidden()).andExpect(jsonPath("$.codigo").value("FORBIDDEN"));
        mockMvc.perform(get("/api/sedes").header("Authorization", "Bearer " + token))
            .andExpect(status().isOk());
        mockMvc.perform(get("/api/sedes").header("Authorization", "Bearer " + token.substring(0, token.length() - 1) + "x"))
            .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.codigo").value("UNAUTHORIZED"));

        MvcResult adminLogin = mockMvc.perform(post("/auth/login").contentType("application/json")
                .content("{\"username\":\"admin\",\"password\":\"coworkhub123\"}"))
            .andExpect(status().isOk()).andReturn();
        String adminToken = adminLogin.getResponse().getContentAsString()
            .replaceFirst(".*\\\"token\\\":\\\"([^\\\"]+)\\\".*", "$1");
        String bookingDate = LocalDate.now().plusDays(10).toString();
        String booking = "{\"salaId\":1,\"miembroId\":2,\"inicio\":\"" + bookingDate +
            "T16:00:00\",\"fin\":\"" + bookingDate +
            "T17:00:00\",\"asistentes\":2,\"servicios\":[{\"servicioId\":1,\"cantidad\":2}]}";
        MvcResult createdBooking = mockMvc.perform(post("/api/reservas").header("Authorization", "Bearer " + adminToken)
                .contentType("application/json").content(booking))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.servicesCost").value(10.0))
            .andExpect(jsonPath("$.items[0].appliedUnitPrice").value(5.0)).andReturn();
        Number createdBookingId = com.jayway.jsonpath.JsonPath.read(createdBooking.getResponse().getContentAsString(), "$.id");
        mockMvc.perform(post("/api/reservas").header("Authorization", "Bearer " + adminToken)
                .contentType("application/json").content(booking))
            .andExpect(status().isConflict()).andExpect(jsonPath("$.codigo").value("RN-01"));
        String adjacent = booking.replace("T16:00:00", "T17:00:00")
            .replace("\"fin\":\"" + bookingDate + "T17:00:00\"", "\"fin\":\"" + bookingDate + "T18:00:00\"");
        mockMvc.perform(post("/api/reservas").header("Authorization", "Bearer " + adminToken)
                .contentType("application/json").content(adjacent))
            .andExpect(status().isCreated());
        String update = "{\"salaId\":1,\"inicio\":\"" + bookingDate +
            "T18:00:00\",\"fin\":\"" + bookingDate + "T19:00:00\",\"asistentes\":3}";
        mockMvc.perform(put("/api/reservas/" + createdBookingId.longValue()).header("Authorization", "Bearer " + adminToken)
                .contentType("application/json").content(update))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.startTime").value(bookingDate + "T18:00:00"))
            .andExpect(jsonPath("$.attendees").value(3));
        mockMvc.perform(put("/api/reservas/" + createdBookingId.longValue()).header("Authorization", "Bearer " + token)
                .contentType("application/json").content(update))
            .andExpect(status().isForbidden()).andExpect(jsonPath("$.codigo").value("FORBIDDEN"));

        mockMvc.perform(get("/v3/api-docs")).andExpect(status().isOk())
            .andExpect(jsonPath("$.paths['/api/reservas/{id}'].put").exists())
            .andExpect(jsonPath("$.paths['/api/reservas/{id}/cancelar']").exists())
            .andExpect(jsonPath("$.components.securitySchemes.bearerAuth.scheme").value("bearer"));
        mockMvc.perform(get("/actuator/health")).andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("UP"));
    }

    private void runSeedData() throws IOException {
        String script;
        try (var input = new ClassPathResource("import.sql").getInputStream()) {
            script = new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
        Pattern dynamicTimestamp = Pattern.compile(
            "TIMESTAMP\\(CURDATE\\(\\)\\s*([+-])\\s*INTERVAL\\s*(\\d+)\\s*DAY,\\s*'([^']+)'\\)");
        for (String line : script.lines().toList()) {
            if (line.isBlank()) continue;
            Matcher matcher = dynamicTimestamp.matcher(line);
            StringBuffer sql = new StringBuffer();
            while (matcher.find()) {
                long days = Long.parseLong(matcher.group(2));
                LocalDate date = matcher.group(1).equals("+") ? LocalDate.now().plusDays(days) : LocalDate.now().minusDays(days);
                matcher.appendReplacement(sql, Matcher.quoteReplacement("TIMESTAMP '" + date + " " + matcher.group(3) + "'"));
            }
            matcher.appendTail(sql);
            jdbc.execute(sql.toString());
        }
    }
}
