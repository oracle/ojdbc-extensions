/*
 *  Copyright (c) 2026 Oracle and/or its affiliates.
 *
 *  The Universal Permissive License (UPL), Version 1.0
 *
 *  Subject to the condition set forth below, permission is hereby granted to any
 *  person obtaining a copy of this software, associated documentation and/or data
 *  (collectively the "Software"), free of charge and under any and all copyright
 *  rights in the Software, and any and all patent rights owned or freely
 *  licensable by each licensor hereunder covering either (i) the unmodified
 *  Software as contributed to or provided by such licensor, or (ii) the Larger
 *  Works (as defined below), to deal in both
 *
 *  (a) the Software, and
 *  (b) any piece of software and/or hardware listed in the lrgrwrks.txt file if
 *  one is included with the Software (each a "Larger Work" to which the Software
 *  is contributed by such licensors),
 *
 *  without restriction, including without limitation the rights to copy, create
 *  derivative works of, display, perform, and distribute the Software and make,
 *  use, sell, offer for sale, import, export, have made, and have sold the
 *  Software and the Larger Work(s), and to sublicense the foregoing rights on
 *  either these or other terms.
 *
 *  This license is subject to the following condition:
 *  The above copyright notice and either this complete permission notice or at
 *  a minimum a reference to the UPL must be included in all copies or
 *  substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 *  SOFTWARE.
 */

package oracle.jdbc.provider.oauth;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.awt.*;
import java.io.IOException;
import java.net.BindException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * <p>
 * A locally running server for use in interactive authentication methods and
 * authorization protocols.
 * </p><p>
 * This server is primarily intended to receive requests from a web browser that
 * runs on the current device. After a user logs in to an account in their web
 * browser, the authorization server redirects the web browser to this server.
 * The protocol flows like this:
 * </p><pre>{@code
 *   [web browser] //////login//////// [Authorization Server]
 *   [web browser] \\\\\\redirect\\\\\ [Authorization Server]
 *   [web browser] //////result/////// [RedirectServer]
 * }</pre><p>
 * An OAuth 2.0 authorization code is an example of a result that can be obtained
 * in this protocol.
 * </p><p>
 * All usages of this class should involve at least three steps:
 * <ol><li>
 * Construct an instance in a try-with-resource block
 * </li><li>
 * Call {@link #setResultHandler(String, ResultHandler)} to define how the
 * redirect is processed into a result.
 * </li><li>
 * Call {@link #awaitResult(URI, int, TimeUnit)} to wait for the result.
 * </li></ol><p>
 * Usage examples can be found in the ojdbc-provider-oci and
 * ojdbc-provider-nimbus modules.
 * </p>
 *
 * @param <T> Class of the result
 */
public final class RedirectServer<T> implements AutoCloseable {

  /**
   * Local HTTP server that accepts a redirect from an authorization server.
   */
  private final HttpServer server;

  /**
   * Future that completes with the result of the redirect.
   */
  private final CompletableFuture<T> resultFuture;

  /**
   * Starts a server that accepts a redirect at the given URI.
   *
   * @param redirectURI URI where an authorization server will redirect the
   * web browser back to. A local HTTP server will be started at this URI.
   * Not null.
   */
  public RedirectServer(URI redirectURI) {
    this.resultFuture = new CompletableFuture<>();
    this.server = createServer(redirectURI);
    server.start();
  }

  /**
   * Adds a request handler that does not return an authorization result, but
   * may perform some intermediate step or other operation. Usage of this method
   * is optional.
   *
   * @param path Path, relative to the redirect URI, where a request is
   * received. Not null.
   *
   * @param handler Handles the response and returns a result. Not null.
   */
  public void setHandler(String path, HttpHandler handler) {
    server.createContext(path, exchange -> {
      try {
        handler.handle(exchange);
      }
      catch (Exception exception) {
        resultFuture.completeExceptionally(exception);
      }
      finally {
        exchange.close();
      }
    });
  }

  /**
   * Adds a request handler that returns an authorization result. This method
   * must be called before calling {@link #awaitResult(URI, int, TimeUnit)}.
   *
   * @param path Path, relative to the redirect URI, where a request is
   * received. Not null.
   *
   * @param handler Handles the response and returns a result. Not null.
   */
  public void setResultHandler(String path, ResultHandler<T> handler) {
    server.createContext(path, exchange -> {
      try {
        resultFuture.complete(handler.handle(exchange));
      }
      catch (Exception exception) {
        resultFuture.completeExceptionally(exception);
      }
      finally {
        exchange.close();
      }
    });
  }

  /**
   * Displays the authorization URI (ideally in a web browser), and awaits a
   * request.
   *
   * @param authorizationURI URI of the authorization server to visit in a web
   * browser. Not null.
   *
   * @param timeout Amount of time to wait for a request. 0 or greater.
   *
   * @param timeoutUnit Unit of the timeout. Not null.
   *
   * @return The result of the response. May be null if the handler returns
   * null.
   */
  public T awaitResult(
    URI authorizationURI,
    int timeout,
    TimeUnit timeoutUnit
  ) {
    displayURI(authorizationURI);

    try {
      return resultFuture.get(timeout, timeoutUnit);
    }
    catch (TimeoutException timeoutException) {
      throw new IllegalStateException(
        "Authorization timed out after "
          + timeout + " " + timeoutUnit.name().toLowerCase()
          + ". The browser login was not completed in time."
          + " Port " + server.getAddress().getPort() + " has been released.",
        timeoutException);
    }
    catch (InterruptedException interruptedException) {
      throw new IllegalStateException(
        "Authorization interrupted", interruptedException);
    }
    catch (ExecutionException executionException) {
      throw new IllegalStateException(executionException);
    }
  }

  @Override
  public void close() {
    server.stop(0);
  }

  /**
   *
   * @param redirectURI
   * @return
   */
  private static HttpServer createServer(URI redirectURI) {
    try {
      return HttpServer.create(
        new InetSocketAddress(redirectURI.getHost(), redirectURI.getPort()),
        0);
    }
    catch (BindException bindException) {
      // Port is already held — most likely by a previous interactive
      // authentication session whose browser login was never completed.
      // The previous server will release the port automatically once its
      // login timeout expires (default: 5 minutes).
      throw new IllegalStateException(
        "Interactive authentication failed: port "
          + redirectURI.getPort() + " is already in use."
          + " A previous interactive authentication session may still be"
          + " running. Please complete the login in your browser, or wait"
          + " for the previous session to time out and then try again.",
        bindException);
    }
    catch (IOException ioException) {
      throw new IllegalStateException(
        "Failed to create an HTTP server", ioException);
    }
  }

  /**
   * Displays a URI, ideally in a web browser, but falling back to stdout if
   * necessary. The fallback is designed for command line tools, such as
   * SQLcl, that block {@link Desktop#browse(URI)} with the
   * java.awt.headless=true system property.
   *
   * @param authorizationURI URI of an authorization endpoint to. Not null.
   */
  private static void displayURI(URI authorizationURI) {
    try {
      Desktop.getDesktop().browse(authorizationURI);
    }
    catch (Exception exception) {
      System.out.println(
        "Unable to open web browser due to: " + exception);
      System.out.println(
        "Visit the URL below to authorize access:\n" + authorizationURI);
    }
  }

  public interface ResultHandler<T> {
    T handle(HttpExchange exchange) throws IOException;
  }
}
