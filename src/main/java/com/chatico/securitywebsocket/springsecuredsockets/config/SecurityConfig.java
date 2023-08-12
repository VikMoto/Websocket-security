package com.chatico.securitywebsocket.springsecuredsockets.config;

import com.chatico.securitywebsocket.springsecuredsockets.security.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;

import static org.springframework.security.config.Customizer.withDefaults;


/**
 * @EnableGlobalAuthentication annotates:
 * @EnableWebSecurity
 * @EnableWebMvcSecurity
 * @EnableGlobalMethodSecurity Passing in 'prePostEnabled = true' allows:
 * <p>
 * Pre/Post annotations such as:
 * @PreAuthorize("hasRole('ROLE_USER')")
 */

@Configuration
@EnableGlobalMethodSecurity(prePostEnabled = true, securedEnabled = true)
@EnableWebSecurity
@ComponentScan("com.chatico.securitywebsocket")
public class SecurityConfig {

    @Autowired
    private CustomUserDetailsService customUserDetailsService;

    /**
     * Login, Logout, Success, and Access Denied beans/handlers
     */

    @Bean
    public AccessDeniedHandler accessDeniedHandler() {
        return new CustomAccessDeniedHandler();
    }

    @Bean
    public LogoutSuccessHandler logoutSuccessHandler() {
        return new CustomLogoutSuccessHandler();
    }

    @Bean
    public AuthenticationSuccessHandler loginSuccessHandler() {
        return new CustomLoginSuccessHandler();
    }

    /**
     * Authentication beans
     */

    @Bean
    public PasswordEncoder encoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        final DaoAuthenticationProvider bean = new CustomDaoAuthenticationProvider();
        bean.setUserDetailsService(customUserDetailsService);
        bean.setPasswordEncoder(encoder());
        return bean;
    }

    /**
     * Order of precedence is very important.
     * <p>
     * Matching occurs from top to bottom - so, the topmost match succeeds first.
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests((requests) -> requests
                    .requestMatchers("/", "/index", "/authenticate").permitAll()
                    .requestMatchers("/secured/**/**", "/secured/**/**/**", "/secured/socket", "/secured/success")
                    .authenticated()
                    .anyRequest()
            )
                            .formLogin(formLogin -> formLogin
            .loginPage("/login")
//	  	.failureUrl("/authentication/login?failed") // default is /login?error
//	  	.loginProcessingUrl("/authentication/login/process") // default is /login
            .usernameParameter("username")
            .passwordParameter("password")
            .defaultSuccessUrl("/*")
            .failureUrl("/denied")
            .permitAll()
            )
            .logout((logout) -> logout
                    .deleteCookies("JSESSIONID")
                    .logoutSuccessHandler(logoutSuccessHandler())
                    .permitAll())

            .csrf(csrf -> csrf.disable())


            /**
             * Applies to User Roles - not to login failures or unauthenticated access attempts.
             */
            .exceptionHandling((exceptionHandling) ->
             exceptionHandling
                        .accessDeniedPage("/errors/access-denied")
            )
            .authenticationProvider(authenticationProvider());




        /** This is solely required to support H2 console viewing in Spring MVC with Spring Security */
        http .headers((headers) ->
                headers
                        .contentTypeOptions(withDefaults())
                        .xssProtection(withDefaults())
                        .cacheControl(withDefaults())
                        .httpStrictTransportSecurity(withDefaults())
                        .frameOptions(frameOptionsConfig -> frameOptionsConfig.disable())
                        );

        return http.build();
    }

    @Bean
    public AuthenticationManager authManager(HttpSecurity http) throws Exception {
        return http.getSharedObject(AuthenticationManagerBuilder.class)
            .authenticationProvider(authenticationProvider())
            .build();
    }

    @Bean
    public WebSecurityCustomizer webSecurityCustomizer() {
        return (web) -> web.ignoring()
                .requestMatchers("/resources/**");
    }

}
