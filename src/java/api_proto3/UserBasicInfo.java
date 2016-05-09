package api_proto3;

import java.util.Date;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import org.hibernate.annotations.GenericGenerator;

@Entity
@Table( name = "USER_BASIC_INFO" )
public class UserBasicInfo {
	private Long id;

	@Column( name = "USERNAME" ) private String username;
	@Column( name = "EMAIL" ) private String email;
	@Column( name = "HASH" ) private String hash;

	@Temporal( TemporalType.TIMESTAMP ) 
	@Column( name = "CREATED" ) 
	private Date created;
	@Temporal( TemporalType.TIMESTAMP ) 
	@Column( name = "UPDATED" ) 
	private Date updated;
	
	// this form used by Hibernate
	public UserBasicInfo() {}

	public UserBasicInfo(String username, String email, String hash) {
		this.username = username;
	 	this.email = email;
		this.hash = hash;	
	}

	public UserBasicInfo(
		String username, String email, String hash, 
		Date created, Date updated
	) {
		this.username = username;
		this.email = email;
	 	this.hash = hash;
		this.created = created;
		this.updated = updated;
	} 	

	@Id
	@GeneratedValue( generator = "increment" )
	@GenericGenerator( name = "increment", strategy = "increment" )
	public Long getId() { return id; }
	private void setId(Long id) { this.id = id; }

	public String getUserName() { return username; } 
	public void setUserName(String username) { this.username = username; } 

	public String getEmail() { return email; } 
	public void setEmail(String email) { this.email = email; } 

	public String getHash() { return hash; } 
	public void setHash(String hash) { this.hash = hash; } 

	public Date getCreated() { return created; }
	public void setCreated(Date created) { this.created = created; }

	public Date getUpdated() { return updated; }
	public void setUpdated(Date updated) { this.updated = updated; }

	@PrePersist
	public void onCreate() { created = new Date(); }
	@PreUpdate
	public void onUpdate() { updated = new Date(); }
}	

