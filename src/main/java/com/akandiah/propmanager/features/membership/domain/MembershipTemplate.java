package com.akandiah.propmanager.features.membership.domain;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.hibernate.annotations.UuidGenerator;

import com.akandiah.propmanager.features.organization.domain.Organization;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Blueprint for a named role. Contains an ordered list of {@link MembershipTemplateItem}s,
 * each defining permissions at one scope level (ORG, PROPERTY, or UNIT).
 * <p>
 * System-wide templates have {@code org == null}. Org-specific templates have {@code org} set.
 * <p>
 * Template permissions are resolved <em>live</em> at JWT hydration time — not materialised into
 * {@link MemberScope} rows — so edits take effect immediately for all linked memberships.
 */
@Entity
@Table(name = "membership_templates", uniqueConstraints = {
		@UniqueConstraint(name = "uk_membership_templates_org_name", columnNames = { "org_id", "name" })
}, indexes = {
		@Index(name = "idx_membership_templates_org_id", columnList = "org_id")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MembershipTemplate {

	@Id
	@UuidGenerator(style = UuidGenerator.Style.TIME)
	private UUID id;

	/** null = system-wide template; non-null = org-specific template. */
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "org_id")
	private Organization org;

	@Column(name = "name", nullable = false, columnDefinition = "text")
	private String name;

	/**
	 * Ordered scope-level permission sets. At most one item per {@code scopeType}.
	 * Fetched eagerly since items are always needed when a template is resolved.
	 */
	@ElementCollection(fetch = FetchType.EAGER)
	@CollectionTable(name = "membership_template_items",
			joinColumns = @JoinColumn(name = "template_id"))
	@OrderColumn(name = "item_order")
	@Builder.Default
	private List<MembershipTemplateItem> items = new ArrayList<>();

	@Version
	@Column(nullable = false)
	private Integer version;

	@Column(name = "created_at", nullable = false, updatable = false)
	@Setter(AccessLevel.NONE)
	private Instant createdAt;

	@Column(name = "updated_at", nullable = false)
	@Setter(AccessLevel.NONE)
	private Instant updatedAt;

	@PrePersist
	void prePersist() {
		Instant now = Instant.now();
		if (createdAt == null) {
			createdAt = now;
		}
		updatedAt = now;
	}

	@PreUpdate
	void preUpdate() {
		updatedAt = Instant.now();
	}
}
