<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN"
        "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html>
    <head>
        <title>eID DSS - Test Service Provider (SP)</title>
    </head>
    <body>
        <h1>eID DSS - Test Service Provider (SP)</h1>

        <h2>Sign using HTTP Post Binding</h2>

        <form enctype="multipart/form-data" action="upload" method="post">

            <table border="1">
                <tr>
                    <td>
                        <b>Choose the file To Upload:</b>
                    </td>
                    <td>
                        <input name="upload" type="file"/>
                    </td>
                    <td align="right">
                        <input type="submit" value="Sign"/>
                    </td>
                </tr>
            </table>

        </form>

        <h2>Sign using Artifact Binding</h2>

        <form enctype="multipart/form-data" action="uploadartifact"
              method="post">

            <table border="1">
                <tr>
                    <td>
                        <b>Choose the file To Upload:</b>
                    </td>
                    <td>
                        <input name="upload" type="file"/>
                    </td>
                    <td align="right">
                        <input type="submit" value="Sign"/>
                    </td>
                </tr>
            </table>

        </form>

    </body>
</html>