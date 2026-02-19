package com.akandiah.propmanager;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.akandiah.propmanager.features.lease.api.dto.CreateLeaseRequest;
import com.akandiah.propmanager.features.lease.domain.LateFeeType;
import com.akandiah.propmanager.features.lease.domain.Lease;
import com.akandiah.propmanager.features.lease.domain.LeaseStatus;
import com.akandiah.propmanager.features.lease.domain.LeaseTemplate;
import com.akandiah.propmanager.features.prop.api.dto.CreatePropRequest;
import com.akandiah.propmanager.features.prop.api.dto.CreatePropRequest.AddressInput;
import com.akandiah.propmanager.features.prop.api.dto.UpdatePropRequest;
import com.akandiah.propmanager.features.prop.domain.Address;
import com.akandiah.propmanager.features.prop.domain.Prop;
import com.akandiah.propmanager.features.prop.domain.PropertyType;
import com.akandiah.propmanager.features.tenant.domain.Tenant;
import com.akandiah.propmanager.features.unit.domain.Unit;
import com.akandiah.propmanager.features.unit.domain.UnitStatus;
import com.akandiah.propmanager.features.unit.domain.UnitType;
import com.akandiah.propmanager.features.user.domain.User;

/**
 * Central factory for creating test data with sensible defaults.
 * Follows builder pattern for flexibility while maintaining clean code.
 */
public final class TestDataFactory {

	private TestDataFactory() {
		throw new UnsupportedOperationException("Utility class");
	}

	// ═══════════════════════════════════════════════════════════════════════
	// User Builders
	// ═══════════════════════════════════════════════════════════════════════

	public static UserBuilder user() {
		return new UserBuilder();
	}

	public static class UserBuilder {
		private UUID id = UUID.randomUUID();
		private String idpSub = "test-user";
		private String name = "Alice Tenant";
		private String email = "alice@example.com";
		private String phoneNumber = "+1-416-555-0100";
		private String avatarUrl = null;

		public UserBuilder id(UUID id) {
			this.id = id;
			return this;
		}

		public UserBuilder idpSub(String idpSub) {
			this.idpSub = idpSub;
			return this;
		}

		public UserBuilder name(String name) {
			this.name = name;
			return this;
		}

		public UserBuilder email(String email) {
			this.email = email;
			return this;
		}

		public UserBuilder phoneNumber(String phoneNumber) {
			this.phoneNumber = phoneNumber;
			return this;
		}

		public User build() {
			return User.builder()
					.id(id)
					.idpSub(idpSub)
					.name(name)
					.email(email)
					.phoneNumber(phoneNumber)
					.avatarUrl(avatarUrl)
					.build();
		}
	}

	// ═══════════════════════════════════════════════════════════════════════
	// Tenant Builders
	// ═══════════════════════════════════════════════════════════════════════

	public static TenantBuilder tenant() {
		return new TenantBuilder();
	}

	public static class TenantBuilder {
		private UUID id = UUID.randomUUID();
		private User user;
		private String emergencyContactName = "Bob Emergency";
		private String emergencyContactPhone = "+1-416-555-0199";
		private Boolean hasPets = false;
		private String petDescription = null;
		private String vehicleInfo = null;
		private String notes = null;
		private Integer version = 0;

		public TenantBuilder id(UUID id) {
			this.id = id;
			return this;
		}

		public TenantBuilder user(User user) {
			this.user = user;
			return this;
		}

		public TenantBuilder emergencyContactName(String emergencyContactName) {
			this.emergencyContactName = emergencyContactName;
			return this;
		}

		public TenantBuilder emergencyContactPhone(String emergencyContactPhone) {
			this.emergencyContactPhone = emergencyContactPhone;
			return this;
		}

		public TenantBuilder hasPets(Boolean hasPets) {
			this.hasPets = hasPets;
			return this;
		}

		public TenantBuilder petDescription(String petDescription) {
			this.petDescription = petDescription;
			return this;
		}

		public TenantBuilder vehicleInfo(String vehicleInfo) {
			this.vehicleInfo = vehicleInfo;
			return this;
		}

		public TenantBuilder notes(String notes) {
			this.notes = notes;
			return this;
		}

		public TenantBuilder version(Integer version) {
			this.version = version;
			return this;
		}

