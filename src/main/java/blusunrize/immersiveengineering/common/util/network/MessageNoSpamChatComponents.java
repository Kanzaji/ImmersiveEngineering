package blusunrize.immersiveengineering.common.util.network;

import blusunrize.immersiveengineering.common.util.ChatUtils;
import io.netty.buffer.ByteBuf;
import net.minecraft.util.text.ITextComponent;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class MessageNoSpamChatComponents implements IMessage
{
	ITextComponent[] chatMessages;
	public MessageNoSpamChatComponents(ITextComponent... chatMessages)
	{
		this.chatMessages = chatMessages;
	}
	public MessageNoSpamChatComponents()
	{
	}

	@Override
	public void fromBytes(ByteBuf buf)
	{
		int l = buf.readInt();
		chatMessages = new ITextComponent[l];
		for(int i=0; i<l; i++)
			chatMessages[i] = ITextComponent.Serializer.jsonToComponent(ByteBufUtils.readUTF8String(buf));
	}

	@Override
	public void toBytes(ByteBuf buf)
	{
		buf.writeInt(chatMessages.length);
		for(ITextComponent component : chatMessages)
			ByteBufUtils.writeUTF8String(buf, ITextComponent.Serializer.componentToJson(component));
	}

	public static class Handler implements IMessageHandler<MessageNoSpamChatComponents, IMessage>
	{
		@Override
		public IMessage onMessage(MessageNoSpamChatComponents message, MessageContext ctx)
		{
			ChatUtils.sendClientNoSpamMessages(message.chatMessages);
			return null;
		}
	}
}