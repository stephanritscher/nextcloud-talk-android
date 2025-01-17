/*
 * Nextcloud Talk application
 *
 * @author Mario Danic
 * Copyright (C) 2017-2018 Mario Danic <mario@lovelyhq.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.nextcloud.talk.adapters.messages

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.text.Spannable
import android.text.SpannableString
import android.util.TypedValue
import android.view.View
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.core.view.ViewCompat
import androidx.emoji.widget.EmojiTextView
import butterknife.BindView
import butterknife.ButterKnife
import com.google.android.flexbox.FlexboxLayout
import com.nextcloud.talk.R
import com.nextcloud.talk.application.NextcloudTalkApplication.Companion.sharedApplication
import com.nextcloud.talk.models.json.chat.ChatMessage
import com.nextcloud.talk.utils.DisplayUtils.getMessageSelector
import com.nextcloud.talk.utils.DisplayUtils.searchAndReplaceWithMentionSpan
import com.nextcloud.talk.utils.TextMatchers
import com.stfalcon.chatkit.messages.MessageHolders.OutcomingTextMessageViewHolder
import org.koin.core.KoinComponent
import org.koin.core.inject
import java.util.*

class MagicOutcomingTextMessageViewHolder(itemView: View) : OutcomingTextMessageViewHolder<ChatMessage>(itemView), KoinComponent {
    @JvmField
    @BindView(R.id.messageText)
    var messageText: EmojiTextView? = null
    @JvmField
    @BindView(R.id.messageTime)
    var messageTimeView: TextView? = null

    @JvmField
    @BindView(R.id.quotedChatMessageView)
    var quotedChatMessageView: RelativeLayout? = null

    @JvmField
    @BindView(R.id.quotedMessageAuthor)
    var quotedUserName: EmojiTextView? = null

    @JvmField
    @BindView(R.id.quotedMessageImage)
    var quotedMessagePreview: ImageView? = null

    @JvmField
    @BindView(R.id.quotedMessage)
    var quotedMessage: EmojiTextView? = null

    @JvmField
    @BindView(R.id.quoteColoredView)
    var quoteColoredView: View? = null

    val context: Context by inject()
    private val realView: View
    override fun onBind(message: ChatMessage) {
        super.onBind(message)
        val messageParameters: HashMap<String, HashMap<String, String>>? = message.messageParameters
        var messageString: Spannable = SpannableString(message.text)
        realView.isSelected = false
        messageTimeView!!.setTextColor(context.resources.getColor(R.color.white60))
        val layoutParams = messageTimeView!!.layoutParams as FlexboxLayout.LayoutParams
        layoutParams.isWrapBefore = false
        var textSize = context.resources.getDimension(R.dimen.chat_text_size)
        if (messageParameters != null && messageParameters.size > 0) {
            for (key in messageParameters.keys) {
                val individualHashMap: HashMap<String, String>? = message.messageParameters!![key]
                if (individualHashMap != null) {
                    if (individualHashMap["type"] == "user" || (individualHashMap["type"]
                                    == "guest") || individualHashMap["type"] == "call") {
                        messageString = searchAndReplaceWithMentionSpan(messageText!!.context,
                                messageString,
                                individualHashMap["id"]!!,
                                individualHashMap["name"]!!,
                                individualHashMap["type"]!!,
                                message.activeUser!!,
                                R.xml.chip_others)
                    } else if (individualHashMap["type"] == "file") {
                        realView.setOnClickListener(View.OnClickListener { v: View? ->
                            val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(individualHashMap["link"]))
                            context.startActivity(browserIntent)
                        })
                    }
                }
            }
        } else if (TextMatchers.isMessageWithSingleEmoticonOnly(message.text)) {
            textSize = (textSize * 2.5).toFloat()
            layoutParams.isWrapBefore = true
            messageTimeView!!.setTextColor(context.resources.getColor(R.color.warm_grey_four))
            realView.isSelected = true
        }
        val resources = sharedApplication!!.resources
        if (message.grouped) {
            val bubbleDrawable = getMessageSelector(
                    resources.getColor(R.color.bg_message_list_outcoming_bubble),
                    resources.getColor(R.color.transparent),
                    resources.getColor(R.color.bg_message_list_outcoming_bubble),
                    R.drawable.shape_grouped_outcoming_message)
            ViewCompat.setBackground(bubble, bubbleDrawable)
        } else {
            val bubbleDrawable = getMessageSelector(
                    resources.getColor(R.color.bg_message_list_outcoming_bubble),
                    resources.getColor(R.color.transparent),
                    resources.getColor(R.color.bg_message_list_outcoming_bubble),
                    R.drawable.shape_outcoming_message)
            ViewCompat.setBackground(bubble, bubbleDrawable)
        }
        messageText!!.setTextSize(TypedValue.COMPLEX_UNIT_PX, textSize)
        messageTimeView!!.layoutParams = layoutParams
        messageText!!.text = messageString

        // parent message handling

        message.parentMessage?.let { parentChatMessage ->
            parentChatMessage.activeUser = message.activeUser
            parentChatMessage.imageUrl?.let {
                quotedMessagePreview?.visibility = View.VISIBLE
                imageLoader.loadImage(quotedMessagePreview, it, null)
            } ?: run {
                quotedMessagePreview?.visibility = View.GONE
            }
            quotedUserName?.text = parentChatMessage.actorDisplayName
                    ?: context.getText(R.string.nc_nick_guest)
            quotedMessage?.text = parentChatMessage.text
            quotedMessage?.setTextColor(context.resources.getColor(R.color.nc_outcoming_text_default))
            quotedUserName?.setTextColor(context.resources.getColor(R.color.nc_grey))

            quoteColoredView?.setBackgroundResource(R.color.white)

            quotedChatMessageView?.visibility = View.VISIBLE
        } ?: run {
            quotedChatMessageView?.visibility = View.GONE
        }

    }

    init {
        ButterKnife.bind(this, itemView)
        this.realView = itemView
    }
}