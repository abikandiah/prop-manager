package com.akandiah.propmanager.features.membership.domain;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import com.akandiah.propmanager.common.permission.ResourceType;

public interface PolicyAssignmentRepository extends JpaRepository<PolicyAssignment, UUID> {

	List<PolicyAssignment> findByMembershipId(UUID membershipId);

	List<PolicyAssignment> findByMembershipIdIn(Collection<UUID> membershipIds);

	List<PolicyAssignment> findByMembershipIdAndResourceType(UUID membershipId, ResourceType resourceType);

	List<PolicyAssignment> findByResourceTypeAndResourceId(ResourceType resourceType, UUID resourceId);

	List<PolicyAssignment> findByPolicyId(UUID policyId);

	long countByPolicyId(UUID policyId);

	@Modifying
	void deleteByMembershipId(UUID membershipId);

	@Modifying
	void deleteByResourceTypeAndResourceId(ResourceType resourceType, UUID resourceId);

	boolean existsByIdAndMembershipId(UUID id, UUID membershipId);

	boolean existsByMembershipIdAndResourceTypeAndResourceId(UUID membershipId, ResourceType resourceType,
			UUID resourceId);

	Optional<PolicyAssignment> findByIdAndMembershipId(UUID id, UUID membershipId);

	/**
	 * Loads assignments with their policy eagerly for JWT hydration.
	 */
	@Query("SELECT pa FROM PolicyAssignment pa LEFT JOIN FETCH pa.policy WHERE pa.membership.id IN :membershipIds")
	List<PolicyAssignment> findByMembershipIdInWithPolicy(Collection<UUID> membershipIds);

	/**
	 * Loads assignments for a specific resource type, with their policy eagerly fetched.
	 * Used to batch-load org-level policy names for the membership list view.
	 */
	@Query("SELECT pa FROM PolicyAssignment pa LEFT JOIN FETCH pa.policy WHERE pa.membership.id IN :membershipIds AND pa.resourceType = :resourceType")
	List<PolicyAssignment> findByMembershipIdInAndResourceTypeWithPolicy(Collection<UUID> membershipIds,
			ResourceType resourceType);
}
