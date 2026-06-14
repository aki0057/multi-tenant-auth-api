package io.github.aki0057.multitenant.auth;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.OffsetDateTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class LoginIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        OffsetDateTime now = OffsetDateTime.now();
        jdbcTemplate.update(
                "INSERT INTO tenants (code, name, is_active, created_at, updated_at, created_by, updated_by) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?)",
                "test-tenant", "テストテナント", true, now, now, "system", "system");

        Long tenantId = jdbcTemplate.queryForObject(
                "SELECT id FROM tenants WHERE code = 'test-tenant'", Long.class);

        // テスト実行時に BCrypt ハッシュを生成する
        String hash = passwordEncoder.encode("password");
        jdbcTemplate.update(
                "INSERT INTO users (tenant_id, email, password_hash, role, is_active, created_at, updated_at, created_by, updated_by) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)",
                tenantId, "test@example.com", hash,
                "ROLE_USER", true, now, now, "system", "system");
    }

    @AfterEach
    void tearDown() {
        jdbcTemplate.update("DELETE FROM users WHERE email = 'test@example.com'");
        jdbcTemplate.update("DELETE FROM tenants WHERE code = 'test-tenant'");
    }

    @Test
    @DisplayName("正常系: 正しい tenantCode + email + password を送信すると 200 OK が返る")
    void login_withValidCredentials_returns200() throws Exception {
        mockMvc.perform(post("/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "tenantCode": "test-tenant",
                                  "email": "test@example.com",
                                  "password": "password"
                                }
                                """))
                .andExpect(status().isOk());
    }
}

