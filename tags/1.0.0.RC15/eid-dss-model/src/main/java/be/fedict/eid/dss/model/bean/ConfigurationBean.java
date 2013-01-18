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

import be.fedict.eid.dss.entity.ConfigPropertyEntity;
import be.fedict.eid.dss.model.ConfigProperty;
import be.fedict.eid.dss.model.Configuration;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.annotation.PostConstruct;
import javax.ejb.Startup;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.util.LinkedList;
import java.util.List;

@Startup
@Stateless
public class ConfigurationBean implements Configuration {

	private static final Log LOG = LogFactory.getLog(ConfigurationBean.class);

	@PersistenceContext
	private EntityManager entityManager;

	@PostConstruct
	public void init() {

		initProperties();
	}

	private void initProperties() {

		for (ConfigProperty configProperty : ConfigProperty.values()) {

			// init defaults if necessary
			if (null == getValue(configProperty, configProperty.getType())
					&& null != configProperty.getDefaultValue()) {

				LOG.debug("Initialize " + configProperty.getName() + " with "
						+ "default value=" + configProperty.getDefaultValue());
				setValue(configProperty, configProperty.getDefaultValue());
			}
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public void setValue(ConfigProperty configProperty, Object value) {

		setValue(configProperty, null, value);
	}

	/**
	 * {@inheritDoc}
	 */
	public void setValue(ConfigProperty configProperty, String index,
			Object value) {

		String propertyValue;
		if (null != value) {
			Class<?> expectedType = configProperty.getType();
			Class<?> type = value.getClass();
			if (!expectedType.isAssignableFrom(type)) {
				throw new IllegalArgumentException("value has incorrect type: "
						+ type.getClass().getName());
			}
			Object castedValue = expectedType.cast(value);
			if (expectedType.isEnum()) {
				Enum<?> enumValue = (Enum<?>) castedValue;
				propertyValue = enumValue.name();
			} else {
				propertyValue = castedValue.toString();
				if (propertyValue.trim().isEmpty()) {
					propertyValue = null;
				}
			}
		} else {
			propertyValue = null;
		}

		String propertyName = getPropertyName(configProperty, index);
		ConfigPropertyEntity configPropertyEntity = this.entityManager.find(
				ConfigPropertyEntity.class, propertyName);
		if (null == configPropertyEntity) {
			configPropertyEntity = new ConfigPropertyEntity(propertyName,
					propertyValue);
			this.entityManager.persist(configPropertyEntity);
		} else {
			configPropertyEntity.setValue(propertyValue);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public void removeValue(ConfigProperty configProperty) {

		removeValue(configProperty, null);
	}

	/**
	 * {@inheritDoc}
	 */
	public void removeValue(ConfigProperty configProperty, String index) {

		String propertyName = getPropertyName(configProperty, index);
		ConfigPropertyEntity configPropertyEntity = this.entityManager.find(
				ConfigPropertyEntity.class, propertyName);
		if (null != configPropertyEntity) {
			this.entityManager.remove(configPropertyEntity);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public <T> T getValue(ConfigProperty configProperty, Class<T> type) {
		return getValue(configProperty, null, type);
	}

	/**
	 * {@inheritDoc}
	 */
	@SuppressWarnings({ "unchecked" })
	public <T> T getValue(ConfigProperty configProperty, String index,
			Class<T> type) {

		if (!type.equals(configProperty.getType())) {
			throw new IllegalArgumentException("incorrect type: "
					+ type.getName());
		}

		String propertyName = getPropertyName(configProperty, index);

		ConfigPropertyEntity configPropertyEntity = this.entityManager.find(
				ConfigPropertyEntity.class, propertyName);
		if (null == configPropertyEntity) {
            if (Boolean.class == configProperty.getType()) {
                return (T) Boolean.FALSE;
            } else {
                return null;
            }
		}
		String value = configPropertyEntity.getValue();
		if (null == value || value.trim().length() == 0) {
            if (Boolean.class == configProperty.getType()) {
                return (T) Boolean.FALSE;
            } else {
                return null;
            }
        }

		if (String.class == configProperty.getType()) {
			return (T) value;
		}
		if (Boolean.class == configProperty.getType()) {
			Boolean booleanValue = Boolean.parseBoolean(value);
			return (T) booleanValue;
		}
		if (Integer.class == configProperty.getType()) {
			Integer integerValue = Integer.parseInt(value);
			return (T) integerValue;
		}
		if (Long.class == configProperty.getType()) {
			Long longValue = Long.parseLong(value);
			return (T) longValue;
		}
		if (configProperty.getType().isEnum()) {
			Enum<?> e = (Enum<?>) configProperty.getType().getEnumConstants()[0];
			return (T) Enum.valueOf(e.getClass(), value);
		}
		throw new RuntimeException("unsupported type: "
				+ configProperty.getType().getName());
	}

	/**
	 * {@inheritDoc}
	 */
	public List<String> getIndexes(ConfigProperty configProperty) {

		List<ConfigPropertyEntity> configs = ConfigPropertyEntity
				.listConfigsWhereNameLike(this.entityManager,
						configProperty.getName());
		List<String> indexes = new LinkedList<String>();

		String prefix = configProperty.getName() + '-';
		for (ConfigPropertyEntity config : configs) {
			if (config.getName().contains(prefix)) {
				indexes.add(config.getName().substring(
						config.getName().indexOf(prefix) + prefix.length()));
			}
		}
		return indexes;
	}

	private String getPropertyName(ConfigProperty configProperty, String index) {

		String propertyName = configProperty.getName();
		if (null != index) {
			propertyName += '-' + index;
		}
		return propertyName;
	}
}
