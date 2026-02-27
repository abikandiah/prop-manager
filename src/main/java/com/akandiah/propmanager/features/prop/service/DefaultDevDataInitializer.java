package com.akandiah.propmanager.features.prop.service;

import java.math.BigDecimal;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.akandiah.propmanager.features.membership.domain.MembershipRepository;
import com.akandiah.propmanager.features.membership.service.MemberScopeService;
import com.akandiah.propmanager.features.membership.service.MembershipService;
import com.akandiah.propmanager.features.organization.domain.Organization;
import com.akandiah.propmanager.features.organization.domain.OrganizationRepository;
import com.akandiah.propmanager.features.prop.domain.Address;
import com.akandiah.propmanager.features.prop.domain.AddressRepository;
import com.akandiah.propmanager.features.prop.domain.Prop;
import com.akandiah.propmanager.features.prop.domain.PropRepository;
import com.akandiah.propmanager.features.prop.domain.PropertyType;
import com.akandiah.propmanager.features.unit.domain.Unit;
import com.akandiah.propmanager.features.unit.domain.UnitRepository;
import com.akandiah.propmanager.features.unit.domain.UnitStatus;
import com.akandiah.propmanager.features.unit.domain.UnitType;
import com.akandiah.propmanager.features.user.domain.User;
import com.akandiah.propmanager.features.user.service.UserService;

/**
 * Seeds default properties and units for local development.
 *
 * <p>Only active in the 'dev' profile. Idempotent — skips seeding if any props
 * already exist. Creates a dev user (identity prop-manager-dev / dev@example.com) if none is found, so
 * seeded props always have a real ownerId.
 *
 * <p>Props and units are defined declaratively via {@link PropBlueprint} and
 * {@link UnitBlueprint}. Unit statuses are assigned by cycling through
 * {@link #STATUS_CYCLE} to produce a realistic occupancy mix.
 */
@Component
@Profile("dev")
public class DefaultDevDataInitializer implements ApplicationRunner {

	private static final Logger log = LoggerFactory.getLogger(DefaultDevDataInitializer.class);

	private static final String DEV_ISSUER = "https://prop-manager-dev";
	private static final String DEV_EMAIL = "dev@example.com";

	/**
	 * Cycles across units to produce a realistic ~65% occupied / 20% vacant /
	 * 15% notice-given occupancy distribution.
	 */
	private static final UnitStatus[] STATUS_CYCLE = {
			UnitStatus.OCCUPIED,
			UnitStatus.OCCUPIED,
			UnitStatus.OCCUPIED,
			UnitStatus.VACANT,
			UnitStatus.OCCUPIED,
			UnitStatus.NOTICE_GIVEN,
			UnitStatus.OCCUPIED,
			UnitStatus.OCCUPIED,
			UnitStatus.VACANT,
			UnitStatus.OCCUPIED,
	};

	// -------------------------------------------------------------------------
	// Blueprints
	// -------------------------------------------------------------------------

	/**
	 * Describes a property and its unit groups to seed.
	 *
	 * @param legalName             property display name
	 * @param addressLine1          street address
	 * @param city                  city
	 * @param stateProvinceRegion   state/province/region code
	 * @param postalCode            postal / ZIP code
	 * @param countryCode           ISO 3166-1 alpha-2 country code
	 * @param propertyType          property classification
	 * @param description           human-readable description
	 * @param totalArea             total area in square feet
	 * @param yearBuilt             year of construction
	 * @param unitGroups            ordered list of unit groups to generate
	 */
	private record PropBlueprint(
			String legalName,
			String addressLine1,
			String city,
			String stateProvinceRegion,
			String postalCode,
			String countryCode,
			PropertyType propertyType,
			String description,
			int totalArea,
			int yearBuilt,
			List<UnitBlueprint> unitGroups) {}

	/**
	 * Describes a group of similar units within a property.
	 *
	 * <p>Unit numbers are generated as {@code unitPrefix + zero-padded index}
	 * (e.g. prefix {@code "1"} with count 4 → "101", "102", "103", "104").
	 *
	 * @param unitPrefix    prefix for unit numbers (e.g. "1" for floor 1, "A-" for building A)
	 * @param count         number of units to generate in this group
	 * @param type          unit type
	 * @param baseRent      monthly rent
	 * @param baseDeposit   security deposit
	 * @param bedrooms      bedroom count (null for commercial units)
	 * @param bathrooms     bathroom count (null for commercial units)
	 * @param squareFootage unit area in square feet
	 * @param balcony       has balcony
	 * @param laundryInUnit has in-unit laundry
	 * @param hardwoodFloors has hardwood floors
	 */
	private record UnitBlueprint(
			String unitPrefix,
			int count,
			UnitType type,
			BigDecimal baseRent,
			BigDecimal baseDeposit,
			Integer bedrooms,
			Integer bathrooms,
			int squareFootage,
			boolean balcony,
			boolean laundryInUnit,
			boolean hardwoodFloors) {}

