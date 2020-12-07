import org.telegram.telegrambots.ApiContextInitializer;
import org.telegram.telegrambots.TelegramBotsApi;
import org.telegram.telegrambots.api.methods.send.SendMessage;
import org.telegram.telegrambots.api.objects.Chat;
import org.telegram.telegrambots.api.objects.Message;
import org.telegram.telegrambots.api.objects.Update;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.exceptions.TelegramApiException;
import org.telegram.telegrambots.exceptions.TelegramApiRequestException;

import java.util.HashMap;


public class Bot extends TelegramLongPollingBot {
    private HashMap<String, Chat> chats = new HashMap<>();
    private SQLiteManager manager = new SQLiteManager();
    private AdminCommands adminCommands = new AdminCommands(this, manager);
    private ChatCleaner chatCleaner = new ChatCleaner(manager, this);

    public HashMap<String, Chat> getChats() {
        return chats;
    }

    public void setChats(HashMap<String, Chat> chats) {
        this.chats = chats;
    }

    public static void main(String[] args) {
        ApiContextInitializer.init();
        TelegramBotsApi telegramBotsApi = new TelegramBotsApi();
        try {
            Bot bot = new Bot();
            telegramBotsApi.registerBot(bot);
        } catch (TelegramApiRequestException e) {
            e.printStackTrace();
        }
    }


    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasCallbackQuery()) {
            String data = update.getCallbackQuery().getData();
            System.out.println(data);
            if (data.startsWith("chatconfig")) {
                try {
                    adminCommands.setUp(data, update.getCallbackQuery());
                } catch (TelegramApiException e) {
                    e.printStackTrace();
                }
            } else if (data.startsWith("changeConfig")) {
                try {
                    adminCommands.changeConfig(data, update.getCallbackQuery());
                } catch (TelegramApiException e) {
                    e.printStackTrace();
                }
            }
            return;
        }
        if (update.hasMessage()) {
            Message message = update.getMessage();
            if (message.equals("/start")) {
                try {
                    sendMessage(new SendMessage().setChatId(message.getChatId()).
                            setText("Please, call /chats in private message to see chats you can configure"));
                } catch (TelegramApiException e) {
                    e.printStackTrace();
                }
            }
            if (!message.isUserMessage()) manager.addChat(message.getChatId().toString(), message.getChat().getTitle());
            if (message.isUserMessage()) {
                if (message.getText().equals("/chats")) {
                    try {
                        adminCommands.getChats(message);
                    } catch (TelegramApiException e) {
                        e.printStackTrace();
                    }
                } else {
                    try {
                        adminCommands.updateGreet(message);
                    } catch (TelegramApiException e) {
                        e.printStackTrace();
                    }
                }
            }

            if (message.getNewChatMembers() != null) {
                try {
                    chatCleaner.notifyNewChatMembers(message.getChatId().toString(), message.getNewChatMembers());
                } catch (TelegramApiException e) {
                    e.printStackTrace();
                }
            } else {
                chatCleaner.checkIfUnbanned(message);
            }
        }
    }


    @Override
    public String getBotUsername() {
        return "";
    }

    @Override
    public String getBotToken() {
        return "";
    }
}
