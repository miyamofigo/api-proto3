package api_proto3;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.OneToOne;
import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Parameter;

@Entity
public class User {
	private Long id;
 	private String hash;
	
	private UserBasicInfo userBasicInfo;
	private UserDetailInfo userDetailInfo;
	private UserContactInfo userContactInfo; 

	public User() {}

	private User(
		String hash,
		UserBasicInfo userBasicInfo,
		UserDetailInfo userDetailInfo,
		UserContactInfo userContactInfo
	) {
		this.hash = hash;
		this.userBasicInfo = userBasicInfo;	
		this.userDetailInfo = userDetailInfo;	
		this.userContactInfo = userContactInfo;	
	}

	public static User createUser(
		String username, String email, String hash,
		String userFirstName, String userFamilyName,
		int year, int month, int day_of_month, UserDetailInfo.Sex sex,
		String postalId, String address, 
		String city, String state, String phoneNum
	) {
		UserBasicInfo userBasicInfo 
			= new UserBasicInfo(username, email, hash);
		return new User(
						 hash, 
						 userBasicInfo, 
						 new UserDetailInfo(userBasicInfo, userFirstName, userFamilyName, year, month, day_of_month, sex),
						 new UserContactInfo(userBasicInfo, postalId, address, city, state, phoneNum)
					 );	
	}

	public static User createUser(
		String username, String email, String hash,
		String userFirstName, String userFamilyName,
		int year, int month, int day_of_month, int sex,
		String postalId, String address, 
		String city, String state, String phoneNum
	) {
		return User.createUser(
		  username, email, hash, userFirstName, userFamilyName,
			year, month, day_of_month, UserDetailInfo.Sex.valueOf(sex).get(), postalId,
			address, city, state, phoneNum
		);
	}		
				
	@GenericGenerator( 
		name = "generator", strategy = "foreign",
	  parameters = @Parameter( name = "property", value = "userBasicInfo" )
	)
	@Id @GeneratedValue( generator = "generator" )
	@Column( unique = true, nullable = false )	
	public Long getId() { return id; }
	private void setId(Long id) { this.id = id; }

	@Column( unique = true, nullable = false )	
	public String getHash() { return hash; }
	private void setHash(String hash) { this.hash = hash; }

	@OneToOne( fetch = FetchType.LAZY, cascade = CascadeType.ALL )
	public UserBasicInfo getUserBasicInfo() { return userBasicInfo; }
	public void setUserBasicInfo(UserBasicInfo userBasicInfo) { 
		this.userBasicInfo = userBasicInfo; 
	}

	@OneToOne( fetch = FetchType.LAZY, cascade = CascadeType.ALL )
	public UserDetailInfo getUserDetailInfo() { return userDetailInfo; }
	public void setUserDetailInfo(UserDetailInfo userDetailInfo) { 
		this.userDetailInfo = userDetailInfo; 
	}

	@OneToOne( fetch = FetchType.LAZY, cascade = CascadeType.ALL )
	public UserContactInfo getUserContactInfo() { return userContactInfo; }
	public void setUserContactInfo(UserContactInfo userContactInfo) { 
		this.userContactInfo = userContactInfo; 
	}
}	
