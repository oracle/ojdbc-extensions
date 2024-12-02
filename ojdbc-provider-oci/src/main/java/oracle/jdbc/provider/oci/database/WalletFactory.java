/*
 ** Copyright (c) 2023 Oracle and/or its affiliates.
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

package oracle.jdbc.provider.oci.database;

import com.oracle.bmc.auth.AbstractAuthenticationDetailsProvider;
import com.oracle.bmc.database.DatabaseClient;
import com.oracle.bmc.database.model.GenerateAutonomousDatabaseWalletDetails;
import com.oracle.bmc.database.requests.GenerateAutonomousDatabaseWalletRequest;
import com.oracle.bmc.database.responses.GenerateAutonomousDatabaseWalletResponse;
import oracle.jdbc.provider.cache.CachedResourceFactory;
import oracle.jdbc.provider.factory.ResourceFactory;
import oracle.jdbc.provider.oci.OciResourceFactory;
import oracle.jdbc.provider.parameter.Parameter;
import oracle.jdbc.provider.parameter.ParameterSet;
import oracle.jdbc.provider.factory.Resource;
import oracle.jdbc.provider.util.Wallet;

import java.io.IOException;
import java.io.InputStream;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.zip.ZipInputStream;

/**
 * <p>
 * Factory for requesting wallets from the ADB service. Wallets contain
 * connection strings and key and trust material used by Oracle JDBC to
 * establish a TLS connection with a database.
 * </p>
 */
public final class WalletFactory extends OciResourceFactory<Wallet> {

  /** OCID of a database that a wallet is provided for */
  public static final Parameter<String> OCID = Parameter.create();

  /**
   * Private constructor that should never be called: This class is a singleton.
   */
  private WalletFactory() { }

  /**
   * A cache of wallet configurations requested from the OCI database service.
   */
  private static final ResourceFactory<Wallet> INSTANCE =
    CachedResourceFactory.create(new WalletFactory());

  /**
   * Returns a singleton of {@code WalletFactory}.
   * @return a singleton of {@code WalletFactory}
   */
  public static ResourceFactory<Wallet> getInstance() {
    return INSTANCE;
  }

  /**
   * {@inheritDoc}
   * <p>
   * Requests a wallet for a database identified by an {@link #OCID} parameter
   * in the {@code parameterSet}.
   * </p>
   */
  @Override
  protected Resource<Wallet> request(
      AbstractAuthenticationDetailsProvider authenticationDetails,
      ParameterSet parameterSet) {

    String ocid = parameterSet.getRequired(OCID);

    char[] password = WalletPasswordGenerator.generatePassword();
    try (DatabaseClient databaseClient =
           DatabaseClient.builder().build(authenticationDetails)) {

      GenerateAutonomousDatabaseWalletDetails details =
        GenerateAutonomousDatabaseWalletDetails.builder()
          .password(new String(password))
          .build();

      GenerateAutonomousDatabaseWalletRequest request =
        GenerateAutonomousDatabaseWalletRequest.builder()
          .autonomousDatabaseId(ocid)
          .generateAutonomousDatabaseWalletDetails(details)
          .build();

      GenerateAutonomousDatabaseWalletResponse response =
        databaseClient.generateAutonomousDatabaseWallet(request);

      InputStream responseStream = response.getInputStream();

      final Wallet wallet;
      try (ZipInputStream zipStream = new ZipInputStream(responseStream)) {
        wallet = Wallet.unzip(zipStream, password);
      }
      catch (IOException ioException) {
        throw new IllegalStateException(
          "Failed to close ZIP stream", ioException);
      }

      OffsetDateTime expiry = wallet.getExpirationDate();
      if (expiry == null) {
        // If expiry could not be determined, treat as permanent
        return Resource.createPermanentResource(wallet, false);
      } else {
        return Resource.createExpiringResource(wallet, expiry, false);
      }
    }
    finally {
      Arrays.fill(password, (char)0);
    }
  }

}
