package org.ironsight.wpplugin.macromachine.REST.DTOs;

import static org.ironsight.wpplugin.macromachine.operations.ValueProviders.IPositionValueSetter.IGNORE_VALUE;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Arrays;
import java.util.Objects;
import org.ironsight.wpplugin.macromachine.operations.ProviderType;
import org.ironsight.wpplugin.macromachine.operations.ValueProviders.IPositionValueGetter;
import org.ironsight.wpplugin.macromachine.operations.ValueProviders.IPositionValueSetter;

@Schema(description = "Describes an input/output provider configuration")
public class InputOutputDTO {

  @Schema(
      description = "Human readable macro name",
      example = "Terrain Height",
      requiredMode = Schema.RequiredMode.REQUIRED)
  private final String displayName;

  @Schema(
      description = "Detailed description of the provider",
      example = "Controls the generated terrain elevation",
      requiredMode = Schema.RequiredMode.REQUIRED)
  private final String description;

  @Schema(
      description = "Minimum supported value",
      example = "0",
      requiredMode = Schema.RequiredMode.REQUIRED)
  private final int min;

  @Schema(
      description = "Maximum supported value",
      example = "255",
      requiredMode = Schema.RequiredMode.REQUIRED)
  private final int max;

  @Schema(
      description = "Value that should be ignored or treated as unset",
      example = "-1",
      requiredMode = Schema.RequiredMode.REQUIRED)
  private final int ignoreValue;

  @ArraySchema(
      schema =
          @Schema(
              description = "Display names for discrete values",
              example = "Blue",
              requiredMode = Schema.RequiredMode.REQUIRED))
  private final String[] valueDisplayNames;

  @Schema(
      description =
          "Whether the values are discrete instead of continuous (colors are discrete, forest strength % is continuous)",
      example = "true",
      requiredMode = Schema.RequiredMode.REQUIRED)
  private final boolean discrete;

  @Schema(
      description = "Provider type",
      implementation = ProviderType.class,
      requiredMode = Schema.RequiredMode.REQUIRED)
  private final ProviderType type;

  @JsonCreator
  public InputOutputDTO(
      @JsonProperty("displayName") String displayName,
      @JsonProperty("description") String description,
      @JsonProperty("min") int min,
      @JsonProperty("max") int max,
      @JsonProperty("ignoreValue") int ignoreValue,
      @JsonProperty("valueDisplayNames") String[] valueDisplayNames,
      @JsonProperty("discrete") boolean discrete,
      @JsonProperty("type") ProviderType type) {
    this.displayName = displayName;
    this.description = description;
    this.min = min;
    this.max = max;
    this.ignoreValue = ignoreValue;
    this.valueDisplayNames = valueDisplayNames;
    this.discrete = discrete;
    this.type = type;
  }

  public static InputOutputDTO fromOutputSetter(IPositionValueSetter setter) {
    return new InputOutputDTO(
        setter.getName(),
        setter.getDescription(),
        setter.getMinValue(),
        setter.getMaxValue(),
        IPositionValueSetter.getIgnoreValue(setter),
        Arrays.stream(setter.getAllOutputValues())
            .filter(i -> !setter.isIgnoreValue(i))
            .mapToObj(setter::valueToString)
            .toArray(String[]::new),
        setter.isDiscrete(),
        setter.getProviderType());
  }

  public static InputOutputDTO fromInputGetter(IPositionValueGetter getter) {
    return new InputOutputDTO(
        getter.getName(),
        getter.getDescription(),
        getter.getMinValue(),
        getter.getMaxValue(),
        (getter instanceof IPositionValueSetter setter)
            ? IPositionValueSetter.getIgnoreValue(setter)
            : IGNORE_VALUE,
        Arrays.stream(getter.getAllInputValues())
            .mapToObj(getter::valueToString)
            .toArray(String[]::new),
        getter.isDiscrete(),
        getter.getProviderType());
  }

  public int getMin() {
    return min;
  }

  public int getMax() {
    return max;
  }

  public int getIgnoreValue() {
    return ignoreValue;
  }

  public String[] getValueDisplayNames() {
    return valueDisplayNames;
  }

  public boolean isDiscrete() {
    return discrete;
  }

  public ProviderType getType() {
    return type;
  }

  public IPositionValueGetter toGetter() {
    var io = ProviderType.fromTypeDefault(type); // FIXME with params
    if (io instanceof IPositionValueGetter getter) return getter;
    else throw new IllegalArgumentException("this provider type is not a getter:" + type);
  }

  public IPositionValueSetter toSetter() {
    var io = ProviderType.fromTypeDefault(type); // FIXME with params
    if (io instanceof IPositionValueSetter setter) return setter;
    else throw new IllegalArgumentException("this provider type is not a setter:" + type);
  }

  public String getDescription() {
    return description;
  }

  public String getDisplayName() {
    return displayName;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    InputOutputDTO that = (InputOutputDTO) o;
    return getMin() == that.getMin()
        && getMax() == that.getMax()
        && getIgnoreValue() == that.getIgnoreValue()
        && isDiscrete() == that.isDiscrete()
        && Objects.equals(getDisplayName(), that.getDisplayName())
        && Objects.equals(getDescription(), that.getDescription())
        && Arrays.equals(getValueDisplayNames(), that.getValueDisplayNames())
        && getType() == that.getType();
  }

  @Override
  public int hashCode() {
    int result =
        Objects.hash(
            getDisplayName(),
            getDescription(),
            getMin(),
            getMax(),
            getIgnoreValue(),
            isDiscrete(),
            getType());
    result = 31 * result + Arrays.hashCode(getValueDisplayNames());
    return result;
  }

  @Override
  public String toString() {
    return "InputOutputDTO{"
        + "displayName='"
        + displayName
        + '\''
        + ", description='"
        + description
        + '\''
        + ", min="
        + min
        + ", max="
        + max
        + ", ignoreValue="
        + ignoreValue
        + ", valueDisplayNames="
        + Arrays.toString(valueDisplayNames)
        + ", discrete="
        + discrete
        + ", type="
        + type
        + '}';
  }
}
