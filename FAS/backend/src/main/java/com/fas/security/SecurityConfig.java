package com.fas.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;
import com.fas.repository.AdministratorRepository;
import com.fas.repository.HodRepository;
import com.fas.repository.FacultyRepository;
import com.fas.repository.StudentRepository;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(Customizer.withDefaults())
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/public/**").permitAll()
                        .requestMatchers("/api/administrator/**").hasRole("ADMINISTRATOR")
                        .requestMatchers("/api/hod/**").hasRole("HOD")
                        .requestMatchers("/api/faculty/**").hasRole("FACULTY")
                        .requestMatchers("/api/student/**").hasRole("STUDENT")
                        .requestMatchers("/api/reports/**").hasAnyRole("HOD", "FACULTY", "ADMINISTRATOR")
                        .anyRequest().authenticated())
                .headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()))
                .httpBasic(Customizer.withDefaults());
        return http.build();
    }

    @Bean
    UserDetailsService userDetailsService(
            AdministratorRepository administrators,
            HodRepository hods,
            FacultyRepository faculties,
            StudentRepository students,
            PasswordEncoder passwordEncoder) {
        return username -> {
            var administrator = administrators.findByUsername(username);
            if (administrator.isPresent() && administrator.get().isActive()) {
                return createUser(username, administrator.get().getPassword(), "ADMINISTRATOR");
            }
            var hod = hods.findByUsername(username);
            if (hod.isPresent() && hod.get().isActive()) {
                return createUser(username, hod.get().getPassword(), "HOD");
            }
            var faculty = faculties.findByEmail(username);
            if (faculty.isEmpty()) {
                faculty = faculties.findByUsername(username);
            }
            if (faculty.isPresent() && faculty.get().isActive()) {
                return createUser(username, faculty.get().getPassword(), "FACULTY");
            }
            var studentByEmail = students.findByEmail(username);
            if (studentByEmail.isPresent() && studentByEmail.get().isActive()) {
                var studentAccount = studentByEmail.get();
                return createUser(studentAccount.getUsername(), studentAccount.getPassword(), "STUDENT");
            }
            throw new UsernameNotFoundException("Unknown active account: " + username);
        };
    }

    private UserDetails createUser(String username, String password, String role) {
        return User.withUsername(username).password(password).roles(role).build();
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource() {
        var configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of("http://localhost:5173"));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        var source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
