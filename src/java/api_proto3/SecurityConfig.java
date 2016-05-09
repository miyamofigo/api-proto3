package api_proto3;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.*;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.*;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.authentication.AnonymousAuthenticationFilter;
import org.springframework.security.web.authentication.logout.LogoutFilter;

@EnableWebSecurity
public class SecurityConfig extends WebSecurityConfigurerAdapter {

	@Autowired
	public void configureGlobal(AuthenticationManagerBuilder auth) 
	 throws Exception {
		auth.inMemoryAuthentication()
				.withUser("user").password("password").roles("User");
	}  

	@Override
	protected void configure(HttpSecurity http)
	 throws Exception {
		http.authorizeRequests()
					.anyRequest().authenticated()
					.and()
				.formLogin()
					.and()
				.httpBasic()
					.and()
				.rememberMe();	
	} 
} 

