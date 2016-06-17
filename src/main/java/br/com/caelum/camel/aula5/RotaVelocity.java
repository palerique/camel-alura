package br.com.caelum.camel.aula5;

import org.apache.camel.CamelContext;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.impl.DefaultCamelContext;

public class RotaVelocity {

    public static void main(String[] args) throws Exception {

        CamelContext context = new DefaultCamelContext();

        context.addRoutes(new RouteBuilder() {

            @Override
            public void configure() throws Exception {

                from("direct:entrada")
                        .setHeader("data", constant("8/12/2015"))
                        .to("velocity:template.vm")
                        .log("${body}");

            }

        });

        context.start();

        ProducerTemplate producer = context.createProducerTemplate();
        producer.sendBody("direct:entrada", "Apache Camel rocks!!!");

        Thread.sleep(20000);
        context.stop();
    }
}

