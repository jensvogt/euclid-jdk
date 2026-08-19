package de.jensvogt.euclid;

import de.jensvogt.euclid.dto.sqs.ReceiveMessagesResponse;
import de.jensvogt.euclid.dto.sqs.model.Message;
import de.jensvogt.euclid.dto.sqs.model.Variant;
import de.jensvogt.euclid.module.access.EuclidSession;
import de.jensvogt.euclid.module.sqs.EuclidSqs;

import java.util.Map;

//TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.
public class Main {
    static void main() throws Exception {

        EuclidSession euclid = Euclid.forServer("https://localhost:5566").access().credentials("jvo", "Tea4TheTillerman").login();
        EuclidSqs sqs = euclid.sqs();

        String queueErn = sqs.createQueue("test-queue3").ern();
        System.out.println(queueErn);


        for (int i = 0; i < 100000; i++) {
            Map<String, Variant> attributes = Map.of(
                    "testattr1", new Variant("string", "testvalue"),
                    "testattr2", new Variant("int", i),
                    "testattr3", new Variant("bool", true));
            sqs.sendMessage(queueErn, "{\"testattr\":\"testvalue\"}", attributes);
            ReceiveMessagesResponse response = sqs.receiveAllMessages(queueErn);
            for (Message message : response.messages()) {
                sqs.deleteMessage(message.receiptHandle());
                System.out.println(message.attributes().get("testattr2").value());
            }
        }
    }
}
