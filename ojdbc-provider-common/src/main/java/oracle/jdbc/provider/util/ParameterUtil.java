package oracle.jdbc.provider.util;

import oracle.jdbc.provider.parameter.Parameter;
import oracle.jdbc.provider.parameter.ParameterSet;

import java.util.Optional;

/**
 * Utility class for handling parameters and environment variables.
 */
public final class ParameterUtil {

  private ParameterUtil() {
    // Prevent instantiation
  }

  /**
   * Fetches a value from system properties or environment variables.
   *
   * @param key the name of the property or environment variable
   * @return the value of the property or environment variable, or null if not found
   */
  public static String getEnvOrProperty(String key) {
    return System.getProperty(key, System.getenv(key));
  }

  /**
   * Fetches a parameter from the ParameterSet or falls back to
   * environment/system properties.
   *
   * @param parameterSet the ParameterSet to search for the parameter
   * @param parameter    the Parameter to fetch from the ParameterSet
   * @param envKey       the environment/system property key to use as fallback
   * @return the parameter value
   * @throws IllegalStateException if neither the parameter nor
   * the environment/system property is found
   */
  public static String getRequiredOrFallback(
          ParameterSet parameterSet, Parameter<String> parameter, String envKey) {
    String value = Optional.ofNullable(parameterSet.getOptional(parameter))
                           .orElse(getEnvOrProperty(envKey));

    if (value == null || value.isEmpty()) {
      throw new IllegalStateException(
              "Required configuration '" + envKey + "' not found in ParameterSet, system properties, or environment variables.");
    }
    return value;
  }

  /**
   * Fetches a parameter from the ParameterSet or falls back to
   * environment/system properties. Returns null if the value is not found.
   *
   * @param parameterSet the ParameterSet to search for the parameter
   * @param parameter the Parameter to fetch from the ParameterSet
   * @param envKey the environment/system property key to use as fallback
   * @return the parameter value or null if not found
   */
  public static String getOptionalOrFallback(
          ParameterSet parameterSet, Parameter<String> parameter, String envKey) {
    return Optional.ofNullable(parameterSet.getOptional(parameter))
            .orElse(getEnvOrProperty(envKey));
  }

}
