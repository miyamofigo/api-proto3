package api_proto3;

import java.util.Calendar;
import java.util.Date;

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
@Table( name = "user_contact_info" )
public class UserContactInfo {
	private Long id;
	private UserBasicInfo userBasicInfo;

	private String postalId;
	private String address;
	private String city;
	private String state;
	private String phoneNum;

	private Date created;
	private Date updated;
	
	// this form used by Hibernate
	public UserContactInfo() {}

	public UserContactInfo(
		UserBasicInfo userBasicInfo, String postalId, String address,
		String city, String state, String phoneNum
	) {
		this.userBasicInfo = userBasicInfo;
		this.postalId = postalId;
		this.address = address;
		this.city = city;
		this.state = state;
		this.phoneNum = phoneNum;
	}

	public UserContactInfo(
		UserBasicInfo userBasicInfo, String postalId, String address,
		String city, String state, String phoneNum,
		Date created, Date updated
	) {
		this.userBasicInfo = userBasicInfo;
		this.postalId = postalId;
		this.address = address;
		this.city = city;
		this.state = state;
		this.phoneNum = phoneNum;
		this.created = created;
		this.updated = updated;	
	}

	@GenericGenerator( 
		name = "generator", strategy = "foreign",
	  parameters = @Parameter( name = "property", value = "userBasicInfo" )
	)
	@Id @GeneratedValue( generator = "generator" )
	@Column( unique = true, nullable = false )	
	public Long getId() { return id; }
	private void setId(Long id) { this.id = id; }

	@OneToOne( fetch = FetchType.LAZY, cascade = CascadeType.ALL )
	public UserBasicInfo getUserBasicInfo() { return userBasicInfo; }
	public void setUserBasicInfo(UserBasicInfo userBasicInfo) { 
		this.userBasicInfo = userBasicInfo; 
	}

	@Column( name = "postalid" )
	public String getPostalId() { return postalId; }
	public void setPostalId(String postalId) { this.postalId = postalId; }

	@Column( name = "address" )
	public String getAddress() { return address; }
	public void setAddress(String address) { this.address = address; }

	@Column( name = "city" )
	public String getCity() { return city; }
	public void setCity(String city) { this.city = city; }

	@Column( name = "state" )
	public String getState() { return state; }
	public void setState(String state) { this.state = state; }

	@Column( name = "phone_number")
	public String getPhoneNum() { return phoneNum; }
	public void setPhoneNum(String phoneNum) { this.phoneNum = phoneNum; }

	@Temporal( TemporalType.TIMESTAMP ) @Column( name = "created" ) 
	public Date getCreated() { return created; }
	public void setCreated(Date created) { this.created = created; }

	@Temporal( TemporalType.TIMESTAMP ) @Column( name = "updated" ) 
	public Date getUpdated() { return updated; }
	public void setUpdated(Date updated) { this.updated = updated; }

	@PrePersist
	public void onCreate() { created = new Date(); }
	@PreUpdate
	public void onUpdate() { updated = new Date(); }
}	

