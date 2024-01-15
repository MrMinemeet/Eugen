package util

import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.InteractionHook
import net.dv8tion.jda.api.requests.restaction.WebhookMessageCreateAction
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyCallbackAction

// ---- SlashCommandInteractionEvent

/**
 * Sends a reply with the [StatusEmoji.OK] emoji pre-pended.
 * @param content The content of the reply
 * @return The [ReplyCallbackAction] of the reply
 */
fun SlashCommandInteractionEvent.replyOK(content: String): ReplyCallbackAction {
	return reply("${StatusEmoji.OK} $content")
}

/**
 * Sends a reply with the [StatusEmoji.USER_ERROR] emoji pre-pended.
 * @param content The content of the reply
 * @return The [ReplyCallbackAction] of the reply
 */
fun SlashCommandInteractionEvent.replyUserError(content: String): ReplyCallbackAction {
	return reply("${StatusEmoji.USER_ERROR} $content")
}

/**
 * Sends a reply with the [StatusEmoji.BOT_ERROR] emoji pre-pended.
 * @param content The content of the reply
 * @return The [ReplyCallbackAction] of the reply
 */
fun SlashCommandInteractionEvent.replyBotError(content: String): ReplyCallbackAction {
	return reply("${StatusEmoji.BOT_ERROR} $content")
}

// ---- Interaction Hooks

/**
 * Sends a message with the [StatusEmoji.OK] emoji pre-pended.
 * @param content The content of the message
 * @return The [WebhookMessageCreateAction] of the message
 */
fun InteractionHook.sendMessageOK(content: String) : WebhookMessageCreateAction<Message> {
	return sendMessage("${StatusEmoji.OK} $content")
}

/**
 * Sends a message with the [StatusEmoji.USER_ERROR] emoji pre-pended.
 * @param content The content of the message
 * @return The [WebhookMessageCreateAction] of the message
 */
fun InteractionHook.sendMessageUserError(content: String) : WebhookMessageCreateAction<Message> {
	return sendMessage("${StatusEmoji.USER_ERROR} $content")
}

/**
 * Sends a message with the [StatusEmoji.BOT_ERROR] emoji pre-pended.
 * @param content The content of the message
 * @return The [WebhookMessageCreateAction] of the message
 */
fun InteractionHook.sendMessageBotError(content: String) : WebhookMessageCreateAction<Message> {
	return sendMessage("${StatusEmoji.BOT_ERROR} $content")
}