	// -------------------------------------------------------------------------
	// Prop definitions
	// -------------------------------------------------------------------------

	private static final List<PropBlueprint> PROP_BLUEPRINTS = List.of(

			// Prop 1: mid-size residential apartment building, 2 floors, 8 units
			new PropBlueprint(
					"Maple Street Apartments",
					"123 Maple Street", "Springfield", "IL", "62701", "US",
					PropertyType.APARTMENT_BUILDING,
					"A mid-size residential apartment building in a quiet Springfield neighborhood.",
					6800, 1998,
					List.of(
							// Floor 1 — 2BR/1BA, 850 sqft
							new UnitBlueprint("1", 4, UnitType.APARTMENT,
									new BigDecimal("1200.00"), new BigDecimal("1200.00"),
									2, 1, 850, false, false, true),
							// Floor 2 — 3BR/2BA, 1100 sqft
							new UnitBlueprint("2", 4, UnitType.APARTMENT,
									new BigDecimal("1600.00"), new BigDecimal("1600.00"),
									3, 2, 1100, true, true, false))),

			// Prop 2: commercial plaza, 3 floors, 12 units
			new PropBlueprint(
					"Riverside Commercial Plaza",
					"400 Riverside Drive", "Chicago", "IL", "60601", "US",
					PropertyType.COMMERCIAL,
					"A modern three-story commercial plaza with ground-floor retail and upper-floor office suites.",
					18500, 2005,
					List.of(
							// Floor 1 — street-level retail
							new UnitBlueprint("R-1", 4, UnitType.RETAIL,
									new BigDecimal("2800.00"), new BigDecimal("5600.00"),
									null, null, 1200, false, false, false),
							// Floor 2 — office suites
							new UnitBlueprint("S-2", 4, UnitType.SUITE,
									new BigDecimal("1900.00"), new BigDecimal("3800.00"),
									null, null, 650, false, false, false),
							// Floor 3 — premium office suites
							new UnitBlueprint("S-3", 4, UnitType.SUITE,
									new BigDecimal("2100.00"), new BigDecimal("4200.00"),
									null, null, 700, true, false, false))),

			// Prop 3: townhome complex, 2 buildings, 15 units
			new PropBlueprint(
					"Oakwood Townhomes",
					"88 Oakwood Boulevard", "Austin", "TX", "78701", "US",
					PropertyType.TOWNHOUSE,
					"A well-maintained townhome complex split across two buildings, with spacious floor plans and private entrances.",
					24000, 2012,
					List.of(
							// Building A — 3BR/2BA
							new UnitBlueprint("A-", 8, UnitType.APARTMENT,
									new BigDecimal("2000.00"), new BigDecimal("2000.00"),
									3, 2, 1400, true, true, true),
							// Building B — 2BR/2BA
							new UnitBlueprint("B-", 7, UnitType.APARTMENT,
									new BigDecimal("1700.00"), new BigDecimal("1700.00"),
									2, 2, 1100, false, true, false))));

	// -------------------------------------------------------------------------
	// Dependencies
	// -------------------------------------------------------------------------

	private final UserService userService;
	private final PropRepository propRepository;
	private final AddressRepository addressRepository;
	private final UnitRepository unitRepository;
	private final OrganizationRepository organizationRepository;
	private final MembershipService membershipService;
	private final MemberScopeService memberScopeService;
	private final MembershipRepository membershipRepository;

	public DefaultDevDataInitializer(
			UserService userService,
			PropRepository propRepository,
			AddressRepository addressRepository,
			UnitRepository unitRepository,
			OrganizationRepository organizationRepository,
			MembershipService membershipService,
			MemberScopeService memberScopeService,
			MembershipRepository membershipRepository) {
		this.userService = userService;
		this.propRepository = propRepository;
		this.addressRepository = addressRepository;
		this.unitRepository = unitRepository;
		this.organizationRepository = organizationRepository;
		this.membershipService = membershipService;
		this.memberScopeService = memberScopeService;
		this.membershipRepository = membershipRepository;
	}

	// -------------------------------------------------------------------------
	// Runner
	// -------------------------------------------------------------------------

