package dev.thriving.poc.kstd.ecommerce.product.fullcontext;

import dev.thriving.poc.ecommerce.avro.*;
import io.micronaut.configuration.kafka.streams.ConfiguredStreamBuilder;
import io.micronaut.context.annotation.Factory;
import jakarta.inject.Singleton;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.kstream.*;
import org.apache.kafka.streams.state.Stores;

import java.time.Duration;
import java.util.ArrayList;

@Factory
public class KStreamsTopologyFactory {

    private static final String SOURCE_TOPIC_PRODUCT_DESCRIPTION = "product_description_v1";
    private static final String SOURCE_TOPIC_PRODUCT_ATTRIBUTE = "product_attribute_v3";
    private static final String SOURCE_TOPIC_PRODUCT_VARIANT_DETAIL = "product_variant_detail_v1";
    private static final String SOURCE_TOPIC_PRODUCT_VARIANT_PRICE = "product_variant_price_v2";
    private static final String SOURCE_TOPIC_PRODUCT_VARIANT_STOCK = "product_variant_stock_v2";
    private static final String SINK_TOPIC_PRODUCT = "product_v1";
    private static final String SINK_TOPIC_PRODUCT_JSON = "product_json_v1";

    @Singleton
    KStream<String, ProductFullContext> productFullContextTopology(ConfiguredStreamBuilder builder) {
        // sources
        KTable<String, ProductDescription> descriptionTable = builder.table(
                SOURCE_TOPIC_PRODUCT_DESCRIPTION,
                Consumed.as("description-source"));
        KStream<String, ProductAttribute> attributeStream = builder.stream(
                SOURCE_TOPIC_PRODUCT_ATTRIBUTE,
                Consumed.as("attribute-source"));
        KTable<String, ProductVariantDetail> variantDetailTable = builder.table(
                SOURCE_TOPIC_PRODUCT_VARIANT_DETAIL,
                Consumed.as("variant-detail-source"));
        KTable<String, ProductVariantPrice> variantPriceTable = builder.table(
                SOURCE_TOPIC_PRODUCT_VARIANT_PRICE,
                Consumed.as("variant-price-source"));
        KTable<String, ProductVariantStock> variantStockTable = builder.table(
                SOURCE_TOPIC_PRODUCT_VARIANT_STOCK,
                Consumed.as("variant-stock-source"));

        // topology
        KTable<String, ProductAttributeList> productAttributesStream = attributeStream
                .groupBy((k, v) -> v.getProductSku())
                .aggregate(() -> ProductAttributeList.newBuilder().setAttributes(new ArrayList<>()).build(),
                        (productSku, productAttribute, productAttributes) -> {
                            productAttributes.getAttributes().add(productAttribute);
                            return productAttributes;
                        }
                );

        KTable<String, ProductFullContext> descriptionAndAttributesTable =
                descriptionTable.leftJoin(
                        productAttributesStream,
                        new ProductDescriptionToAttributesJoiner());

        KTable<String, ProductVariantList> variantsTable = variantDetailTable.join(
                        variantPriceTable.join(variantStockTable, (price, stock) ->
                                ProductVariantPriceAndStock.newBuilder()
                                        .setPrice(price)
                                        .setStock(stock)
                                        .build()
                        ),
                        new ProductVariantJoiner())
                .groupBy((k, v) -> KeyValue.pair(v.getProductSku(), v))
                .aggregate(() -> ProductVariantList.newBuilder().setVariants(new ArrayList<>()).build(),
                        (k, v, variants) -> {
                            variants.getVariants().add(v);
                            return variants;
                        }, (k, v, variants) -> {
                            variants.getVariants().remove(v);
                            return variants;
                        }
                );

        KStream<String, ProductFullContext> productFullContextStream =
                descriptionAndAttributesTable.join(variantsTable, new FullContextJoiner())
                        .toStream();

        // sinks
        productFullContextStream
                .to(SINK_TOPIC_PRODUCT, Produced.as("product-sink"));
        productFullContextStream
                .mapValues(new AvroToJsonMapper())
                .to(SINK_TOPIC_PRODUCT_JSON,
                        Produced.<String, String>as("product-json-sink")
                                .withValueSerde(Serdes.String()));

        // (return any KStream for micronaut-kafka)
        return productFullContextStream;
    }

}
