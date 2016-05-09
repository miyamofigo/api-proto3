package api_proto3;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.InternalAuthenticationServiceException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AbstractAuthenticationProcessingFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

public class NonOperationFilter extends AbstractAuthenticationProcessingFilter {
	public NonOperationFilter() { super("/*"); }

	public Authentication attemptAuthentication(
		HttpServletRequest req, HttpServletResponse res
	//) throws AuthenticationException { return new NonOperationToken(); }
	) throws AuthenticationException { return null; }
	//) throws AuthenticationException { throw new InternalAuthenticationServiceException("error"); }

	public static class NonOperationToken extends AbstractAuthenticationToken {
		public NonOperationToken() { super(null); }
		@Override
		public Object getCredentials() { return null; } 
		@Override
		public Object getPrincipal() { return null; }
	}

	public static class NonOperationProvider implements AuthenticationProvider {  
		@Override
		public Authentication authenticate(Authentication auth)
		 throws AuthenticationException {
		 	auth.setAuthenticated(true);
			return auth;
		}

		@Override
		public boolean supports(Class<?> authentication) {
			if (authentication.isAssignableFrom(NonOperationToken.class)) return true;
			else return false;
		}	
	}
} 

