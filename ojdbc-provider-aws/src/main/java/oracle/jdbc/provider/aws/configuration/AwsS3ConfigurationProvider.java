/*
 ** Copyright (c) 2025 Oracle and/or its affiliates.
 **
 ** The Universal Permissive License (UPL), Version 1.0
 **
 ** Subject to the condition set forth below, permission is hereby granted to any
 ** person obtaining a copy of this software, associated documentation and/or data
 ** (collectively the "Software"), free of charge and under any and all copyright
 ** rights in the Software, and any and all patent rights owned or freely
 ** licensable by each licensor hereunder covering either (i) the unmodified
 ** Software as contributed to or provided by such licensor, or (ii) the Larger
 ** Works (as defined below), to deal in both
 **
 ** (a) the Software, and
 ** (b) any piece of software and/or hardware listed in the lrgrwrks.txt file if
 ** one is included with the Software (each a "Larger Work" to which the Software
 ** is contributed by such licensors),
 **
 ** without restriction, including without limitation the rights to copy, create
 ** derivative works of, display, perform, and distribute the Software and make,
 ** use, sell, offer for sale, import, export, have made, and have sold the
 ** Software and the Larger Work(s), and to sublicense the foregoing rights on
 ** either these or other terms.
 **
 ** This license is subject to the following condition:
 ** The above copyright notice and either this complete permission notice or at
 ** a minimum a reference to the UPL must be included in all copies or
 ** substantial portions of the Software.
 **
 ** THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 ** IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 ** FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 ** AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 ** LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 ** OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 ** SOFTWARE.
 */
package oracle.jdbc.provider.aws.configuration;

import oracle.jdbc.driver.OracleConfigurationJsonProvider;
import oracle.jdbc.provider.aws.s3.S3Factory;
import oracle.jdbc.provider.parameter.ParameterSet;
import oracle.jdbc.provider.parameter.ParameterSetParser;
import oracle.jdbc.util.OracleConfigurationCache;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import static oracle.jdbc.provider.aws.configuration.AwsConfigurationParameters.REGION;
import static oracle.jdbc.provider.aws.s3.S3Factory.S3_URL;

/**
 * A provider for JSON payload which contains configuration from AWS S3.
 * See {@link #getJson(String)} for the spec of the JSON payload.
 **/
public class AwsS3ConfigurationProvider extends OracleConfigurationJsonProvider {
    private static final ParameterSetParser PARAMETER_SET_PARSER =
        AwsConfigurationParameters.configureBuilder(
                ParameterSetParser.builder()
                    .addParameter("s3_url", S3_URL, "")
                    .addParameter("key", AwsConfigurationParameters.KEY)
                    .addParameter("AWS_REGION", REGION))
            .build();

    /**
     * {@inheritDoc}
     * <p>
     * Returns the JSON payload stored in AWS S3.
     * </p>
     *
     * @param s3Url URI of the object stored in AWS S3
     * @return JSON payload
     */
    @Override
    public InputStream getJson(String s3Url) {
        // Appends "s3://" as the URL prefix if it is not already present
        if (!s3Url.startsWith("s3://")) {
            s3Url = "s3://" + s3Url;
        }

        // Add objectUrl to the "options" Map, so it can be parsed.
        Map<String, String> optionsWithUrl = new HashMap<>(options);
        optionsWithUrl.put("s3_url", s3Url);

        ParameterSet parameters =
            PARAMETER_SET_PARSER.parseNamedValues(optionsWithUrl);

        return S3Factory.getInstance()
            .request(parameters)
            .getContent();
    }

    @Override
    public String getType() {
        return "awss3";
    }

    /**
     * {@inheritDoc}
     * @return cache of this provider which is used to store configuration
     */
    @Override
    public OracleConfigurationCache getCache() {
        return CACHE;
    }
}