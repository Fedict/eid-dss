/*
 * eID Digital Signature Service Project.
 * Copyright (C) 2009 FedICT.
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

package be.fedict.eid.dss.spi;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.activation.MimetypesFileTypeMap;
import java.util.List;

/**
 * Maps {@link be.fedict.eid.dss.spi.MimeType}s returned to file extensions.
 * Used for DocumentService's to know whether to display a download or view link
 */
public class MimeTypeMapper {

    private static final Log LOG = LogFactory
            .getLog(MimeTypeMapper.class);

    public static boolean browserViewable(final List<MimeType> mimeTypes, final String fileName) {

        String mimeTypeString = new MimetypesFileTypeMap().getContentType(fileName);
        LOG.debug("File " + fileName + " -> mimeType: " + mimeTypeString);

        if (mimeTypeString.toLowerCase().startsWith("image/")) {
            return true;
        }
        for (MimeType mimeType : mimeTypes) {
            if (mimeType.getType().toLowerCase().startsWith(mimeTypeString.toLowerCase())) {
                return true;
            }
        }

        return false;
    }
}
