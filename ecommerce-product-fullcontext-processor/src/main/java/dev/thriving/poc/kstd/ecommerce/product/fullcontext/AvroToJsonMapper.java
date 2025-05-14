package dev.thriving.poc.kstd.ecommerce.product.fullcontext;

import dev.thriving.poc.ecommerce.avro.ProductFullContext;
import org.apache.kafka.streams.kstream.ValueMapper;

public class AvroToJsonMapper implements ValueMapper<ProductFullContext, String> {
    @Override
    public String apply(ProductFullContext value) {
        return "";
    }
}
