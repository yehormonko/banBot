import org.telegram.telegrambots.api.methods.groupadministration.GetChat;
import org.telegram.telegrambots.api.methods.groupadministration.GetChatAdministrators;
import org.telegram.telegrambots.api.methods.send.SendMessage;
import org.telegram.telegrambots.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.api.objects.CallbackQuery;
import org.telegram.telegrambots.api.objects.ChatMember;
import org.telegram.telegrambots.api.objects.Message;
import org.telegram.telegrambots.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.exceptions.TelegramApiException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class AdminCommands {
    private Bot bot;
    private SQLiteManager sqLiteManager;
    private HashMap<String, String> newGreets = new HashMap<>();

    public AdminCommands(Bot bot, SQLiteManager sqLiteManager) {
        this.bot = bot;
        this.sqLiteManager = sqLiteManager;
    }

    public void getChats(Message message) throws TelegramApiException {
        String result = "Here is list of chats where you are admin: " + System.lineSeparator();
        ArrayList<String> chats = sqLiteManager.getChats();
        InlineKeyboardMarkup chatsMarkup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> chatRows = new ArrayList<>();
        for (String userChat : chats) {
            if (checkIfUserAdmin(userChat, message.getFrom().getId().toString())) {
                InlineKeyboardButton inlineKeyboardButton = new InlineKeyboardButton();
                inlineKeyboardButton.setText(bot.getChat(new GetChat(userChat)).getTitle());
                inlineKeyboardButton.setCallbackData("chatconfig" + " " + userChat);
                List<InlineKeyboardButton> keyboardButtonsRow1 = new ArrayList<>();
                keyboardButtonsRow1.add(inlineKeyboardButton);
                chatRows.add(keyboardButtonsRow1);
            }
        }
        chatsMarkup.setKeyboard(chatRows);
        result += System.lineSeparator() + "Select chat to configure" + System.lineSeparator() +
                "If some values are not filled, bot will not work. Also check if bot is admin in your chat";
        SendMessage sendMessage = new SendMessage().setText(result).setChatId(message.getChat().getId()).setReplyMarkup(chatsMarkup);
        bot.sendMessage(sendMessage);
    }

    public void setUp(String data, CallbackQuery callbackQuery) throws TelegramApiException {
        String[] split = data.split("\\s+");
        String chatId = split[1];
        String userId = callbackQuery.getFrom().getId().toString();
        if (!checkIfUserAdmin(chatId, userId)) return;

        bot.sendMessage(new SendMessage(
                callbackQuery.getMessage().getChat().getId().toString(),
                getChatInfo(chatId))
                .setReplyMarkup(getConfigMarkup(chatId)));
    }

    public void changeConfig(String data, CallbackQuery callbackQuery) throws TelegramApiException {
        String[] split = data.split("\\s+");
        String chatId = split[1];
        String action = split[0];
        String value = split[2];
        String userId = callbackQuery.getFrom().getId().toString();
        Message messageToEdit = callbackQuery.getMessage();
        if (action.equals("changeConfigType")) {
            sqLiteManager.update("value_type", value, chatId);
            bot.editMessageText(new EditMessageText().setText(getChatInfo(chatId))
                    .setMessageId(messageToEdit.getMessageId())
                    .setChatId(messageToEdit.getChatId())
                    .setReplyMarkup(getConfigMarkup(chatId)));
        } else if (action.equals("changeConfigValue")) {
            sqLiteManager.changeValue(value, chatId);
            bot.editMessageText(new EditMessageText().setText(getChatInfo(chatId))
                    .setMessageId(messageToEdit.getMessageId())
                    .setChatId(messageToEdit.getChatId())
                    .setReplyMarkup(getConfigMarkup(chatId)));
        } else if (data.startsWith("changeConfigText")) {
            newGreets.put(callbackQuery.getFrom().getId().toString(), chatId);
            bot.sendMessage(new SendMessage(
                    callbackQuery.getMessage().getChatId(),
                    "Now send the text. Remember that it always start with username"));
        }
    }

    private boolean checkIfUserAdmin(String chatId, String userId) throws TelegramApiException {
        List<ChatMember> chatAdministrators =
                bot.getChatAdministrators(new GetChatAdministrators().setChatId(chatId));
        for (ChatMember chatAdministrator : chatAdministrators) {
            if (chatAdministrator.getUser().getId().toString().equals(userId)) {
                return true;
            }
        }
        return false;
    }

    private InlineKeyboardMarkup getConfigMarkup(String chatId) {
        InlineKeyboardMarkup chatsMarkup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> chatRows = new ArrayList<>();
        InlineKeyboardButton days = new InlineKeyboardButton();
        days.setText("days");
        days.setCallbackData("changeConfigType " + chatId + " day");
        InlineKeyboardButton hours = new InlineKeyboardButton();
        hours.setText("hours");
        hours.setCallbackData("changeConfigType " + chatId + " hours");
        InlineKeyboardButton minutes = new InlineKeyboardButton();
        minutes.setText("minutes");
        minutes.setCallbackData("changeConfigType " + chatId + " minutes");
        List<InlineKeyboardButton> typeRow = new ArrayList<>();
        typeRow.add(days);
        typeRow.add(hours);
        typeRow.add(minutes);
        InlineKeyboardButton valuePlus = new InlineKeyboardButton();
        valuePlus.setText("+");
        valuePlus.setCallbackData("changeConfigValue " + chatId + " valuePlus");
        InlineKeyboardButton valueMinus = new InlineKeyboardButton();
        valueMinus.setText("-");
        valueMinus.setCallbackData("changeConfigValue " + chatId + " valueMinus");
        List<InlineKeyboardButton> valueRow = new ArrayList<>();
        InlineKeyboardButton textValue = new InlineKeyboardButton();
        textValue.setText("change text value");
        textValue.setCallbackData("changeConfigText " + chatId + " textValue");
        List<InlineKeyboardButton> textRow = new ArrayList<>();
        textRow.add(textValue);
        valueRow.add(valuePlus);
        valueRow.add(valueMinus);
        chatRows.add(typeRow);
        chatRows.add(valueRow);
        chatRows.add(textRow);
        chatsMarkup.setKeyboard(chatRows);
        return chatsMarkup;
    }

    public void updateGreet(Message message) throws TelegramApiException {
        String from = message.getFrom().getId().toString();
        if (!newGreets.containsKey(from)) return;
        String chatId = newGreets.get(from);
        if (!checkIfUserAdmin(chatId, from)) return;
        String text = message.getText();
        sqLiteManager.update("greet", text, chatId);
        bot.sendMessage(new SendMessage(
                message.getChatId(),
                getChatInfo(chatId))
                .setReplyMarkup(getConfigMarkup(chatId)));
    }

    private String getChatInfo(String chatId) {
        ChatInfo chatInfo = sqLiteManager.getChatInfo(chatId);
        String info = chatInfo.getChat_title() + System.lineSeparator() +
                "ban after: " + chatInfo.getValue() + " " + chatInfo.getValue_type() + System.lineSeparator() +
                "message: @username " + chatInfo.getGreet() + System.lineSeparator() +
                "[Please, remember that greetings will start from username]";
        return info;
    }

    public Bot getBot() {
        return bot;
    }

    public void setBot(Bot bot) {
        this.bot = bot;
    }
}
