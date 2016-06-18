package br.com.caelum.camel.aula6;

import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.http4.HttpMethods;
import org.apache.camel.impl.DefaultCamelContext;

public class RotaPedidosDeadLetterChannel {

    public static void main(String[] args) throws Exception {

        CamelContext context = new DefaultCamelContext();
        context.addRoutes(new RouteBuilder() {

            @Override
            public void configure() throws Exception {

                errorHandler(deadLetterChannel("file:erro")
                                     .logExhaustedMessageHistory(true)
                                     .useOriginalMessage()
                                     .maximumRedeliveries(3)
                                     .maximumRedeliveryDelay(1000).onRedelivery(exchange -> {
                            int counter = (int) exchange.getIn().getHeader(Exchange.REDELIVERY_COUNTER);
                            int maxCounter = (int) exchange.getIn().getHeader(Exchange.REDELIVERY_MAX_COUNTER);
                            System.out.println("REDELIVERY " + counter + " de " + maxCounter + " !!!!");
                        }));

                from("file:pedidos?delay=5s&noop=true")
                        .routeId("rota-pedidos")
                        .multicast()
                        .to("validator:pedido.xsd");
//                        .to("direct:soap")
//                        .to("direct:http");

                from("direct:http")
                        .routeId("rota-http")
                        .setProperty("pedidoId", xpath("/pedido/id/text()"))
                        .setProperty("clienteId", xpath("/pedido/pagamento/email-titular/text()"))
                        .split()
                        .xpath("/pedido/itens/item")
                        .filter()
                        .xpath("/item/formato[text()='EBOOK']")
                        .setProperty("ebookId", xpath("/item/livro/codigo/text()"))
                        .marshal().xmljson()
                        .log("${id} - ${body}")
                        .setHeader(Exchange.HTTP_METHOD, HttpMethods.GET)
                        .setHeader(Exchange.HTTP_QUERY, simple("ebookId=${property.ebookId}&pedidoId=${property.pedidoId}&clienteId=${property.clienteId}"))
                        .to("http4://localhost:8080/webservices/ebook/item");

                //inserindo a utilização do xslt!!!
                from("direct:soap")
                        .routeId("rota-soap")
                        .to("xslt:pedido-para-soap.xslt")
                        .log("Resultado do Template: ${body}")
                        .setHeader(Exchange.CONTENT_TYPE, constant("text/xml"))
                        .to("http4://localhost:8080/webservices/financeiro");
            }

        });

        context.start();
        Thread.sleep(20000);
        context.stop();
    }
}

