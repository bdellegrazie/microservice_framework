package uk.gov.justice.services.test.utils.core.messaging;

import static javax.jms.Session.AUTO_ACKNOWLEDGE;
import static uk.gov.justice.services.test.utils.core.enveloper.EnvelopeFactory.createEnvelope;

import uk.gov.justice.services.messaging.JsonEnvelope;

import javax.jms.Connection;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.MessageProducer;
import javax.jms.Session;
import javax.jms.TextMessage;
import javax.json.JsonObject;

import org.apache.activemq.artemis.jms.client.ActiveMQConnectionFactory;

public class MessageProducerClient implements AutoCloseable {

    private static final String QUEUE_URI = System.getProperty("queueUri", "tcp://localhost:61616");

    private Session session;
    private MessageProducer messageProducer;

    public void startProducer(final String topicName) {

        try {
            final ActiveMQConnectionFactory factory = new ActiveMQConnectionFactory(QUEUE_URI);
            final Connection connection = factory.createConnection();

            connection.start();

            session = connection.createSession(false, AUTO_ACKNOWLEDGE);
            final Destination destination = session.createTopic(topicName);
            messageProducer = session.createProducer(destination);
        } catch (JMSException e) {
            close();
            throw new RuntimeException("Failed to create message producer to topic '" + topicName + "'", e);
        }
    }

    public void sendMessage(final String commandName, final JsonObject payload) {

        if (messageProducer == null) {
            close();
            throw new RuntimeException("Message producer not started. Please call startProducer(...) first.");
        }

        final JsonEnvelope jsonEnvelope = createEnvelope(commandName, payload);
        final String json = jsonEnvelope.toDebugStringPrettyPrint();

        try {
            final TextMessage message = session.createTextMessage();

            message.setText(json);
            message.setStringProperty("CPPNAME", commandName);

            messageProducer.send(message);
        } catch (JMSException e) {
            throw new RuntimeException("Failed to send message. commandName: '" + commandName + "', json: " + json, e);
        }
    }

    @Override
    public void close() {
        close(messageProducer);
        close(session);

        session = null;
        messageProducer = null;
    }

    private void close(final AutoCloseable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (Exception ignored) {
            }
        }
    }
}
