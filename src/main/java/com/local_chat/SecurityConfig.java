package com.local_chat;
import javax.sql.DataSource;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.rememberme.JdbcTokenRepositoryImpl;
import org.springframework.security.web.authentication.rememberme.PersistentTokenRepository;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    PersistentTokenRepository tokenRepository(DataSource dataSource) {
        JdbcTokenRepositoryImpl repo = new JdbcTokenRepositoryImpl();
        repo.setDataSource(dataSource);
        return repo;
    }

    @Bean
    DaoAuthenticationProvider authProvider(UserDetailsService uds, PasswordEncoder encoder) {
        var provider = new DaoAuthenticationProvider(uds);
        provider.setPasswordEncoder(encoder);
        provider.setHideUserNotFoundExceptions(false);
        return provider;
    }

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http,
                                    UserDetailsService uds,
                                    PersistentTokenRepository tokenRepo) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/login", "/register", "/css/**", "/js/**").permitAll()
                .anyRequest().authenticated())
            .formLogin(form -> form.loginPage("/login")
                .failureHandler((req, res, ex) -> {
                    res.setStatus(401);
                    res.setContentType("text/plain");
                    System.out.println(ex.getClass());
                    res.getWriter().write(ex instanceof 
                        UsernameNotFoundException ? "BAD_USERNAME" : "BAD_PASSWORD");
                })
                .permitAll())
            .rememberMe(rm -> rm
                .userDetailsService(uds)
                .tokenRepository(tokenRepo)
                .tokenValiditySeconds(60 * 60 * 24 * 180))
            .logout(l -> l.deleteCookies("JSESSIONID", "remember-me"));
        return http.build();
    }
}
