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

package be.fedict.eid.dss.portal.control.bean;

import be.fedict.eid.dss.portal.control.LanguageSelector;
import org.jboss.ejb3.annotation.LocalBinding;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.international.LocaleSelector;

import javax.ejb.Stateless;
import javax.faces.model.SelectItem;
import java.util.List;
import java.util.Locale;

@Stateless
@Name("dssLanguageSelector")
@LocalBinding(jndiBinding = "fedict/eid/dss/portal/LanguageSelectorBean")
public class LanguageSelectorBean implements LanguageSelector {

    @In
    private LocaleSelector localeSelector;

    @Override
    public String getLocaleString() {
        return this.localeSelector.getLocaleString();
    }

    @Override
    public void setLocaleString(String localeString) {
        this.localeSelector.setLocaleString(localeString);
        this.localeSelector.select();
    }

    @Override
    public List<SelectItem> getSupportedLocales() {
        return this.localeSelector.getSupportedLocales();
    }

    @Override
    public Locale getLocale() {
        return this.localeSelector.getLocale();
    }

    @Override
    public String french() {

        this.localeSelector.setLocaleString("fr");
        this.localeSelector.select();
        return null;
    }

    @Override
    public String dutch() {

        this.localeSelector.setLocaleString("nl");
        this.localeSelector.select();
        return null;
    }

    @Override
    public String english() {

        this.localeSelector.setLocaleString("en");
        this.localeSelector.select();
        return null;
    }

    @Override
    public String german() {

        this.localeSelector.setLocaleString("de");
        this.localeSelector.select();
        return null;
    }

    @Override
    public boolean isDutchActive() {

        return this.localeSelector.getLocale().getLanguage().equals("nl");
    }

    @Override
    public boolean isFrenchActive() {
        return this.localeSelector.getLocale().getLanguage().equals("fr");
    }

    @Override
    public boolean isEnglishActive() {
        return this.localeSelector.getLocale().getLanguage().equals("en");
    }

    @Override
    public boolean isGermanActive() {
        return this.localeSelector.getLocale().getLanguage().equals("de");
    }
}
