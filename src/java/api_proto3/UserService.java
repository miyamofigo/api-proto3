package api_proto3;

import java.util.Optional;
import com.miyamofigo.java8.nursery.Result;

public interface UserService {

	Result<User,Error> save(User user);

	enum Error {
		// errors with entitymanager
		NoTx,
		UserNotFound,
		InstanceIsNotEntity,
		EntityIsDuplicated,
		NonEntityInstance,
		InvalidQuery,
		// errors with query execution	
		IllegalQueryState,
		QueryTimeout,
		PessimisticLockFailed,
		LockTimeout,
		UnknownQueryError,
		NoQueryResult,
		// errors with transaction
		TxIsAlive,
		TxIsDead,
		CommitFailed,
		UnknownTxError,
		// unexpected errors
		UnexpectedError;
	}

	Result<User,Error> findById(Long id);

	Result<User,Error> findByHash(String hash);

	Result<Integer,Error> delete(String hash);

	void close();
}
