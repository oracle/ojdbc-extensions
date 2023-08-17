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

package oracle.jdbc.provider.azure.authentication;

/**
 * A method of authentication using the Azure SDK.
 */
public enum AzureAuthenticationMethod {

  /** Authentication using {@link com.azure.identity.DefaultAzureCredential} */
  DEFAULT,

  /**
   * Authentication using the most appropriate method detected for the current
   * environment. Managed identity authentication is used if running in an Azure
   * hosted VM. Otherwise, service principle authentication is used.
   */
  AUTO_DETECT,

  /**
   * Authentication as a service principal registered with Azure Active
   * Directory (AD).
   */
  SERVICE_PRINCIPLE,

  /**
   * Authentication with the username and password of an Azure account.
   */
  PASSWORD,

  /**
   * Authentication, without credentials, as a managed identity in a VM hosted
   * by Azure.
   */
  MANAGED_IDENTITY,

  /**
   * Authentication with the username and password of an Azure account, entered
   * into a web browser by a human; The web browser is opened by the JVM.
   */
  INTERACTIVE,

  /**
   * Authentication with the username and password of an Azure account, entered
   * into a web browser by a human; A URL is written to the standard output
   * stream, and a human enters the URL into a web browser.
   */
  DEVICE_CODE
}
