package org.ironsight.wpplugin.macromachine.operations.FileIO;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.pepsoft.worldpainter.layers.Layer;

import java.io.*;
import java.util.Arrays;
import java.util.Base64;
import java.util.Objects;

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class ExportContainer implements Serializable {

    static class LayerArrayJavaDeserializer extends JsonDeserializer<Layer[]> {
        @Override
        public Layer[] deserialize(JsonParser p, DeserializationContext ctxt) {
            try {
                byte[] data = Base64.getDecoder().decode(p.getText());
                ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(data));
                return (Layer[]) ois.readObject();
            } catch (Exception e) {
                throw new RuntimeException("Failed to deserialize Layer[]", e);
            }
        }
    }
    static class LayerArrayJavaSerializer extends JsonSerializer<Layer[]> {
        @Override
        public void serialize(Layer[] layers, JsonGenerator gen, SerializerProvider serializers) {
            try {
                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                ObjectOutputStream oos = new ObjectOutputStream(bos);
                oos.writeObject(layers);
                oos.flush();
                oos.close();

                String base64 = Base64.getEncoder().encodeToString(bos.toByteArray());
                gen.writeString(base64);
            } catch (Exception e) {
                throw new RuntimeException("Failed to serialize Layer[]", e);
            }
        }
    }
    private final String exportDate;
    private final String comment;
    private final MacroJsonWrapper[] macros;
    private final ActionJsonWrapper[] actions;
    private final Layer[] layers;

    public Layer[] getLayers() {
        return layers;
    }

    @JsonCreator
    public ExportContainer(
            @JsonProperty("exportDate") String exportDate,
            @JsonProperty("comment") String comment,
            @JsonProperty("macros") MacroJsonWrapper[] macros,
            @JsonProperty("actions") ActionJsonWrapper[] actions,
            @JsonSerialize(using = LayerArrayJavaSerializer.class)
            @JsonDeserialize(using =  LayerArrayJavaDeserializer.class)
            @JsonProperty("layers") Layer[] layers ) {
        this.exportDate = exportDate;
        this.comment = comment;
        this.macros = macros;
        this.actions = actions;
        this.layers = layers == null ? new Layer[0] : layers;
    }

    public String getExportDate() {
        return exportDate;
    }

    public String getComment() {
        return comment;
    }

    public MacroJsonWrapper[] getMacros() {
        return macros;
    }

    public ActionJsonWrapper[] getActions() {
        return actions;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ExportContainer container = (ExportContainer) o;
        return Objects.equals(getExportDate(), container.getExportDate()) &&
                Objects.equals(getComment(), container.getComment()) &&
                Arrays.equals(getMacros(), container.getMacros()) &&
                Arrays.equals(getActions(), container.getActions());
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(getExportDate(), getComment());
        result = 31 * result + Arrays.hashCode(getMacros());
        result = 31 * result + Arrays.hashCode(getActions());
        return result;
    }
}
