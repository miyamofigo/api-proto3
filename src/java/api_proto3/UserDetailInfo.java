package api_proto3;

import java.util.Calendar;
import java.util.Date;
import java.util.Optional;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
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
@Table( name = "user_detail_info" )
public class UserDetailInfo {
	private Long id;
	private UserBasicInfo userBasicInfo; 

	private String firstname;
	private String familyname;
	private Calendar birthdate;
	private int age;

	//public static enum Sex { Male, Female; }
	public static enum Sex { 

		Male, Female;

		public static Optional<Sex> valueOf(int id) {
			for (Sex sex : values())
				if (sex.ordinal() == id) return Optional.of(sex);
			return Optional.empty();
		}
	}

	private Sex sex;
	 		
	private Date created;
	private Date updated;
	
	// this form used by Hibernate
	public UserDetailInfo() {}

	public UserDetailInfo(
		UserBasicInfo userBasicInfo, String firstname, String familyname,
		int year, int month, int day_of_month, Sex sex
	) {
		this.userBasicInfo = userBasicInfo;
		this.firstname = firstname;
		this.familyname = familyname;
	 	this.birthdate = new Calendar.Builder()
														 		 .setDate(year, month, day_of_month)
														 		 .build();
		final Calendar today = Calendar.getInstance();
		this.age = today.get(Calendar.YEAR) - this.birthdate.get(Calendar.YEAR);
	 	if (today.get(Calendar.DAY_OF_YEAR) 
				 < this.birthdate.get(Calendar.DAY_OF_YEAR))
			this.age--;
		this.sex = sex;	
	}

	public UserDetailInfo(
		UserBasicInfo userBasicInfo, String firstname, String familyname,
		int year, int month, int day_of_month, 
		Sex sex, Date created, Date updated
	) {
		this.userBasicInfo = userBasicInfo;
		this.firstname = firstname;
		this.familyname = familyname;
	 	this.birthdate = new Calendar.Builder()
														 		 .setDate(year, month, day_of_month)
														 		 .build();
		final Calendar today = Calendar.getInstance();
		this.age = today.get(Calendar.YEAR) - this.birthdate.get(Calendar.YEAR);
	 	if (today.get(Calendar.DAY_OF_YEAR) 
				 < this.birthdate.get(Calendar.DAY_OF_YEAR))
			this.age--;
		this.sex = sex;
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

	@Column( name = "first_name" )
	public String getFirstname() { return firstname; } 
	public void setFirstname(String firstname) { this.firstname = firstname; }

	@Column( name = "family_name" )
	public String getFamilyname() { return familyname; } 
	public void setFamilyname(String familyname) { this.familyname = familyname; }

	@Temporal( TemporalType.TIMESTAMP ) @Column( name = "birth_date" ) 
	public Calendar getBirthDate() { return birthdate; }
	public void setBirthDate(Calendar birthdate) { this.birthdate = birthdate; } 

	@Column( name = "age" )
	public int getAge() { return age; } 
	public void setAge(int age) { this.age = age; }

	@Column( name = "sex" ) @Enumerated( EnumType.ORDINAL ) 
	public Sex getSex() { return sex; }
	public void setSex(Sex sex) { this.sex = sex; }

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

