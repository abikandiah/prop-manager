package com.akandiah.propmanager.features.unit.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import com.akandiah.propmanager.TestDataFactory;
import com.akandiah.propmanager.common.exception.ResourceNotFoundException;
import com.akandiah.propmanager.common.permission.ResourceType;
import com.akandiah.propmanager.features.asset.domain.AssetRepository;
import com.akandiah.propmanager.features.auth.domain.PermissionsChangedEvent;
import com.akandiah.propmanager.features.lease.domain.LeaseRepository;
import com.akandiah.propmanager.features.membership.domain.MemberScope;
import com.akandiah.propmanager.features.membership.domain.MemberScopeRepository;
import com.akandiah.propmanager.features.membership.domain.Membership;
import com.akandiah.propmanager.features.unit.domain.Unit;
import com.akandiah.propmanager.features.unit.domain.UnitRepository;
import com.akandiah.propmanager.features.user.domain.User;

@ExtendWith(MockitoExtension.class)
class UnitServiceTest {

	@Mock
	private UnitRepository unitRepository;
	@Mock
	private MemberScopeRepository memberScopeRepository;
	@Mock
	private AssetRepository assetRepository;
	@Mock
	private LeaseRepository leaseRepository;
	@Mock
	private ApplicationEventPublisher eventPublisher;

	@InjectMocks
	private UnitService service;

	@Test
	void deleteById_deletesAndPublishesEventForAffectedUsers() {
		UUID unitId = UUID.randomUUID();
		UUID userId = UUID.randomUUID();
		User user = TestDataFactory.user().id(userId).build();
		Membership membership = Membership.builder().user(user).build();
		MemberScope scope = MemberScope.builder().membership(membership).build();

		when(unitRepository.existsById(unitId)).thenReturn(true);
		when(assetRepository.countByUnit_Id(unitId)).thenReturn(0L);
		when(leaseRepository.countByUnit_Id(unitId)).thenReturn(0L);
		when(memberScopeRepository.findByScopeTypeAndScopeId(ResourceType.UNIT, unitId))
				.thenReturn(List.of(scope));

		service.deleteById(unitId);

		verify(memberScopeRepository).deleteByScopeTypeAndScopeId(ResourceType.UNIT, unitId);
		verify(unitRepository).deleteById(unitId);

		ArgumentCaptor<PermissionsChangedEvent> eventCaptor = ArgumentCaptor.forClass(PermissionsChangedEvent.class);
		verify(eventPublisher).publishEvent(eventCaptor.capture());
		assertThat(eventCaptor.getValue().affectedUserIds()).isEqualTo(Set.of(userId));
	}

	@Test
	void deleteById_deletesWithoutEventIfNoAffectedUsers() {
		UUID unitId = UUID.randomUUID();

		when(unitRepository.existsById(unitId)).thenReturn(true);
		when(assetRepository.countByUnit_Id(unitId)).thenReturn(0L);
		when(leaseRepository.countByUnit_Id(unitId)).thenReturn(0L);
		when(memberScopeRepository.findByScopeTypeAndScopeId(ResourceType.UNIT, unitId))
				.thenReturn(List.of());

		service.deleteById(unitId);

		verify(memberScopeRepository).deleteByScopeTypeAndScopeId(ResourceType.UNIT, unitId);
		verify(unitRepository).deleteById(unitId);
		verify(eventPublisher, never()).publishEvent(any(PermissionsChangedEvent.class));
	}

	@Test
	void deleteById_throwsWhenNotFound() {
		UUID unitId = UUID.randomUUID();
		when(unitRepository.existsById(unitId)).thenReturn(false);

		assertThatThrownBy(() -> service.deleteById(unitId))
				.isInstanceOf(ResourceNotFoundException.class);
	}
}
