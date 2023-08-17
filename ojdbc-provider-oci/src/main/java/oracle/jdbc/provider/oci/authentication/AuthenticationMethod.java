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

package oracle.jdbc.provider.oci.authentication;

/**
 * Enumeration of authentication methods supported by OCI.
 */
public enum AuthenticationMethod {

  /**
   * Authentication using credentials specified in an
   * <a href="https://docs.oracle.com/en-us/iaas/Content/API/Concepts/sdkconfig.htm">
   * OCI Configuration File
   * </a>
   */
  CONFIG_FILE,

  /**
   * Authentication in a Compute Instance, without credentials, as an instance
   * principal.
   */
  INSTANCE_PRINCIPAL,

  /**
   * Authentication in a Cloud Shell session using a delegation token
   */
  CLOUD_SHELL,

  /**
   * Authentication in a web browser using credentials typed in by a human
   */
  INTERACTIVE,

  /**
   * Authentication using the most appropriate method for runtime environment.
   * <ol><li>
   * {@link #CLOUD_SHELL}, if running a cloud shell session.
   * </li><li>
   * {@link #INSTANCE_PRINCIPAL}, if running in a compute instance.
   * </li><li>
   * {@link #CONFIG_FILE} otherwise.
   * </li></ol>
   */
  AUTO_DETECT,

  /**
   * Authentication using credentials specified in
   * <a href="https://docs.oracle.com/en-us/iaas/Content/API/Concepts/sdk_authentication_methods.htm#sdk_authentication_methods_api_key">API Keys</a>.
   * This method creates a token using credentials directly from user inputs, if
   * any of them are provided. Otherwise, {@link #CONFIG_FILE} will be chosen.
   */
  API_KEY,

  /**
   * Authentication in a Function, without credentials, as a resource
   * principal
   */
  RESOURCE_PRINCIPAL,
}
