/*
 ** Copyright (c) 2026 Oracle and/or its affiliates.
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
 ** The above copyright notice and either this complete permission notice or
 ** at a minimum a reference to the UPL must be included in all copies or
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

package oracle.jdbc.provider.util.cli;

/**
 * Base class for the {@code main(String[])} entry point of every provider
 * jar in this project (ie: {@code java -jar ojdbc-provider-<name>-
 * <version>.jar}). A bare invocation just prints the module name, version,
 * and a link to its README, then returns without reading from {@code
 * System.in} -- this keeps every provider jar safe to run in a script or CI
 * job, since nothing blocks waiting for input that may never come.
 * <p>
 * {@link ProviderSetupCli} extends this class to additionally offer an
 * interactive setup wizard, unlocked by passing {@value #SETUP_FLAG}.
 * Providers with no interactive wizard (eg: Observability, Pkl, Jackson
 * OSON) extend this class directly instead; passing {@value #SETUP_FLAG}
 * to one of those just explains that no wizard is available, rather than
 * silently ignoring the flag.
 * </p>
 */
public abstract class ProviderJarInfo {

  /**
   * Command-line flag that requests the interactive setup wizard, where
   * one exists.
   */
  protected static final String SETUP_FLAG = "--setup";

  /** Display name printed in the banner, eg: "Oracle JDBC Providers for OCI". */
  protected abstract String displayName();

  /**
   * One-line description printed in the banner, eg: "Providers for
   * integration between Oracle JDBC and Oracle Cloud Infrastructure
   * (OCI)." Should read like the opening sentence of the module's README,
   * in plain text (no Markdown).
   */
  protected abstract String description();

  /** README URL printed in the banner. */
  protected abstract String readmeUrl();

  /**
   * Entry point subclasses call from their own {@code main(String[])}
   * method. Prints the info banner if {@value #SETUP_FLAG} is absent from
   * {@code args}; otherwise calls {@link #onSetupRequested()}.
   *
   * @param args Arguments passed to {@code main(String[])}.
   */
  protected final void start(String[] args) {
    if (containsSetupFlag(args)) {
      onSetupRequested();
    }
    else {
      printInfo();
    }
  }

  /**
   * Called by {@link #start(String[])} when {@value #SETUP_FLAG} is
   * present. The default implementation prints the info banner followed by
   * a note that this module has no interactive setup helper.
   * {@link ProviderSetupCli} overrides this to run its wizard instead (and
   * does not call {@link #printInfo()} itself, since the wizard prints its
   * own banner).
   */
  protected void onSetupRequested() {
    printInfo();
    System.out.println();
    System.out.println(
      "This module has no interactive setup helper. See the README "
        + "above for configuration examples.");
  }

  /**
   * Prints the module name, version, description, and README link, without
   * reading any input. Subclasses that have more to say here (eg: how to
   * unlock a wizard) should override this and call {@code super.printInfo()}
   * first.
   */
  protected void printInfo() {
    System.out.println();
    System.out.println(displayName() + " " + version());
    System.out.println(description());
    System.out.println();
    System.out.println("More information: " + readmeUrl());
  }

  private static boolean containsSetupFlag(String[] args) {
    for (String arg : args) {
      if (SETUP_FLAG.equals(arg)) {
        return true;
      }
    }
    return false;
  }

  /**
   * @return The jar's version from its manifest (eg: "1.1.0"), or
   *   "unknown" when not running from a built jar (eg: from an IDE).
   */
  protected final String version() {
    String version = getClass().getPackage().getImplementationVersion();
    return version != null ? version : "unknown";
  }
}