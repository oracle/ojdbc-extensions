package oracle.jdbc.provider.aws.configuration;

import oracle.jdbc.driver.OracleConfigurationJsonProvider;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;

import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.sql.SQLException;

/**
 * A provider for JSON payload which contains configuration from AWS S3.
 * See {@link #getJson(String)} for the spec of the JSON payload.
 **/
public class AwsS3ConfigurationProvider extends OracleConfigurationJsonProvider {

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
    public InputStream getJson(String s3Url) throws SQLException {

        URI uri = null;
        try {
            uri = getURI(s3Url);
        } catch (URISyntaxException uriSyntaxException) {
            throw new SQLException(uriSyntaxException);
        }

        try (S3Client client = S3Client.builder().build()) {

            String bucketName = uri.getHost();
            String objectKey = uri.getPath()
                    .substring(1);

            GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                    .bucket(bucketName)
                    .key(objectKey)
                    .build();

            return client.getObjectAsBytes(getObjectRequest).asInputStream();
        }
    }

    @Override
    public String getType() {
        return "awss3";
    }

    private URI getURI(String s3Url) throws URISyntaxException {
        if (!s3Url.startsWith("s3://")) {
            s3Url = "s3://" + s3Url;
        }
        return new URI(s3Url);
    }
}