package in.shvms.famt.famtbe.controllers;

import in.shvms.famt.famtbe.dtos.AuthRequest;
import in.shvms.famt.famtbe.dtos.AuthResponse;
import in.shvms.famt.famtbe.dtos.TenantCreationRequest;
import in.shvms.famt.famtbe.entities.Tenant;
import in.shvms.famt.famtbe.services.TenantService;
import in.shvms.famt.famtbe.util.JwtUtil;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final UserDetailsService userDetailsService;
    private final TenantService tenantService;
    private final JwtUtil jwtUtil;

    public AuthController(AuthenticationManager authenticationManager, UserDetailsService userDetailsService, TenantService tenantService, JwtUtil jwtUtil) {
        this.authenticationManager = authenticationManager;
        this.userDetailsService = userDetailsService;
        this.tenantService = tenantService;
        this.jwtUtil = jwtUtil;
    }

    @PostMapping("/login")
    public ResponseEntity<?> createAuthenticationToken(@RequestBody AuthRequest authRequest) throws Exception {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(authRequest.getUsername(), authRequest.getPassword())
        );

        final UserDetails userDetails = userDetailsService.loadUserByUsername(authRequest.getUsername());
        final String jwt = jwtUtil.generateToken(userDetails);

        return ResponseEntity.ok(new AuthResponse(jwt));
    }

    @PostMapping("/tenants")
    public ResponseEntity<Tenant> createTenant(@RequestBody TenantCreationRequest tenantCreationRequest) {
        return ResponseEntity.ok(tenantService.createTenant(tenantCreationRequest.getName()));
    }
}
