package api_proto3;

import java.util.Optional;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

public class UserServiceFactoryImpl implements UserServiceFactory {

	final private EntityManagerFactory emFactory;

	private UserServiceFactoryImpl(EntityManagerFactory emFactory) { this.emFactory = emFactory; }

	@Override
	public Optional<UserService> createUserService() {
	  return emFactory == null ? Optional.empty() 
						: Optional.of((UserService) new UserServiceImpl(emFactory.createEntityManager()));
	}

	@Override public void close() { emFactory.close(); }

	public static UserServiceFactory createUserServiceFactory(String persistentUnitName) {
		return (UserServiceFactory) new UserServiceFactoryImpl(
										              Persistence.createEntityManagerFactory(persistentUnitName)
																);
	}

}
