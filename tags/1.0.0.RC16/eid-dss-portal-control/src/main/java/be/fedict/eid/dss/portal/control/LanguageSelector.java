/*
 * eID Digital Signature Service Project.
 * Copyright (C) 2009-2010 FedICT.
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

package be.fedict.eid.dss.portal.control;

import javax.ejb.Local;
import javax.faces.model.SelectItem;
import java.util.List;
import java.util.Locale;

@Local
public interface LanguageSelector {

    String getLocaleString();

    void setLocaleString(java.lang.String localeString);

    List<SelectItem> getSupportedLocales();

    Locale getLocale();

    String dutch();

    String french();

    String english();

    String german();

    boolean isDutchActive();

    boolean isFrenchActive();

    boolean isEnglishActive();

    boolean isGermanActive();

}
