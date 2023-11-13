package eu.merloteducation.organisationsorchestrator.models.gxfscatalog;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class StringTypeValue {
    @JsonProperty("@type")
    private String type;
    @JsonProperty("@value")
    private String value;

    public StringTypeValue(String value) {
        this.value = value;
        this.type = "xsd:string";
    }

    public StringTypeValue(String value, String type) {
        this.value = value;
        this.type = type;
    }
}
