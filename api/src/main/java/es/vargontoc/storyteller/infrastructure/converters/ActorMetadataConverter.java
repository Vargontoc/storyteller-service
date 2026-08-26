package es.vargontoc.storyteller.infrastructure.converters;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import es.vargontoc.storyteller.domain.model.ActorMetadata;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply =  false)
public class ActorMetadataConverter implements AttributeConverter<ActorMetadata, String> {

    public static final ObjectMapper mapper = new ObjectMapper();

    @Override
    public String convertToDatabaseColumn(ActorMetadata attribute) {
        if(attribute == null) return null;
        try {
            return mapper.writeValueAsString(attribute);
        }catch(JsonProcessingException e){
            throw new IllegalArgumentException("Error converting entity to json", e);
        }
    }

    @Override
    public ActorMetadata convertToEntityAttribute(String dbData) {
        if(dbData == null || dbData.isBlank()) return null;
        try {
            return mapper.readValue(dbData, ActorMetadata.class);
        }catch(IOException e) {
            throw new IllegalArgumentException("Error converting entity to json", e);
        }
    }
    
}
