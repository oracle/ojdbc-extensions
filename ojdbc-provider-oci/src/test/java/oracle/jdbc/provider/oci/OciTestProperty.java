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

package oracle.jdbc.provider.oci;

/**
 * Names of properties that configure OCI tests. Descriptions and examples of
 * each property can be found in the "example-test.properties" file within the
 * root directory of the project.
 */
public enum OciTestProperty {
  OCI_CONFIG_FILE,

  OCI_CONFIG_PROFILE,

  OCI_CLOUD_SHELL,

  OCI_INSTANCE_PRINCIPAL,

  OCI_RESOURCE_PRINCIPAL,

  OCI_INTERACTIVE,

  OCI_TOKEN_SCOPE,

  OCI_PASSWORD_OCID,

  OCI_USERNAME_OCID,

  OCI_DATABASE_OCID,

  OCI_OBJECT_STORAGE_URL,

  OCI_OBJECT_STORAGE_URL_MULTIPLE_KEYS,

  OCI_OBJECT_STORAGE_URL_KEY,

  OCI_DB_TOOLS_CONNECTION_OCID_KEYSTORE,

  OCI_DB_TOOLS_CONNECTION_OCID_PKCS12,

  OCI_DB_TOOLS_CONNECTION_OCID_SSO,

  OCI_PASSWORD_PAYLOAD_OCID,
  
  OCI_COMPARTMENT_ID,

  OCI_PKCS12_SEPS_WALLET_OCID,

  OCI_PKCS12_SEPS_WALLET_PASSWORD,

  OCI_SSO_SEPS_WALLET_OCID,

  OCI_SEPS_CONNECTION_STRING_INDEX,

  OCI_CORRUPTED_SEPS_WALLET_OCID,

  OCI_PKCS12_TLS_WALLET_OCID,

  OCI_PKCS12_TLS_WALLET_PASSWORD,

  OCI_PEM_TLS_WALLET_OCID,

  OCI_PEM_TLS_WALLET_PASSWORD,

  OCI_SSO_TLS_WALLET_OCID,

  OCI_CORRUPTED_TLS_WALLET_OCID,

  OCI_PASSWORD_PAYLOAD_OCID_MULTIPLE_KEYS,

  OCI_PASSWORD_PAYLOAD_OCID_KEY,

  OCI_TNS_NAMES_OCID,

  OCI_TNS_NAMES_ALIAS,

  OCI_NON_BASE64_TNS_NAMES_OCID;
}
