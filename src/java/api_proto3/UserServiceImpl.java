package api_proto3;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import javax.persistence.EntityExistsException;
import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;
import javax.persistence.LockTimeoutException;
import javax.persistence.PersistenceContext;
import javax.persistence.PersistenceException;
import javax.persistence.PessimisticLockException;
import javax.persistence.Query;
import javax.persistence.QueryTimeoutException;
import javax.persistence.RollbackException;
import javax.persistence.TypedQuery;
import javax.transaction.TransactionRequiredException;
import org.jboss.logging.Logger;
import com.miyamofigo.java8.nursery.Result;

public class UserServiceImpl implements UserService {

	@PersistenceContext 
	private final EntityManager entityManager;

	private final String SELECT_USER_BY_HASH = "select u from User u where u.hash = ?1";
	private final String DELETE_USER_BY_HASH = "delete from User u where u.hash = ?1";
	private final Logger logger = Logger.getLogger(UserService.class);

	private final String COMMIT_FAILED_MESSAGE = "UserService: transaction failed to commit.";
	private final String ROLLBACK_FAILED_MESSAGE = "UserService: transaction failed rollback.";

	private final String USER_DUPLICATED_MESSAGE = "UserService: duplicated user can't be persisted.";
	private final String USER_NOT_FOUND_MESSAGE = "UserService: user not found.";

	private final String UNEXPECTED_ERROR_MESSAGE = "UserService: something unexpected has happened.";
	private final String INTERNAL_ERROR_MESSAGE = "UserService: an internal error occured.";

	public UserServiceImpl(EntityManager entityManager) { this.entityManager = entityManager; }
	
	@Override
	public Result<User,Error> save(User user) {
		try {
			txBegin();
			Result<User,Error> res = user.getId() != null ? emMerge(user) : emPersist(user);

			if (res.isErr()) { return res; } 

			Result<TxWrapper,Error> committed = txCommit(); 
			if (committed.isOk()) return res;
			else {
				try { 
					txRollback().expect(ROLLBACK_FAILED_MESSAGE + ": "); 
				}
				catch (RuntimeException e) { 
					logger.warn(COMMIT_FAILED_MESSAGE);
					logger.warn(e.getMessage() + " error occured."); 
				}
	 			return committed.iterErr().rewrap(error -> Result.<User,Error>err(error)); 
			}	
		} 
		catch (TransactionRequiredException e) { 
			return Result.err(Error.NoTx);
		}
	}

	@Override
	public Result<User,Error> findById(Long id) { 
		try {
			User result = entityManager.find(User.class, id);
		 	if ( result == null ) 
				return Result.err(Error.UserNotFound);
			else
				return Result.ok(result);	
		}
		catch (IllegalArgumentException e) {
			warn(UNEXPECTED_ERROR_MESSAGE, e);
			return Result.err(Error.UnexpectedError);
		}
	}

	@Override
	public Result<User,Error> findByHash(String hash) { 
		try {
			//List<User> result = createQuery(SELECT_USER_BY_HASH).setParameter(1, hash).getResultList();
			List<User> result = createTypedQuery(SELECT_USER_BY_HASH).setParameter(1, hash).getResultList();
			if (result.isEmpty())
				return Result.err(Error.UserNotFound);
			else
				return Result.ok(result.get(0));
		}
		// In the case IllegalArgumentException has been thrown, on TypedQuery.setParameter method,
		// a possitional parameter is illegal or the hash is not be a string instance, 
		// so the service should stop and this would happen during development only.
		// IllegalStateException is ignored with the same reason. 
		catch (QueryTimeoutException e) {
			warn(INTERNAL_ERROR_MESSAGE, e);
			return Result.err(Error.QueryTimeout);
		}
		catch (PessimisticLockException e) {
			warn(INTERNAL_ERROR_MESSAGE, e);
			return Result.err(Error.PessimisticLockFailed);
		}
		catch (LockTimeoutException e) {
			warn(INTERNAL_ERROR_MESSAGE, e);
			return Result.err(Error.LockTimeout);
		}
		catch	(PersistenceException e) {
			warn(INTERNAL_ERROR_MESSAGE, e);
			return Result.err(Error.UnknownQueryError); 
		}
	}

	@Override
	public Result<Integer,Error> delete(String hash) {
		try {
			txBegin();

			Result<TxWrapper,Error> committed; 
			switch (createQuery(DELETE_USER_BY_HASH).setParameter(1, hash).executeUpdate()) {
				case 0 	:
					return Result.err(Error.UserNotFound);
				case 1 	:
					committed = txCommit(); 
					if (committed.isOk()) 
						return Result.ok((Integer) 1);
				default	:
					committed = Result.err(Error.UnexpectedError);
			}

			try {
				txRollback().expect(ROLLBACK_FAILED_MESSAGE + ": ");	
			}
			catch (RuntimeException e) { 
				logger.warn(COMMIT_FAILED_MESSAGE);
				logger.warn(e.getMessage() + " error occured."); 
			}
			finally {
	 			return committed.iterErr().rewrap(error -> Result.<Integer,Error>err(error));
			}
		}	
		catch (QueryTimeoutException e) {
			warn(INTERNAL_ERROR_MESSAGE, e);
			return Result.err(Error.QueryTimeout);
		}
		catch (PersistenceException e) {
			warn(INTERNAL_ERROR_MESSAGE, e);
			return Result.err(Error.UnknownQueryError);
		}
	} 

