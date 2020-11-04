package com.tokbox.accelerator.textchat;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import junit.framework.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@RunWith(AndroidJUnit4.class)
public class MessagesAdapterTest extends BaseTest {

    private List<ChatMessage> messagesList;
    private MessagesAdapter messagesAdapter;

    @Test
    public void testGetItemCountWhenMessagesListIsNull()  {
        try {
            messagesAdapter = new MessagesAdapter(null);
            Assert.fail("Should have thrown an exception with null messages list");

        }catch(Exception e){
            Assert.assertNull(messagesAdapter);
        }
    }

    @Test
    public void testGetItemCountWhenMessagesListIsEmpty() throws Exception {

        messagesList = new ArrayList<ChatMessage>();
        messagesAdapter = new MessagesAdapter(messagesList);

        //Item count should be zero
        Assert.assertTrue(messagesAdapter.getItemCount() == 0);

    }

    @Test
     public void testGetItemCountWhenMessagesListIsNotEmpty() throws Exception {
        messagesList = new ArrayList<ChatMessage>();
        messagesList.add(new ChatMessage.ChatMessageBuilder("1",UUID.randomUUID(), ChatMessage.MessageStatus.SENT_MESSAGE).build());

        messagesAdapter = new MessagesAdapter(messagesList);

        //Item count should be greater than zero
        Assert.assertTrue(messagesAdapter.getItemCount() == 1);

    }

    @Test
    public void testGetItemViewTypeWhenIndexIsZero() throws Exception {
        messagesList = new ArrayList<ChatMessage>();
        messagesList.add(new ChatMessage.ChatMessageBuilder("1",UUID.randomUUID(), ChatMessage.MessageStatus.RECEIVED_MESSAGE).build());
        messagesAdapter = new MessagesAdapter(messagesList);

        //Item View should be gotten properly
        Assert.assertNotNull(messagesAdapter.getItemViewType(0));
    }

    @Test
    public void testGetItemViewType_When_IndexIsLast() throws Exception {
        messagesList = new ArrayList<ChatMessage>();
        messagesList.add(new ChatMessage.ChatMessageBuilder("1",UUID.randomUUID(), ChatMessage.MessageStatus.RECEIVED_MESSAGE).build());
        messagesList.add(new ChatMessage.ChatMessageBuilder("2",UUID.randomUUID(), ChatMessage.MessageStatus.SENT_MESSAGE).build());
        messagesList.add(new ChatMessage.ChatMessageBuilder("3",UUID.randomUUID(), ChatMessage.MessageStatus.RECEIVED_MESSAGE).build());
        messagesAdapter = new MessagesAdapter(messagesList);

        //Item View should be gotten properly
        Assert.assertNotNull(messagesAdapter.getItemViewType(2));
    }

}