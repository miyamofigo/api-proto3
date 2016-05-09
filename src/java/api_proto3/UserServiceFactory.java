package api_proto3;

import java.util.Optional; 

public interface UserServiceFactory { 
	public Optional<UserService> createUserService(); 
	public void close();
}	