	private void warn(String message, Exception e) {
		logger.warn(message + ": " + e.getClass().getName() + ": " + e.getMessage());
	}	

	//private TypedQuery<User> createQuery(String query) {
	//	return entityManager.createQuery(query, User.class); 
	private TypedQuery<User> createTypedQuery(String query) {
		return entityManager.createQuery(query, User.class); 
		// In the case IllegalArgumentException has been thrown, the query should be illegal,
		// so the service should stop and this would happen during development only.
	}

	private Query createQuery(String query) { return entityManager.createQuery(query); }

	@Override 
	public void close() { entityManager.close(); }

	private TxWrapper getTransaction() { 
		return new TxWrapper(entityManager.getTransaction()); 
	}

	private Result<TxWrapper,Error> txBegin() { return getTransaction().begin(); }	 

	private Result<TxWrapper,Error> txCommit() { return getTransaction().commit(); }

	private Result<TxWrapper,Error> txRollback() { return getTransaction().rollback(); }		

	private static <E extends Exception> String generateExceptionMessage(E e) {
		return e.getClass().getName() + ": " + e.getMessage();
	}
 
	private Result<User,Error> emPersist(User user) throws TransactionRequiredException { 
		try { 
			entityManager.persist(user); return Result.ok(user); 
		} 
		catch (EntityExistsException e) { 
			warn(USER_DUPLICATED_MESSAGE, e);	return Result.err(Error.EntityIsDuplicated);
		}
		catch (IllegalArgumentException e) {
			warn(UNEXPECTED_ERROR_MESSAGE, e); return Result.err(Error.InstanceIsNotEntity);
		}
	}	

	private Result<User,Error> emMerge(User user) 
	 throws TransactionRequiredException { 
	  try { 
			entityManager.merge(user); return Result.ok(user); 
		}
		catch (IllegalArgumentException e) {
			warn(UNEXPECTED_ERROR_MESSAGE, e); return Result.err(Error.InstanceIsNotEntity);
		}
	}

	private Result<UserService,Error> ok() { return Result.ok(this); }

	class TxWrapper {

		private final EntityTransaction tx;

		private TxWrapper(EntityTransaction tx) { 
			Objects.requireNonNull(tx);
			this.tx = tx; 
		}

		private TxWrapper assertActive() throws IllegalStateException {
			if (!isActive())  
				throw new IllegalStateException("The transaction should be active, but isn't."); 
			else 
				return this;  
		}

		private TxWrapper assertInactive() throws IllegalStateException {
			if (isActive())  
				throw new IllegalStateException("The transaction has been active, but it shouldn't.");  
			else 
				return this;  
		}

		public Result<TxWrapper,Error> begin() { 
			try { 
				assertInactive()._begin(); return ok(); 
			} 
			catch (IllegalStateException e) { 
				return Result.err(Error.TxIsAlive); 
			}
		}

		private void _begin() throws IllegalStateException { tx.begin(); }

		public Result<TxWrapper,Error> commit() {
			try { 
				assertActive()._commit(); return ok(); 
			} 
			catch (IllegalStateException e) { 
				return Result.err(Error.TxIsDead); 
			}
			catch (RollbackException e) { 
				return Result.err(Error.CommitFailed); 
			}
		}

		private void _commit() throws IllegalStateException, RollbackException { tx.commit(); }  

		public Result<TxWrapper,Error> rollback() {
			try { 
				assertActive()._rollback(); return ok(); 
			} 
			catch (IllegalStateException e) { 
				return Result.err(Error.TxIsDead); 
			}
			catch (PersistenceException e) {
				return Result.err(Error.UnknownTxError);
			}
		}	

		private void _rollback() throws IllegalStateException, PersistenceException { tx.rollback(); }

		public Result<TxWrapper,Error> setRollbackOnly() {
			try	{ 
				assertActive()._setRollbackOnly(); return ok(); 
			} 
			catch (IllegalStateException e) { 
				return Result.err(Error.TxIsDead);
			}
		}

		private void _setRollbackOnly() throws IllegalStateException { tx.setRollbackOnly(); }

		public Result<Boolean,Error> getRollbackOnly() {
			try { 
				return Result.ok(assertActive()._getRollbackOnly()); 
			} 
			catch (IllegalStateException e) { 
				return Result.err(Error.TxIsDead);
			}
		}

		private boolean _getRollbackOnly() throws IllegalStateException { return tx.getRollbackOnly(); } 

		public boolean isActive() { return tx.isActive(); }

		private Result<TxWrapper,Error> ok() { return Result.ok(this); }

	}
} 