		public Tenant build() {
			User tenantUser = this.user != null ? this.user : TestDataFactory.user().build();
			return Tenant.builder()
					.id(id)
					.user(tenantUser)
					.emergencyContactName(emergencyContactName)
					.emergencyContactPhone(emergencyContactPhone)
					.hasPets(hasPets)
					.petDescription(petDescription)
					.vehicleInfo(vehicleInfo)
					.notes(notes)
					.version(version)
					.build();
		}
	}

	// ═══════════════════════════════════════════════════════════════════════
	// Address Builders
	// ═══════════════════════════════════════════════════════════════════════

	public static AddressBuilder address() {
		return new AddressBuilder();
	}

	public static class AddressBuilder {
		private UUID id;
		private String addressLine1 = "123 Main Street";
		private String addressLine2;
		private String city = "Toronto";
		private String stateProvinceRegion = "Ontario";
		private String postalCode = "M5V 3A8";
		private String countryCode = "CA";
		private BigDecimal latitude = new BigDecimal("43.651070");
		private BigDecimal longitude = new BigDecimal("-79.347015");

		public AddressBuilder id(UUID id) {
			this.id = id;
			return this;
		}

		public AddressBuilder addressLine1(String addressLine1) {
			this.addressLine1 = addressLine1;
			return this;
		}

		public AddressBuilder addressLine2(String addressLine2) {
			this.addressLine2 = addressLine2;
			return this;
		}

		public AddressBuilder city(String city) {
			this.city = city;
			return this;
		}

		public AddressBuilder stateProvinceRegion(String stateProvinceRegion) {
			this.stateProvinceRegion = stateProvinceRegion;
			return this;
		}

		public AddressBuilder postalCode(String postalCode) {
			this.postalCode = postalCode;
			return this;
		}

		public AddressBuilder countryCode(String countryCode) {
			this.countryCode = countryCode;
			return this;
		}

		public AddressBuilder latitude(BigDecimal latitude) {
			this.latitude = latitude;
			return this;
		}

		public AddressBuilder longitude(BigDecimal longitude) {
			this.longitude = longitude;
			return this;
		}

		public Address build() {
			return Address.builder()
					.id(id)
					.addressLine1(addressLine1)
					.addressLine2(addressLine2)
					.city(city)
					.stateProvinceRegion(stateProvinceRegion)
					.postalCode(postalCode)
					.countryCode(countryCode)
					.latitude(latitude)
					.longitude(longitude)
					.build();
		}

		public AddressInput buildInput() {
			return new AddressInput(
					addressLine1,
					addressLine2,
					city,
					stateProvinceRegion,
					postalCode,
					countryCode,
					latitude,
					longitude);
		}
	}

	// ═══════════════════════════════════════════════════════════════════════
	// Prop Builders
	// ═══════════════════════════════════════════════════════════════════════

	public static PropBuilder prop() {
		return new PropBuilder();
	}

	public static class PropBuilder {
		private UUID id;
		private String legalName = "Sunrise Apartments";
		private Address address;
		private PropertyType propertyType = PropertyType.APARTMENT_BUILDING;
		private String description = "Modern apartment building in downtown";
		private String parcelNumber = "1234-5678-9012";
		private UUID ownerId = UUID.randomUUID();
		private Integer totalArea = 10000;
		private Integer yearBuilt = 2015;
		private Integer version = 0;

		public PropBuilder id(UUID id) {
			this.id = id;
			return this;
		}

		public PropBuilder legalName(String legalName) {
			this.legalName = legalName;
			return this;
		}

		public PropBuilder address(Address address) {
			this.address = address;
			return this;
		}

		public PropBuilder propertyType(PropertyType propertyType) {
			this.propertyType = propertyType;
			return this;
		}

		public PropBuilder description(String description) {
			this.description = description;
			return this;
		}

		public PropBuilder parcelNumber(String parcelNumber) {
			this.parcelNumber = parcelNumber;
			return this;
		}

		public PropBuilder ownerId(UUID ownerId) {
			this.ownerId = ownerId;
			return this;
		}

		public PropBuilder totalArea(Integer totalArea) {
			this.totalArea = totalArea;
			return this;
		}

		public PropBuilder yearBuilt(Integer yearBuilt) {
			this.yearBuilt = yearBuilt;
			return this;
		}

