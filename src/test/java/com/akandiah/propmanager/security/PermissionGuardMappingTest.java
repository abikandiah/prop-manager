package com.akandiah.propmanager.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

import org.junit.jupiter.api.Test;

import com.akandiah.propmanager.common.permission.Actions;
import com.akandiah.propmanager.common.permission.PermissionDomains;
import com.akandiah.propmanager.common.permission.ResourceType;

class PermissionGuardMappingTest {

	@Test
	void actionMap_shouldContainAllActionsFromActionsClass() throws IllegalAccessException {
		Field[] fields = Actions.class.getDeclaredFields();
		for (Field field : fields) {
			int modifiers = field.getModifiers();
			if (Modifier.isPublic(modifiers) && Modifier.isStatic(modifiers) &&
					Modifier.isFinal(modifiers) && field.getType() == int.class) {
				String actionName = field.getName();
				int actionValue = field.getInt(null);
				assertThat(PermissionGuard.ACTION_MAP)
						.withFailMessage("PermissionGuard.ACTION_MAP is missing action: %s", actionName)
						.containsKey(actionName);
				assertThat(PermissionGuard.ACTION_MAP.get(actionName))
						.withFailMessage("PermissionGuard.ACTION_MAP value for %s does not match Actions.%s", actionName,
								actionName)
						.isEqualTo(actionValue);
			}
		}
	}

	@Test
	void domainMap_shouldContainAllDomainsFromPermissionDomainsClass() throws IllegalAccessException {
		Field[] fields = PermissionDomains.class.getDeclaredFields();
		for (Field field : fields) {
			int modifiers = field.getModifiers();
			if (Modifier.isPublic(modifiers) && Modifier.isStatic(modifiers) &&
					Modifier.isFinal(modifiers) && field.getType() == String.class &&
					!field.getName().equals("VALID_KEYS")) {
				String domainName = field.getName();
				String domainValue = (String) field.get(null);

				// ORGANIZATION is mapped to 'ORG' in our map for brevity
				String mapKey = domainName.equals("ORGANIZATION") ? "ORG" : domainName;

				assertThat(PermissionGuard.DOMAIN_MAP)
						.withFailMessage("PermissionGuard.DOMAIN_MAP is missing domain: %s (mapped as %s)", domainName, mapKey)
						.containsKey(mapKey);
				assertThat(PermissionGuard.DOMAIN_MAP.get(mapKey))
						.withFailMessage("PermissionGuard.DOMAIN_MAP value for %s does not match PermissionDomains.%s", mapKey,
								domainName)
						.isEqualTo(domainValue);
			}
		}
	}

	@Test
	void resourceType_valueOf_shouldWorkForAllEnumConstants() {
		for (ResourceType type : ResourceType.values()) {
			assertThat(ResourceType.valueOf(type.name())).isEqualTo(type);
		}
	}
}