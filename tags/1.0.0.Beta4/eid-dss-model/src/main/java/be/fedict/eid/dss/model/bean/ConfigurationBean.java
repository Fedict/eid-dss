/*
 * eID Digital Signature Service Project.
 * Copyright (C) 2010 FedICT.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License version
 * 3.0 as published by the Free Software Foundation.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, see 
 * http://www.gnu.org/licenses/.
 */

package be.fedict.eid.dss.model.bean;

import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import be.fedict.eid.dss.entity.ConfigPropertyEntity;
import be.fedict.eid.dss.model.ConfigProperty;
import be.fedict.eid.dss.model.Configuration;

@Stateless
public class ConfigurationBean implements Configuration {

	@PersistenceContext
	private EntityManager entityManager;

	public void setValue(ConfigProperty configProperty, Object value) {
		String propertyValue;
		if (null != value) {
			Class<?> expectedType = configProperty.getType();
			Class<?> type = value.getClass();
			if (false == expectedType.isAssignableFrom(type)) {
				throw new IllegalArgumentException("value has incorrect type: "
						+ type.getClass().getName());
			}
			Object castedValue = expectedType.cast(value);
			if (expectedType.isEnum()) {
				Enum<?> enumValue = (Enum<?>) castedValue;
				propertyValue = enumValue.name();
			} else {
				propertyValue = castedValue.toString();
			}
		} else {
			propertyValue = null;
		}
		ConfigPropertyEntity configPropertyEntity = this.entityManager.find(
				ConfigPropertyEntity.class, configProperty.getName());
		if (null == configPropertyEntity) {
			configPropertyEntity = new ConfigPropertyEntity(
					configProperty.getName(), propertyValue);
			this.entityManager.persist(configPropertyEntity);
		} else {
			configPropertyEntity.setValue(propertyValue);
		}
	}

	@SuppressWarnings({ "unchecked", "static-access" })
	public <T> T getValue(ConfigProperty configProperty, Class<T> type) {
		if (false == type.equals(configProperty.getType())) {
			throw new IllegalArgumentException("incorrect type: "
					+ type.getName());
		}
		ConfigPropertyEntity configPropertyEntity = this.entityManager.find(
				ConfigPropertyEntity.class, configProperty.getName());
		if (null == configPropertyEntity) {
			return null;
		}
		String strValue = configPropertyEntity.getValue();
		if (null == strValue) {
			return null;
		}
		if (String.class == configProperty.getType()) {
			return (T) strValue;
		}
		if (Boolean.class == configProperty.getType()) {
			Boolean value = Boolean.parseBoolean(strValue);
			return (T) value;
		}
		if (Integer.class == configProperty.getType()) {
			Integer value = Integer.parseInt(strValue);
			return (T) value;
		}
		if (configProperty.getType().isEnum()) {
			Enum<?> e = (Enum<?>) configProperty.getType().getEnumConstants()[0];
			return (T) e.valueOf(e.getClass(), strValue);
		}
		throw new RuntimeException("unsupported type: "
				+ configProperty.getType().getName());
	}
}