		public PropBuilder version(Integer version) {
			this.version = version;
			return this;
		}

		public Prop build() {
			// Create default address if not set
			Address propAddress = this.address != null ? this.address : TestDataFactory.address().build();

			return Prop.builder()
					.id(id)
					.legalName(legalName)
					.address(propAddress)
					.propertyType(propertyType)
					.description(description)
					.parcelNumber(parcelNumber)
					.ownerId(ownerId)
					.totalArea(totalArea)
					.yearBuilt(yearBuilt)
					.version(version)
					.build();
		}

		public CreatePropRequest buildCreateRequest() {
			Address propAddress = this.address != null ? this.address : TestDataFactory.address().build();
			return new CreatePropRequest(
					legalName,
					new AddressInput(
							propAddress.getAddressLine1(),
							propAddress.getAddressLine2(),
							propAddress.getCity(),
							propAddress.getStateProvinceRegion(),
							propAddress.getPostalCode(),
							propAddress.getCountryCode(),
							propAddress.getLatitude(),
							propAddress.getLongitude()),
					propertyType,
					description,
					parcelNumber,
					ownerId,
					totalArea,
					yearBuilt);
		}

		public UpdatePropRequest buildUpdateRequest() {
			Address propAddress = this.address != null ? this.address : TestDataFactory.address().build();
			return new UpdatePropRequest(
					legalName,
					new AddressInput(
							propAddress.getAddressLine1(),
							propAddress.getAddressLine2(),
							propAddress.getCity(),
							propAddress.getStateProvinceRegion(),
							propAddress.getPostalCode(),
							propAddress.getCountryCode(),
							propAddress.getLatitude(),
							propAddress.getLongitude()),
					propertyType,
					description,
					parcelNumber,
					ownerId,
					totalArea,
					yearBuilt,
					version);
		}
	}

	// ═══════════════════════════════════════════════════════════════════════
	// Unit Builders
	// ═══════════════════════════════════════════════════════════════════════

	public static UnitBuilder unit() {
		return new UnitBuilder();
	}

	public static class UnitBuilder {
		private UUID id;
		private Prop prop;
		private String unitNumber = "101";
		private UnitStatus status = UnitStatus.VACANT;
		private UnitType unitType = UnitType.APARTMENT;
		private String description = "Spacious 2-bedroom unit";
		private BigDecimal rentAmount = new BigDecimal("2000.00");
		private BigDecimal securityDeposit = new BigDecimal("2000.00");
		private Integer bedrooms = 2;
		private Integer bathrooms = 1;
		private Integer squareFootage = 850;
		private Boolean balcony = true;
		private Boolean laundryInUnit = false;
		private Boolean hardwoodFloors = true;
		private Integer version = 0;

		public UnitBuilder id(UUID id) {
			this.id = id;
			return this;
		}

		public UnitBuilder prop(Prop prop) {
			this.prop = prop;
			return this;
		}

		public UnitBuilder unitNumber(String unitNumber) {
			this.unitNumber = unitNumber;
			return this;
		}

		public UnitBuilder status(UnitStatus status) {
			this.status = status;
			return this;
		}

		public UnitBuilder unitType(UnitType unitType) {
			this.unitType = unitType;
			return this;
		}

		public UnitBuilder description(String description) {
			this.description = description;
			return this;
		}

		public UnitBuilder rentAmount(BigDecimal rentAmount) {
			this.rentAmount = rentAmount;
			return this;
		}

		public UnitBuilder version(Integer version) {
			this.version = version;
			return this;
		}

		public Unit build() {
			// Create default prop if not set
			Prop unitProp = this.prop != null ? this.prop : TestDataFactory.prop().id(UUID.randomUUID()).build();

			return Unit.builder()
					.id(id)
					.prop(unitProp)
					.unitNumber(unitNumber)
					.status(status)
					.unitType(unitType)
					.description(description)
					.rentAmount(rentAmount)
					.securityDeposit(securityDeposit)
					.bedrooms(bedrooms)
					.bathrooms(bathrooms)
					.squareFootage(squareFootage)
					.balcony(balcony)
					.laundryInUnit(laundryInUnit)
					.hardwoodFloors(hardwoodFloors)
					.version(version)
					.build();
		}
	}

