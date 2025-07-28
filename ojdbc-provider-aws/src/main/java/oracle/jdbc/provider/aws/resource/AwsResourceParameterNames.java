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

package oracle.jdbc.provider.aws.resource;

/**
 * Centralized parameter name constants used by all AWS resource providers.
 */
public final class AwsResourceParameterNames {

  private AwsResourceParameterNames() {}

  // Common parameters
  /**
   *  The AWS region where the resource is stored (e.g., eu-north-1).
   */
  public static final String AWS_REGION = "awsRegion";

  /**
   *  The alias used to retrieve a connection string from tnsnames.ora.
   */
  public static final String TNS_ALIAS = "tnsAlias";

  /**
   *  Optional password used to decrypt the wallet (for PKCS12 or encrypted PEM).
   */
  public static final String WALLET_PASSWORD = "walletPassword";

  /**
   *  Index of the credential set in the wallet
   */
  public static final String CONNECTION_STRING_INDEX = "connectionStringIndex";

  /**
   *  The wallet/file format: SSO, PKCS12, or PEM.
   */
  public static final String TYPE = "type";

  // Parameter Store specific
  /**
   *  The name of the parameter stored in AWS Parameter Store.
   */
  public static final String PARAMETER_NAME = "parameterName";

  // Secrets Manager specific
  /**
   *  The name of the secret stored in AWS Secrets Manager.
   */
  public static final String SECRET_NAME = "secretName";

  /**
   *  Optional field name to extract from a JSON secret.
   */
  public static final String FIELD_NAME = "fieldName";
}