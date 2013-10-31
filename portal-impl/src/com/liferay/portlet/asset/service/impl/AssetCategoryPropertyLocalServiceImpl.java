/**
 * Copyright (c) 2000-2013 Liferay, Inc. All rights reserved.
 *
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2.1 of the License, or (at your option)
 * any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 */

package com.liferay.portlet.asset.service.impl;

import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.kernel.util.CharPool;
import com.liferay.portal.kernel.util.GetterUtil;
import com.liferay.portal.kernel.util.ListUtil;
import com.liferay.portal.kernel.util.StringPool;
import com.liferay.portal.kernel.util.StringUtil;
import com.liferay.portal.kernel.util.Validator;
import com.liferay.portal.model.User;
import com.liferay.portlet.asset.CategoryPropertyKeyException;
import com.liferay.portlet.asset.CategoryPropertyValueException;
import com.liferay.portlet.asset.model.AssetCategoryConstants;
import com.liferay.portlet.asset.model.AssetCategoryProperty;
import com.liferay.portlet.asset.service.base.AssetCategoryPropertyLocalServiceBaseImpl;
import com.liferay.portlet.asset.util.AssetUtil;

import java.util.Date;
import java.util.Iterator;
import java.util.List;

/**
 * @author Brian Wing Shun Chan
 * @author Jorge Ferrer
 */
public class AssetCategoryPropertyLocalServiceImpl
	extends AssetCategoryPropertyLocalServiceBaseImpl {

	@Override
	public AssetCategoryProperty addCategoryProperty(
			long userId, long categoryId, String key, String value)
		throws PortalException, SystemException {

		User user = userPersistence.findByPrimaryKey(userId);
		Date now = new Date();

		validate(key, value);

		long categoryPropertyId = counterLocalService.increment();

		AssetCategoryProperty categoryProperty =
			assetCategoryPropertyPersistence.create(categoryPropertyId);

		categoryProperty.setCompanyId(user.getCompanyId());
		categoryProperty.setUserId(user.getUserId());
		categoryProperty.setUserName(user.getFullName());
		categoryProperty.setCreateDate(now);
		categoryProperty.setModifiedDate(now);
		categoryProperty.setCategoryId(categoryId);
		categoryProperty.setKey(key);
		categoryProperty.setValue(value);

		assetCategoryPropertyPersistence.update(categoryProperty);

		return categoryProperty;
	}

	@Override
	public void deleteCategoryProperties(long entryId) throws SystemException {
		List<AssetCategoryProperty> categoryProperties =
			assetCategoryPropertyPersistence.findByCategoryId(entryId);

		for (AssetCategoryProperty categoryProperty : categoryProperties) {
			deleteCategoryProperty(categoryProperty);
		}
	}

	@Override
	public void deleteCategoryProperty(AssetCategoryProperty categoryProperty)
		throws SystemException {

		assetCategoryPropertyPersistence.remove(categoryProperty);
	}

	@Override
	public void deleteCategoryProperty(long categoryPropertyId)
		throws PortalException, SystemException {

		AssetCategoryProperty categoryProperty =
			assetCategoryPropertyPersistence.findByPrimaryKey(
				categoryPropertyId);

		deleteCategoryProperty(categoryProperty);
	}

	@Override
	public List<AssetCategoryProperty> getCategoryProperties()
		throws SystemException {

		return assetCategoryPropertyPersistence.findAll();
	}

	@Override
	public List<AssetCategoryProperty> getCategoryProperties(long entryId)
		throws SystemException {

		return assetCategoryPropertyPersistence.findByCategoryId(entryId);
	}

	@Override
	public AssetCategoryProperty getCategoryProperty(long categoryPropertyId)
		throws PortalException, SystemException {

		return assetCategoryPropertyPersistence.findByPrimaryKey(
			categoryPropertyId);
	}

	@Override
	public AssetCategoryProperty getCategoryProperty(
			long categoryId, String key)
		throws PortalException, SystemException {

		return assetCategoryPropertyPersistence.findByCA_K(categoryId, key);
	}

	@Override
	public List<AssetCategoryProperty> getCategoryPropertyValues(
			long groupId, String key)
		throws SystemException {

		return assetCategoryPropertyFinder.findByG_K(groupId, key);
	}

	@Override
	public void updateCategoryProperty(
			long userId, long categoryId, String[] categoryProperties)
		throws PortalException, SystemException {

		List<AssetCategoryProperty> oldCategoryProperties =
				assetCategoryPropertyPersistence.findByCategoryId(categoryId);

		oldCategoryProperties = ListUtil.copy(oldCategoryProperties);

		for (int i = 0; i < categoryProperties.length; i++) {
			String[] categoryProperty = StringUtil.split(
				categoryProperties[i],
				AssetCategoryConstants.PROPERTY_KEY_VALUE_SEPARATOR);

			if (categoryProperty.length <= 1) {
				categoryProperty = StringUtil.split(
					categoryProperties[i], CharPool.COLON);
			}

			String key = StringPool.BLANK;

			if (categoryProperty.length > 0) {
				key = GetterUtil.getString(categoryProperty[0]);
			}

			String value = StringPool.BLANK;

			if (categoryProperty.length > 1) {
				value = GetterUtil.getString(categoryProperty[1]);
			}

			validate(key, value);

			if (Validator.isNotNull(key)) {
				boolean addCategoryProperty = true;
				boolean updateCategoryProperty = false;

				AssetCategoryProperty oldCategoryProperty = null;

				Iterator<AssetCategoryProperty> iterator =
					oldCategoryProperties.iterator();

				while (iterator.hasNext()) {
					oldCategoryProperty = iterator.next();

					if ((userId == oldCategoryProperty.getUserId()) &&
						(categoryId == oldCategoryProperty.getCategoryId()) &&
						key.equals(oldCategoryProperty.getKey())) {

						addCategoryProperty = false;

						if (!value.equals(oldCategoryProperty.getValue())) {
							updateCategoryProperty = true;

							oldCategoryProperty.setValue(value);
							oldCategoryProperty.setModifiedDate(new Date());
						}

						iterator.remove();

						break;
					}
				}

				if (addCategoryProperty) {
					addCategoryProperty(userId, categoryId, key, value);
				}
				else if (updateCategoryProperty) {
					updateAssetCategoryProperty(oldCategoryProperty);
				}
			}
		}

		for (AssetCategoryProperty categoryProperty : oldCategoryProperties) {
			deleteAssetCategoryProperty(categoryProperty);
		}
	}

	@Override
	public AssetCategoryProperty updateCategoryProperty(
			long categoryPropertyId, String key, String value)
		throws PortalException, SystemException {

		validate(key, value);

		AssetCategoryProperty categoryProperty =
			assetCategoryPropertyPersistence.findByPrimaryKey(
				categoryPropertyId);

		categoryProperty.setModifiedDate(new Date());
		categoryProperty.setKey(key);
		categoryProperty.setValue(value);

		assetCategoryPropertyPersistence.update(categoryProperty);

		return categoryProperty;
	}

	protected void validate(String key, String value) throws PortalException {
		if (!AssetUtil.isValidWord(key)) {
			throw new CategoryPropertyKeyException();
		}

		if (!AssetUtil.isValidWord(value)) {
			throw new CategoryPropertyValueException();
		}
	}

}