	// ═══════════════════════════════════════════════════════════════════════
	// LeaseTemplate Builders
	// ═══════════════════════════════════════════════════════════════════════

	public static LeaseTemplateBuilder leaseTemplate() {
		return new LeaseTemplateBuilder();
	}

	public static class LeaseTemplateBuilder {
		private UUID id;
		private String name = "Standard Residential Lease";
		private String versionTag = "v1.0";
		private Integer version = 0;
		private String templateMarkdown = "# Lease Agreement\n\n"
				+ "Property: {{property_name}}\n"
				+ "Unit: {{unit_number}}\n"
				+ "Start Date: {{start_date}}\n"
				+ "End Date: {{end_date}}\n"
				+ "Rent: ${{rent_amount}} due on day {{rent_due_day}} of each month\n"
				+ "Security Deposit: ${{security_deposit}}\n";
		private LateFeeType defaultLateFeeType = LateFeeType.FLAT_FEE;
		private BigDecimal defaultLateFeeAmount = new BigDecimal("50.00");
		private Integer defaultNoticePeriodDays = 60;
		private boolean active = true;
		private Map<String, String> templateParameters = new HashMap<>();

		public LeaseTemplateBuilder id(UUID id) {
			this.id = id;
			return this;
		}

		public LeaseTemplateBuilder name(String name) {
			this.name = name;
			return this;
		}

		public LeaseTemplateBuilder versionTag(String versionTag) {
			this.versionTag = versionTag;
			return this;
		}

		public LeaseTemplateBuilder version(Integer version) {
			this.version = version;
			return this;
		}

		public LeaseTemplateBuilder templateMarkdown(String templateMarkdown) {
			this.templateMarkdown = templateMarkdown;
			return this;
		}

		public LeaseTemplateBuilder defaultLateFeeType(LateFeeType defaultLateFeeType) {
			this.defaultLateFeeType = defaultLateFeeType;
			return this;
		}

		public LeaseTemplateBuilder defaultLateFeeAmount(BigDecimal defaultLateFeeAmount) {
			this.defaultLateFeeAmount = defaultLateFeeAmount;
			return this;
		}

		public LeaseTemplateBuilder defaultNoticePeriodDays(Integer defaultNoticePeriodDays) {
			this.defaultNoticePeriodDays = defaultNoticePeriodDays;
			return this;
		}

		public LeaseTemplateBuilder active(boolean active) {
			this.active = active;
			return this;
		}

		public LeaseTemplateBuilder templateParameters(Map<String, String> templateParameters) {
			this.templateParameters = templateParameters;
			return this;
		}

		public LeaseTemplate build() {
			return LeaseTemplate.builder()
					.id(id)
					.name(name)
					.versionTag(versionTag)
					.version(version)
					.templateMarkdown(templateMarkdown)
					.defaultLateFeeType(defaultLateFeeType)
					.defaultLateFeeAmount(defaultLateFeeAmount)
					.defaultNoticePeriodDays(defaultNoticePeriodDays)
					.active(active)
					.templateParameters(templateParameters)
					.build();
		}
	}

	// ═══════════════════════════════════════════════════════════════════════
	// Lease Builders
	// ═══════════════════════════════════════════════════════════════════════

	public static LeaseBuilder lease() {
		return new LeaseBuilder();
	}

	public static class LeaseBuilder {
		private UUID id;
		private LeaseTemplate leaseTemplate;
		private String leaseTemplateName = "Standard Residential Lease";
		private String leaseTemplateVersionTag = "v1.0";
		private Unit unit;
		private Prop property;
		private Integer version = 0;
		private LeaseStatus status = LeaseStatus.DRAFT;
		private LocalDate startDate = LocalDate.now().plusMonths(1);
		private LocalDate endDate = LocalDate.now().plusMonths(13);
		private BigDecimal rentAmount = new BigDecimal("2000.00");
		private String executedContentMarkdown;
		private Integer rentDueDay = 1;
		private BigDecimal securityDepositHeld = new BigDecimal("2000.00");
		private LateFeeType lateFeeType = LateFeeType.FLAT_FEE;
		private BigDecimal lateFeeAmount = new BigDecimal("50.00");
		private Integer noticePeriodDays = 60;
		private Map<String, Object> additionalMetadata = new HashMap<>();
		private Map<String, String> templateParameters = new HashMap<>();

