package com.safetypin.post.serializers;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import org.locationtech.jts.geom.Point;

import java.io.IOException;

public class PointSerializer extends JsonSerializer<Point> {
    @Override
    public void serialize(Point point, com.fasterxml.jackson.core.JsonGenerator jsonGenerator, SerializerProvider provider) throws IOException, IOException {
        jsonGenerator.writeStartObject();
        jsonGenerator.writeNumberField("latitude", point.getY());
        jsonGenerator.writeNumberField("longitude", point.getX());
        jsonGenerator.writeEndObject();
    }
}