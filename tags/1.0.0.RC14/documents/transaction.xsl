<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:tns="urn:be:fedict:eid:dss:example">

    <xsl:param name="language">en</xsl:param>

    <xsl:template match="/tns:Transaction">

        <xsl:variable name="from">
            <xsl:choose>
                <xsl:when test="$language = 'nl'">
                    Van
                </xsl:when>
                <xsl:otherwise>
                    From
                </xsl:otherwise>
            </xsl:choose>
        </xsl:variable>

        <html>
            <body>
                <h1>Transaction</h1>

                <table>
                    <tr>
                        <th>
                            <xsl:copy-of select="$from" />
                            :
                        </th>
                        <td>
                            <xsl:value-of select="tns:From" />
                        </td>
                    </tr>
                    <tr>
                        <th>To:</th>
                        <td>
                            <xsl:value-of select="tns:To" />
                        </td>
                    </tr>
                    <tr>
                        <th>Amount:</th>
                        <td>
                            <xsl:value-of select="tns:Amount" />
                        </td>
                    </tr>

                    <xsl:for-each select="tns:Description">
                        <tr>
                            <th>Description:</th>
                            <td>
                                <xsl:value-of select="." />
                            </td>
                        </tr>
                    </xsl:for-each>
                </table>
            </body>
        </html>

    </xsl:template>

</xsl:stylesheet>