		public LeaseBuilder id(UUID id) {
			this.id = id;
			return this;
		}

		public LeaseBuilder leaseTemplate(LeaseTemplate leaseTemplate) {
			this.leaseTemplate = leaseTemplate;
			this.leaseTemplateName = leaseTemplate.getName();
			this.leaseTemplateVersionTag = leaseTemplate.getVersionTag();
			return this;
		}

		public LeaseBuilder unit(Unit unit) {
			this.unit = unit;
			return this;
		}

		public LeaseBuilder property(Prop property) {
			this.property = property;
			return this;
		}

		public LeaseBuilder version(Integer version) {
			this.version = version;
			return this;
		}

		public LeaseBuilder status(LeaseStatus status) {
			this.status = status;
			return this;
		}

		public LeaseBuilder startDate(LocalDate startDate) {
			this.startDate = startDate;
			return this;
		}

		public LeaseBuilder endDate(LocalDate endDate) {
			this.endDate = endDate;
			return this;
		}

		public LeaseBuilder rentAmount(BigDecimal rentAmount) {
			this.rentAmount = rentAmount;
			return this;
		}

		public LeaseBuilder executedContentMarkdown(String executedContentMarkdown) {
			this.executedContentMarkdown = executedContentMarkdown;
			return this;
		}

		public LeaseBuilder rentDueDay(Integer rentDueDay) {
			this.rentDueDay = rentDueDay;
			return this;
		}

		public LeaseBuilder securityDepositHeld(BigDecimal securityDepositHeld) {
			this.securityDepositHeld = securityDepositHeld;
			return this;
		}

		public LeaseBuilder lateFeeType(LateFeeType lateFeeType) {
			this.lateFeeType = lateFeeType;
			return this;
		}

		public LeaseBuilder lateFeeAmount(BigDecimal lateFeeAmount) {
			this.lateFeeAmount = lateFeeAmount;
			return this;
		}

		public LeaseBuilder noticePeriodDays(Integer noticePeriodDays) {
			this.noticePeriodDays = noticePeriodDays;
			return this;
		}

		public LeaseBuilder templateParameters(Map<String, String> templateParameters) {
			this.templateParameters = templateParameters;
			return this;
		}

		public Lease build() {
			// Create defaults if not set
			LeaseTemplate template = this.leaseTemplate != null ? this.leaseTemplate : TestDataFactory.leaseTemplate().id(UUID.randomUUID()).build();
			Unit leaseUnit = this.unit != null ? this.unit : TestDataFactory.unit().id(UUID.randomUUID()).build();
			Prop leaseProp = this.property != null ? this.property : TestDataFactory.prop().id(UUID.randomUUID()).build();

			return Lease.builder()
					.id(id)
					.leaseTemplate(template)
					.leaseTemplateName(leaseTemplateName)
					.leaseTemplateVersionTag(leaseTemplateVersionTag)
					.unit(leaseUnit)
					.property(leaseProp)
					.version(version)
					.status(status)
					.startDate(startDate)
					.endDate(endDate)
					.rentAmount(rentAmount)
					.executedContentMarkdown(executedContentMarkdown)
					.rentDueDay(rentDueDay)
					.securityDepositHeld(securityDepositHeld)
					.lateFeeType(lateFeeType)
					.lateFeeAmount(lateFeeAmount)
					.noticePeriodDays(noticePeriodDays)
					.additionalMetadata(additionalMetadata)
					.templateParameters(templateParameters)
					.build();
		}

		public CreateLeaseRequest buildCreateRequest() {
			LeaseTemplate template = this.leaseTemplate != null ? this.leaseTemplate : TestDataFactory.leaseTemplate().id(UUID.randomUUID()).build();
			Unit leaseUnit = this.unit != null ? this.unit : TestDataFactory.unit().id(UUID.randomUUID()).build();
			Prop leaseProp = this.property != null ? this.property : TestDataFactory.prop().id(UUID.randomUUID()).build();

			return new CreateLeaseRequest(
					template.getId(),
					leaseUnit.getId(),
					leaseProp.getId(),
					startDate,
					endDate,
					rentAmount,
					rentDueDay,
					securityDepositHeld,
					lateFeeType,
					lateFeeAmount,
					noticePeriodDays,
					additionalMetadata,
					templateParameters);
		}
	}
}
