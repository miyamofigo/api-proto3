package api_proto3;

import org.springframework.security.web.context.*;

public class SecurityWebApplicationInitializer 
 extends AbstractSecurityWebApplicationInitializer {

	public SecurityWebApplicationInitializer() {	
		super(SecurityConfig.class, SessionConfig.class);
	}
}