	@Override
	@Transactional
	public void run(ApplicationArguments args) {
		User devUser = resolveDevUser();
		Organization devOrg = resolveDevOrganization();

		// Seed membership if not exists
		seedDevMembership(devUser, devOrg);

		long propCount = propRepository.count();
		if (propCount > 0) {
			log.info("[Data Init] Found {} existing prop(s), skipping dev seed", propCount);
			return;
		}

		log.info("[Data Init] No props found. Seeding {} dev props...", PROP_BLUEPRINTS.size());

		int totalUnits = 0;
		for (PropBlueprint blueprint : PROP_BLUEPRINTS) {
			int count = seedProp(blueprint, devUser, devOrg);
			totalUnits += count;
		}

		log.info("[Data Init] Dev seed complete — {} props, {} units total", PROP_BLUEPRINTS.size(), totalUnits);
	}

	// -------------------------------------------------------------------------
	// Seeding helpers
	// -------------------------------------------------------------------------

	/**
	 * Ensures the dev user has an ORG-level admin membership in the dev organization.
	 */
	private void seedDevMembership(User user, Organization org) {
		if (membershipRepository.existsByUserIdAndOrganizationId(user.getId(), org.getId())) {
			log.info("[Data Init] Dev user already has membership in organization '{}'", org.getName());
			return;
		}

		log.info("[Data Init] Seeding admin membership for dev user in organization '{}'", org.getName());
		var membership = membershipService.create(org.getId(),
				new com.akandiah.propmanager.features.membership.api.dto.CreateMembershipRequest(null, user.getId()));

		memberScopeService.create(membership.id(), new com.akandiah.propmanager.features.membership.api.dto.CreateMemberScopeRequest(
				null,
				com.akandiah.propmanager.common.permission.ResourceType.ORG,
				org.getId(),
				java.util.Map.of("l", "rcud", "m", "rcud", "f", "rcud", "t", "rcud", "o", "rcud", "p", "rcud")));
	}

	/**
	 * Finds the dev user by identity (prop-manager-dev / dev@example.com) or creates one if absent.
	 * Matches the issuer and subject used by DevAuthController's dev login.
	 */
	private User resolveDevUser() {
		return userService.getOrCreateUser(DEV_ISSUER, DEV_EMAIL, "Dev User", DEV_EMAIL);
	}

	/**
	 * Returns the first existing organization, or creates a "Dev Organization" if none exists.
	 */
	private Organization resolveDevOrganization() {
		return organizationRepository.findAll().stream()
				.findFirst()
				.orElseGet(() -> organizationRepository.save(
						Organization.builder().name("Dev Organization").build()));
	}

	/** Creates a prop from a blueprint and generates all its units. Returns the unit count. */
	private int seedProp(PropBlueprint blueprint, User owner, Organization organization) {
		Address address = addressRepository.save(Address.builder()
				.addressLine1(blueprint.addressLine1())
				.city(blueprint.city())
				.stateProvinceRegion(blueprint.stateProvinceRegion())
				.postalCode(blueprint.postalCode())
				.countryCode(blueprint.countryCode())
				.build());

		Prop prop = propRepository.save(Prop.builder()
				.legalName(blueprint.legalName())
				.address(address)
				.propertyType(blueprint.propertyType())
				.description(blueprint.description())
				.organization(organization)
				.ownerId(owner.getId())
				.totalArea(blueprint.totalArea())
				.yearBuilt(blueprint.yearBuilt())
				.build());

		int unitCount = seedUnits(prop, blueprint.unitGroups());
		log.info("[Data Init] Created prop '{}' with {} units", prop.getLegalName(), unitCount);
		return unitCount;
	}

	/**
	 * Generates units for all groups in order. Statuses are assigned by cycling
	 * through {@link #STATUS_CYCLE} across the full unit sequence for the prop,
	 * so the distribution is consistent regardless of group boundaries.
	 */
	private int seedUnits(Prop prop, List<UnitBlueprint> groups) {
		int globalIndex = 0;
		for (UnitBlueprint group : groups) {
			for (int i = 0; i < group.count(); i++) {
				String unitNumber = group.unitPrefix() + String.format("%02d", i + 1);
				UnitStatus status = STATUS_CYCLE[globalIndex % STATUS_CYCLE.length];

				unitRepository.save(Unit.builder()
						.prop(prop)
						.unitNumber(unitNumber)
						.unitType(group.type())
						.status(status)
						.rentAmount(group.baseRent())
						.securityDeposit(group.baseDeposit())
						.bedrooms(group.bedrooms())
						.bathrooms(group.bathrooms())
						.squareFootage(group.squareFootage())
						.balcony(group.balcony())
						.laundryInUnit(group.laundryInUnit())
						.hardwoodFloors(group.hardwoodFloors())
						.build());

				globalIndex++;
			}
		}
		return globalIndex;
	}
}
