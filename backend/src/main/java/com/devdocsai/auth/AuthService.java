package com.devdocsai.auth;

import com.devdocsai.auth.dto.LoginRequest;
import com.devdocsai.auth.dto.LoginResponse;
import com.devdocsai.auth.dto.RegisterRequest;
import com.devdocsai.auth.model.User;
import com.devdocsai.common.DevDocsException;
import com.devdocsai.tenant.Tenant;
import com.devdocsai.tenant.TenantRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
public class AuthService {

    private static final Pattern NON_ALPHANUM = Pattern.compile("[^a-z0-9]+");

    private final UserRepository userRepository;
    private final TenantRepository tenantRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    @Value("${jwt.access-token.expiry}")
    private long accessTokenExpiry;

    public AuthService(UserRepository userRepository, TenantRepository tenantRepository,
                       PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.userRepository = userRepository;
        this.tenantRepository = tenantRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Transactional
    public void register(RegisterRequest request) {
        // Check if email already registered (globally — one account per email)
        if (userRepository.findByEmail(request.email()).isPresent()) {
            throw DevDocsException.conflict("EMAIL_ALREADY_EXISTS",
                "An account with this email already exists.");
        }

        // Create tenant
        String slug = toSlug(request.companyName());
        // Ensure slug uniqueness by appending random suffix if needed
        if (tenantRepository.existsBySlug(slug)) {
            slug = slug + "-" + UUID.randomUUID().toString().substring(0, 6);
        }
        String apiKey = UUID.randomUUID().toString().replace("-", "") +
                        UUID.randomUUID().toString().replace("-", "");

        Tenant tenant = new Tenant(request.companyName(), slug, apiKey);
        tenant = tenantRepository.save(tenant);

        // Create admin user for the tenant
        User user = new User(
            tenant.getId(),
            request.email(),
            passwordEncoder.encode(request.password()),
            "ADMIN"
        );
        userRepository.save(user);
    }

    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.email())
            .orElseThrow(() -> DevDocsException.unauthorized("INVALID_CREDENTIALS",
                "Invalid email or password."));

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw DevDocsException.unauthorized("INVALID_CREDENTIALS", "Invalid email or password.");
        }

        String token = jwtService.generateAccessToken(
            user.getId().toString(),
            user.getTenantId().toString(),
            user.getRole()
        );

        return LoginResponse.of(token, accessTokenExpiry / 1000,
            user.getId(), user.getTenantId(), user.getRole());
    }

    private String toSlug(String input) {
        String normalized = Normalizer.normalize(input, Normalizer.Form.NFD);
        return NON_ALPHANUM.matcher(normalized.toLowerCase(Locale.ROOT)).replaceAll("-")
                .replaceAll("^-+|-+$", "");
    }
}
