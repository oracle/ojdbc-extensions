package oracle.jdbc.provider.aws.configuration;

import oracle.jdbc.driver.OracleConfigurationJsonProvider;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;

import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.sql.SQLException;

public class AwsS3ConfigurationProvider extends OracleConfigurationJsonProvider {

    @Override
    public InputStream getJson(String s3Url) throws SQLException {

        URI uri = null;
        try {
            uri = new URI(s3Url);
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
}